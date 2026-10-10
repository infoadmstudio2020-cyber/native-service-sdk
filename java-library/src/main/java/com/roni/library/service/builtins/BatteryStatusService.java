package com.roni.library.service.builtins;

import com.roni.library.contracts.platform.BatteryStatusDTO;
import com.roni.library.service.BaseService;

/**
 * Built-in service providing battery telemetry.
 */
public class BatteryStatusService extends BaseService {
    public static final String SERVICE_NAME = "BatteryStatusService";
    private volatile BatteryStatusDTO currentBatteryStatus;

    public BatteryStatusService() {
        super(SERVICE_NAME, 20);
        this.currentBatteryStatus = new BatteryStatusDTO(100, false, "UNPLUGGED", 25.0f);
    }

    @Override
    protected void onInitialize() {
        updateBatteryStatus(100, false, "UNPLUGGED", 25.0f);
    }

    public synchronized void updateBatteryStatus(int level, boolean isCharging, String plugType, float temperatureCelsius) {
        this.currentBatteryStatus = new BatteryStatusDTO(level, isCharging, plugType, temperatureCelsius);
    }

    public BatteryStatusDTO getBatteryStatus() {
        return currentBatteryStatus;
    }
}
