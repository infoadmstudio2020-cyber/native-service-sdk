package com.roni.library.service;

import com.roni.library.contracts.service.IService;
import com.roni.library.contracts.service.IServiceRegistry;
import com.roni.library.service.lifecycle.ServiceLifecycleListener;
import com.roni.library.service.provider.IServiceProvider;
import com.roni.library.service.provider.SingletonServiceProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe centralized implementation of IServiceRegistry.
 */
public class ServiceRegistry implements IServiceRegistry {
    private static volatile ServiceRegistry instance;
    private static final Object INSTANCE_LOCK = new Object();

    private final Map<Class<? extends IService>, IServiceProvider<? extends IService>> classProviders = new ConcurrentHashMap<>();
    private final Map<String, IServiceProvider<? extends IService>> nameProviders = new ConcurrentHashMap<>();
    private final List<ServiceLifecycleListener> globalListeners = new CopyOnWriteArrayList<>();
    private final ThreadLocal<Set<Class<?>>> resolutionChain = ThreadLocal.withInitial(HashSet::new);
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);

    public ServiceRegistry() {}

    public static ServiceRegistry getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new ServiceRegistry();
                }
            }
        }
        return instance;
    }

    public static void setInstance(ServiceRegistry registry) {
        synchronized (INSTANCE_LOCK) {
            if (instance != null && instance != registry) {
                instance.dispose();
            }
            instance = registry;
        }
    }

    public void addGlobalLifecycleListener(ServiceLifecycleListener listener) {
        if (listener != null && !globalListeners.contains(listener)) {
            globalListeners.add(listener);
        }
    }

    public void removeGlobalLifecycleListener(ServiceLifecycleListener listener) {
        if (listener != null) {
            globalListeners.remove(listener);
        }
    }

    @Override
    public <T extends IService> void registerService(Class<T> serviceClass, T serviceInstance) {
        if (serviceClass == null || serviceInstance == null) {
            throw new IllegalArgumentException("Service class and instance cannot be null");
        }
        registerProvider(serviceClass, SingletonServiceProvider.of(serviceInstance));
    }

    public <T extends IService> void registerProvider(Class<T> serviceClass, IServiceProvider<T> provider) {
        if (serviceClass == null || provider == null) {
            throw new IllegalArgumentException("Service class and provider cannot be null");
        }
        classProviders.put(serviceClass, provider);
    }

    public void registerProviderByName(String name, IServiceProvider<? extends IService> provider) {
        if (name == null || provider == null) {
            throw new IllegalArgumentException("Name and provider cannot be null");
        }
        nameProviders.put(name.toLowerCase(Locale.US), provider);
    }

    @Override
    public <T extends IService> boolean unregisterService(Class<T> serviceClass) {
        if (serviceClass == null) return false;
        IServiceProvider<? extends IService> provider = classProviders.remove(serviceClass);
        if (provider != null) {
            try {
                IService svc = provider.get();
                if (svc != null) {
                    svc.dispose();
                    nameProviders.remove(svc.getServiceId().toLowerCase(Locale.US));
                }
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IService> T getService(Class<T> serviceClass) {
        if (serviceClass == null || isDisposed.get()) {
            return null;
        }

        Set<Class<?>> chain = resolutionChain.get();
        if (chain.contains(serviceClass)) {
            throw new IllegalStateException("Circular dependency detected resolving service: " + serviceClass.getName());
        }

        IServiceProvider<? extends IService> provider = classProviders.get(serviceClass);
        if (provider == null) {
            for (Map.Entry<Class<? extends IService>, IServiceProvider<? extends IService>> entry : classProviders.entrySet()) {
                if (serviceClass.isAssignableFrom(entry.getKey())) {
                    provider = entry.getValue();
                    break;
                }
            }
        }

        if (provider == null) {
            return null;
        }

        chain.add(serviceClass);
        try {
            T resolved = (T) provider.get();
            attachLifecycleBroadcaster(resolved);
            if (resolved != null) {
                nameProviders.put(resolved.getServiceId().toLowerCase(Locale.US), provider);
            }
            return resolved;
        } finally {
            chain.remove(serviceClass);
        }
    }

    public IService getServiceByName(String name) {
        if (name == null || isDisposed.get()) {
            return null;
        }
        IServiceProvider<? extends IService> provider = nameProviders.get(name.toLowerCase(Locale.US));
        if (provider != null) {
            IService svc = provider.get();
            attachLifecycleBroadcaster(svc);
            return svc;
        }

        for (IService svc : getAllServices()) {
            if (svc.getServiceId().equalsIgnoreCase(name)) {
                return svc;
            }
        }
        return null;
    }

    @Override
    public boolean hasService(Class<? extends IService> serviceClass) {
        return classProviders.containsKey(serviceClass);
    }

    public List<IService> getAllServices() {
        List<IService> services = new ArrayList<>();
        for (IServiceProvider<? extends IService> provider : classProviders.values()) {
            try {
                IService svc = provider.get();
                if (svc != null && !services.contains(svc)) {
                    services.add(svc);
                    attachLifecycleBroadcaster(svc);
                }
            } catch (Throwable ignored) {}
        }
        services.sort((a, b) -> {
            int p1 = (a instanceof BaseService) ? ((BaseService) a).getPriority() : 100;
            int p2 = (b instanceof BaseService) ? ((BaseService) b).getPriority() : 100;
            return Integer.compare(p1, p2);
        });
        return Collections.unmodifiableList(services);
    }

    private void attachLifecycleBroadcaster(IService service) {
        if (service instanceof BaseService) {
            BaseService base = (BaseService) service;
            base.addLifecycleListener((svc, oldState, newState) -> {
                for (ServiceLifecycleListener global : globalListeners) {
                    try {
                        global.onStateChanged(svc, oldState, newState);
                    } catch (Throwable ignored) {}
                }
            });
        }
    }

    public void initializeAll() {
        List<IService> services = getAllServices();
        for (IService svc : services) {
            try {
                svc.initialize();
            } catch (Throwable t) {
                System.err.println("Failed initializing " + svc.getServiceId() + ": " + t.getMessage());
            }
        }
    }

    public void startAll() {
        List<IService> services = getAllServices();
        for (IService svc : services) {
            try {
                if (svc instanceof BaseService) {
                    ((BaseService) svc).start();
                }
            } catch (Throwable t) {
                System.err.println("Failed starting " + svc.getServiceId() + ": " + t.getMessage());
            }
        }
    }

    public void stopAll() {
        List<IService> services = getAllServices();
        List<IService> reversed = new ArrayList<>(services);
        Collections.reverse(reversed);
        for (IService svc : reversed) {
            try {
                if (svc instanceof BaseService) {
                    ((BaseService) svc).stop();
                }
            } catch (Throwable t) {
                System.err.println("Failed stopping " + svc.getServiceId() + ": " + t.getMessage());
            }
        }
    }

    public void dispose() {
        if (isDisposed.compareAndSet(false, true)) {
            stopAll();
            for (IService svc : getAllServices()) {
                try {
                    svc.dispose();
                } catch (Throwable ignored) {}
            }
            classProviders.clear();
            nameProviders.clear();
            globalListeners.clear();
        }
    }

    public boolean isDisposed() {
        return isDisposed.get();
    }
}
