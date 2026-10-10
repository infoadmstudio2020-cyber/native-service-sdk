package com.roni.library.service;

import com.roni.library.contracts.api.IApiCallback;
import com.roni.library.contracts.api.NativeApiError;
import com.roni.library.contracts.api.NativeApiRequest;
import com.roni.library.contracts.api.NativeApiResponse;
import com.roni.library.contracts.service.IService;
import com.roni.library.service.builtins.BatteryStatusService;
import com.roni.library.service.builtins.DeviceInfoService;
import com.roni.library.service.builtins.NetworkStateService;
import com.roni.library.service.handler.ServiceCommandHandler;
import com.roni.library.service.lifecycle.ServiceState;
import com.roni.library.service.provider.SingletonServiceProvider;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class NativeServiceTest {
    private static int passed = 0;
    private static int failed = 0;

    private static void assertTrue(String name, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + name);
            passed++;
        } else {
            System.err.println("  [FAIL] " + name);
            failed++;
        }
    }

    private static void assertEquals(String name, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            System.out.println("  [PASS] " + name);
            passed++;
        } else {
            System.err.println("  [FAIL] " + name + " - Expected: " + expected + ", Actual: " + actual);
            failed++;
        }
    }

    static class ServiceA extends BaseService {
        private final ServiceRegistry registry;
        ServiceA(ServiceRegistry registry) { super("ServiceA"); this.registry = registry; }
        @Override protected void onInitialize() {
            registry.getService(ServiceB.class);
        }
    }

    static class ServiceB extends BaseService {
        private final ServiceRegistry registry;
        ServiceB(ServiceRegistry registry) { super("ServiceB"); this.registry = registry; }
        @Override protected void onInitialize() {
            registry.getService(ServiceA.class);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("NativeServiceSDK (Phase 03) Execution Tests");
        System.out.println("=================================================");

        // 1. Test BaseService Lifecycle Transitions
        AtomicInteger transitions = new AtomicInteger(0);
        BaseService customService = new BaseService("CustomTestService", 50) {};
        customService.addLifecycleListener((svc, oldState, newState) -> transitions.incrementAndGet());

        assertEquals("Initial state is CREATED", ServiceState.CREATED, customService.getState());
        customService.initialize();
        assertEquals("State after initialize is INITIALIZED", ServiceState.INITIALIZED, customService.getState());
        assertTrue("Service is available when initialized", customService.isAvailable());

        customService.start();
        assertEquals("State after start is RUNNING", ServiceState.RUNNING, customService.getState());
        assertTrue("Service is running", customService.isRunning());
        assertTrue("Service is available when running", customService.isAvailable());

        customService.stop();
        assertEquals("State after stop is STOPPED", ServiceState.STOPPED, customService.getState());
        customService.dispose();
        assertEquals("State after dispose is DESTROYED", ServiceState.DESTROYED, customService.getState());
        assertTrue("Transitions count >= 5", transitions.get() >= 5);

        // 2. Test ServiceRegistry and Priority Ordering
        ServiceRegistry registry = new ServiceRegistry();
        DeviceInfoService devService = new DeviceInfoService();     // Priority 10
        NetworkStateService netService = new NetworkStateService(); // Priority 15
        BatteryStatusService batService = new BatteryStatusService(); // Priority 20

        registry.registerService(DeviceInfoService.class, devService);
        registry.registerService(NetworkStateService.class, netService);
        registry.registerService(BatteryStatusService.class, batService);

        List<IService> all = registry.getAllServices();
        assertEquals("Total registered services", 3, all.size());
        assertEquals("First priority is 10 (DeviceInfoService)", 10, ((BaseService) all.get(0)).getPriority());
        assertEquals("Second priority is 15 (NetworkStateService)", 15, ((BaseService) all.get(1)).getPriority());
        assertEquals("Third priority is 20 (BatteryStatusService)", 20, ((BaseService) all.get(2)).getPriority());

        // 3. Test Lifecycle Orchestration
        registry.initializeAll();
        registry.startAll();
        assertTrue("DeviceInfoService is running", devService.isRunning());
        assertTrue("BatteryStatusService is running", batService.isRunning());
        assertTrue("NetworkStateService is running", netService.isRunning());

        // 4. Test Service Lookups
        IService byName = registry.getServiceByName("deviceinfoservice");
        assertTrue("Lookup by name case-insensitive", byName != null);
        assertEquals("Lookup matches instance", devService, byName);

        // 5. Test Lazy Singleton Provider
        AtomicInteger factoryInvocations = new AtomicInteger(0);
        SingletonServiceProvider<DeviceInfoService> lazyProvider = SingletonServiceProvider.lazy(() -> {
            factoryInvocations.incrementAndGet();
            return new DeviceInfoService();
        });
        assertEquals("Lazy provider not invoked before get()", 0, factoryInvocations.get());
        DeviceInfoService s1 = lazyProvider.get();
        DeviceInfoService s2 = lazyProvider.get();
        assertEquals("Singleton provider returns same instance", s1, s2);
        assertEquals("Factory called exactly once", 1, factoryInvocations.get());

        // 6. Test Circular Dependency Detection
        ServiceRegistry circRegistry = new ServiceRegistry();
        circRegistry.registerProvider(ServiceA.class, () -> {
            ServiceA a = new ServiceA(circRegistry);
            a.initialize();
            return a;
        });
        circRegistry.registerProvider(ServiceB.class, () -> {
            ServiceB b = new ServiceB(circRegistry);
            b.initialize();
            return b;
        });

        boolean caughtCircular = false;
        try {
            circRegistry.getService(ServiceA.class);
        } catch (Throwable e) {
            Throwable cur = e;
            while (cur != null) {
                if (cur instanceof IllegalStateException && cur.getMessage() != null && cur.getMessage().contains("Circular dependency detected")) {
                    caughtCircular = true;
                    break;
                }
                cur = cur.getCause();
            }
        }
        assertTrue("Circular dependency detected and blocked", caughtCircular);

        // 7. Test Built-in Services Output
        assertTrue("DeviceInfoDTO has model", devService.getDeviceInfo().getModel() != null);
        assertTrue("BatteryStatusDTO has level", batService.getBatteryStatus().getLevel() >= 0);
        assertTrue("NetworkStateService is connected", netService.isConnected());

        // 8. Test ServiceCommandHandler API Route Dispatching
        ServiceCommandHandler handler = new ServiceCommandHandler(registry);
        assertEquals("ServiceCommandHandler route prefix", "service/", handler.getRoutePrefix());

        // Test action: list
        NativeApiRequest listReq = new NativeApiRequest("req_s1", "service/list", "list", Collections.emptyMap(), System.currentTimeMillis());
        CountDownLatch latch1 = new CountDownLatch(1);
        AtomicReference<NativeApiResponse> resp1 = new AtomicReference<>();
        handler.handle(listReq, new IApiCallback() {
            @Override public void onSuccess(NativeApiResponse res) { resp1.set(res); latch1.countDown(); }
            @Override public void onError(NativeApiError err) { latch1.countDown(); }
        });
        assertTrue("Handler finished list callback", latch1.await(1, TimeUnit.SECONDS));
        assertTrue("Handler list response success", resp1.get() != null && resp1.get().isSuccess());
        assertTrue("Response has services list", resp1.get().getData().containsKey("services"));

        // Test action: getDeviceInfo
        NativeApiRequest devReq = new NativeApiRequest("req_s2", "service/getDeviceInfo", "getDeviceInfo", Collections.emptyMap(), System.currentTimeMillis());
        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicReference<NativeApiResponse> resp2 = new AtomicReference<>();
        handler.handle(devReq, new IApiCallback() {
            @Override public void onSuccess(NativeApiResponse res) { resp2.set(res); latch2.countDown(); }
            @Override public void onError(NativeApiError err) { latch2.countDown(); }
        });
        assertTrue("Handler finished getDeviceInfo callback", latch2.await(1, TimeUnit.SECONDS));
        assertTrue("Handler getDeviceInfo response success", resp2.get() != null && resp2.get().isSuccess());
        assertTrue("Response has model key", resp2.get().getData().containsKey("model"));

        // Cleanup
        registry.dispose();
        assertTrue("Registry is disposed", registry.isDisposed());

        System.out.println("=================================================");
        System.out.println("Service Results: " + passed + " passed, " + failed + " failed.");
        System.out.println("=================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
