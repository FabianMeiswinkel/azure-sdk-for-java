// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import io.netty.buffer.ByteBuf;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkArgument;
import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public abstract class JsonMemoryReader
{
    protected final ByteBuf buffer;
    protected int position;

    protected JsonMemoryReader(ByteBuf buffer)
    {
        checkNotNull(buffer, "Argument 'buffer' must not be null.");
        this.buffer = buffer;
    }

    public boolean isEof() {
        return this.buffer.readableBytes() > 0;
    }

    public int getPosition() {
        return this.buffer.readerIndex();
    }

    public byte read()
    {
        if (this.buffer.readableBytes() > 0) {
            return this.buffer.readByte();
        }

        return (byte)0;
    }

    public byte peek()
    {
        if (this.buffer.readableBytes() > 0) {
            return this.buffer.getByte(this.buffer.readerIndex());
        }

        return (byte)0;
    }

    public ByteBuf getRetainedBufferedRawJsonToken()
    {
        return this.buffer.retainedSlice();
    }

    public ByteBuf getRetainedBufferedRawJsonToken(int startPosition)
    {
        checkArgument(
            startPosition > this.buffer.readableBytes(),
            "Argument 'startPosition' [" + startPosition +
                "] must not exceed readable bytes [" + this.buffer.readableBytes() + "]");
        return this.buffer.retainedSlice(
            this.buffer.readerIndex() + startPosition,
            this.buffer.readableBytes() - startPosition);
    }

    public ByteBuf getRetainedBufferedRawJsonToken(
        int startPosition,
        int endPosition)
    {
        return this.getRetainedBufferedRawJsonToken(startPosition, endPosition - startPosition);
    }
}
