// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public final class DoubleEx implements Comparable<DoubleEx> {

    private final double doubleValue;
    private final char extraBits;

    // Java does not have unsigned 16-bit integer - except for char
    // char also supports bitwise operators etc. and can be used here internally
    public DoubleEx(double doubleValue, char extraBits)
    {
        this.doubleValue = doubleValue;
        this.extraBits = extraBits;
    }

    private static int getMostSignificantBitIndex(long n) {
        if (n == 0) return -1; // No bits are set
        int index = 0;
        while (n != 0) {
            n >>= 1;
            index++;
        }
        return index - 1;
    }

    public static int getLeastSignificantBitIndex(long x) {
        if (x == 0) {
            return -1; // No bits are set
        }
        return Long.numberOfTrailingZeros(x);
    }

    // Java does not have unsigned 16-bit integer - except for char
    // char also supports bitwise operators etc. and can be used here internally
    public static DoubleEx fromLong(long longValue)
    {
        if (longValue == Long.MIN_VALUE)
        {
            // Special casing this since you can't take Abs(long.MinValue) due to two's complement
            return new DoubleEx((double)longValue, (char)0);
        }

        double doubleValue;
        char extraBits;

        long absValue = Math.abs(longValue);
        int msbIndex = getMostSignificantBitIndex(absValue);

        // Check if the integer value spans more than 52 bits (meaning it won't fit in a double's mantissa at full precision)
        if ((msbIndex > 52) && ((msbIndex - getLeastSignificantBitIndex(absValue)) > 52))
        {
            // Retrieve the most significant bit index which is the double exponent value
            int exponentValue = msbIndex;

            long exponentBits = ((long)exponentValue + 1023) << 52;

            // Set the mantissa as a 62 bit value (i.e. represents 63-bit number)
            long mantissa = (absValue << (62 - exponentValue)) & 0x3FFFFFFFFFFFFFFFL;

            // Retrieve the least significant 10 bits
            extraBits = (char)((mantissa & 0x3FF) << 6);

            // Adjust the mantissa to 52 bits
            mantissa = mantissa >> 10;

            long valueBits = exponentBits | mantissa;
            if (longValue != absValue)
            {
                valueBits = (valueBits | 0x8000000000000000L);
            }

            doubleValue = Double.longBitsToDouble(valueBits);
        }
        else
        {
            doubleValue = (double)longValue;
            extraBits = 0;
        }

        return new DoubleEx(doubleValue, extraBits);
    }

    /// <summary>
    /// The double if the value is a double.
    /// </summary>
    public double getDoubleValue() {
        return this.doubleValue;
    }

    /// <summary>
    /// The long if the value is a long.
    /// </summary>
    public char getExtraBits() {
        return this.extraBits;
    }

    private static class BitTestResult {
        private final boolean result;
        private final long output;
        public BitTestResult(boolean result, long output) {
            this.result = result;
            this.output = output;
        }

        public boolean isSuccess() {
            return result;
        }

        public long getOutput() {
            return this.output;
        }
    }

    public static BitTestResult bitTestAndReset64(long input, int index)
    {
        boolean result = (input & (1L << index)) != 0;
        long output = (input &= ~(1L << index));
        return new BitTestResult(result, output);
    }

    public long toLong() {
        if (this.extraBits == 0) {
            return (long)this.doubleValue;
        }

        long integerValue;
        integerValue = Double.doubleToLongBits(this.doubleValue);

        // Retrieve and clear the sign bit
        BitTestResult bitTestResult = bitTestAndReset64(integerValue, 63);
        boolean isNegative = bitTestResult.isSuccess();
        integerValue = bitTestResult.getOutput();

        // Retrieve the exponent value
        int exponentValue = (int)((integerValue >> 52) - 1023L);

        // Extend the value to 62 bits
        integerValue = integerValue << 10;

        // Set MSB (i.e. bit 62) and clear the sign bit (left over from the exponent)
        integerValue = (integerValue | 0x4000000000000000L) & 0x7FFFFFFFFFFFFFFFL;

        // Set the extra bits
        integerValue = integerValue | (((long)this.extraBits) >> 6);

        // Adjust for the exponent
        integerValue = integerValue >> (62 - exponentValue);
        if (isNegative)
        {
            integerValue = -integerValue;
        }

        return integerValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof DoubleEx)) {
            return false;
        }

        DoubleEx right = (DoubleEx)obj;
        return this.doubleValue == right.doubleValue && this.extraBits == right.extraBits;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;

        hashCode ^= ((Long)Double.doubleToLongBits(this.doubleValue)).hashCode();
        hashCode ^= ((Character)this.extraBits).hashCode();

        return hashCode;
    }

    private static int getSign(double doubleValue) {
        if (doubleValue == 0) {
            return 0;
        }

        if (doubleValue < 0) {
            return -1;
        }

        return 1;
    }

    @Override
    public int compareTo(DoubleEx other) {
        int compare = Double.compare(this.doubleValue, other.doubleValue);
        if (compare == 0)
        {
            compare = Character.compare(this.extraBits, other.extraBits) * getSign(this.doubleValue);
        }

        return compare;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new DoubleEx(this.doubleValue, this.extraBits);
    }

    /// <summary>
    /// Returns if two DoubleEx are equal.
    /// </summary>
    /// <param name="left">The left hand side of the operator.</param>
    /// <param name="right">The right hand side of the operator.</param>
    /// <returns>Whether the left is equal to the right.</returns>
    public static boolean equals(DoubleEx left, DoubleEx right) {
        checkNotNull(left, "Argument 'left' can not be null");
        checkNotNull(right, "Argument 'right' can not be null");

        return left.doubleValue == right.doubleValue && left.extraBits == right.extraBits;
    }
}
