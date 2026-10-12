package com.roni.library.service;

import android.content.Context;
import androidx.annotation.NonNull;

/**
 * NativeServiceSDK — main entry point.
 *
 * <p>Usage:
 * <pre>
 *   NativeServiceSDK lib = NativeServiceSDK.getInstance();
 *   lib.init(context, new NativeServiceSDKConfig.Builder().build());
 * </pre>
 *
 * @author  infoadmstudio2020-cyber
 * @version 1.0.0
 */
public final class NativeServiceSDK {

    private static volatile NativeServiceSDK sInstance;
    private NativeServiceSDKConfig mConfig;
    private boolean mInitialized;

    /** Private constructor — use {@link #getInstance()}. */
    private NativeServiceSDK() {}

    /**
     * Returns the singleton instance of {@link NativeServiceSDK}.
     *
     * @return singleton instance
     */
    @NonNull
    public static NativeServiceSDK getInstance() {
        if (sInstance == null) {
            synchronized (NativeServiceSDK.class) {
                if (sInstance == null) {
                    sInstance = new NativeServiceSDK();
                }
            }
        }
        return sInstance;
    }

    /**
     * Initialize the library.
     *
     * @param context application context
     * @param config  library configuration
     */
    public void init(@NonNull Context context, @NonNull NativeServiceSDKConfig config) {
        if (mInitialized) {
            NativeServiceSDKLogger.w("NativeServiceSDK", "Already initialized");
            return;
        }
        mConfig      = config;
        mInitialized = true;
        NativeServiceSDKLogger.i("NativeServiceSDK", "Initialized — version 1.0.0");
    }

    /**
     * Check if the library has been initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() { return mInitialized; }

    /**
     * Return the current configuration.
     *
     * @return config, or null if not initialized
     */
    public NativeServiceSDKConfig getConfig() { return mConfig; }

    /** Release all resources held by this instance. */
    public void release() {
        mInitialized = false;
        mConfig      = null;
        sInstance    = null;
    }
}
