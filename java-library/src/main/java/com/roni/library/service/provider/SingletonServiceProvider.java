package com.roni.library.service.provider;

import com.roni.library.contracts.service.IService;

/**
 * Thread-safe singleton wrapper around a lazy IServiceProvider factory.
 *
 * @param <T> service type extending IService
 */
public class SingletonServiceProvider<T extends IService> implements IServiceProvider<T> {
    private final IServiceProvider<T> factory;
    private volatile T instance;
    private final Object lock = new Object();

    public SingletonServiceProvider(IServiceProvider<T> factory) {
        if (factory == null) {
            throw new IllegalArgumentException("Factory cannot be null");
        }
        this.factory = factory;
    }

    public static <T extends IService> SingletonServiceProvider<T> of(T existingInstance) {
        if (existingInstance == null) {
            throw new IllegalArgumentException("Instance cannot be null");
        }
        return new SingletonServiceProvider<>(() -> existingInstance);
    }

    public static <T extends IService> SingletonServiceProvider<T> lazy(IServiceProvider<T> factory) {
        return new SingletonServiceProvider<>(factory);
    }

    @Override
    public T get() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = factory.get();
                }
            }
        }
        return instance;
    }
}
