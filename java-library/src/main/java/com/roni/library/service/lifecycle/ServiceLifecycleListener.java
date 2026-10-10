package com.roni.library.service.lifecycle;

import com.roni.library.contracts.service.IService;

/**
 * Callback listener invoked on service lifecycle state transitions.
 */
public interface ServiceLifecycleListener {
    /**
     * Called whenever a registered service changes its lifecycle state.
     *
     * @param service  the service transitioning state.
     * @param oldState previous state.
     * @param newState new current state.
     */
    void onStateChanged(IService service, ServiceState oldState, ServiceState newState);
}
