// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.ErrorReportConfiguration;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.io.ContentReference;
import io.netty.buffer.ByteBuf;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkArgument;
import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public abstract class JsonMemoryReader
{
    protected final ByteBuf buffer;
    protected final ContentReference contenRef;

    private final int initialOffset;
    private final int initialSize;

    protected JsonMemoryReader(ByteBuf buffer)
    {
        checkNotNull(buffer, "Argument 'buffer' must not be null.");
        this.buffer = buffer;
        this.initialOffset = this.buffer.readerIndex();
        this.initialSize = this.buffer.readableBytes();
        this.contenRef = ContentReference.construct(
            false,
            this.buffer,
            this.initialOffset,
            this.initialSize,
            ErrorReportConfiguration.defaults());
    }

    public boolean isEof() {
        return this.buffer.readableBytes() <= 0;
    }

    public int getPosition() {
        return this.buffer.readerIndex();
    }

    public int getRelativePosition() {
        return this.buffer.readerIndex() - this.initialOffset;
    }

    public JsonLocation getCurrentJsonLocation(int offset) {
        return new JsonLocation(this.contenRef, this.initialSize, 1, this.getRelativePosition() + offset);
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
        return this.buffer.duplicate();
    }

    public ByteBuf getRetainedBufferedRawJsonToken(int startPosition)
    {
        checkArgument(
            startPosition > this.buffer.readableBytes(),
            "Argument 'startPosition' [" + startPosition +
                "] must not exceed readable bytes [" + this.buffer.readableBytes() + "]");
        return this.buffer.slice(
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
