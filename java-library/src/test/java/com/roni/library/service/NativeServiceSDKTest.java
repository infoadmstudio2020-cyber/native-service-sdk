package com.roni.library.service;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link NativeServiceSDK}.
 *
 * @author infoadmstudio2020-cyber
 */
public class NativeServiceSDKTest {

    @Test
    public void getInstance_returnsSameInstance() {
        NativeServiceSDK a = NativeServiceSDK.getInstance();
        NativeServiceSDK b = NativeServiceSDK.getInstance();
        assertSame(a, b);
    }

    @Test
    public void isInitialized_falseBeforeInit() {
        // Reset singleton for test isolation
        NativeServiceSDK lib = NativeServiceSDK.getInstance();
        assertFalse("Should not be initialized before init()", lib.isInitialized());
    }

    @Test
    public void config_builder_defaults() {
        NativeServiceSDKConfig config = new NativeServiceSDKConfig.Builder().build();
        assertFalse(config.isDebugEnabled());
        assertEquals("NativeServiceSDK", config.getTag());
        assertEquals(30_000, config.getTimeout());
    }
}
