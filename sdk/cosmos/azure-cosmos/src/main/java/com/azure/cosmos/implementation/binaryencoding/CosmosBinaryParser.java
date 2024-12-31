// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.azure.cosmos.implementation.Configs;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkArgument;
import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

// NOTE: This Parser can not be used as general purpose Jackson JsonParser
// it only allows parsing a ByteBuf - which is sufficient for the usage within the
// Cosmos DB SDK - but means parsing from other sources (or non-blocking parsing) are not
// possible.
public class CosmosBinaryParser extends ParserMinimalBase {
    private final static Logger LOG = LoggerFactory.getLogger(CosmosBinaryParser.class);

    private static JsonTokenType[] TypeMarkerToTokenType = new JsonTokenType[] {
        // Encoded literal integer value (32 values)
        JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number,
        JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number,
        JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number,
        JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number,
        JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number,
        JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number,
        JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number,
        JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number, JsonTokenType.Number,

        // Encoded 1-byte system string (32 values)
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,

        // Encoded 1-byte user string (32 values)
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,

        // Encoded 2-byte user string (8 values)
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,

        // String Values [0x68, 0x70)
        JsonTokenType.String,  // <empty> 0x68
        JsonTokenType.String,  // <empty> 0x69
        JsonTokenType.String,  // <empty> 0x6A
        JsonTokenType.String,  // <empty> 0x6B
        JsonTokenType.String,  // <empty> 0x6C
        JsonTokenType.String,  // <empty> 0x6D
        JsonTokenType.String,  // <empty> 0x6E
        JsonTokenType.String,  // <empty> 0x6F

        // String Values [0x70, 0x78)
        JsonTokenType.String,  // <empty> 0x70
        JsonTokenType.String,  // <empty> 0x71
        JsonTokenType.String,  // <empty> 0x72
        JsonTokenType.String,  // <empty> 0x73
        JsonTokenType.String,  // <empty> 0x74
        JsonTokenType.String,  // StrGL (Lowercase GUID string)
        JsonTokenType.String,  // StrGU (Uppercase GUID string)
        JsonTokenType.String,  // StrGQ (Double-quoted lowercase GUID string)

        // Compressed strings [0x78, 0x80)
        JsonTokenType.String,  // String 1-byte length - Lowercase hexadecimal digits encoded as 4-bit characters
        JsonTokenType.String,  // String 1-byte length - Uppercase hexadecimal digits encoded as 4-bit characters
        JsonTokenType.String,  // String 1-byte length - Date-time character set encoded as 4-bit characters
        JsonTokenType.String,  // String 1-byte Length - 4-bit packed characters relative to a base value
        JsonTokenType.String,  // String 1-byte Length - 5-bit packed characters relative to a base value
        JsonTokenType.String,  // String 1-byte Length - 6-bit packed characters relative to a base value
        JsonTokenType.String,  // String 1-byte Length - 7-bit packed characters
        JsonTokenType.String,  // String 2-byte Length - 7-bit packed characters

        // TypeMarker-encoded string length (64 values)
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,
        JsonTokenType.String, JsonTokenType.String, JsonTokenType.String, JsonTokenType.String,

        // Variable Length String Values
        JsonTokenType.String,       // StrL1 (1-byte length)
        JsonTokenType.String,       // StrL2 (2-byte length)
        JsonTokenType.String,       // StrL4 (4-byte length)
        JsonTokenType.String,       // StrR1 (Reference string of 1-byte offset)
        JsonTokenType.String,       // StrR2 (Reference string of 2-byte offset)
        JsonTokenType.String,       // StrR3 (Reference string of 3-byte offset)
        JsonTokenType.String,       // StrR4 (Reference string of 4-byte offset)
        JsonTokenType.Number,       // NumUI64

        // Number Values
        JsonTokenType.Number,       // NumUI8
        JsonTokenType.Number,       // NumI16,
        JsonTokenType.Number,       // NumI32,
        JsonTokenType.Number,       // NumI64,
        JsonTokenType.Number,       // NumDbl,
        JsonTokenType.Float32,      // Float32
        JsonTokenType.Float64,      // Float64
        JsonTokenType.NotStarted,   // Float16

        // Other Value Types
        JsonTokenType.Null,         // Null
        JsonTokenType.False,        // False
        JsonTokenType.True,         // True
        JsonTokenType.Guid,         // GUID
        JsonTokenType.NotStarted,   // <empty> 0xD4
        JsonTokenType.NotStarted,   // <empty> 0xD5
        JsonTokenType.NotStarted,   // <empty> 0xD6
        JsonTokenType.UInt8,        // UInt8

        JsonTokenType.Int8,         // Int8
        JsonTokenType.Int16,        // Int16
        JsonTokenType.Int32,        // Int32
        JsonTokenType.Int64,        // Int64
        JsonTokenType.UInt32,       // UInt32
        JsonTokenType.Binary,       // BinL1 (1-byte length)
        JsonTokenType.Binary,       // BinL2 (2-byte length)
        JsonTokenType.Binary,       // BinL4 (4-byte length)

        // Array Type Markers
        JsonTokenType.BeginArray,   // Arr0
        JsonTokenType.BeginArray,   // Arr1
        JsonTokenType.BeginArray,   // ArrL1 (1-byte length)
        JsonTokenType.BeginArray,   // ArrL2 (2-byte length)
        JsonTokenType.BeginArray,   // ArrL4 (4-byte length)
        JsonTokenType.BeginArray,   // ArrLC1 (1-byte length and count)
        JsonTokenType.BeginArray,   // ArrLC2 (2-byte length and count)
        JsonTokenType.BeginArray,   // ArrLC4 (4-byte length and count)

        // Object Type Markers
        JsonTokenType.BeginObject,  // Obj0
        JsonTokenType.BeginObject,  // Obj1
        JsonTokenType.BeginObject,  // ObjL1 (1-byte length)
        JsonTokenType.BeginObject,  // ObjL2 (2-byte length)
        JsonTokenType.BeginObject,  // ObjL4 (4-byte length)
        JsonTokenType.BeginObject,  // ObjLC1 (1-byte length and count)
        JsonTokenType.BeginObject,  // ObjLC2 (2-byte length and count)
        JsonTokenType.BeginObject,  // ObjLC4 (4-byte length and count)

        // Array and Object Special Type Markers
        JsonTokenType.BeginArray,   // ArrNumC1 Uniform number array of 1-byte item count
        JsonTokenType.BeginArray,   // ArrNumC2 Uniform number array of 2-byte item count
        JsonTokenType.BeginArray,   // Array of 1-byte item count of Uniform number array of 1-byte item count
        JsonTokenType.BeginArray,   // Array of 2-byte item count of Uniform number array of 2-byte item count
        JsonTokenType.NotStarted,   // <empty> 0xF4
        JsonTokenType.NotStarted,   // <empty> 0xF5
        JsonTokenType.NotStarted,   // <empty> 0xF7
        JsonTokenType.NotStarted,   // <empty> 0xF8

        // Special Values
        JsonTokenType.NotStarted,   // <special value reserved> 0xF8
        JsonTokenType.NotStarted,   // <special value reserved> 0xF9
        JsonTokenType.NotStarted,   // <special value reserved> 0xFA
        JsonTokenType.NotStarted,   // <special value reserved> 0xFB
        JsonTokenType.NotStarted,   // <special value reserved> 0xFC
        JsonTokenType.NotStarted,   // <special value reserved> 0xFD
        JsonTokenType.NotStarted,   // <special value reserved> 0xFE
        JsonTokenType.NotStarted    // Invalid
    };

    private int cosmosBinaryFeatures;
    private final IOContext ioContext;
    private final ByteQuadsCanonicalizer symbols;
    private final boolean symbolsCanonical;

    private final JsonReadContext streamReadContext;

    private final ByteBuf rootBuffer;
    private ByteBuf remainingPayloadBuffer;
    private ByteBuf currentTokenBuffer;

    private final JsonObjectState jsonObjectState;

    /// <summary>
    /// For binary there is no end of token marker in the actual binary, but the JsonReader interface still needs to surface ObjectEndToken and ArrayEndToken.
    /// To accommodate for this we have a progress stack to let us know how many bytes there are left to read for all levels of nesting.
    /// With this information we know that we are at the end of a context and can now surface an end object / array token.
    /// </summary>
    //private readonly Stack<int> arrayAndObjectEndStack;
    private final ArrayAndObjectEndStack arrayAndObjectEndStack;

    private ObjectCodec codec;

    protected CosmosBinaryParser(
        ByteBuf buffer,
        IOContext ctxt,
        int parserFeatures,
        ByteQuadsCanonicalizer byteSymbolCanonicalizer,
        ObjectCodec codec)
    {
        super(parserFeatures);

        checkNotNull(ctxt, "Argument 'ctxt' must not be null");
        checkNotNull(codec, "Argument 'codec' must not be null");
        checkNotNull(byteSymbolCanonicalizer, "Argument 'byteSymbolCanonicalizer' must not be null");
        checkNotNull(buffer, "Argument 'buffer' must not be null");
        checkArgument(buffer.readableBytes() > 0, "Argument 'rootBuffer' must not be empty");

        this.cosmosBinaryFeatures = cosmosBinaryFeatures;
        this.ioContext = ctxt;
        this.codec = codec;
        this.symbols = byteSymbolCanonicalizer;
        this.symbolsCanonical = byteSymbolCanonicalizer.isCanonicalizing();
        DupDetector dups = Feature.STRICT_DUPLICATE_DETECTION.enabledIn(parserFeatures)
            ? DupDetector.rootDetector(this) : null;
        this.streamReadContext= JsonReadContext.createRootContext(dups);
        this.rootBuffer = buffer.asReadOnly();

        // Only navigate the outer most JSON value and trim off trailing bytes
        long jsonValueLength = JsonBinaryEncoding.getValueLength(this.rootBuffer.duplicate());
        checkArgument(
            this.rootBuffer.readableBytes() >= jsonValueLength,
            "Argument 'rootBuffer' is shorter than the length prefix.");
        this.remainingPayloadBuffer = this.rootBuffer.duplicate();
        this.currentTokenBuffer = this.rootBuffer.duplicate();
        this.arrayAndObjectEndStack = new ArrayAndObjectEndStack();
        this.jsonObjectState = new JsonObjectState(true, Configs.getMaxJsonBinaryNestingDepth());
        LOG.info("CosmosBinaryParser initialized");
    }

    @Override
    public JsonToken nextToken() throws IOException {
        // Check if we just finished an array or object context
        LOG.info("--> nextToken");
        if (!this.arrayAndObjectEndStack.isEmpty()
            && this.arrayAndObjectEndStack.peek() == this.remainingPayloadBuffer.readerIndex())
        {
            if (this.jsonObjectState.isInArrayContext())
            {
                this.jsonObjectState.registerEndArray();
            }
            else if (this.jsonObjectState.isInObjectContext())
            {
                this.jsonObjectState.registerEndObject();
            }
            else
            {
                throw new JsonInvalidTokenException();
            }

            this.arrayAndObjectEndStack.pop();
        }
        else if (this.remainingPayloadBuffer.readableBytes() == 0)
        {
            // Need to check if we are still inside an object or array
            if (this.jsonObjectState.getCurrentDepth() != 0)
            {
                if (this.jsonObjectState.isInObjectContext())
                {
                    throw new JsonMissingEndObjectException();
                }

                if (this.jsonObjectState.isInArrayContext())
                {
                    throw new JsonMissingEndArrayException();
                }

                throw new IllegalStateException("Expected to be in either array or object context");
            }

            LOG.info("<-- nextToken, null");
            this.currentTokenBuffer =  this.remainingPayloadBuffer = this.remainingPayloadBuffer.duplicate().setIndex(
                this.rootBuffer.readerIndex() + this.rootBuffer.readableBytes(),
                0
            );
            return _currToken = null;
        }
        else if (this.jsonObjectState.getCurrentDepth() == 0
            && this.jsonObjectState.getCurrentTokenType() != JsonTokenType.NotStarted)
        {
            // There are trailing characters outside the outermost object or array
            throw new JsonUnexpectedTokenException();
        }
        else
        {
            ByteBuf readOnlySpan = this.remainingPayloadBuffer.duplicate();

            byte typeMarker;
            int nextTokenOffset;

            UniformArrayInfo currentArrayInfo = this.arrayAndObjectEndStack.getUniformArrayInfo();
            if (currentArrayInfo != null) {
                typeMarker = currentArrayInfo.ItemTypeMarker;
                nextTokenOffset = currentArrayInfo.ItemSize;
            } else {
                typeMarker = readOnlySpan.getByte(readOnlySpan.readerIndex());
                nextTokenOffset = JsonBinaryEncoding.getValueLength(readOnlySpan.duplicate());
                readOnlySpan.skipBytes(1);
            }

            JsonTokenType tokenType = getJsonTokenType(typeMarker, currentArrayInfo);

            if (tokenType == JsonTokenType.String) {
                if (this.jsonObjectState.isPropertyExpected()) {
                    tokenType = JsonTokenType.FieldName;
                }
            } else if ((tokenType == JsonTokenType.BeginArray) || (tokenType == JsonTokenType.BeginObject)) {
                // If we are currently within a nested array, then the BeginArray token must be for
                // a uniform number array that is within a uniform array of number arrays.
                if (this.arrayAndObjectEndStack.isWithinUniformArray()) {
                    // ASSERT(tokenType == JsonTokenType.BeginArray);
                    this.arrayAndObjectEndStack.pushNestedArray(this.remainingPayloadBuffer.readerIndex());

                    nextTokenOffset = 0;
                } else {
                    // If this is the beginning of an array/object token then we need to identify where
                    // array/object end token is.
                    // Also, the next token offset is just the array type marker + length prefix + count prefix
                    UniformArrayInfo uniformArrayInfo =
                        JsonBinaryEncoding.getUniformArrayInfo(readOnlySpan.duplicate(), false);
                    this.arrayAndObjectEndStack.push(
                        this.remainingPayloadBuffer.readerIndex() + nextTokenOffset, uniformArrayInfo);

                    nextTokenOffset = JsonBinaryEncoding.getArrayOrObjectPrefixLength(typeMarker);
                }
            }

            this.jsonObjectState.registerToken(tokenType);
            if (nextTokenOffset > 0) {
                this.currentTokenBuffer =
                    this
                        .remainingPayloadBuffer
                        .duplicate()
                        .setIndex(
                            this.remainingPayloadBuffer.readerIndex(),
                            this.remainingPayloadBuffer.readerIndex() + nextTokenOffset);
                this.remainingPayloadBuffer = this.remainingPayloadBuffer.skipBytes(nextTokenOffset);
            } else {
                this.currentTokenBuffer = this.remainingPayloadBuffer.duplicate().setIndex(
                    this.rootBuffer.readerIndex() + this.rootBuffer.readableBytes(),
                    this.rootBuffer.readerIndex() + this.rootBuffer.readableBytes()
                );
            }
        }

        JsonToken jsonToken = this.jsonObjectState.getCurrentJsonToken(this.currentTokenBuffer.duplicate());
        LOG.info(
            "<-- nextToken: {}, remainingBuffer: {}/{}, currentTokenBuffer: {}/{}",
            jsonToken,
            this.remainingPayloadBuffer.readerIndex(),
            this.remainingPayloadBuffer.readableBytes(),
            this.currentTokenBuffer != null ? this.currentTokenBuffer.readerIndex() : "n/a",
            this.currentTokenBuffer != null ? this.currentTokenBuffer.readableBytes() : "n/a");
        return _currToken = jsonToken;
    }

    @Override
    protected void _handleEOF() throws JsonParseException {
        LOG.info("_handleEOF");
        if (this.jsonObjectState.getCurrentDepth() != 0) {
            throw new JsonParseException("HandleEOF");
        }
    }

    @Override
    public String getCurrentName() throws IOException {
        LOG.info("--> getCurrentName");

        String name = this.readStringValue();
        LOG.info("<-- [{}]", name);
        return name;
    }

    @Override
    public ObjectCodec getCodec() {
        return this.codec;
    }

    @Override
    public void setCodec(ObjectCodec objectCodec) {
        this.codec = objectCodec;
    }

    @Override
    public Version version() {
        return HttpConstants.Versions.getSdkVersionAsVersion();
    }

    @Override
    public void close() throws IOException {
        LOG.info("close");
    }

    @Override
    public boolean isClosed() {
        LOG.info("isClosed");
        return false;
    }

    @Override
    public JsonStreamContext getParsingContext() {
        LOG.info("getParsingContext");
        return null;
    }

    @Override
    public JsonLocation getCurrentLocation() {
        LOG.info("getCurrentLocation");
        return new JsonLocation(
            this.rootBuffer,
            this.rootBuffer.capacity(),
            1,
            this.currentTokenBuffer.readerIndex());
    }

    @Override
    public JsonLocation getTokenLocation() {
        LOG.info("getTokenLocation");
        return JsonLocation.NA;
    }

    @Override
    public void overrideCurrentName(String s) {
        LOG.info("overrideCurrentName");
    }

    @Override
    public String getText() throws IOException {
        LOG.info("--> getText");

        String textValue = this.readStringValue();
        LOG.info("<-- [{}]", textValue);
        return textValue;
    }

    @Override
    public char[] getTextCharacters() throws IOException {
        LOG.info("getTextCharacters");
        return new char[0];
    }

    @Override
    public boolean hasTextCharacters() {
        LOG.info("hasTextCharacters");
        return false;
    }

    @Override
    public Number getNumberValue() throws IOException {
        LOG.info("getNumberValue");
        return null;
    }

    private NumberType getNumberTypeCore() throws IOException {
        switch (this.jsonObjectState.getCurrentTokenType()) {
            case Number:
                Number64 numberValue = JsonBinaryEncoding.decodeNumberValue(this.currentTokenBuffer);
                if (numberValue.isInteger()) {
                    return NumberType.LONG;
                }

                return NumberType.DOUBLE;
            case Float32:
                return NumberType.FLOAT;
            case Float64:
                return NumberType.DOUBLE;
            case Int8:
            case Int16:
            case Int32:
            case UInt8:
                return NumberType.INT;
            case Int64:
            case UInt32:
                return NumberType.LONG;
            default:
                throw new JsonNotNumberTokenException();
        }
    }

    @Override
    public NumberType getNumberType() throws IOException {
        NumberType numberType = getNumberTypeCore();
        LOG.info("getNumberType --> {}", numberType);

        return numberType;
    }

    private int getIntValueCore() throws IOException {
        switch (this.jsonObjectState.getCurrentTokenType()) {
            case Number:
                return (int)Number64.toLong(JsonBinaryEncoding.decodeNumberValue(this.currentTokenBuffer));
            case Float32:
                return (int)JsonBinaryEncoding.decodeFloat32Value(this.currentTokenBuffer);
            case Float64:
                return (int)JsonBinaryEncoding.decodeFloat64Value(this.currentTokenBuffer);
            case Int8:
                return JsonBinaryEncoding.decodeInt8Value(this.currentTokenBuffer);
            case Int16:
                return JsonBinaryEncoding.decodeInt16Value(this.currentTokenBuffer);
            case Int32:
                return JsonBinaryEncoding.decodeInt32Value(this.currentTokenBuffer);
            case Int64:
                return (int)JsonBinaryEncoding.decodeInt64Value(this.currentTokenBuffer);
            case UInt8:
                return (short)(JsonBinaryEncoding.decodeInt8Value(this.currentTokenBuffer) & 0xff);
            case UInt32:
                return (int)JsonBinaryEncoding.decodeUInt32Value(this.currentTokenBuffer);
            default:
                throw new JsonNotNumberTokenException();
        }
    }

    @Override
    public int getIntValue() throws IOException {
        int value = getIntValueCore();
        LOG.info("getIntValue: {}", value);

        return  value;
    }

    private long getLongValueCore() throws IOException {
        switch (this.jsonObjectState.getCurrentTokenType()) {
            case Number:
                return Number64.toLong(JsonBinaryEncoding.decodeNumberValue(this.currentTokenBuffer));
            case Float32:
                return (long)JsonBinaryEncoding.decodeFloat32Value(this.currentTokenBuffer);
            case Float64:
                return (long)JsonBinaryEncoding.decodeFloat64Value(this.currentTokenBuffer);
            case Int8:
                return JsonBinaryEncoding.decodeInt8Value(this.currentTokenBuffer);
            case Int16:
                return JsonBinaryEncoding.decodeInt16Value(this.currentTokenBuffer);
            case Int32:
                return JsonBinaryEncoding.decodeInt32Value(this.currentTokenBuffer);
            case Int64:
                return JsonBinaryEncoding.decodeInt64Value(this.currentTokenBuffer);
            case UInt8:
                return (short)(JsonBinaryEncoding.decodeInt8Value(this.currentTokenBuffer) & 0xff);
            case UInt32:
                return JsonBinaryEncoding.decodeUInt32Value(this.currentTokenBuffer);
            default:
                throw new JsonNotNumberTokenException();
        }
    }

    @Override
    public long getLongValue() throws IOException {
        long value = getLongValueCore();
        LOG.info("getLongValue: {}", value);

        return value;
    }

    private float getFloatValueCore() throws IOException {
        switch (this.jsonObjectState.getCurrentTokenType()) {
            case Number:
                return (float)Number64.toDouble(JsonBinaryEncoding.decodeNumberValue(this.currentTokenBuffer));
            case Float32:
                return JsonBinaryEncoding.decodeFloat32Value(this.currentTokenBuffer);
            case Float64:
                return (float)JsonBinaryEncoding.decodeFloat64Value(this.currentTokenBuffer);
            case Int8:
                return JsonBinaryEncoding.decodeInt8Value(this.currentTokenBuffer);
            case Int16:
                return JsonBinaryEncoding.decodeInt16Value(this.currentTokenBuffer);
            case Int32:
                return JsonBinaryEncoding.decodeInt32Value(this.currentTokenBuffer);
            case Int64:
                return JsonBinaryEncoding.decodeInt64Value(this.currentTokenBuffer);
            case UInt8:
                return (short)(JsonBinaryEncoding.decodeInt8Value(this.currentTokenBuffer) & 0xff);
            case UInt32:
                return JsonBinaryEncoding.decodeUInt32Value(this.currentTokenBuffer);
            default:
                throw new JsonNotNumberTokenException();
        }
    }

    @Override
    public float getFloatValue() throws IOException {
        float value = getFloatValueCore();
        LOG.info("getFloatValue: {}", value);

        return value;
    }

    private double getDoubleValueCore() throws IOException {
        switch (this.jsonObjectState.getCurrentTokenType()) {
            case Number:
                return Number64.toDouble(JsonBinaryEncoding.decodeNumberValue(this.currentTokenBuffer));
            case Float32:
                return JsonBinaryEncoding.decodeFloat32Value(this.currentTokenBuffer);
            case Float64:
                return JsonBinaryEncoding.decodeFloat64Value(this.currentTokenBuffer);
            case Int8:
                return JsonBinaryEncoding.decodeInt8Value(this.currentTokenBuffer);
            case Int16:
                return JsonBinaryEncoding.decodeInt16Value(this.currentTokenBuffer);
            case Int32:
                return JsonBinaryEncoding.decodeInt32Value(this.currentTokenBuffer);
            case Int64:
                return JsonBinaryEncoding.decodeInt64Value(this.currentTokenBuffer);
            case UInt8:
                return (short)(JsonBinaryEncoding.decodeInt8Value(this.currentTokenBuffer) & 0xff);
            case UInt32:
                return JsonBinaryEncoding.decodeUInt32Value(this.currentTokenBuffer);
            default:
                throw new JsonNotNumberTokenException();
        }
    }

    @Override
    public double getDoubleValue() throws IOException {
        double value = getDoubleValueCore();
        LOG.info("getDoubleValue: {}", value);

        return value;
    }

    private BigInteger getBigIntegerValueCore() throws IOException {
        switch (this.jsonObjectState.getCurrentTokenType()) {
            case Number:
                return BigInteger.valueOf(
                    Number64.toLong(JsonBinaryEncoding.decodeNumberValue(this.currentTokenBuffer)));
            case Float32:
                return BigInteger.valueOf(
                    (long)JsonBinaryEncoding.decodeFloat32Value(this.currentTokenBuffer));
            case Float64:
                return BigInteger.valueOf(
                    (long)JsonBinaryEncoding.decodeFloat64Value(this.currentTokenBuffer));
            case Int8:
                return BigInteger.valueOf(
                    JsonBinaryEncoding.decodeInt8Value(this.currentTokenBuffer));
            case Int16:
                return BigInteger.valueOf(
                    JsonBinaryEncoding.decodeInt16Value(this.currentTokenBuffer));
            case Int32:
                return BigInteger.valueOf(
                    JsonBinaryEncoding.decodeInt32Value(this.currentTokenBuffer));
            case Int64:
                return BigInteger.valueOf(
                    JsonBinaryEncoding.decodeInt64Value(this.currentTokenBuffer));
            case UInt8:
                return BigInteger.valueOf(
                    (short)(JsonBinaryEncoding.decodeInt8Value(this.currentTokenBuffer) & 0xff));
            case UInt32:
                return BigInteger.valueOf(
                    JsonBinaryEncoding.decodeUInt32Value(this.currentTokenBuffer));
            default:
                throw new JsonNotNumberTokenException();
        }
    }

    @Override
    public BigInteger getBigIntegerValue()throws IOException {
        BigInteger value = getBigIntegerValueCore();
        LOG.info("getBigIntegerValue: {}", value);

        return value;
    }

    private BigDecimal getDecimalValueCore() throws IOException {
        switch (this.jsonObjectState.getCurrentTokenType()) {
            case Number:
                return BigDecimal.valueOf(Number64.toDouble(JsonBinaryEncoding.decodeNumberValue(this.currentTokenBuffer)));
            case Float32:
                return BigDecimal.valueOf(JsonBinaryEncoding.decodeFloat32Value(this.currentTokenBuffer));
            case Float64:
                return BigDecimal.valueOf(JsonBinaryEncoding.decodeFloat64Value(this.currentTokenBuffer));
            case Int8:
                return BigDecimal.valueOf(JsonBinaryEncoding.decodeInt8Value(this.currentTokenBuffer));
            case Int16:
                return BigDecimal.valueOf(JsonBinaryEncoding.decodeInt16Value(this.currentTokenBuffer));
            case Int32:
                return BigDecimal.valueOf(JsonBinaryEncoding.decodeInt32Value(this.currentTokenBuffer));
            case Int64:
                return BigDecimal.valueOf(JsonBinaryEncoding.decodeInt64Value(this.currentTokenBuffer));
            case UInt8:
                return BigDecimal.valueOf((short)(JsonBinaryEncoding.decodeInt8Value(this.currentTokenBuffer) & 0xff));
            case UInt32:
                return BigDecimal.valueOf(JsonBinaryEncoding.decodeUInt32Value(this.currentTokenBuffer));
            default:
                throw new JsonNotNumberTokenException();
        }
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException {
        BigDecimal value = getDecimalValueCore();
        LOG.info("getDecimalValue: {}", value);

        return value;
    }

    @Override
    public int getTextLength() throws IOException {
        LOG.info("getTextLength");
        return 0;
    }

    @Override
    public int getTextOffset() throws IOException {
        LOG.info("getTextOffset");
        return 0;
    }

    @Override
    public byte[] getBinaryValue(Base64Variant base64Variant) throws IOException {
        LOG.info("getBinaryValue");
        return new byte[0];
    }

    private static JsonTokenType getJsonTokenType(byte typeMarker, UniformArrayInfo arrayInfo) throws JsonParseException {
        if (arrayInfo != null)
        {
            switch (arrayInfo.ItemTypeMarker)
            {
                case TypeMarker.Int8:
                case TypeMarker.NumberUInt8:
                case TypeMarker.Int16:
                case TypeMarker.Int32:
                case TypeMarker.Int64:
                case TypeMarker.Float32:
                case TypeMarker.Float64:
                    return JsonTokenType.Number;

                case TypeMarker.ArrNumC1:
                case TypeMarker.ArrNumC2:
                case TypeMarker.ArrArrNumC1C1:
                case TypeMarker.ArrArrNumC2C2:
                    return JsonTokenType.BeginArray;

                default:
                    throw new JsonInvalidTokenException();
            }
        }

        short unsignedTypeMarker = (short)(typeMarker & 0xff);
        JsonTokenType tokenType = TypeMarkerToTokenType[unsignedTypeMarker];
        if (tokenType == JsonTokenType.NotStarted)
        {
            throw new JsonInvalidTokenException();
        }

        return tokenType;
    }

    private String readStringValue() throws JsonParseException {
        if (!(
            (this.jsonObjectState.getCurrentTokenType() == JsonTokenType.String) ||
                (this.jsonObjectState.getCurrentTokenType() == JsonTokenType.FieldName)))
        {
            throw new JsonInvalidTokenException();
        }

        byte typeMarker = this.currentTokenBuffer.getByte(this.currentTokenBuffer.readerIndex());
        LOG.info("--> readStringValue: {} - {}/{}",
            typeMarker,
            this.currentTokenBuffer.readerIndex(),
            this.currentTokenBuffer.writableBytes());

        if (TypeMarker.isBufferedStringCandidate(typeMarker))
        {
            TryResult<String> tryResult = JsonBinaryEncoding.tryGetBufferedStringValue(
                this.rootBuffer.duplicate(),
                this.currentTokenBuffer.duplicate());
            if (!tryResult.isSuccess())
            {
                throw new JsonInvalidTokenException();
            }

            return tryResult.getResult();
        }

        if (TypeMarker.IsCompressedString(typeMarker) || TypeMarker.IsGuidString(typeMarker))
        {
            String decodedStringValue = JsonBinaryEncoding.decodeString(this.currentTokenBuffer);
            LOG.info("Decoded String: {}", decodedStringValue);
            return decodedStringValue;
        }

        throw new JsonInvalidTokenException();
    }
}
