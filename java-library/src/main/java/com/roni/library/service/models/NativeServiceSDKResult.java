package com.roni.library.service.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Generic result wrapper for NativeServiceSDK operations.
 *
 * @param <T> the result data type
 * @author infoadmstudio2020-cyber
 */
public final class NativeServiceSDKResult<T> {

    private final boolean success;
    private final T       data;
    private final String  errorMessage;
    private final int     errorCode;

    private NativeServiceSDKResult(boolean success, T data, String errorMessage, int errorCode) {
        this.success      = success;
        this.data         = data;
        this.errorMessage = errorMessage;
        this.errorCode    = errorCode;
    }

    /** Create a success result with data. */
    @NonNull
    public static <T> NativeServiceSDKResult<T> success(@Nullable T data) {
        return new NativeServiceSDKResult<>(true, data, null, 0);
    }

    /** Create an error result. */
    @NonNull
    public static <T> NativeServiceSDKResult<T> error(@NonNull String message, int code) {
        return new NativeServiceSDKResult<>(false, null, message, code);
    }

    /** @return true if the operation succeeded */
    public boolean isSuccess() { return success; }

    /** @return the result data, or null on error */
    @Nullable
    public T getData() { return data; }

    /** @return error message, or null on success */
    @Nullable
    public String getErrorMessage() { return errorMessage; }

    /** @return numeric error code */
    public int getErrorCode() { return errorCode; }
}
