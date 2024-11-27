// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;

import java.io.IOException;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkArgument;
import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public class CosmosBinaryAwareJsonFactory extends MappingJsonFactory {
    private static final long serialVersionUID = 1L;

    public CosmosBinaryAwareJsonFactory(ObjectMapper mapper) {
        super();
    }

    //
    @Override
    protected JsonParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        checkNotNull(data, "Argument 'data' must not be null");
        checkArgument(data.length > offset, "Argument 'offset' is invalid.");
        checkArgument(data.length >= offset + len, "Argument 'len' is invalid.");

        if (data[offset] == JsonSerializationFormat.Binary) {
            checkArgument(
                data.length > offset + 1,
                "Argument 'offset' is invalid for binary encoded payload.");
            return new CosmosBinaryParser(
                // NOTE: It is ok here to use the unooled wrapped buffer, because this byte[]
                // is backed by the netty buffer pool - and will be released accordingly after
                // parsing is done.
                Unpooled.wrappedBuffer(data, offset + 1, len - 1),
                ctxt,
                this._parserFeatures,
                _byteSymbolCanonicalizer.makeChild(this._factoryFeatures));
        }

        return super._createParser(data, offset, len, ctxt);
    }
}
