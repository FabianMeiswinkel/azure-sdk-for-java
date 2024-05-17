// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.azure.cosmos.implementation.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.EmptyByteBuf;
import io.netty.util.CharsetUtil;

import java.util.Objects;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkArgument;
import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public final class Utf8ByteBuffer {
    private final static int UINT_SIZE = 4;
    private final ByteBuf memory;

    private Utf8ByteBuffer(ByteBuf utf8Bytes) {
        checkNotNull(utf8Bytes, "Parameter 'utf8Bytes' MUST NOT be null.");
        checkArgument(utf8Bytes.isReadOnly(), "Parameter 'utf8Bytes' MUST be read-only.");
        this.memory = utf8Bytes;
    }

    public Utf8ByteBuffer slice(int start) {
        return this.slice(start, this.memory.readableBytes() - start);
    }

    public Utf8ByteBuffer slice(int start, int length) {
        return new Utf8ByteBuffer(this.memory.slice(start, length));
    }

    public boolean isEmpty() {
        return this.memory.readableBytes() == 0;
    }

    public int getLength() {
        return this.memory.readableBytes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Utf8ByteBuffer that = (Utf8ByteBuffer) o;
        return Objects.equals(memory, that.memory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memory);
    }

    @Override
    public String toString() {
        return this.memory.toString(CharsetUtil.UTF_8);
    }

    public static TryResult<Utf8ByteBuffer> tryCreate(ByteBuf utf8Bytes) {
        if (!ByteBufUtil.isText(utf8Bytes, CharsetUtil.UTF_8)) {
            return TryResult.failed(Utf8ByteBuffer.class);
        }

        return TryResult.success(unsafeCreateNoValidation(utf8Bytes));
    }

    public static Utf8ByteBuffer create(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return unsafeCreateNoValidation(new EmptyByteBuf(JsonBinaryEncoding.allocator));
        }

        return unsafeCreateNoValidation(ByteBufUtil.writeUtf8(JsonBinaryEncoding.allocator, value));
    }

    public static Utf8ByteBuffer create(ByteBuf utf8Bytes) {
        checkNotNull(utf8Bytes, "Parameter 'utf8Bytes' MUST NOT be null.");
        checkArgument(utf8Bytes.isReadOnly(), "Parameter 'utf8Bytes' MUST be read-only.");
        checkArgument(
            ByteBufUtil.isText(utf8Bytes, CharsetUtil.UTF_8),
            "Parameter 'utf8Bytes' is not a valid UTF-8 byte sequence.");
        return unsafeCreateNoValidation(utf8Bytes);
    }

    public static Utf8ByteBuffer unsafeCreateNoValidation(ByteBuf utf8Bytes) {
        return new Utf8ByteBuffer(utf8Bytes);
    }
}
