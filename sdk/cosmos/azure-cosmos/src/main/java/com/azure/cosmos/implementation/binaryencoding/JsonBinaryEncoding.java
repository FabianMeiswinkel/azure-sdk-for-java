// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.azure.cosmos.implementation.apachecommons.lang.NotImplementedException;
import com.fasterxml.jackson.core.JsonParseException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.BitSet;
import java.util.EnumSet;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkArgument;
import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public class JsonBinaryEncoding {
    private final static Logger LOG = LoggerFactory.getLogger(JsonBinaryEncoding.class);
    private static final int DEFAULT_TINY_CACHE_SIZE = 0;
    private static final long UINT32_MAX_VALUE = 4294967295L;
    static final PooledByteBufAllocator allocator = new PooledByteBufAllocator(
        false,
        PooledByteBufAllocator.defaultNumHeapArena(),
        PooledByteBufAllocator.defaultNumDirectArena(),
        PooledByteBufAllocator.defaultPageSize(),
        PooledByteBufAllocator.defaultMaxOrder(),
        DEFAULT_TINY_CACHE_SIZE,
        PooledByteBufAllocator.defaultSmallCacheSize(),
        PooledByteBufAllocator.defaultNormalCacheSize(),
        PooledByteBufAllocator.defaultUseCacheForAllThreads()
    );

    /// <summary>
    /// A type marker is a single byte.
    /// </summary>
    public final static int TypeMarkerLength = 1;

    /// <summary>
    /// Some type markers are followed by a single byte representing the length.
    /// </summary>
    public final static int OneByteLength = 1;

    /// <summary>
    /// Some type markers are followed by 1 byte for the length and then optionally 1 byte for the count.
    /// </summary>
    public final static int OneByteCount = 1;

    /// <summary>
    /// Some type markers are followed by 2 bytes representing the length as a ushort.
    /// </summary>
    public final static int TwoByteLength = 2;

    /// <summary>
    /// Some type markers are followed by 2 bytes for the length and then optionally 2 bytes for the count (both are ushorts).
    /// </summary>
    public final static int TwoByteCount = 2;

    /// <summary>
    /// Some type markers are followed by 4 bytes for representing the length as a uint32.
    /// </summary>
    public final static int FourByteLength = 4;

    /// <summary>
    /// Some type markers are followed by 4 bytes for the length and then optionally 4 bytes for the count (both are uint32).
    /// </summary>
    public final static int FourByteCount = 4;

    /// <summary>
    /// For compressed strings we use a single byte base character.
    /// </summary>
    public final static int OneByteBaseChar = 1;

    /// <summary>
    /// Reference strings are followed by an offset; this is for 1 byte offset reference strings.
    /// </summary>
    public final static int OneByteOffset = 1;

    /// <summary>
    /// Reference strings are followed by an offset; this is for 2 byte offset reference strings.
    /// </summary>
    public final static int TwoByteOffset = 2;

    /// <summary>
    /// Reference strings are followed by an offset; this is for 2 byte offset reference strings.
    /// </summary>
    public final static int ThreeByteOffset = 3;

    /// <summary>
    /// Reference strings are followed by an offset; this is for 4 byte offset reference strings.
    /// </summary>
    public final static int FourByteOffset = 4;

    public static final int GuidLength = 36;
    public static final int GuidWithQuotesLength = GuidLength + 2;
    public static final int EncodedGuidLength = 17;

    private static final int MaxStackAlloc = 4 * 1024;
    private static final int Min4BitCharSetStringLength = 16;
    private static final int MinCompressedStringLength4 = 24;
    private static final int MinCompressedStringLength5 = 32;
    private static final int MinCompressedStringLength6 = 40;
    private static final int MinCompressedStringLength7 = 88;
    private static final int MinCompressedStringLength = Min4BitCharSetStringLength;

    private enum EncodeGuidParseFlags {
        None(0x0),
        LowerCase(0x1),
        UpperCase(0x2),
        Invalid(0xFF);

        public final int value;

        private EncodeGuidParseFlags(int value) {
            this.value = value;
        }
    }

    private static int getFixedSizedValueAsUInt16(ByteBuf buffer)
    {
        return buffer.readUnsignedShortLE();
    }

    private static short getFixedSizedValueAsUByte(ByteBuf buffer)
    {
         return (short)(buffer.readByte() & 0xFF);
    }

    private static int getEncodedStringValueLength(ByteBuf stringToken)
    {
        byte typeMarker = stringToken.getByte(stringToken.readerIndex());

        switch (typeMarker)
        {
            case TypeMarker.CompressedLowercaseHexString:
            case TypeMarker.CompressedUppercaseHexString:
            case TypeMarker.CompressedDateTimeString:
            case TypeMarker.Packed4BitString:
            case TypeMarker.Packed5BitString:
            case TypeMarker.Packed6BitString:
            case TypeMarker.Packed7BitStringLength1:
                return stringToken.getByte(1);

            case TypeMarker.Packed7BitStringLength2:
                return getFixedSizedValueAsUInt16(stringToken.slice(stringToken.readerIndex() + 1, 2));

            case TypeMarker.LowercaseGuidString:
            case TypeMarker.UppercaseGuidString:
                return GuidLength;

            case TypeMarker.DoubleQuotedLowercaseGuidString:
                return GuidWithQuotesLength;

            default:
                throw getIllegalTypeMarkerException(typeMarker);
        }
    }

    public static boolean tryEncodeGuidString(ByteBuf guidString, ByteBuf destinationBuffer)
    {
        checkNotNull(guidString, "Parameter 'guidString' MUST NOT be null.");
        checkNotNull(destinationBuffer, "Parameter 'destinationBuffer' MUST NOT be null.");

        if (guidString.readableBytes() < GuidLength)
        {
            return false;
        }

        if (destinationBuffer.readableBytes() < EncodedGuidLength)
        {
            return false;
        }

        EnumSet<EncodeGuidParseFlags> flags = EnumSet.of(EncodeGuidParseFlags.None);
        ByteBuf writePointer = destinationBuffer.writerIndex(1);

        int dashIndex = 8;
        int oddEven = 0;

        for (int index = 0; index < GuidLength; index++)
        {
            char c = (char)guidString.getByte(index);

            if ((index == dashIndex) && (index <= 23))
            {
                if (c != '-')
                {
                    flags = EnumSet.of(EncodeGuidParseFlags.Invalid);
                    break;
                }

                dashIndex += 5;
                oddEven = oddEven == 0 ? 1 : 0;
                continue;
            }

            byte value;
            if ((c >= '0') && (c <= '9'))
            {
                value = (byte)(c - '0');
            }
            else if ((c >= 'a') && (c <= 'f'))
            {
                value = (byte)(10 + c - 'a');
                flags.add(EncodeGuidParseFlags.LowerCase);
            }
            else if ((c >= 'A') && (c <= 'F'))
            {
                value = (byte)(10 + c - 'A');
                flags.add(EncodeGuidParseFlags.UpperCase);
            }
            else
            {
                flags = EnumSet.of(EncodeGuidParseFlags.Invalid);
                break;
            }

            if ((index % 2) == oddEven)
            {
                writePointer.setByte(0, value);
            }
            else
            {
                writePointer.setByte(0, (byte)(writePointer.getByte(0) | (value << 4)));
                writePointer = writePointer.writerIndex(1);
            }
        }

        // Set the type marker
        if (flags.equals(EnumSet.of(EncodeGuidParseFlags.None)) || flags.equals(EnumSet.of(EncodeGuidParseFlags.LowerCase)))
        {
            destinationBuffer.setByte(0, TypeMarker.LowercaseGuidString);
        }
        else if (flags.equals(EnumSet.of(EncodeGuidParseFlags.UpperCase)))
        {
            destinationBuffer.setByte(0, TypeMarker.UppercaseGuidString);
        }
        else
        {
            return false;
        }

        return true;
    }

    private static IllegalArgumentException getIllegalTypeMarkerException(byte typeMarker) {
        return new IllegalArgumentException("Invalid type marker: " + typeMarker + ".");
    }

    private static int getEncodedStringBufferLength(ByteBuf stringToken) {
        byte typeMarker = stringToken.getByte(stringToken.readerIndex());

        switch (typeMarker) {
            case TypeMarker.CompressedLowercaseHexString:
            case TypeMarker.CompressedUppercaseHexString:
            case TypeMarker.CompressedDateTimeString:
            case TypeMarker.Packed4BitString:
                return getCompressedStringLength(stringToken.getByte(stringToken.readerIndex() + 1), 4);

            case TypeMarker.Packed5BitString:
                return getCompressedStringLength(stringToken.getByte(stringToken.readerIndex() + 1), 5);

            case TypeMarker.Packed6BitString:
                return getCompressedStringLength(stringToken.getByte(stringToken.readerIndex() + 1), 6);

            case TypeMarker.Packed7BitStringLength1:
                return getCompressedStringLength(stringToken.getByte(stringToken.readerIndex() + 1), 7);

            case TypeMarker.Packed7BitStringLength2:
                return getCompressedStringLength(
                    getFixedSizedValueAsUInt16(stringToken.slice(stringToken.readerIndex() + 1, 2)),
                    7);

            case TypeMarker.LowercaseGuidString:
            case TypeMarker.UppercaseGuidString:
            case TypeMarker.DoubleQuotedLowercaseGuidString:
                return 16;

            default:
                throw getIllegalTypeMarkerException(typeMarker);
        }
    }

    private static byte getEncodedStringBaseChar(ByteBuf stringToken)
    {
        byte typeMarker = stringToken.getByte(stringToken.readerIndex());

        switch (typeMarker)
        {
            case TypeMarker.CompressedLowercaseHexString:
            case TypeMarker.CompressedUppercaseHexString:
            case TypeMarker.CompressedDateTimeString:
            case TypeMarker.Packed7BitStringLength1:
            case TypeMarker.Packed7BitStringLength2:
            case TypeMarker.LowercaseGuidString:
            case TypeMarker.UppercaseGuidString:
            case TypeMarker.DoubleQuotedLowercaseGuidString:
                return 0;

            case TypeMarker.Packed4BitString:
            case TypeMarker.Packed5BitString:
            case TypeMarker.Packed6BitString:
                return stringToken.getByte(stringToken.readerIndex() + 2);

            default:
                throw getIllegalTypeMarkerException(typeMarker);
        }
    }

    public static String decodeString(ByteBuf stringToken) throws JsonParseException {
        checkNotNull(stringToken, "Parameter 'stringToken' MUST NOT be null.");
        byte typeMarker = stringToken.getByte(stringToken.readerIndex());

        boolean isHexadecimalString = TypeMarker.IsHexadecimalString(typeMarker);
        boolean isDateTimeString = TypeMarker.IsDateTimeString(typeMarker);
        boolean isCompressedString = TypeMarker.IsCompressedString(typeMarker);
        boolean isGuidString = TypeMarker.IsGuidString(typeMarker);

        checkArgument(
            isHexadecimalString || isDateTimeString || isCompressedString || isGuidString,
            "token must be a hex, datetime, compressed, or guid string.");

        int lengthByteCount = (isHexadecimalString || isDateTimeString)
            ? 1
            : (isCompressedString ? ((typeMarker == TypeMarker.Packed7BitStringLength2) ? 2 : 1) : 0);
        int baseCharByteCount = TypeMarker.InRange(
            typeMarker, TypeMarker.Packed4BitString, 
            TypeMarker.Packed6BitString + 1)
            ? 1
            : 0;
        int prefixByteCount = TypeMarkerLength + lengthByteCount + baseCharByteCount;

        if (stringToken.readableBytes() < prefixByteCount)
        {
            throw new IllegalStateException(
                "Parameter stringToken must have at least a length of " + prefixByteCount + " bytes.");
        }

        int bytesWritten = getEncodedStringValueLength(stringToken);
        int encodedLength = getEncodedStringBufferLength(stringToken);
        byte baseChar = getEncodedStringBaseChar(stringToken);

        if (stringToken.readableBytes() < (prefixByteCount + encodedLength))
        {
            throw new JsonInvalidTokenException();
        }


        if (bytesWritten == 0) {
            return "";
        }

        ByteBuf destinationBuffer = Unpooled.wrappedBuffer(new byte[bytesWritten]).setIndex(0, 0);
        ByteBuf encodedString = stringToken.slice(stringToken.readerIndex() + prefixByteCount, encodedLength);
        decodeStringValue(typeMarker, encodedString, baseChar, destinationBuffer);
        destinationBuffer.setIndex(0, bytesWritten);
        Utf8ByteBuffer decodedStringValue = Utf8ByteBuffer.create(destinationBuffer.asReadOnly());
        return decodedStringValue.toString();
    }

    public static long byteArrayToLong(byte[] byteArray) {
        checkArgument(byteArray.length == 8, "Parameter 'byteArray' must have exactly 64 bits.");

        long value = 0;
        for (int i = 0; i < 8; i++) {
            value <<= 8;
            value |= (byteArray[i] & 0xFFL);
        }

        return value;
    }

    private static void DecodeCompressedStringValue(
        int numberOfBits,
        ByteBuf encodedString,
        byte baseChar,
        ByteBuf destinationBuffer)
    {
        checkNotNull(encodedString, "Parameter 'encodedString' MUST NOT be null.");
        checkArgument(
            encodedString.readerIndex() == 0,
            "Parameter 'encodedString.readerIndex()' MUST be 0.");
        checkNotNull(destinationBuffer, "Parameter 'destinationBuffer' MUST NOT be null.");

        checkArgument(
            numberOfBits >= 0 && numberOfBits <= 8,
            "Invalid number of bits.");

        long mask = 0x000000FF >> (8 - numberOfBits);
        int index = 0;
        long packedValue = 0;
        int iterations = destinationBuffer.writableBytes() / 8 * 8;
        int inputStart = 0;
        int outputStart = 0;
        for (; index < iterations; index += 8)
        {
            if (encodedString.readableBytes() - inputStart >= 8) {
                packedValue = encodedString.getLongLE(inputStart) & 0x00FFFFFFFFFFFFFFL;
            } else {
                byte[] temp = new byte[8];
                for (int i = 0; i < encodedString.readableBytes() - inputStart; i++) {
                    temp[7-i] = encodedString.getByte(inputStart + i);
                }

                packedValue = byteArrayToLong(temp);
            }

            destinationBuffer.setByte(outputStart + 0, (byte)(((byte)(packedValue & mask)) + baseChar));
            packedValue >>= numberOfBits;

            destinationBuffer.setByte(outputStart + 1, (byte)(((byte)(packedValue & mask)) + baseChar));
            packedValue >>= numberOfBits;

            destinationBuffer.setByte(outputStart + 2, (byte)(((byte)(packedValue & mask)) + baseChar));
            packedValue >>= numberOfBits;

            destinationBuffer.setByte(outputStart + 3, (byte)(((byte)(packedValue & mask)) + baseChar));
            packedValue >>= numberOfBits;

            destinationBuffer.setByte(outputStart + 4, (byte)(((byte)(packedValue & mask)) + baseChar));
            packedValue >>= numberOfBits;

            destinationBuffer.setByte(outputStart + 5, (byte)(((byte)(packedValue & mask)) + baseChar));
            packedValue >>= numberOfBits;

            destinationBuffer.setByte(outputStart + 6, (byte)(((byte)(packedValue & mask)) + baseChar));
            packedValue >>= numberOfBits;

            destinationBuffer.setByte(outputStart + 7, (byte)(((byte)(packedValue & mask)) + baseChar));
            packedValue >>= numberOfBits;

            if (packedValue != 0)
            {
                throw new IllegalStateException("Variable 'packedValue' must be 0 at this point.");
            }

            inputStart += numberOfBits;
            outputStart += 8;
        }

        if (outputStart < destinationBuffer.writableBytes())
        {
            ByteBuf paddedString = ByteBufAllocator.DEFAULT.buffer(8);
            ByteBuf decodedPaddedString = ByteBufAllocator.DEFAULT.buffer(8);
            paddedString.setBytes(0, encodedString, inputStart, encodedString.readableBytes() - inputStart);
            paddedString.setIndex(0, 8);
            DecodeCompressedStringValue(numberOfBits, paddedString, baseChar, decodedPaddedString);
            decodedPaddedString.setIndex(0, 8);
            destinationBuffer.setBytes(
                outputStart,
                decodedPaddedString,
                Math.min(destinationBuffer.writableBytes() - outputStart, decodedPaddedString.capacity()));
        }
    }

    private static void decodeStringValue(byte typeMarker, ByteBuf encodedString, byte baseChar, ByteBuf destinationBuffer)
    {
        checkNotNull(encodedString, "Parameter 'encodedString' MUST NOT be null.");
        checkNotNull(destinationBuffer, "Parameter 'destinationBuffer' MUST NOT be null.");

        switch (typeMarker)
        {
            case TypeMarker.CompressedLowercaseHexString:
                checkArgument(baseChar == 0, "base char needs to be 0.");

                Decode4BitCharacterStringValue(StringCompressionLookupTables.LowercaseHex, encodedString, destinationBuffer);
                break;

            case TypeMarker.CompressedUppercaseHexString:
                checkArgument(baseChar == 0, "base char needs to be 0.");

                Decode4BitCharacterStringValue(StringCompressionLookupTables.UppercaseHex, encodedString, destinationBuffer);
                break;

            case TypeMarker.CompressedDateTimeString:
                checkArgument(baseChar == 0, "base char needs to be 0.");

                Decode4BitCharacterStringValue(StringCompressionLookupTables.DateTime, encodedString, destinationBuffer);
                break;

            case TypeMarker.Packed4BitString:
                DecodeCompressedStringValue(4, encodedString, baseChar, destinationBuffer);
                break;

            case TypeMarker.Packed5BitString:
                DecodeCompressedStringValue(5, encodedString, baseChar, destinationBuffer);
                break;

            case TypeMarker.Packed6BitString:
                DecodeCompressedStringValue( 6, encodedString, baseChar, destinationBuffer);
                break;

            case TypeMarker.Packed7BitStringLength1:
            case TypeMarker.Packed7BitStringLength2:
                checkArgument(baseChar == 0, "base char needs to be 0.");

                DecodeCompressedStringValue(7, encodedString, baseChar, destinationBuffer);
                break;

            case TypeMarker.LowercaseGuidString:
            case TypeMarker.UppercaseGuidString:
                DecodeGuidStringValue(encodedString, typeMarker == TypeMarker.UppercaseGuidString, destinationBuffer);
                break;

            case TypeMarker.DoubleQuotedLowercaseGuidString:
                destinationBuffer.setChar(0, '"');
                DecodeGuidStringValue(encodedString, false, destinationBuffer.slice(1, destinationBuffer.writableBytes() - 2));
                destinationBuffer.setChar(GuidWithQuotesLength - 1, '"');
                break;

            default:
                throw new IllegalStateException("Invalid token " + typeMarker);
        }
    }

    private static void Decode4BitCharacterStringValue(
        StringCompressionLookupTables chars,
        ByteBuf encodedString,
        ByteBuf destinationBuffer)
    {
        checkNotNull(chars, "Parameter 'chars' MUST NOT be null.");
        checkNotNull(encodedString, "Parameter 'encodedString' MUST NOT be null.");
        checkNotNull(destinationBuffer, "Parameter 'destinationBuffer' MUST NOT be null.");
        checkArgument(
            encodedString.readableBytes() ==
                JsonBinaryEncoding.getCompressedStringLength(destinationBuffer.readableBytes(), 4),
            "Destination buffer is too small.");

        for (int index = 0; index < destinationBuffer.writableBytes() / 2; index++)
        {
            short value = chars.getByteToTwoChars()[encodedString.getUnsignedByte(index)];
            destinationBuffer.setShort(index * 2, value);
        }

        if ((destinationBuffer.writableBytes() % 2) == 1)
        {
            if (encodedString.getUnsignedByte(encodedString.readableBytes() - 1) > 0x0F)
            {
                throw new IllegalStateException();
            }

            destinationBuffer.setByte(
                destinationBuffer.writableBytes() - 1,
                (byte)chars.getList().charAt(encodedString.getUnsignedByte(encodedString.readableBytes() -1)));
        }
    }

    private static void DecodeGuidStringValue(ByteBuf encodedString, boolean isUpperCaseGuid, ByteBuf destinationBuffer)
    {
        short[] byteLookupTable = isUpperCaseGuid
            ? StringCompressionLookupTables.UppercaseHexLittleEndian.getByteToTwoChars()
            : StringCompressionLookupTables.LowercaseHexLittleEndian.getByteToTwoChars();

        // GUID Format: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
        destinationBuffer.setShort(0, byteLookupTable[encodedString.getUnsignedByte(0)]);
        destinationBuffer.setShort(2, byteLookupTable[encodedString.getUnsignedByte(1)]);
        destinationBuffer.setShort(4, byteLookupTable[encodedString.getUnsignedByte(2)]);
        destinationBuffer.setShort(6, byteLookupTable[encodedString.getUnsignedByte(3)]);
        destinationBuffer.setByte(8, (byte)'-');
        destinationBuffer.setShort(9, byteLookupTable[encodedString.getUnsignedByte(4)]);
        destinationBuffer.setShort(11, byteLookupTable[encodedString.getUnsignedByte(5)]);
        destinationBuffer.setByte(13, (byte)'-');
        destinationBuffer.setShort(14, byteLookupTable[encodedString.getUnsignedByte(6)]);
        destinationBuffer.setShort(16, byteLookupTable[encodedString.getUnsignedByte(7)]);
        destinationBuffer.setByte(18, (byte)'-');
        destinationBuffer.setShort(19, byteLookupTable[encodedString.getUnsignedByte(8)]);
        destinationBuffer.setShort(21, byteLookupTable[encodedString.getUnsignedByte(9)]);
        destinationBuffer.setByte(23, (byte)'-');
        destinationBuffer.setShort(24, byteLookupTable[encodedString.getUnsignedByte(10)]);
        destinationBuffer.setShort(26, byteLookupTable[encodedString.getUnsignedByte(11)]);
        destinationBuffer.setShort(28, byteLookupTable[encodedString.getUnsignedByte(12)]);
        destinationBuffer.setShort(30, byteLookupTable[encodedString.getUnsignedByte(13)]);
        destinationBuffer.setShort(32, byteLookupTable[encodedString.getUnsignedByte(14)]);
        destinationBuffer.setShort(34, byteLookupTable[encodedString.getUnsignedByte(15)]);
    }

    /// <summary>
    /// Try Get NumberValue
    /// </summary>
    /// <param name="numberToken">The buffer.</param>
    /// <param name="number64">The number.</param>
    /// <param name="bytesConsumed">The number of bytes consumed</param>
    /// <returns>Whether a number was parsed.</returns>
    public static TryBiResult<Number64, Integer> tryDecodeNumberValue(ByteBuf numberToken) throws JsonParseException {
        Number64 number64 = null;
        int bytesConsumed = 0;

        if (numberToken.readableBytes() == 0)
        {
            return TryBiResult.failed(Number64.class, Integer.class);
        }

        byte typeMarker = numberToken.getByte(numberToken.readerIndex());

        if (TypeMarker.IsEncodedNumberLiteral(typeMarker))
        {
            number64 = new Number64(typeMarker - TypeMarker.LiteralIntMin);
            bytesConsumed = 1;
        }
        else
        {
            switch (typeMarker)
            {
                case TypeMarker.NumberUInt8:
                    if (numberToken.readableBytes() < (1 + 1))
                    {
                        return TryBiResult.failed(Number64.class, Integer.class);
                    }

                    number64 = new Number64(numberToken.getByte(numberToken.readerIndex() + 1));
                    bytesConsumed = 1 + 1;
                    break;

                case TypeMarker.NumberInt16:
                    if (numberToken.readableBytes() < (1 + 2))
                    {
                        return TryBiResult.failed(Number64.class, Integer.class);
                    }

                    number64 = new Number64(numberToken.getShortLE(numberToken.readerIndex() + 1));
                    bytesConsumed = 1 + 2;
                    break;

                case TypeMarker.NumberInt32:
                    if (numberToken.readableBytes() < (1 + 4))
                    {
                        return TryBiResult.failed(Number64.class, Integer.class);
                    }

                    number64 = new Number64(numberToken.getIntLE(numberToken.readerIndex() + 1));
                    bytesConsumed = 1 + 4;
                    break;

                case TypeMarker.NumberInt64:
                    if (numberToken.readableBytes() < (1 + 8))
                    {
                        return TryBiResult.failed(Number64.class, Integer.class);
                    }

                    number64 = new Number64(numberToken.getLongLE(numberToken.readerIndex() + 1));
                    bytesConsumed = 1 + 8;
                    break;

                case TypeMarker.NumberDouble:
                    if (numberToken.readableBytes() < (1 + 8))
                    {
                        return TryBiResult.failed(Number64.class, Integer.class);
                    }

                    number64 = new Number64(numberToken.getDoubleLE(numberToken.readerIndex() + 1));
                    bytesConsumed = 1 + 8;
                    break;

                default:
                    return TryBiResult.failed(Number64.class, Integer.class);
            }
        }

        return TryBiResult.success(number64, bytesConsumed);
    }

    /// <summary>
    /// Gets the number value from the binary reader.
    /// </summary>
    /// <param name="numberToken">The buffer to read the number from.</param>
    /// <returns>The number value from the binary reader.</returns>
    public static Number64 decodeNumberValue(ByteBuf numberToken) throws JsonParseException {
        TryBiResult<Number64, Integer> result = JsonBinaryEncoding.tryDecodeNumberValue(numberToken);
        if (!result.isSuccess())
        {
            throw new JsonNotNumberTokenException();
        }

        return result.getResultA();
    }

    @SuppressWarnings("unchecked")
    private static <T> TryBiResult<T, Integer>  tryGetFixedWidthValue(
        ByteBuf token,
        byte expectedTypeMarker,
        int sizeofType,
        Class<T> clazz) {

        if (token.readableBytes() < 1 + sizeofType) {
            TryBiResult.failed(clazz, Integer.class);
        }

        byte typeMarker = token.readByte();
        if (typeMarker != expectedTypeMarker) {
            TryBiResult.failed(clazz, Integer.class);
        }

        if (clazz == Byte.class && sizeofType == 1) {
            return (TryBiResult<T, Integer>) TryBiResult.success(token.readByte(), sizeofType);
        } else if (clazz == Short.class && sizeofType == 2) {
            return (TryBiResult<T, Integer>) TryBiResult.success(token.readShortLE(), sizeofType);
        } else if (clazz == Integer.class && sizeofType == 4) {
            return (TryBiResult<T, Integer>) TryBiResult.success(token.readIntLE(), sizeofType);
        } else if (clazz == Long.class && sizeofType == 8) {
            return (TryBiResult<T, Integer>) TryBiResult.success(token.readLongLE(), sizeofType);
        } else if (clazz == Long.class && sizeofType == 4) {
            return (TryBiResult<T, Integer>) TryBiResult.success(token.readUnsignedIntLE(), sizeofType);
        } else if (clazz == Float.class && sizeofType == 4) {
            return (TryBiResult<T, Integer>) TryBiResult.success(token.readFloatLE(), sizeofType);
        } else if (clazz == Double.class && sizeofType == 8) {
            return (TryBiResult<T, Integer>) TryBiResult.success(token.readDoubleLE(), sizeofType);
        } else {
            return TryBiResult.failed(clazz, Integer.class);
        }
    }

    public static byte decodeInt8Value(ByteBuf int8Token) throws JsonParseException {
        TryBiResult<Byte, Integer> result = JsonBinaryEncoding.tryDecodeInt8Value(int8Token);
        if (!result.isSuccess())
        {
            throw new JsonInvalidNumberException();
        }

        return result.getResultA();
    }

    public static TryBiResult<Byte, Integer> tryDecodeInt8Value(ByteBuf int8Token) {
        return JsonBinaryEncoding.tryGetFixedWidthValue(
            int8Token,
            TypeMarker.Int8,
            1,
            Byte.class);
    }

    public static short decodeInt16Value(ByteBuf int16Token) throws JsonParseException {
        TryBiResult<Short, Integer> result = JsonBinaryEncoding.tryDecodeInt16Value(int16Token);
        if (!result.isSuccess())
        {
            throw new JsonInvalidNumberException();
        }

        return result.getResultA();
    }

    public static TryBiResult<Short, Integer> tryDecodeInt16Value(ByteBuf int16Token) {
        return JsonBinaryEncoding.tryGetFixedWidthValue(
            int16Token,
            TypeMarker.Int16,
            2,
            Short.class
        );
    }

    public static int decodeInt32Value(ByteBuf int32Token) throws JsonParseException
    {
        TryBiResult<Integer, Integer> result = JsonBinaryEncoding.tryDecodeInt32Value(int32Token);
        if (!result.isSuccess())
        {
            throw new JsonInvalidNumberException();
        }

        return result.getResultA();
    }

    public static TryBiResult<Integer, Integer> tryDecodeInt32Value(ByteBuf int32Token) {
        return JsonBinaryEncoding.tryGetFixedWidthValue(
            int32Token,
            TypeMarker.Int32,
            4,
            Integer.class
        );
    }

    public static long decodeInt64Value(ByteBuf int64Token) throws JsonParseException
    {
        TryBiResult<Long, Integer> result = JsonBinaryEncoding.tryDecodeInt64Value(int64Token);
        if (!result.isSuccess())
        {
            throw new JsonInvalidNumberException();
        }

        return result.getResultA();
    }

    public static TryBiResult<Long, Integer> tryDecodeInt64Value(ByteBuf int64Token) {
        return JsonBinaryEncoding.tryGetFixedWidthValue(
            int64Token,
            TypeMarker.Int64,
            8,
            Long.class
        );
    }

    public static long decodeUInt32Value(ByteBuf uInt32Token) throws JsonParseException
    {
        TryBiResult<Long, Integer> result = JsonBinaryEncoding.tryDecodeUInt32Value(uInt32Token);
        if (!result.isSuccess())
        {
            throw new JsonInvalidNumberException();
        }

        return result.getResultA();
    }

    public static TryBiResult<Long, Integer> tryDecodeUInt32Value(ByteBuf uInt32Token) {
        return JsonBinaryEncoding.tryGetFixedWidthValue(
            uInt32Token,
            TypeMarker.UInt32,
            4,
            Long.class
        );
    }

    public static float decodeFloat32Value(ByteBuf float32Token) throws JsonParseException
    {
        TryBiResult<Float, Integer> result = JsonBinaryEncoding.tryDecodeFloat32Value(float32Token);
        if (!result.isSuccess())
        {
            throw new JsonInvalidNumberException();
        }

        return result.getResultA();
    }

    public static TryBiResult<Float, Integer> tryDecodeFloat32Value(ByteBuf float32Token) {
        return JsonBinaryEncoding.tryGetFixedWidthValue(
            float32Token,
            TypeMarker.Float32,
            4,
            Float.class
        );
    }

    public static double decodeFloat64Value(ByteBuf float64Token) throws JsonParseException
    {
        TryBiResult<Double, Integer> result = JsonBinaryEncoding.tryDecodeFloat64Value(float64Token);
        if (!result.isSuccess())
        {
            throw new JsonInvalidNumberException();
        }

        return result.getResultA();
    }

    public static TryBiResult<Double, Integer> tryDecodeFloat64Value(ByteBuf float64Token) {
        return JsonBinaryEncoding.tryGetFixedWidthValue(
            float64Token,
            TypeMarker.Float64,
            8,
            Double.class
        );
    }

    public static TryResult<Integer> tryEncodeInt64(ByteBuf buf, long value) {
        int requiredSize = TypeMarkerLength + 8;
        if (buf.writableBytes() < requiredSize) {
            return TryResult.failed(requiredSize);
        }

        buf.writeByte(TypeMarker.Int64);
        buf.writeLongLE(value);

        return TryResult.success(requiredSize);
    }

    public static TryResult<Integer> tryEncodeInt32(ByteBuf buf, int value) {
        int requiredSize = TypeMarkerLength + 4;
        if (buf.writableBytes() < requiredSize) {
            return TryResult.failed(requiredSize);
        }

        buf.writeByte(TypeMarker.Int32);
        buf.writeIntLE(value);

        return TryResult.success(requiredSize);
    }

    public static TryResult<Integer> tryEncodeInt16(ByteBuf buf, short value) {
        int requiredSize = TypeMarkerLength + 2;
        if (buf.writableBytes() < requiredSize) {
            return TryResult.failed(requiredSize);
        }

        buf.writeByte(TypeMarker.Int16);
        buf.writeShortLE(value);

        return TryResult.success(requiredSize);
    }

    public static TryResult<Integer> tryEncodeByte(ByteBuf buf, byte value) {
        int requiredSize = TypeMarkerLength + 1;
        if (buf.writableBytes() < requiredSize) {
            return TryResult.failed(requiredSize);
        }

        buf.writeByte(TypeMarker.Int8);
        buf.writeByte(value);

        return TryResult.success(requiredSize);
    }

    public static TryResult<Integer> tryEncodeUInt32(ByteBuf buf, long value) {
        checkArgument(
            value >= 0 && value <= UINT32_MAX_VALUE,
            "Parameter 'value' must be an unsigned Int32.");
        int requiredSize = TypeMarkerLength + 4;
        if (buf.writableBytes() < requiredSize) {
            return TryResult.failed(requiredSize);
        }

        buf.writeByte(TypeMarker.UInt32);
        buf.writeIntLE((int)value);

        return TryResult.success(requiredSize);
    }

    public static TryResult<Integer> tryEncodeFloat(ByteBuf buf, float value) {
        int requiredSize = TypeMarkerLength + 4;
        if (buf.writableBytes() < requiredSize) {
            return TryResult.failed(requiredSize);
        }

        buf.writeByte(TypeMarker.Float32);
        buf.writeFloatLE(value);

        return TryResult.success(requiredSize);
    }

    public static TryResult<Integer> tryEncodeDouble(ByteBuf buf, double value) {
        int requiredSize = TypeMarkerLength + 8;
        if (buf.writableBytes() < requiredSize) {
            return TryResult.failed(requiredSize);
        }

        buf.writeByte(TypeMarker.Float64);
        buf.writeDoubleLE(value);

        return TryResult.success(requiredSize);
    }

    private static TryResult<String> tryGetBufferedLengthPrefixedString(
        ByteBuf rootBuffer,
        ByteBuf stringToken)
    {
        byte typeMarker = stringToken.readByte();

        int length;
        if (TypeMarker.IsEncodedLengthString(typeMarker))
        {
            length = JsonBinaryEncoding.getStringLength(typeMarker);
        }
        else
        {
            int referenceStringOffset;
            switch (typeMarker)
            {
                case TypeMarker.String1ByteLength:
                    if (stringToken.readableBytes() < JsonBinaryEncoding.OneByteLength)
                    {
                        return TryResult.failed(String.class);
                    }

                    length = stringToken.readUnsignedByte();
                    break;

                case TypeMarker.String2ByteLength:
                    if (stringToken.readableBytes() < JsonBinaryEncoding.TwoByteLength)
                    {
                        return TryResult.failed(String.class);
                    }

                    length = stringToken.readUnsignedShortLE();
                    break;

                case TypeMarker.String4ByteLength:
                    if (stringToken.readableBytes() < JsonBinaryEncoding.FourByteLength)
                    {
                        return TryResult.failed(String.class);
                    }

                    length = (int)stringToken.readUnsignedIntLE();
                    if (length < 0)
                    {
                        return TryResult.failed(String.class);
                    }

                    break;

                case TypeMarker.ReferenceString1ByteOffset:
                    if (stringToken.readableBytes() < JsonBinaryEncoding.OneByteOffset)
                    {
                        return TryResult.failed(String.class);
                    }

                    length = stringToken.readUnsignedByte();
                    if (rootBuffer.readableBytes() < length) {
                        return TryResult.failed(String.class);
                    }
                    referenceStringOffset = length + rootBuffer.readerIndex();

                    return tryGetBufferedStringValue(
                        rootBuffer,
                        rootBuffer.slice(referenceStringOffset - 1, rootBuffer.readableBytes() - referenceStringOffset + 1));

                case TypeMarker.ReferenceString2ByteOffset:
                    if (stringToken.readableBytes() < JsonBinaryEncoding.TwoByteOffset)
                    {
                        return TryResult.failed(String.class);
                    }

                    length = stringToken.readUnsignedShortLE();
                    if (rootBuffer.readableBytes() < length) {
                        return TryResult.failed(String.class);
                    }
                    referenceStringOffset = length + rootBuffer.readerIndex();

                    return tryGetBufferedStringValue(
                        rootBuffer,
                        rootBuffer.slice(referenceStringOffset, rootBuffer.readableBytes() - referenceStringOffset));

                case TypeMarker.ReferenceString3ByteOffset:
                    if (stringToken.readableBytes() < JsonBinaryEncoding.ThreeByteOffset)
                    {
                        return TryResult.failed(String.class);
                    }

                    length = stringToken.readUnsignedMediumLE();
                    if (rootBuffer.readableBytes() < length) {
                        return TryResult.failed(String.class);
                    }
                    referenceStringOffset = length + rootBuffer.readerIndex();

                    return tryGetBufferedStringValue(
                        rootBuffer,
                        rootBuffer.slice(referenceStringOffset, rootBuffer.readableBytes() - referenceStringOffset));

                case TypeMarker.ReferenceString4ByteOffset:
                    if (stringToken.readableBytes() < JsonBinaryEncoding.FourByteOffset)
                    {
                        return TryResult.failed(String.class);
                    }

                    length = (int) stringToken.readUnsignedIntLE();
                    if (length < 0)
                    {
                        return TryResult.failed(String.class);
                    }

                    if (rootBuffer.readableBytes() < length) {
                        return TryResult.failed(String.class);
                    }

                    referenceStringOffset = length + rootBuffer.readerIndex();

                    return tryGetBufferedStringValue(
                        rootBuffer,
                        rootBuffer.slice(referenceStringOffset, rootBuffer.readableBytes() - referenceStringOffset));

                default:
                    return TryResult.failed(String.class);
            }

            if (stringToken.readableBytes() < length)
            {
                return TryResult.failed(String.class);
            }
        }

        TryResult<Utf8ByteBuffer> utf8Buffer =  Utf8ByteBuffer.tryCreate(stringToken.readSlice(length));
        if (!utf8Buffer.isSuccess()) {
            return TryResult.failed(String.class);
        }

        return TryResult.success(utf8Buffer.getResult().toString());
    }

    public static TryResult<String> tryGetBufferedStringValue(
        ByteBuf rootBuffer,
        ByteBuf stringToken)
    {
        if (stringToken.readableBytes() == 0)
        {
            return TryResult.failed(String.class);
        }

        TryResult<String> prefixedAttempt = tryGetBufferedLengthPrefixedString(
            rootBuffer,
            stringToken.duplicate());

        if (prefixedAttempt.isSuccess())
        {
            return prefixedAttempt;
        }

        return tryGetEncodedStringValue(stringToken);
   }

    /// <summary>
    /// Try Get Encoded String Value
    /// </summary>
    /// <param name="stringToken">The string token to read from.</param>
    /// <param name="value">The encoded string if found.</param>
    /// <returns>Encoded String Value</returns>
    private static TryResult<String> tryGetEncodedStringValue(ByteBuf stringToken)
    {
        return tryGetEncodedSystemStringValue(stringToken);

        // UserDictionaryEncoding irrelevant in Java SDK (already decoded in svc
        // if (JsonBinaryEncoding.TryGetEncodedUserStringValue(stringToken, out value))
        // {
        //   return true;
        // }
    }

    /// <summary>
    /// Try Get Encoded System String Value
    /// </summary>
    /// <param name="stringToken">The buffer to read from..</param>
    /// <param name="value">The encoded system string.</param>
    /// <returns>Encoded System String Value</returns>
    private static TryResult<String> tryGetEncodedSystemStringValue(
        ByteBuf stringToken)
    {
        if (stringToken.readableBytes() < 1)
        {
            return TryResult.failed(String.class);
        }

        byte typeMarker = stringToken.readByte();
        if (!TypeMarker.IsSystemString(typeMarker))
        {
            return TryResult.failed(String.class);
        }

        int systemStringId = typeMarker - TypeMarker.SystemString1ByteLengthMin;
        TryResult<UtfAllString> result = SystemStrings.tryGetSystemStringById(systemStringId);
        if (!result.isSuccess()) {
            return TryResult.failed(String.class);
        }

        return TryResult.success(result.getResult().getUtf16String());
    }

    private static class StringCompressionLookupTables
    {
        public static final StringCompressionLookupTables DateTime = Create(
        " 0123456789:-.TZ",
        new byte[] {
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x60, (byte)0xff, (byte)0x07,
            (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        },
        new byte[]
        {
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x0C, (byte)0x0D, (byte)0xFF,
            (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08,
            (byte)0x09, (byte)0x0A, (byte)0x0B, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x0E, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0x0F, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
        },
        new short[]
        {
            (short)0x2020, (short)0x2030, (short)0x2031, (short)0x2032, (short)0x2033, (short)0x2034, (short)0x2035, (short)0x2036,
            (short)0x2037, (short)0x2038, (short)0x2039, (short)0x203a, (short)0x202d, (short)0x202e, (short)0x2054, (short)0x205a,
            (short)0x3020, (short)0x3030, (short)0x3031, (short)0x3032, (short)0x3033, (short)0x3034, (short)0x3035, (short)0x3036,
            (short)0x3037, (short)0x3038, (short)0x3039, (short)0x303a, (short)0x302d, (short)0x302e, (short)0x3054, (short)0x305a,
            (short)0x3120, (short)0x3130, (short)0x3131, (short)0x3132, (short)0x3133, (short)0x3134, (short)0x3135, (short)0x3136,
            (short)0x3137, (short)0x3138, (short)0x3139, (short)0x313a, (short)0x312d, (short)0x312e, (short)0x3154, (short)0x315a,
            (short)0x3220, (short)0x3230, (short)0x3231, (short)0x3232, (short)0x3233, (short)0x3234, (short)0x3235, (short)0x3236,
            (short)0x3237, (short)0x3238, (short)0x3239, (short)0x323a, (short)0x322d, (short)0x322e, (short)0x3254, (short)0x325a,
            (short)0x3320, (short)0x3330, (short)0x3331, (short)0x3332, (short)0x3333, (short)0x3334, (short)0x3335, (short)0x3336,
            (short)0x3337, (short)0x3338, (short)0x3339, (short)0x333a, (short)0x332d, (short)0x332e, (short)0x3354, (short)0x335a,
            (short)0x3420, (short)0x3430, (short)0x3431, (short)0x3432, (short)0x3433, (short)0x3434, (short)0x3435, (short)0x3436,
            (short)0x3437, (short)0x3438, (short)0x3439, (short)0x343a, (short)0x342d, (short)0x342e, (short)0x3454, (short)0x345a,
            (short)0x3520, (short)0x3530, (short)0x3531, (short)0x3532, (short)0x3533, (short)0x3534, (short)0x3535, (short)0x3536,
            (short)0x3537, (short)0x3538, (short)0x3539, (short)0x353a, (short)0x352d, (short)0x352e, (short)0x3554, (short)0x355a,
            (short)0x3620, (short)0x3630, (short)0x3631, (short)0x3632, (short)0x3633, (short)0x3634, (short)0x3635, (short)0x3636,
            (short)0x3637, (short)0x3638, (short)0x3639, (short)0x363a, (short)0x362d, (short)0x362e, (short)0x3654, (short)0x365a,
            (short)0x3720, (short)0x3730, (short)0x3731, (short)0x3732, (short)0x3733, (short)0x3734, (short)0x3735, (short)0x3736,
            (short)0x3737, (short)0x3738, (short)0x3739, (short)0x373a, (short)0x372d, (short)0x372e, (short)0x3754, (short)0x375a,
            (short)0x3820, (short)0x3830, (short)0x3831, (short)0x3832, (short)0x3833, (short)0x3834, (short)0x3835, (short)0x3836,
            (short)0x3837, (short)0x3838, (short)0x3839, (short)0x383a, (short)0x382d, (short)0x382e, (short)0x3854, (short)0x385a,
            (short)0x3920, (short)0x3930, (short)0x3931, (short)0x3932, (short)0x3933, (short)0x3934, (short)0x3935, (short)0x3936,
            (short)0x3937, (short)0x3938, (short)0x3939, (short)0x393a, (short)0x392d, (short)0x392e, (short)0x3954, (short)0x395a,
            (short)0x3a20, (short)0x3a30, (short)0x3a31, (short)0x3a32, (short)0x3a33, (short)0x3a34, (short)0x3a35, (short)0x3a36,
            (short)0x3a37, (short)0x3a38, (short)0x3a39, (short)0x3a3a, (short)0x3a2d, (short)0x3a2e, (short)0x3a54, (short)0x3a5a,
            (short)0x2d20, (short)0x2d30, (short)0x2d31, (short)0x2d32, (short)0x2d33, (short)0x2d34, (short)0x2d35, (short)0x2d36,
            (short)0x2d37, (short)0x2d38, (short)0x2d39, (short)0x2d3a, (short)0x2d2d, (short)0x2d2e, (short)0x2d54, (short)0x2d5a,
            (short)0x2e20, (short)0x2e30, (short)0x2e31, (short)0x2e32, (short)0x2e33, (short)0x2e34, (short)0x2e35, (short)0x2e36,
            (short)0x2e37, (short)0x2e38, (short)0x2e39, (short)0x2e3a, (short)0x2e2d, (short)0x2e2e, (short)0x2e54, (short)0x2e5a,
            (short)0x5420, (short)0x5430, (short)0x5431, (short)0x5432, (short)0x5433, (short)0x5434, (short)0x5435, (short)0x5436,
            (short)0x5437, (short)0x5438, (short)0x5439, (short)0x543a, (short)0x542d, (short)0x542e, (short)0x5454, (short)0x545a,
            (short)0x5a20, (short)0x5a30, (short)0x5a31, (short)0x5a32, (short)0x5a33, (short)0x5a34, (short)0x5a35, (short)0x5a36,
            (short)0x5a37, (short)0x5a38, (short)0x5a39, (short)0x5a3a, (short)0x5a2d, (short)0x5a2e, (short)0x5a54, (short)0x5a5a,
        });

        public static final StringCompressionLookupTables LowercaseHex = Create(
        "0123456789abcdef",
        new byte[] {
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xff, (byte)0x03,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x7e, (byte)0x00, (byte)0x00, (byte)0x00
        },
        new byte[]
        {
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07,
            (byte)0x08, (byte)0x09, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0x0A, (byte)0x0B, (byte)0x0C, (byte)0x0D, (byte)0x0E, (byte)0x0F, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
        },
        new short[]
        {
            (short)0x3030, (short)0x3031, (short)0x3032, (short)0x3033, (short)0x3034, (short)0x3035, (short)0x3036, (short)0x3037,
            (short)0x3038, (short)0x3039, (short)0x3061, (short)0x3062, (short)0x3063, (short)0x3064, (short)0x3065, (short)0x3066,
            (short)0x3130, (short)0x3131, (short)0x3132, (short)0x3133, (short)0x3134, (short)0x3135, (short)0x3136, (short)0x3137,
            (short)0x3138, (short)0x3139, (short)0x3161, (short)0x3162, (short)0x3163, (short)0x3164, (short)0x3165, (short)0x3166,
            (short)0x3230, (short)0x3231, (short)0x3232, (short)0x3233, (short)0x3234, (short)0x3235, (short)0x3236, (short)0x3237,
            (short)0x3238, (short)0x3239, (short)0x3261, (short)0x3262, (short)0x3263, (short)0x3264, (short)0x3265, (short)0x3266,
            (short)0x3330, (short)0x3331, (short)0x3332, (short)0x3333, (short)0x3334, (short)0x3335, (short)0x3336, (short)0x3337,
            (short)0x3338, (short)0x3339, (short)0x3361, (short)0x3362, (short)0x3363, (short)0x3364, (short)0x3365, (short)0x3366,
            (short)0x3430, (short)0x3431, (short)0x3432, (short)0x3433, (short)0x3434, (short)0x3435, (short)0x3436, (short)0x3437,
            (short)0x3438, (short)0x3439, (short)0x3461, (short)0x3462, (short)0x3463, (short)0x3464, (short)0x3465, (short)0x3466,
            (short)0x3530, (short)0x3531, (short)0x3532, (short)0x3533, (short)0x3534, (short)0x3535, (short)0x3536, (short)0x3537,
            (short)0x3538, (short)0x3539, (short)0x3561, (short)0x3562, (short)0x3563, (short)0x3564, (short)0x3565, (short)0x3566,
            (short)0x3630, (short)0x3631, (short)0x3632, (short)0x3633, (short)0x3634, (short)0x3635, (short)0x3636, (short)0x3637,
            (short)0x3638, (short)0x3639, (short)0x3661, (short)0x3662, (short)0x3663, (short)0x3664, (short)0x3665, (short)0x3666,
            (short)0x3730, (short)0x3731, (short)0x3732, (short)0x3733, (short)0x3734, (short)0x3735, (short)0x3736, (short)0x3737,
            (short)0x3738, (short)0x3739, (short)0x3761, (short)0x3762, (short)0x3763, (short)0x3764, (short)0x3765, (short)0x3766,
            (short)0x3830, (short)0x3831, (short)0x3832, (short)0x3833, (short)0x3834, (short)0x3835, (short)0x3836, (short)0x3837,
            (short)0x3838, (short)0x3839, (short)0x3861, (short)0x3862, (short)0x3863, (short)0x3864, (short)0x3865, (short)0x3866,
            (short)0x3930, (short)0x3931, (short)0x3932, (short)0x3933, (short)0x3934, (short)0x3935, (short)0x3936, (short)0x3937,
            (short)0x3938, (short)0x3939, (short)0x3961, (short)0x3962, (short)0x3963, (short)0x3964, (short)0x3965, (short)0x3966,
            (short)0x6130, (short)0x6131, (short)0x6132, (short)0x6133, (short)0x6134, (short)0x6135, (short)0x6136, (short)0x6137,
            (short)0x6138, (short)0x6139, (short)0x6161, (short)0x6162, (short)0x6163, (short)0x6164, (short)0x6165, (short)0x6166,
            (short)0x6230, (short)0x6231, (short)0x6232, (short)0x6233, (short)0x6234, (short)0x6235, (short)0x6236, (short)0x6237,
            (short)0x6238, (short)0x6239, (short)0x6261, (short)0x6262, (short)0x6263, (short)0x6264, (short)0x6265, (short)0x6266,
            (short)0x6330, (short)0x6331, (short)0x6332, (short)0x6333, (short)0x6334, (short)0x6335, (short)0x6336, (short)0x6337,
            (short)0x6338, (short)0x6339, (short)0x6361, (short)0x6362, (short)0x6363, (short)0x6364, (short)0x6365, (short)0x6366,
            (short)0x6430, (short)0x6431, (short)0x6432, (short)0x6433, (short)0x6434, (short)0x6435, (short)0x6436, (short)0x6437,
            (short)0x6438, (short)0x6439, (short)0x6461, (short)0x6462, (short)0x6463, (short)0x6464, (short)0x6465, (short)0x6466,
            (short)0x6530, (short)0x6531, (short)0x6532, (short)0x6533, (short)0x6534, (short)0x6535, (short)0x6536, (short)0x6537,
            (short)0x6538, (short)0x6539, (short)0x6561, (short)0x6562, (short)0x6563, (short)0x6564, (short)0x6565, (short)0x6566,
            (short)0x6630, (short)0x6631, (short)0x6632, (short)0x6633, (short)0x6634, (short)0x6635, (short)0x6636, (short)0x6637,
            (short)0x6638, (short)0x6639, (short)0x6661, (short)0x6662, (short)0x6663, (short)0x6664, (short)0x6665, (short)0x6666,
        });

        public static final StringCompressionLookupTables UppercaseHex = Create(
        "0123456789ABCDEF",
        new byte[] {
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xff, (byte)0x03,
            (byte)0x7e, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 },
        new byte[]
        {
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07,
            (byte)0x08, (byte)0x09, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0x0A, (byte)0x0B, (byte)0x0C, (byte)0x0D, (byte)0x0E, (byte)0x0F, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
        },
        new short[]
        {
            (short)0x3030, (short)0x3031, (short)0x3032, (short)0x3033, (short)0x3034, (short)0x3035, (short)0x3036, (short)0x3037,
            (short)0x3038, (short)0x3039, (short)0x3041, (short)0x3042, (short)0x3043, (short)0x3044, (short)0x3045, (short)0x3046,
            (short)0x3130, (short)0x3131, (short)0x3132, (short)0x3133, (short)0x3134, (short)0x3135, (short)0x3136, (short)0x3137,
            (short)0x3138, (short)0x3139, (short)0x3141, (short)0x3142, (short)0x3143, (short)0x3144, (short)0x3145, (short)0x3146,
            (short)0x3230, (short)0x3231, (short)0x3232, (short)0x3233, (short)0x3234, (short)0x3235, (short)0x3236, (short)0x3237,
            (short)0x3238, (short)0x3239, (short)0x3241, (short)0x3242, (short)0x3243, (short)0x3244, (short)0x3245, (short)0x3246,
            (short)0x3330, (short)0x3331, (short)0x3332, (short)0x3333, (short)0x3334, (short)0x3335, (short)0x3336, (short)0x3337,
            (short)0x3338, (short)0x3339, (short)0x3341, (short)0x3342, (short)0x3343, (short)0x3344, (short)0x3345, (short)0x3346,
            (short)0x3430, (short)0x3431, (short)0x3432, (short)0x3433, (short)0x3434, (short)0x3435, (short)0x3436, (short)0x3437,
            (short)0x3438, (short)0x3439, (short)0x3441, (short)0x3442, (short)0x3443, (short)0x3444, (short)0x3445, (short)0x3446,
            (short)0x3530, (short)0x3531, (short)0x3532, (short)0x3533, (short)0x3534, (short)0x3535, (short)0x3536, (short)0x3537,
            (short)0x3538, (short)0x3539, (short)0x3541, (short)0x3542, (short)0x3543, (short)0x3544, (short)0x3545, (short)0x3546,
            (short)0x3630, (short)0x3631, (short)0x3632, (short)0x3633, (short)0x3634, (short)0x3635, (short)0x3636, (short)0x3637,
            (short)0x3638, (short)0x3639, (short)0x3641, (short)0x3642, (short)0x3643, (short)0x3644, (short)0x3645, (short)0x3646,
            (short)0x3730, (short)0x3731, (short)0x3732, (short)0x3733, (short)0x3734, (short)0x3735, (short)0x3736, (short)0x3737,
            (short)0x3738, (short)0x3739, (short)0x3741, (short)0x3742, (short)0x3743, (short)0x3744, (short)0x3745, (short)0x3746,
            (short)0x3830, (short)0x3831, (short)0x3832, (short)0x3833, (short)0x3834, (short)0x3835, (short)0x3836, (short)0x3837,
            (short)0x3838, (short)0x3839, (short)0x3841, (short)0x3842, (short)0x3843, (short)0x3844, (short)0x3845, (short)0x3846,
            (short)0x3930, (short)0x3931, (short)0x3932, (short)0x3933, (short)0x3934, (short)0x3935, (short)0x3936, (short)0x3937,
            (short)0x3938, (short)0x3939, (short)0x3941, (short)0x3942, (short)0x3943, (short)0x3944, (short)0x3945, (short)0x3946,
            (short)0x4130, (short)0x4131, (short)0x4132, (short)0x4133, (short)0x4134, (short)0x4135, (short)0x4136, (short)0x4137,
            (short)0x4138, (short)0x4139, (short)0x4141, (short)0x4142, (short)0x4143, (short)0x4144, (short)0x4145, (short)0x4146,
            (short)0x4230, (short)0x4231, (short)0x4232, (short)0x4233, (short)0x4234, (short)0x4235, (short)0x4236, (short)0x4237,
            (short)0x4238, (short)0x4239, (short)0x4241, (short)0x4242, (short)0x4243, (short)0x4244, (short)0x4245, (short)0x4246,
            (short)0x4330, (short)0x4331, (short)0x4332, (short)0x4333, (short)0x4334, (short)0x4335, (short)0x4336, (short)0x4337,
            (short)0x4338, (short)0x4339, (short)0x4341, (short)0x4342, (short)0x4343, (short)0x4344, (short)0x4345, (short)0x4346,
            (short)0x4430, (short)0x4431, (short)0x4432, (short)0x4433, (short)0x4434, (short)0x4435, (short)0x4436, (short)0x4437,
            (short)0x4438, (short)0x4439, (short)0x4441, (short)0x4442, (short)0x4443, (short)0x4444, (short)0x4445, (short)0x4446,
            (short)0x4530, (short)0x4531, (short)0x4532, (short)0x4533, (short)0x4534, (short)0x4535, (short)0x4536, (short)0x4537,
            (short)0x4538, (short)0x4539, (short)0x4541, (short)0x4542, (short)0x4543, (short)0x4544, (short)0x4545, (short)0x4546,
            (short)0x4630, (short)0x4631, (short)0x4632, (short)0x4633, (short)0x4634, (short)0x4635, (short)0x4636, (short)0x4637,
            (short)0x4638, (short)0x4639, (short)0x4641, (short)0x4642, (short)0x4643, (short)0x4644, (short)0x4645, (short)0x4646,
        });

        public static final StringCompressionLookupTables LowercaseHexLittleEndian = LowercaseHex.toLittleEndian();

        public static final StringCompressionLookupTables UppercaseHexLittleEndian = UppercaseHex.toLittleEndian();


        private final CharSequence list;
    private final BitSet bitmap;
    private final byte[] charToByte;
    private final short[] byteToTwoChars;

    private StringCompressionLookupTables(
        CharSequence list,
        BitSet bitmap,
        byte[] charToByte,
        short[] byteToTwoChars) {

            checkNotNull(bitmap, "Parameter 'bitmap' MUST NOT be null.");
            checkNotNull(charToByte, "Parameter 'charToByte' MUST NOT be null.");
            checkNotNull(byteToTwoChars, "Parameter 'byteToTwoChars' MUST NOT be null.");
            checkArgument(list.length() == 16, "Parameter 'list' must be of length 16.");
            checkArgument(bitmap.length() <= 128, "Parameter 'bitmap' must be of length less than or equal to 128.");
            checkArgument(charToByte.length == 256, "Parameter 'charToByte' must be of length 256.");
            checkArgument(
                byteToTwoChars.length == 256,
                "Parameter 'byteToTwoChars' must be of length 256.");

            this.list = list;
            this.bitmap = bitmap;
            this.charToByte = charToByte;
            this.byteToTwoChars = byteToTwoChars;
        }

        public CharSequence getList() {
            return this.list;
        }

        public BitSet getBitmap() {
            return this.bitmap;
        }

        public byte[] getCharToByte() {
            return this.charToByte;
        }

        public short[] getByteToTwoChars() {
            return this.byteToTwoChars;
        }

        private static StringCompressionLookupTables Create(CharSequence list, byte[] charSet, byte[] charToByte, short[] byteToTwoChars)
        {
            checkNotNull(list, "Parameter 'list' MUST NOT be null.");
            checkNotNull(charSet, "Parameter 'charSet' MUST NOT be null.");
            checkNotNull(charToByte, "Parameter 'charToByte' MUST NOT be null.");
            checkNotNull(byteToTwoChars, "Parameter 'byteToTwoChars' MUST NOT be null.");

            return new StringCompressionLookupTables(list, BitSet.valueOf(charSet), charToByte, byteToTwoChars);
        }

        public StringCompressionLookupTables toLittleEndian() {
            short[] byteToTwoCharsLittleEndian = new short[this.byteToTwoChars.length];
            for (int i= 0; i < byteToTwoCharsLittleEndian.length; i++) {
                short original = this.byteToTwoChars[i];
                byteToTwoCharsLittleEndian[i] = (short)(original << 8 | original >>> 8);
            }

            return new StringCompressionLookupTables(
                this.list,
                this.bitmap,
                this.charToByte,
                byteToTwoCharsLittleEndian);
        }
    }

    private static class StringLengths
    {
        private final static int UsrStr1 = -1;
        private final static int UsrStr2 = -2;
        private final static int StrL1 = -3;
        private final static int StrL2 = -4;
        private final static int StrL4 = -5;
        private final static int StrR1 = -6;
        private final static int StrR2 = -7;
        private final static int StrR3 = -8;
        private final static int StrR4 = -9;
        private final static int StrComp = -10;
        private final static int NotStr = -11;

        /// <summary>
        /// Lookup table for encoded string length for each TypeMarker value (0 to 255)
        /// The lengths are encoded as follows:
        /// - Non-Negative Value: The TypeMarker encodes the string length
        /// - Negative Value: System or user dictionary encoded string, or encoded string length that follows the TypeMarker
        /// </summary>
        public static final int[] Lengths = new int[]
        {
            // Encoded literal integer value (32 values)
            NotStr, NotStr, NotStr, NotStr, NotStr, NotStr, NotStr, NotStr,
            NotStr, NotStr, NotStr, NotStr, NotStr, NotStr, NotStr, NotStr,
            NotStr, NotStr, NotStr, NotStr, NotStr, NotStr, NotStr, NotStr,
            NotStr, NotStr, NotStr, NotStr, NotStr, NotStr, NotStr, NotStr,

            // Encoded 1-byte system string (32 values)
            SystemStrings.Strings[0].getUtf8String().getLength(), SystemStrings.Strings[1].getUtf8String().getLength(),
            SystemStrings.Strings[2].getUtf8String().getLength(), SystemStrings.Strings[3].getUtf8String().getLength(),
            SystemStrings.Strings[4].getUtf8String().getLength(), SystemStrings.Strings[5].getUtf8String().getLength(),
            SystemStrings.Strings[6].getUtf8String().getLength(), SystemStrings.Strings[7].getUtf8String().getLength(),
            SystemStrings.Strings[8].getUtf8String().getLength(), SystemStrings.Strings[9].getUtf8String().getLength(),
            SystemStrings.Strings[10].getUtf8String().getLength(), SystemStrings.Strings[11].getUtf8String().getLength(),
            SystemStrings.Strings[12].getUtf8String().getLength(), SystemStrings.Strings[13].getUtf8String().getLength(),
            SystemStrings.Strings[14].getUtf8String().getLength(), SystemStrings.Strings[15].getUtf8String().getLength(),
            SystemStrings.Strings[16].getUtf8String().getLength(), SystemStrings.Strings[17].getUtf8String().getLength(),
            SystemStrings.Strings[18].getUtf8String().getLength(), SystemStrings.Strings[19].getUtf8String().getLength(),
            SystemStrings.Strings[20].getUtf8String().getLength(), SystemStrings.Strings[21].getUtf8String().getLength(),
            SystemStrings.Strings[22].getUtf8String().getLength(), SystemStrings.Strings[23].getUtf8String().getLength(),
            SystemStrings.Strings[24].getUtf8String().getLength(), SystemStrings.Strings[25].getUtf8String().getLength(),
            SystemStrings.Strings[26].getUtf8String().getLength(), SystemStrings.Strings[27].getUtf8String().getLength(),
            SystemStrings.Strings[28].getUtf8String().getLength(), SystemStrings.Strings[29].getUtf8String().getLength(),
            SystemStrings.Strings[30].getUtf8String().getLength(), SystemStrings.Strings[31].getUtf8String().getLength(),

            // Encoded 1-byte user string (32 values)
            UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1,
            UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1,
            UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1,
            UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1, UsrStr1,

            // Encoded 2-byte user string (8 values)
            UsrStr2, UsrStr2, UsrStr2, UsrStr2, UsrStr2, UsrStr2, UsrStr2, UsrStr2,

            // String Values [0x68, 0x70)
            NotStr,     // <empty> 0x68
            NotStr,     // <empty> 0x69
            NotStr,     // <empty> 0x6A
            NotStr,     // <empty> 0x6B
            NotStr,     // <empty> 0x6C
            NotStr,     // <empty> 0x6D
            NotStr,     // <empty> 0x6E
            NotStr,     // <empty> 0x6F

            // Empty Range
            NotStr,     // <empty> 0x70
            NotStr,     // <empty> 0x71
            NotStr,     // <empty> 0x72
            NotStr,     // <empty> 0x73
            NotStr,     // <empty> 0x74
            36,         // StrGL (Lowercase GUID string)
            36,         // StrGU (Uppercase GUID string)
            38,         // StrGQ (Double-quoted lowercase GUID string)

            // Compressed strings [0x78, 0x80)
            StrComp,    // String 1-byte length - Lowercase hexadecimal digits encoded as 4-bit characters
            StrComp,    // String 1-byte length - Uppercase hexadecimal digits encoded as 4-bit characters
            StrComp,    // String 1-byte length - Date-time character set encoded as 4-bit characters
            StrComp,    // String 1-byte Length - 4-bit packed characters relative to a base value
            StrComp,    // String 1-byte Length - 5-bit packed characters relative to a base value
            StrComp,    // String 1-byte Length - 6-bit packed characters relative to a base value
            StrComp,    // String 1-byte Length - 7-bit packed characters
            StrComp,    // String 2-byte Length - 7-bit packed characters

            // TypeMarker-encoded string length (64 values)
            0, 1, 2, 3, 4, 5, 6, 7,
            8, 9, 10, 11, 12, 13, 14, 15,
            16, 17, 18, 19, 20, 21, 22, 23,
            24, 25, 26, 27, 28, 29, 30, 31,
            32, 33, 34, 35, 36, 37, 38, 39,
            40, 41, 42, 43, 44, 45, 46, 47,
            48, 49, 50, 51, 52, 53, 54, 55,
            56, 57, 58, 59, 60, 61, 62, 63,

            // Variable Length String Values
            StrL1,      // StrL1 (1-byte length)
            StrL2,      // StrL2 (2-byte length)
            StrL4,      // StrL4 (4-byte length)
            StrR1,      // StrR1 (Reference string of 1-byte offset)
            StrR2,      // StrR2 (Reference string of 2-byte offset)
            StrR3,      // StrR3 (Reference string of 3-byte offset)
            StrR4,      // StrR4 (Reference string of 4-byte offset)
            NotStr,     // <empty> 0xC7

            // Number Values
            NotStr,     // NumUI8
            NotStr,     // NumI16,
            NotStr,     // NumI32,
            NotStr,     // NumI64,
            NotStr,     // NumDbl,
            NotStr,     // Float32
            NotStr,     // Float64
            NotStr,     // <empty> 0xCF

            // Other Value Types
            NotStr,     // Null
            NotStr,     // False
            NotStr,     // True
            NotStr,     // GUID
            NotStr,     // <empty> 0xD4
            NotStr,     // <empty> 0xD5
            NotStr,     // <empty> 0xD6
            NotStr,     // <empty> 0xD7

            NotStr,     // Int8
            NotStr,     // Int16
            NotStr,     // Int32
            NotStr,     // Int64
            NotStr,     // UInt32
            NotStr,     // BinL1 (1-byte length)
            NotStr,     // BinL2 (2-byte length)
            NotStr,     // BinL4 (4-byte length)

            // Array Type Markers
            NotStr,     // Arr0
            NotStr,     // Arr1
            NotStr,     // ArrL1 (1-byte length)
            NotStr,     // ArrL2 (2-byte length)
            NotStr,     // ArrL4 (4-byte length)
            NotStr,     // ArrLC1 (1-byte length and count)
            NotStr,     // ArrLC2 (2-byte length and count)
            NotStr,     // ArrLC4 (4-byte length and count)

            // Object Type Markers
            NotStr,     // Obj0
            NotStr,     // Obj1
            NotStr,     // ObjL1 (1-byte length)
            NotStr,     // ObjL2 (2-byte length)
            NotStr,     // ObjL4 (4-byte length)
            NotStr,     // ObjLC1 (1-byte length and count)
            NotStr,     // ObjLC2 (2-byte length and count)
            NotStr,     // ObjLC4 (4-byte length and count)

            // Empty Range
            NotStr,     // <empty> 0xF0
            NotStr,     // <empty> 0xF1
            NotStr,     // <empty> 0xF2
            NotStr,     // <empty> 0xF3
            NotStr,     // <empty> 0xF4
            NotStr,     // <empty> 0xF5
            NotStr,     // <empty> 0xF7
            NotStr,     // <empty> 0xF8

            // Special Values
            NotStr,     // <special value reserved> 0xF8
            NotStr,     // <special value reserved> 0xF9
            NotStr,     // <special value reserved> 0xFA
            NotStr,     // <special value reserved> 0xFB
            NotStr,     // <special value reserved> 0xFC
            NotStr,     // <special value reserved> 0xFD
            NotStr,     // <special value reserved> 0xFE
            NotStr,     // Invalid
        };
    }

    /// <summary>
    /// Gets the length of a particular string given its TypeMarker.
    /// </summary>
    /// <param name="typeMarker">The type marker as input</param>
    /// <returns>
    /// - Non-Negative Value: The TypeMarker encodes the string length
    /// - Negative Value: System or user dictionary encoded string, or encoded string length that follows The TypeMarker
    /// </returns>
    public static int getStringLength(byte typeMarker)
    {
        short unsignedTypeMarker = (short)(typeMarker & 0xFF);
        return JsonBinaryEncoding.StringLengths.Lengths[unsignedTypeMarker];
    }

    public static int getValueLength(ByteBuf buffer)
    {
        checkNotNull(buffer, "Parameter 'buffer' MUST NOT be null.");
        int unsignedTypeMarker = buffer.readUnsignedByte();
        int lengthFromLookup = ValueLengths.Lookup[unsignedTypeMarker];
        long length = lengthFromLookup;
        if (lengthFromLookup < 0)
        {
            // Length was negative meaning we need to look into the buffer to find the length
            switch (lengthFromLookup)
            {
                case ValueLengths.L1:
                    length = TypeMarkerLength + OneByteLength + buffer.readUnsignedByte();
                    break;
                case ValueLengths.L2:
                    length = TypeMarkerLength + TwoByteLength + buffer.readUnsignedShortLE();
                    break;
                case ValueLengths.L4:
                    length = TypeMarkerLength + FourByteLength + buffer.readUnsignedIntLE();
                    break;

                case ValueLengths.LC1:
                    length = TypeMarkerLength + OneByteLength + OneByteCount + buffer.readUnsignedByte();
                    break;
                case ValueLengths.LC2:
                    length = TypeMarkerLength + TwoByteLength + TwoByteCount + buffer.readUnsignedShortLE();
                    break;
                case ValueLengths.LC4:
                    buffer.skipBytes(1);
                    length = TypeMarkerLength + FourByteLength + FourByteCount + buffer.readUnsignedIntLE();
                    break;

                case ValueLengths.Arr1:
                    long arrayOneItemLength = getValueLength(buffer);
                    length = arrayOneItemLength == 0 ? 0 : 1 + arrayOneItemLength;
                    break;

                case ValueLengths.Obj1:
                    long nameLength = getValueLength(buffer.slice());
                    if (nameLength == 0)
                    {
                        length = 0;
                    }
                    else
                    {
                        long valueLength = getValueLength(buffer.skipBytes((int)nameLength));
                        length = TypeMarkerLength + nameLength + valueLength;
                    }
                    break;

                case ValueLengths.CS4L1:
                    length = TypeMarkerLength + OneByteLength + getCompressedStringLength(buffer.readUnsignedByte(), 4);
                    break;
                case ValueLengths.CS7L1:
                    length = TypeMarkerLength + OneByteLength + getCompressedStringLength(buffer.readUnsignedByte(), 7);
                    break;
                case ValueLengths.CS7L2:
                    length = TypeMarkerLength + TwoByteLength + getCompressedStringLength(buffer.readUnsignedShortLE(), 7);
                    break;

                case ValueLengths.CS4BL1:
                    length = TypeMarkerLength + OneByteLength + OneByteBaseChar + getCompressedStringLength(buffer.readUnsignedByte(), 4);
                    break;
                case ValueLengths.CS5BL1:
                    length = TypeMarkerLength + OneByteLength + OneByteBaseChar + getCompressedStringLength(buffer.readUnsignedByte(), 5);
                    break;
                case ValueLengths.CS6BL1:
                    length = TypeMarkerLength + OneByteLength + OneByteBaseChar + getCompressedStringLength(buffer.readUnsignedByte(), 6);
                    break;

                default:
                    throw new IllegalArgumentException("Invalid variable length type marker length: " + length);
            }
        }

        return (int)length;
    }


    public static int getCompressedStringLength(int length, int numberOfBits) {
        return ((length * numberOfBits) + 7) / 8;
    }

    public static int getUniformNumberArrayItemSize(byte typeMarker)
    {
        return ValueLengths.Lookup[typeMarker] - 1;
    }

    public static int getArrayOrObjectPrefixLength(byte typeMarker)
    {
        int prefixLength;
        switch (typeMarker)
        {
            case TypeMarker.EmptyArray:
            case TypeMarker.SingleItemArray:
            case TypeMarker.EmptyObject:
            case TypeMarker.SinglePropertyObject:
                prefixLength = 1;
                break;

            case TypeMarker.Object1ByteLength:
            case TypeMarker.Array1ByteLength:
                prefixLength = 1 + 1;
                break;
            case TypeMarker.Object2ByteLength:
            case TypeMarker.Array2ByteLength:
                prefixLength = 1 + 2;
                break;
            case TypeMarker.Array4ByteLength:
            case TypeMarker.Object4ByteLength:
                prefixLength = 1 + 4;
                break;

            case TypeMarker.Array1ByteLengthAndCount:
            case TypeMarker.Object1ByteLengthAndCount:
            case TypeMarker.ArrNumC1:
                prefixLength = 1 + 1 + 1;
                break;
            case TypeMarker.Array2ByteLengthAndCount:
            case TypeMarker.Object2ByteLengthAndCount:
                prefixLength = 1 + 2 + 2;
                break;
            case TypeMarker.Array4ByteLengthAndCount:
            case TypeMarker.Object4ByteLengthAndCount:
                prefixLength = 1 + 4 + 4;
                break;

            case TypeMarker.ArrNumC2:
                prefixLength = 1 + 1 + 2;
                break;
            case TypeMarker.ArrArrNumC1C1:
                prefixLength = 1 + 1 + 1 + 1 + 1;
                break;
            case TypeMarker.ArrArrNumC2C2:
                prefixLength = 1 + 1 + 1 + 2 + 2;
                break;

            default:
                throw new IllegalArgumentException("Unknown typemarker: " + typeMarker);
        }

        return prefixLength;
    }

    public static UniformArrayInfo getUniformArrayInfo(ByteBuf arrayPrefix, boolean isNested)
    {
        checkArgument(
            arrayPrefix.readableBytes() > 0,
            "Argument 'arrayPrefix' is  empty");

        byte arrayTypeMarker = arrayPrefix.readByte();
        switch (arrayTypeMarker)
        {
            case TypeMarker.ArrNumC1:
            {
                byte itemTypeMarker = arrayPrefix.readByte();
                // | Array TM | Number Item TM | Number Item Count |
                return new UniformArrayInfo(
                    itemTypeMarker,
                    isNested ? 0 : 3,
                    getFixedSizedValueAsUByte(arrayPrefix),
                    getUniformNumberArrayItemSize(itemTypeMarker),
                    null);
            }

            case TypeMarker.ArrNumC2:
            {
                byte itemTypeMarker = arrayPrefix.readByte();
                // | Array TM | Number Item TM | Number Item Count |
                return new UniformArrayInfo(
                    itemTypeMarker,
                    isNested ? 0 : 4,
                    getFixedSizedValueAsUInt16(arrayPrefix),
                    getUniformNumberArrayItemSize(itemTypeMarker),
                    null);
            }

            case TypeMarker.ArrArrNumC1C1:
            case TypeMarker.ArrArrNumC2C2:
            {
                byte itemTypeMarker = arrayPrefix.getByte(1);
                // | Array TM | Array Item TM | Number Item TM | Number Item Count | Array Item Count |
                UniformArrayInfo nestedArrayInfo = getUniformArrayInfo(arrayPrefix, true);

                int itemCount;
                if (arrayTypeMarker == TypeMarker.ArrArrNumC1C1) {
                    arrayPrefix.skipBytes(2);
                    itemCount = getFixedSizedValueAsUByte(arrayPrefix);
                } else {
                    arrayPrefix.skipBytes(3);
                    itemCount = getFixedSizedValueAsUInt16(arrayPrefix);
                }

                return new UniformArrayInfo(
                    itemTypeMarker,
                    itemCount,
                    nestedArrayInfo.ItemCount * nestedArrayInfo.ItemSize,
                    getArrayOrObjectPrefixLength(arrayTypeMarker),
                    nestedArrayInfo);
            }

            default:
                return null;
        }
    }

    private static class ValueLengths
    {
        private static final int L1 = -1;           // 1-byte length
        private static final int L2 = -2;           // 2-byte length
        private static final int L4 = -3;           // 4-byte length
        private static final int LC1 = -4;          // 1-byte length followed by 1-byte count
        private static final int LC2 = -5;          // 2-byte length followed by 2-byte count
        private static final int LC4 = -6;          // 4-byte length followed by 4-byte count
        private static final int CS4L1 = -7;        // 4-bit Compressed string w/ 1-byte length
        private static final int CS7L1 = -8;        // 7-bit Compressed string w/ 1-byte length
        private static final int CS7L2 = -9;        // 7-bit Compressed string w/ 2-byte length
        private static final int CS4BL1 = -10;      // 4-bit Compressed string w/ 1-byte length followed by 1-byte base char
        private static final int CS5BL1 = -11;      // 5-bit Compressed string w/ 1-byte length followed by 1-byte base char
        private static final int CS6BL1 = -12;      // 6-bit Compressed string w/ 1-byte length followed by 1-byte base char
        private static final int Arr1 = -13;        // 1-item array
        private static final int Obj1 = -14;        // 1-property object

        /// <summary>
        /// Lookup table for encoded value length for each TypeMarker value (0 to 255)
        /// The lengths are encoded as follows:
        /// - Positive Value: The length of the value including its TypeMarker
        /// - Negative Value: The length is encoded as an integer of size equals to abs(value) following the TypeMarker byte
        /// - Zero Value: The length is unknown (for instance an unassigned type marker)
        /// </summary>
        public static final int[] Lookup = new int[] {
            // Encoded literal integer value (32 values)
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,

            // Encoded 1-byte system string (32 values)
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,

            // Encoded 1-byte user string (32 values)
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,

            // Encoded 2-byte user string (8 values)
            2, 2, 2, 2, 2, 2, 2, 2,

            // String Values [0x68, 0x70)
            0,      // <empty> 0x68
            0,      // <empty> 0x69
            0,      // <empty> 0x6A
            0,      // <empty> 0x6B
            0,      // <empty> 0x6C
            0,      // <empty> 0x6D
            0,      // <empty> 0x6E
            0,      // <empty> 0x6F

            // String Values [0x70, 0x78)
            0,      // <empty> 0x70
            0,      // <empty> 0x71
            0,      // <empty> 0x72
            0,      // <empty> 0x73
            0,      // <empty> 0x74
            17,     // StrGL (Lowercase GUID string)
            17,     // StrGU (Uppercase GUID string)
            17,     // StrGQ (Double-quoted lowercase GUID string)

            // Compressed strings [0x78, 0x80)
            CS4L1,  // String 1-byte length - Lowercase hexadecimal digits encoded as 4-bit characters
            CS4L1,  // String 1-byte length - Uppercase hexadecimal digits encoded as 4-bit characters
            CS4L1,  // String 1-byte length - Date-time character set encoded as 4-bit characters
            CS4BL1, // String 1-byte Length - 4-bit packed characters relative to a base value
            CS5BL1, // String 1-byte Length - 5-bit packed characters relative to a base value
            CS6BL1, // String 1-byte Length - 6-bit packed characters relative to a base value
            CS7L1,  // String 1-byte Length - 7-bit packed characters
            CS7L2,  // String 2-byte Length - 7-bit packed characters

            // TypeMarker-encoded string length (64 values)
            1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16,
            17, 18, 19, 20, 21, 22, 23, 24,
            25, 26, 27, 28, 29, 30, 31, 32,
            33, 34, 35, 36, 37, 38, 39, 40,
            41, 42, 43, 44, 45, 46, 47, 48,
            49, 50, 51, 52, 53, 54, 55, 56,
            57, 58, 59, 60, 61, 62, 63, 64,

            // Variable Length String Values
            L1,     // StrL1 (1-byte length)
            L2,     // StrL2 (2-byte length)
            L4,     // StrL4 (4-byte length)
            2,      // StrR1 (Reference string of 1-byte offset)
            3,      // StrR2 (Reference string of 2-byte offset)
            4,      // StrR3 (Reference string of 3-byte offset)
            5,      // StrR4 (Reference string of 4-byte offset)
            0,      // <empty> 0xC7

            // Number Values
            2,      // NumUI8
            3,      // NumI16,
            5,      // NumI32,
            9,      // NumI64,
            9,      // NumDbl,
            5,      // Float32
            9,      // Float64
            0,      // <empty> 0xCF

            // Other Value Types
            1,      // Null
            1,      // False
            1,      // True
            17,     // GUID
            0,      // <empty> 0xD4
            0,      // <empty> 0xD5
            0,      // <empty> 0xD6
            0,      // <empty> 0xD7

            2,      // Int8
            3,      // Int16
            5,      // Int32
            9,      // Int64
            5,      // UInt32
            L1,     // BinL1 (1-byte length)
            L2,     // BinL2 (2-byte length)
            L4,     // BinL4 (4-byte length)

            // Array Type Markers
            1,      // Arr0
            Arr1,   // Arr1
            L1,     // ArrL1 (1-byte length)
            L2,     // ArrL2 (2-byte length)
            L4,     // ArrL4 (4-byte length)
            LC1,    // ArrLC1 (1-byte length and count)
            LC2,    // ArrLC2 (2-byte length and count)
            LC4,    // ArrLC4 (4-byte length and count)

            // Object Type Markers
            1,      // Obj0
            Obj1,   // Obj1
            L1,     // ObjL1 (1-byte length)
            L2,     // ObjL2 (2-byte length)
            L4,     // ObjL4 (4-byte length)
            LC1,    // ObjLC1 (1-byte length and count)
            LC2,    // ObjLC2 (2-byte length and count)
            LC4,    // ObjLC4 (4-byte length and count)

            // Empty Range
            0,      // <empty> 0xF0
            0,      // <empty> 0xF1
            0,      // <empty> 0xF2
            0,      // <empty> 0xF3
            0,      // <empty> 0xF4
            0,      // <empty> 0xF5
            0,      // <empty> 0xF6
            0,      // <empty> 0xF7

            // Special Values
            0,      // <special value reserved> 0xF8
            0,      // <special value reserved> 0xF9
            0,      // <special value reserved> 0xFA
            0,      // <special value reserved> 0xFB
            0,      // <special value reserved> 0xFC
            0,      // <special value reserved> 0xFD
            0,      // <special value reserved> 0xFE
            0,      // Invalid
        };
    }
}
