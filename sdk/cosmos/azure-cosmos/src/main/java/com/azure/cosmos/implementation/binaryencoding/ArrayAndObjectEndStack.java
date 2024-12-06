// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public final class ArrayAndObjectEndStack
{
    private final Deque<Integer> endOffsets;
    private UniformArrayInfo arrayInfo;
    private UniformArrayInfo nestedArrayInfo;

    public ArrayAndObjectEndStack()
    {
        this.endOffsets = new ArrayDeque<>(16);
    }

    public boolean isEmpty()
    {
        return this.endOffsets.isEmpty();
    }

    public int peek()
    {
        return this.endOffsets.peek();
    }

    public void push(int endOffset, UniformArrayInfo arrayInfo)
    {
        if (this.arrayInfo != null) {
            throw new IllegalStateException();
        }

        this.endOffsets.push(endOffset);
        this.arrayInfo = arrayInfo;
    }

    public void pushNestedArray(int currentOffset)
    {
        if (this.arrayInfo == null
            || this.arrayInfo.NestedArrayInfo == null
            || this.nestedArrayInfo != null) {

            throw new IllegalStateException();
        }

        this.endOffsets.push(currentOffset + this.arrayInfo.ItemSize);
        this.nestedArrayInfo = this.arrayInfo.NestedArrayInfo;
    }

    public void pop()
    {
        this.endOffsets.pop();

        if (this.nestedArrayInfo != null)
        {
            this.nestedArrayInfo = null;
        }
        else if (this.arrayInfo != null)
        {
            this.arrayInfo = null;
        }
    }

    public boolean isWithinUniformArray()
    {
        return this.arrayInfo != null;
    }

    public boolean isWithinNestedUniformArray()
    {
        return this.nestedArrayInfo != null;
    }

    public UniformArrayInfo getUniformArrayInfo()
    {
        return this.nestedArrayInfo != null ? this.nestedArrayInfo : this.arrayInfo;
    }

    public byte getUniformArrayItemTypeMarkerOrDefault(byte defaultValue)
    {
        UniformArrayInfo arrayInfo = this.getUniformArrayInfo();
        return arrayInfo != null ? arrayInfo.ItemTypeMarker : defaultValue;
    }
}