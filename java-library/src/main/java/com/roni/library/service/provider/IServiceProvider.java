package com.roni.library.service.provider;

import com.roni.library.contracts.service.IService;

/**
 * Provider interface for on-demand lazy instantiation and dependency resolution of an IService.
 *
 * @param <T> service type extending IService
 */
public interface IServiceProvider<T extends IService> {
    /**
     * Resolves and provides the service instance.
     *
     * @return the resolved service instance.
     */
    T get();
}
