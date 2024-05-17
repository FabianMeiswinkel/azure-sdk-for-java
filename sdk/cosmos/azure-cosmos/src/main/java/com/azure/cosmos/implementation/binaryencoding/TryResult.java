// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import java.util.concurrent.ConcurrentHashMap;

public class TryResult<TResult> {
    private static ConcurrentHashMap<Class<?>, TryResult<?>> failedResults = new ConcurrentHashMap<>();
    private boolean mSuccess = false;
    private TResult mResult = null;

    private TryResult(boolean success, TResult result) {
        super();
        this.mSuccess = success;
        this.mResult = result;
    }

    public boolean isSuccess() {
        return mSuccess;
    }

    public TResult getResult() {
        return mResult;
    }

    public static <T> TryResult<T> failed(Class<T> clazz) {
        return (TryResult<T>)failedResults.computeIfAbsent(clazz, (c -> new TryResult<>(false, (T)null)));
    }

    public static <T> TryResult<T> success(T result) {
        return new TryResult<>(true, result);
    }
}
