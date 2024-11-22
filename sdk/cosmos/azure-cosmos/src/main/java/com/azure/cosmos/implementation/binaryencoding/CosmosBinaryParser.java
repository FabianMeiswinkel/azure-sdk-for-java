// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.azure.cosmos.implementation.HttpConstants;
import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.DupDetector;
import com.fasterxml.jackson.core.json.JsonReadContext;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public class CosmosBinaryParser extends ParserMinimalBase {
    private int cosmosBinaryFeatures;
    private final IOContext ioContext;
    private final ByteQuadsCanonicalizer symbols;
    private final boolean symbolsCanonical;

    private final JsonReadContext streamReadContext;

    private final ByteBuf payload;

    protected CosmosBinaryParser(IOContext ctxt, int cosmosBinaryFeatures, int parserFeatures,
                                 ByteQuadsCanonicalizer sym, ByteBuf payload)
    {
        super(parserFeatures);

        checkNotNull(ctxt, "Argument 'ctxt' must not be null");
        checkNotNull(sym, "Argument 'sym' must not be null");
        checkNotNull(payload, "Argument 'payload' must not be null");

        this.cosmosBinaryFeatures = cosmosBinaryFeatures;
        this.ioContext = ctxt;
        this.symbols = sym;
        this.symbolsCanonical = sym.isCanonicalizing();
        DupDetector dups = Feature.STRICT_DUPLICATE_DETECTION.enabledIn(parserFeatures)
            ? DupDetector.rootDetector(this) : null;
        this.streamReadContext= JsonReadContext.createRootContext(dups);
        this.payload = payload;
    }

    Continue here

    @Override
    public JsonToken nextToken() throws IOException {
        return null;
    }

    @Override
    protected void _handleEOF() throws JsonParseException {

    }

    @Override
    public String getCurrentName() throws IOException {
        return null;
    }

    @Override
    public ObjectCodec getCodec() {
        return null;
    }

    @Override
    public void setCodec(ObjectCodec objectCodec) {

    }

    @Override
    public Version version() {
        return HttpConstants.Versions.getSdkVersionAsVersion();
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public JsonStreamContext getParsingContext() {
        return null;
    }

    @Override
    public JsonLocation getCurrentLocation() {
        return null;
    }

    @Override
    public JsonLocation getTokenLocation() {
        return null;
    }

    @Override
    public void overrideCurrentName(String s) {

    }

    @Override
    public String getText() throws IOException {
        return null;
    }

    @Override
    public char[] getTextCharacters() throws IOException {
        return new char[0];
    }

    @Override
    public boolean hasTextCharacters() {
        return false;
    }

    @Override
    public Number getNumberValue() throws IOException {
        return null;
    }

    @Override
    public NumberType getNumberType() throws IOException {
        return null;
    }

    @Override
    public int getIntValue() throws IOException {
        return 0;
    }

    @Override
    public long getLongValue() throws IOException {
        return 0;
    }

    @Override
    public BigInteger getBigIntegerValue() throws IOException {
        return null;
    }

    @Override
    public float getFloatValue() throws IOException {
        return 0;
    }

    @Override
    public double getDoubleValue() throws IOException {
        return 0;
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException {
        return null;
    }

    @Override
    public int getTextLength() throws IOException {
        return 0;
    }

    @Override
    public int getTextOffset() throws IOException {
        return 0;
    }

    @Override
    public byte[] getBinaryValue(Base64Variant base64Variant) throws IOException {
        return new byte[0];
    }
}
