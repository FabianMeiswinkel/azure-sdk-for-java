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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkArgument;
import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public class CosmosBinaryParser extends ParserMinimalBase {

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

    /// <summary>
    /// Buffer to read from.
    /// </summary>
    private final JsonBinaryMemoryReader jsonBinaryBuffer;

    private final ByteBuf rootBuffer;

    private final JsonObjectState jsonObjectState;

    /// <summary>
    /// For binary there is no end of token marker in the actual binary, but the JsonReader interface still needs to surface ObjectEndToken and ArrayEndToken.
    /// To accommodate for this we have a progress stack to let us know how many bytes there are left to read for all levels of nesting.
    /// With this information we know that we are at the end of a context and can now surface an end object / array token.
    /// </summary>
    //private readonly Stack<int> arrayAndObjectEndStack;
    private final ArrayAndObjectEndStack arrayAndObjectEndStack;

    protected CosmosBinaryParser(
        ByteBuf rootBuffer,
        IOContext ctxt,
        int parserFeatures,
        ByteQuadsCanonicalizer byteSymbolCanonicalizer)
    {
        super(parserFeatures);

        checkNotNull(ctxt, "Argument 'ctxt' must not be null");
        checkNotNull(byteSymbolCanonicalizer, "Argument 'byteSymbolCanonicalizer' must not be null");
        checkNotNull(rootBuffer, "Argument 'rootBuffer' must not be null");
        checkArgument(rootBuffer.readableBytes() > 0, "Argument 'rootBuffer' must not be empty");

        this.cosmosBinaryFeatures = cosmosBinaryFeatures;
        this.ioContext = ctxt;
        this.symbols = byteSymbolCanonicalizer;
        this.symbolsCanonical = byteSymbolCanonicalizer.isCanonicalizing();
        DupDetector dups = Feature.STRICT_DUPLICATE_DETECTION.enabledIn(parserFeatures)
            ? DupDetector.rootDetector(this) : null;
        this.streamReadContext= JsonReadContext.createRootContext(dups);
        this.rootBuffer = rootBuffer;

        // Only navigate the outer most JSON value and trim off trailing bytes
        long jsonValueLength = JsonBinaryEncoding.getValueLength(rootBuffer);
        checkArgument(
            rootBuffer.readableBytes() >= jsonValueLength,
            "Argument 'rootBuffer' is shorter than the length prefix.");
        this.jsonBinaryBuffer = new JsonBinaryMemoryReader(this.rootBuffer);
        this.arrayAndObjectEndStack = new ArrayAndObjectEndStack();
        this.jsonObjectState = new JsonObjectState(true, Configs.getMaxJsonBinaryNestingDepth());
    }

    @Override
    public JsonToken nextToken() throws IOException {
        // Check if we just finished an array or object context
        if (!this.arrayAndObjectEndStack.isEmpty()
            && this.arrayAndObjectEndStack.peek() == this.jsonBinaryBuffer.getPosition())
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
        else if (this.jsonBinaryBuffer.isEof())
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

            return null;
        }
        else if (this.jsonObjectState.getCurrentDepth() == 0
            && this.jsonObjectState.getCurrentTokenType() != JsonTokenType.NotStarted)
        {
            // There are trailing characters outside the outermost object or array
            throw new JsonUnexpectedTokenException();
        }
        else
        {
            ByteBuf readOnlySpan = this.jsonBinaryBuffer.getRetainedBufferedRawJsonToken();

            try {
                byte typeMarker;
                int nextTokenOffset;

                UniformArrayInfo currentArrayInfo = this.arrayAndObjectEndStack.getUniformArrayInfo();
                if (currentArrayInfo != null) {
                    typeMarker = currentArrayInfo.ItemTypeMarker;
                    nextTokenOffset = currentArrayInfo.ItemSize;
                } else {
                    typeMarker = readOnlySpan.getByte(readOnlySpan.readerIndex());
                    nextTokenOffset = JsonBinaryEncoding.getValueLength(readOnlySpan);
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
                        this.arrayAndObjectEndStack.pushNestedArray(this.jsonBinaryBuffer.getPosition());

                        nextTokenOffset = 0;
                    } else {
                        // If this is the beginning of an array/object token then we need to identify where
                        // array/object end token is.
                        // Also, the next token offset is just the array type marker + length prefix + count prefix
                        UniformArrayInfo uniformArrayInfo =
                            JsonBinaryEncoding.getUniformArrayInfo(readOnlySpan, false);
                        this.arrayAndObjectEndStack.push(
                            this.jsonBinaryBuffer.getPosition() + nextTokenOffset, uniformArrayInfo);

                        nextTokenOffset = JsonBinaryEncoding.getArrayOrObjectPrefixLength(typeMarker);
                    }
                }

                this.jsonObjectState.registerToken(tokenType);
                if (nextTokenOffset > 0) {
                    this.jsonBinaryBuffer.skipBytes(nextTokenOffset);
                }
            }
            finally {
                readOnlySpan.release();
            }
        }

        // fix me
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
}
