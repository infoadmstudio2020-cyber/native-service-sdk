package com.roni.library.service.builtins;

import com.roni.library.service.BaseService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Built-in service tracking network connectivity and transport type.
 */
public class NetworkStateService extends BaseService {
    public static final String SERVICE_NAME = "NetworkStateService";

    public enum NetworkType {
        WIFI,
        CELLULAR,
        ETHERNET,
        OFFLINE,
        UNKNOWN
    }

    private volatile boolean isConnected = true;
    private volatile NetworkType networkType = NetworkType.WIFI;

    public NetworkStateService() {
        super(SERVICE_NAME, 15);
    }

    @Override
    protected void onInitialize() {
        this.isConnected = true;
        this.networkType = NetworkType.WIFI;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public NetworkType getNetworkType() {
        return networkType;
    }

    public synchronized void updateNetworkState(boolean connected, NetworkType type) {
        this.isConnected = connected;
        this.networkType = type != null ? type : NetworkType.UNKNOWN;
    }

    public Map<String, Object> getNetworkStatusMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("connected", isConnected);
        map.put("type", networkType.name());
        map.put("isWifi", networkType == NetworkType.WIFI);
        return Collections.unmodifiableMap(map);
    }
}
