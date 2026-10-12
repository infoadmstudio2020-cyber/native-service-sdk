package com.roni.library.service;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Internal logger for NativeServiceSDK.
 * Respects {@link NativeServiceSDKConfig#isDebugEnabled()}.
 *
 * @author infoadmstudio2020-cyber
 */
public final class NativeServiceSDKLogger {

    private static final String DEFAULT_TAG = "NativeServiceSDK";
    private static boolean sDebug = false;

    private NativeServiceSDKLogger() {}

    /** Enable or disable debug logging. */
    public static void setDebug(boolean debug) { sDebug = debug; }

    /** Log a debug message. */
    public static void d(@NonNull String tag, @NonNull String msg) {
        if (sDebug) Log.d(tag, msg);
    }

    /** Log an info message. */
    public static void i(@NonNull String tag, @NonNull String msg) {
        Log.i(tag, msg);
    }

    /** Log a warning. */
    public static void w(@NonNull String tag, @NonNull String msg) {
        Log.w(tag, msg);
    }

    /** Log an error. */
    public static void e(@NonNull String tag, @NonNull String msg) {
        Log.e(tag, msg);
    }

    /** Log an error with throwable. */
    public static void e(@NonNull String tag, @NonNull String msg, @Nullable Throwable t) {
        Log.e(tag, msg, t);
    }
}
