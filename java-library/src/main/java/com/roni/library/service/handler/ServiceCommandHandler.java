package com.roni.library.service.handler;

import com.roni.library.contracts.api.IApiCallback;
import com.roni.library.contracts.api.IApiHandler;
import com.roni.library.contracts.api.NativeApiError;
import com.roni.library.contracts.api.NativeApiRequest;
import com.roni.library.contracts.api.NativeApiResponse;
import com.roni.library.contracts.platform.BatteryStatusDTO;
import com.roni.library.contracts.platform.DeviceInfoDTO;
import com.roni.library.contracts.service.IService;
import com.roni.library.service.BaseService;
import com.roni.library.service.ServiceRegistry;
import com.roni.library.service.builtins.BatteryStatusService;
import com.roni.library.service.builtins.DeviceInfoService;
import com.roni.library.service.builtins.NetworkStateService;

import java.util.*;

/**
 * Command handler processing routed API requests under the prefix "service/".
 */
public class ServiceCommandHandler implements IApiHandler {
    public static final String ROUTE_PREFIX = "service/";
    private final ServiceRegistry registry;

    public ServiceCommandHandler(ServiceRegistry registry) {
        this.registry = registry != null ? registry : ServiceRegistry.getInstance();
    }

    public ServiceCommandHandler() {
        this(ServiceRegistry.getInstance());
    }

    @Override
    public String getRoutePrefix() {
        return ROUTE_PREFIX;
    }

    @Override
    public void handle(NativeApiRequest request, IApiCallback callback) {
        if (request == null) {
            if (callback != null) {
                callback.onError(new NativeApiError(NativeApiError.CODE_INVALID_PARAM, "Request cannot be null"));
            }
            return;
        }

        String action = request.getAction();
        if (action == null || action.isEmpty()) {
            String path = request.getPath();
            if (path.startsWith(ROUTE_PREFIX)) {
                action = path.substring(ROUTE_PREFIX.length());
            }
        }

        try {
            if ("list".equalsIgnoreCase(action) || "getAll".equalsIgnoreCase(action)) {
                List<Map<String, Object>> serviceList = new ArrayList<>();
                for (IService svc : registry.getAllServices()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", svc.getServiceId());
                    item.put("available", svc.isAvailable());
                    if (svc instanceof BaseService) {
                        item.put("priority", ((BaseService) svc).getPriority());
                        item.put("state", ((BaseService) svc).getState().name());
                    }
                    serviceList.add(item);
                }
                Map<String, Object> resData = Collections.singletonMap("services", serviceList);
                if (callback != null) {
                    callback.onSuccess(NativeApiResponse.success(request.getRequestId(), resData));
                }
            } else if ("getDeviceInfo".equalsIgnoreCase(action) || "deviceInfo".equalsIgnoreCase(action)) {
                DeviceInfoService service = registry.getService(DeviceInfoService.class);
                if (service == null) {
                    service = new DeviceInfoService();
                    service.initialize();
                }
                DeviceInfoDTO dto = service.getDeviceInfo();
                Map<String, Object> data = new HashMap<>();
                data.put("manufacturer", dto.getManufacturer());
                data.put("model", dto.getModel());
                data.put("osVersion", dto.getOsVersion());
                data.put("sdkInt", dto.getSdkInt());
                data.put("deviceUuid", dto.getDeviceUuid());
                data.put("totalMemory", dto.getTotalMemory());

                if (callback != null) {
                    callback.onSuccess(NativeApiResponse.success(request.getRequestId(), data));
                }
            } else if ("getBatteryStatus".equalsIgnoreCase(action) || "battery".equalsIgnoreCase(action)) {
                BatteryStatusService service = registry.getService(BatteryStatusService.class);
                if (service == null) {
                    service = new BatteryStatusService();
                    service.initialize();
                }
                BatteryStatusDTO dto = service.getBatteryStatus();
                Map<String, Object> data = new HashMap<>();
                data.put("level", dto.getLevel());
                data.put("isCharging", dto.isCharging());
                data.put("pluggedType", dto.getPluggedType());
                data.put("temperature", dto.getTemperature());

                if (callback != null) {
                    callback.onSuccess(NativeApiResponse.success(request.getRequestId(), data));
                }
            } else if ("getNetworkState".equalsIgnoreCase(action) || "network".equalsIgnoreCase(action)) {
                NetworkStateService service = registry.getService(NetworkStateService.class);
                if (service == null) {
                    service = new NetworkStateService();
                    service.initialize();
                }
                Map<String, Object> data = service.getNetworkStatusMap();
                if (callback != null) {
                    callback.onSuccess(NativeApiResponse.success(request.getRequestId(), data));
                }
            } else {
                if (callback != null) {
                    callback.onError(new NativeApiError(NativeApiError.CODE_NOT_FOUND, "Unsupported service action: " + action));
                }
            }
        } catch (Throwable t) {
            if (callback != null) {
                callback.onError(new NativeApiError(NativeApiError.CODE_EXECUTION_FAILED, "Service execution failed: " + t.getMessage()));
            }
        }
    }
}
