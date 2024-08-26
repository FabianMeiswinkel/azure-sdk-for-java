// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.io.IOContext;


import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

public class Generator extends GeneratorBase {

    public Generator(IOContext ioCtxt, int stdFeatures, int binaryFeatures,
                     ObjectCodec codec, OutputStream out) {
        super(stdFeatures, codec, ioCtxt);

    }

    @Override
    public void writeStartArray() throws IOException {

    }

    @Override
    public void writeEndArray() throws IOException {

    }

    @Override
    public void writeStartObject() throws IOException {

    }

    @Override
    public void writeEndObject() throws IOException {

    }

    @Override
    public void writeFieldName(String s) throws IOException {

    }

    @Override
    public void writeString(String s) throws IOException {

    }

    @Override
    public void writeString(char[] chars, int i, int i1) throws IOException {

    }

    @Override
    public void writeRawUTF8String(byte[] bytes, int i, int i1) throws IOException {

    }

    @Override
    public void writeUTF8String(byte[] bytes, int i, int i1) throws IOException {

    }

    @Override
    public void writeRaw(String s) throws IOException {

    }

    @Override
    public void writeRaw(String s, int i, int i1) throws IOException {

    }

    @Override
    public void writeRaw(char[] chars, int i, int i1) throws IOException {

    }

    @Override
    public void writeRaw(char c) throws IOException {

    }

    @Override
    public void writeBinary(Base64Variant base64Variant, byte[] bytes, int i, int i1) throws IOException {

    }

    @Override
    public void writeNumber(int i) throws IOException {

    }

    @Override
    public void writeNumber(long l) throws IOException {

    }

    @Override
    public void writeNumber(BigInteger bigInteger) throws IOException {

    }

    @Override
    public void writeNumber(double v) throws IOException {

    }

    @Override
    public void writeNumber(float v) throws IOException {

    }

    @Override
    public void writeNumber(BigDecimal bigDecimal) throws IOException {

    }

    @Override
    public void writeNumber(String s) throws IOException {

    }

    @Override
    public void writeBoolean(boolean b) throws IOException {

    }

    @Override
    public void writeNull() throws IOException {

    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    protected void _releaseBuffers() {

    }

    @Override
    protected void _verifyValueWrite(String s) throws IOException {

    }
}
