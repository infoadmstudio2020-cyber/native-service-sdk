package com.roni.library.service.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utility methods for NativeServiceSDK.
 *
 * @author infoadmstudio2020-cyber
 */
public final class NativeServiceSDKUtils {

    private NativeServiceSDKUtils() {}

    /**
     * Check whether the context is valid and non-null.
     *
     * @param context the context to check
     * @return true if non-null
     */
    public static boolean isValidContext(@Nullable Context context) {
        return context != null;
    }

    /**
     * Return the application version name.
     *
     * @param context application context
     * @return version name, or "unknown"
     */
    @NonNull
    public static String getAppVersion(@NonNull Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }

    /**
     * Check if the device is running at least the given API level.
     *
     * @param apiLevel minimum API level
     * @return true if the device API level &gt;= apiLevel
     */
    public static boolean isApiAtLeast(int apiLevel) {
        return Build.VERSION.SDK_INT >= apiLevel;
    }
}
