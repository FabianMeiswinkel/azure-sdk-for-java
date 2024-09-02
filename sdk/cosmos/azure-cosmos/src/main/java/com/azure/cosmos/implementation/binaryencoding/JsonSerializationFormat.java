package com.azure.cosmos.implementation.binaryencoding;

public final class JsonSerializationFormat {
    public static final byte Text = 0;
    public static final byte Binary = (byte) 128;
    public static final byte HybridRow = (byte) 129;
}