// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public final class Number64 implements Comparable<Number64> {
    /// <summary>
    /// Maximum Number64.
    /// </summary>
    public static final Number64 MaxValue = new Number64(Double.MAX_VALUE);

    /// <summary>
    /// Maximum Number64.
    /// </summary>
    public static final Number64 MinValue = new Number64(Double.MIN_VALUE);

    /// <summary>
    /// The double if the value is a double.
    /// </summary>
    private final Double doubleValue;

    /// <summary>
    /// The long if the value is a long.
    /// </summary>
    private final Long longValue;

    public Number64(double value) {
        this.doubleValue = value;
        this.longValue = null;
    }

    public Number64(long value) {
        this.longValue = value;
        this.doubleValue = null;
    }

    public boolean isInteger() {
        return this.longValue != null;
    }

    public boolean isDouble() {
        return this.doubleValue != null;
    }

    public boolean isInfinity() {
        return !this.isInteger() && Double.isInfinite(this.doubleValue);
    }

    public boolean isNaN() {
        return !this.isInteger() && Double.isNaN(this.doubleValue);
    }

    @Override
    public int hashCode() {
        return Number64.ToDoubleEx(this).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Number64)) {
            return false;
        }

        Number64 other = (Number64) obj;
        return this.doubleValue == other.doubleValue && this.longValue == other.longValue;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        if (this.isDouble()) {
            return new Number64(this.doubleValue);
        }

        return new Number64(this.longValue);
    }

    public static long toLong(Number64 number64)
    {
        if (number64.isInteger())
        {
            return number64.longValue;
        }

        return number64.doubleValue.longValue();
    }

    public static double toDouble(Number64 number64)
    {
        if (number64.isDouble())
        {
            return number64.doubleValue;
        }

        return number64.longValue.doubleValue();
    }

    public static DoubleEx ToDoubleEx(Number64 number64)
    {
        DoubleEx doubleEx;
        if (number64.isInteger())
        {
            return DoubleEx.fromLong(number64.longValue);
        }

        return new DoubleEx(number64.doubleValue, (char)0);
    }

    @Override
    public String toString() {
        if (this.isDouble()) {
            return String.valueOf(Number64.toDouble(this));
        }

        return String.valueOf(Number64.toLong(this));
    }

    @Override
    public int compareTo(Number64 other) {
        checkNotNull(other, "Parameter 'other' MUST NOT be null.");
        if (this.isInteger() && other.isInteger()) {
            return Long.compare(this.longValue, other.longValue);
        }

        if (this.isDouble() && other.isDouble()) {
            return Double.compare(this.doubleValue, other.doubleValue);
        }

        DoubleEx first = Number64.ToDoubleEx(this);
        DoubleEx second = Number64.ToDoubleEx(other);

        return first.compareTo(second);
    }
}
