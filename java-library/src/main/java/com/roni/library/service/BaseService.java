package com.roni.library.service;

import com.roni.library.contracts.service.IService;
import com.roni.library.service.lifecycle.ServiceLifecycleListener;
import com.roni.library.service.lifecycle.ServiceState;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Robust abstract base service implementing state transitions and lifecycle callbacks.
 */
public abstract class BaseService implements IService {
    private final String serviceId;
    private final int priority;
    private volatile ServiceState state = ServiceState.CREATED;
    private volatile boolean disposed = false;
    private final List<ServiceLifecycleListener> lifecycleListeners = new CopyOnWriteArrayList<>();

    protected BaseService(String serviceId, int priority) {
        this.serviceId = serviceId != null ? serviceId : getClass().getSimpleName();
        this.priority = priority;
    }

    protected BaseService(String serviceId) {
        this(serviceId, 100);
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public boolean isAvailable() {
        return (state == ServiceState.RUNNING || state == ServiceState.INITIALIZED) && !disposed;
    }

    public ServiceState getState() {
        return state;
    }

    public boolean isRunning() {
        return state == ServiceState.RUNNING;
    }

    public void addLifecycleListener(ServiceLifecycleListener listener) {
        if (listener != null && !lifecycleListeners.contains(listener)) {
            lifecycleListeners.add(listener);
        }
    }

    public void removeLifecycleListener(ServiceLifecycleListener listener) {
        if (listener != null) {
            lifecycleListeners.remove(listener);
        }
    }

    protected synchronized void transitionTo(ServiceState newState) {
        if (this.state == newState) {
            return;
        }
        ServiceState oldState = this.state;
        this.state = newState;
        for (ServiceLifecycleListener listener : lifecycleListeners) {
            try {
                listener.onStateChanged(this, oldState, newState);
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public synchronized void initialize() {
        if (state != ServiceState.CREATED && state != ServiceState.STOPPED) {
            return;
        }
        transitionTo(ServiceState.INITIALIZING);
        try {
            onInitialize();
            transitionTo(ServiceState.INITIALIZED);
        } catch (Throwable t) {
            transitionTo(ServiceState.ERROR);
            throw new RuntimeException("Failed to initialize service " + serviceId, t);
        }
    }

    public synchronized void start() {
        if (state == ServiceState.RUNNING) {
            return;
        }
        if (state != ServiceState.INITIALIZED && state != ServiceState.STOPPED) {
            initialize();
        }
        transitionTo(ServiceState.STARTING);
        try {
            onStart();
            transitionTo(ServiceState.RUNNING);
        } catch (Throwable t) {
            transitionTo(ServiceState.ERROR);
            throw new RuntimeException("Failed to start service " + serviceId, t);
        }
    }

    public synchronized void stop() {
        if (state != ServiceState.RUNNING) {
            return;
        }
        transitionTo(ServiceState.STOPPING);
        try {
            onStop();
            transitionTo(ServiceState.STOPPED);
        } catch (Throwable t) {
            transitionTo(ServiceState.ERROR);
            throw new RuntimeException("Failed to stop service " + serviceId, t);
        }
    }

    @Override
    public synchronized void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        if (state == ServiceState.RUNNING) {
            try {
                stop();
            } catch (Throwable ignored) {}
        }
        try {
            onDestroy();
        } catch (Throwable ignored) {}
        transitionTo(ServiceState.DESTROYED);
        lifecycleListeners.clear();
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    protected void onInitialize() {}
    protected void onStart() {}
    protected void onStop() {}
    protected void onDestroy() {}
}
