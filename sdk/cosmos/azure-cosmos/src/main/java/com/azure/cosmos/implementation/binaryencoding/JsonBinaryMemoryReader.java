// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import io.netty.buffer.ByteBuf;

public final class JsonBinaryMemoryReader extends JsonMemoryReader {

    public JsonBinaryMemoryReader(ByteBuf buffer) {
        super(buffer);
    }

    public void skipBytes(int offset)
    {
        this.buffer.skipBytes(offset);
    }
}
