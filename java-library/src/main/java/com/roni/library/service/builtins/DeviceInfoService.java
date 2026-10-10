package com.roni.library.service.builtins;

import com.roni.library.contracts.platform.DeviceInfoDTO;
import com.roni.library.service.BaseService;

import java.util.UUID;

/**
 * Built-in service managing device hardware information extraction.
 */
public class DeviceInfoService extends BaseService {
    public static final String SERVICE_NAME = "DeviceInfoService";
    private DeviceInfoDTO cachedDeviceInfo;

    public DeviceInfoService() {
        super(SERVICE_NAME, 10);
    }

    @Override
    protected void onInitialize() {
        String manufacturer = getSystemProp("ro.product.manufacturer", "Android");
        String model = getSystemProp("ro.product.model", "Generic-Device");
        String osVersion = getSystemProp("ro.build.version.release", "14.0");
        int sdkInt = 34;
        try {
            sdkInt = Integer.parseInt(getSystemProp("ro.build.version.sdk", "34"));
        } catch (NumberFormatException ignored) {}

        String deviceId = UUID.randomUUID().toString();
        long totalMemoryMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);

        this.cachedDeviceInfo = new DeviceInfoDTO(
                manufacturer,
                model,
                osVersion,
                sdkInt,
                deviceId,
                totalMemoryMb
        );
    }

    public DeviceInfoDTO getDeviceInfo() {
        if (cachedDeviceInfo == null) {
            onInitialize();
        }
        return cachedDeviceInfo;
    }

    private String getSystemProp(String key, String fallback) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Object val = systemProperties.getMethod("get", String.class, String.class).invoke(null, key, fallback);
            return val != null ? val.toString() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
