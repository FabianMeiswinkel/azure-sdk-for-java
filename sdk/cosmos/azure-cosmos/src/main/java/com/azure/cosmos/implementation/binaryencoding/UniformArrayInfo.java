// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

public class UniformArrayInfo
{
    public final byte ItemTypeMarker;
    public final int ItemCount;
    public final int ItemSize;
    public final int PrefixSize;
    public final UniformArrayInfo NestedArrayInfo;

    public UniformArrayInfo(
        byte itemTypeMarker,
        int itemCount,
        int itemSize,
        int prefixSize,
        UniformArrayInfo nestedArrayInfo)
    {
        this.ItemTypeMarker = itemTypeMarker;
        this.ItemCount = itemCount;
        this.ItemSize = itemSize;
        this.PrefixSize = prefixSize;
        this.NestedArrayInfo = nestedArrayInfo;
    }
}
