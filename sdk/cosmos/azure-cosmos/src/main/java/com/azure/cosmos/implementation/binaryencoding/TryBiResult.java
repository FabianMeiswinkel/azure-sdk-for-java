// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import java.util.concurrent.ConcurrentHashMap;

public class TryBiResult<TResultA, TResultB> {
    private static ConcurrentHashMap<String, TryBiResult<?, ?>> failedResults = new ConcurrentHashMap<>();
    private boolean mSuccess = false;
    private TResultA mResultA = null;

    private TResultB mResultB = null;

    private TryBiResult(boolean success, TResultA resultA, TResultB resultB) {
        super();
        this.mSuccess = success;
        this.mResultA = resultA;
        this.mResultB = resultB;
    }

    public boolean isSuccess() {
        return mSuccess;
    }

    public TResultA getResultA() {
        return mResultA;
    }

    public TResultB getResultB() {
        return mResultB;
    }

    @SuppressWarnings("unchecked")
    public static <TA, TB> TryBiResult<TA, TB> failed(Class<TA> clazzA, Class<TB> clazzB) {
        return (TryBiResult<TA, TB>)failedResults.computeIfAbsent(
            clazzA.getCanonicalName() + "|" + clazzB.getCanonicalName(),
            (c -> new TryBiResult<>(false, (TA)null, (TB)null)));
    }

    public static <TA, TB> TryBiResult<TA, TB> success(TA resultA, TB resultB) {
        return new TryBiResult<>(true, resultA, resultB);
    }
}
