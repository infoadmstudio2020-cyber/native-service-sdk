package com.roni.library.service.interfaces;

import androidx.annotation.NonNull;

/**
 * Listener interface for NativeServiceSDK events.
 *
 * @author infoadmstudio2020-cyber
 */
public interface INativeServiceSDKListener {

    /**
     * Called when the library is successfully initialized.
     */
    void onInitialized();

    /**
     * Called when an error occurs.
     *
     * @param errorCode numeric error code
     * @param message   human-readable message
     */
    void onError(int errorCode, @NonNull String message);
}
