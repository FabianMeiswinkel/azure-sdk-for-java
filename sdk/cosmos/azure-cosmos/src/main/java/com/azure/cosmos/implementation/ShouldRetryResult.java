package com.azure.cosmos.implementation;

import java.time.Duration;

public class ShouldRetryResult {
    /// <summary>
    /// How long to wait before next retry. 0 indicates retry immediately.
    /// </summary>
    public final Duration backOffTime;
    public final Exception exception;
    public boolean shouldRetry;
    public final Quadruple<Boolean, Boolean, Duration, Integer> policyArg;

    private ShouldRetryResult(Duration dur, Exception e, boolean shouldRetry,
            Quadruple<Boolean, Boolean, Duration, Integer> policyArg) {
        this.backOffTime = dur;
        this.exception = e;
        this.shouldRetry = shouldRetry;
        this.policyArg = policyArg;
    }

    public static ShouldRetryResult retryAfter(Duration dur) {
        Utils.checkNotNullOrThrow(dur, "duration", "cannot be null");
        return new ShouldRetryResult(dur, null, true, null);
    }

    public static ShouldRetryResult retryAfter(Duration dur,
            Quadruple<Boolean, Boolean, Duration, Integer> policyArg) {
        Utils.checkNotNullOrThrow(dur, "duration", "cannot be null");
        return new ShouldRetryResult(dur, null, true, policyArg);
    }

    public static ShouldRetryResult error(Exception e) {
        Utils.checkNotNullOrThrow(e, "exception", "cannot be null");
        return new ShouldRetryResult(null, e, false, null);
    }

    public static ShouldRetryResult noRetry() {
        return new ShouldRetryResult(null, null, false, null);
    }

    public void throwIfDoneTrying(Exception capturedException) throws Exception {
        if (this.shouldRetry) {
            return;
        }

        if (this.exception == null) {
            throw capturedException;
        } else {
            throw this.exception;
        }
    }
}