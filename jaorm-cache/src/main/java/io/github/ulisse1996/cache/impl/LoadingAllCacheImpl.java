package io.github.ulisse1996.cache.impl;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.ulisse1996.Arguments;
import io.github.ulisse1996.cache.JaormAllCache;

import java.util.List;

public class LoadingAllCacheImpl<T> implements JaormAllCache<T> {

    private final LoadingCache<Arguments, List<T>> cache;

    public LoadingAllCacheImpl(LoadingCache<Arguments, List<T>> cache) {
        this.cache = cache;
    }

    @Override
    public List<T> getAll() {
        return cache.get(Arguments.empty());
    }

}
