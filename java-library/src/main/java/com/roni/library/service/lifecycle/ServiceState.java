package com.roni.library.service.lifecycle;

/**
 * Lifecycle states of an IService instance.
 */
public enum ServiceState {
    CREATED,
    INITIALIZING,
    INITIALIZED,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    ERROR,
    DESTROYED;

    public boolean isActive() {
        return this == RUNNING;
    }

    public boolean isTerminal() {
        return this == ERROR || this == DESTROYED;
    }
}
