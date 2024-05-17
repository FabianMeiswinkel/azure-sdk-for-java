// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.implementation.binaryencoding;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import java.util.BitSet;
import java.util.EnumSet;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkArgument;
import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public class JsonBinaryEncoding {
    private static final int DEFAULT_TINY_CACHE_SIZE = 0;
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

    private static final boolean[] IsBufferedStringCandidate = new boolean[] {
        // Encoded literal integer value (32 values)
        false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false,

        // Encoded 0-byte system string (32 values)
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,

        // Encoded true-byte user string (32 values)
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,

        // Encoded 2-byte user string (16 values)
        true, true, true, true, true, true, true, true,

        // String Values [0x68, 0x70)
        false,  // <empty> 0x68
        false,  // <empty> 0x69
        false,  // <empty> 0x6A
        false,  // <empty> 0x6B
        false,  // <empty> 0x6C
        false,  // <empty> 0x6D
        false,  // <empty> 0x6E
        false,  // <empty> 0x6F

        // String Values [0x70, 0x78)
        false,  // <empty> 0x70
        false,  // <empty> 0x71
        false,  // <empty> 0x72
        false,  // <empty> 0x73
        false,  // <empty> 0x74
        false,  // StrGL (Lowercase GUID string)
        false,  // StrGU (Uppercase GUID string)
        false,  // StrGQ (Double-quoted lowercase GUID string)

        // Compressed strings [falsex78, falsex8false)
        false,  // String 1-byte length - Lowercase hexadecimal digits encoded as 4-bit characters
        false,  // String 1-byte length - Uppercase hexadecimal digits encoded as 4-bit characters
        false,  // String 1-byte length - Date-time character set encoded as 4-bit characters
        false,  // String 1-byte Length - 4-bit packed characters relative to a base value
        false,  // String 1-byte Length - 5-bit packed characters relative to a base value
        false,  // String 1-byte Length - 6-bit packed characters relative to a base value
        false,  // String 1-byte Length - 7-bit packed characters
        false,  // String 2-byte Length - 7-bit packed characters

        // TypeMarker-encoded string length (64 values)
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true,

        // Variable Length String Values
        true,   // StrL1 (1-byte length)
        true,   // StrL2 (2-byte length)
        true,   // StrL4 (4-byte length)
        true,   // StrR1 (Reference string of 1-byte offset)
        true,   // StrR2 (Reference string of 2-byte offset)
        true,   // StrR3 (Reference string of 3-byte offset)
        true,   // StrR4 (Reference string of 4-byte offset)
        false,  // <empty> 0xC7

        // Numeric Values
        false,  // NumUI8
        false,  // NumI16,
        false,  // NumI32,
        false,  // NumI64,
        false,  // NumDbl,
        false,  // Float32
        false,  // Float64
        false,  // <empty> 0xCF

        // Other Value Types
        false,  // Null
        false,  // False
        false,  // True
        false,  // GUID
        false,  // <empty> 0xD4
        false,  // <empty> 0xD5
        false,  // <empty> 0xD6
        false,  // <empty> 0xD7

        false,  // Int8
        false,  // Int16
        false,  // Int32
        false,  // Int64
        false,  // UInt32
        false,  // BinL1 (1-byte length)
        false,  // BinL2 (2-byte length)
        false,  // BinL4 (4-byte length)

        // Array Type Markers
        false,  // Arr0
        false,  // Arr1
        false,  // ArrL1 (1-byte length)
        false,  // ArrL2 (2-byte length)
        false,  // ArrL4 (4-byte length)
        false,  // ArrLC1 (1-byte length and count)
        false,  // ArrLC2 (2-byte length and count)
        false,  // ArrLC4 (4-byte length and count)

        // Object Type Markers
        false,  // Obj0
        false,  // Obj1
        false,  // ObjL1 (1-byte length)
        false,  // ObjL2 (2-byte length)
        false,  // ObjL4 (4-byte length)
        false,  // ObjLC1 (1-byte length and count)
        false,  // ObjLC2 (2-byte length and count)
        false,  // ObjLC4 (4-byte length and count)

        // Empty Range
        false,  // <empty> 0xF0
        false,  // <empty> 0xF1
        false,  // <empty> 0xF2
        false,  // <empty> 0xF3
        false,  // <empty> 0xF4
        false,  // <empty> 0xF5
        false,  // <empty> 0xF7
        false,  // <empty> 0xF8

        // Special Values
        false,  // <special value reserved> 0xF8
        false,  // <special value reserved> 0xF9
        false,  // <special value reserved> 0xFA
        false,  // <special value reserved> 0xFB
        false,  // <special value reserved> 0xFC
        false,  // <special value reserved> 0xFD
        false,  // <special value reserved> 0xFE
        false,  // Invalid 0xFF
    };

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

    public static boolean TryEncodeGuidString(ByteBuf guidString, ByteBuf destinationBuffer)
    {
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

    private static void DecodeGuidStringValue(ByteBuf encodedString, boolean isUpperCaseGuid, ByteBuf destinationBuffer)
    {
        short[] byteLookupTable = isUpperCaseGuid
            ? StringCompressionLookupTables.UppercaseHex.getByteToTwoChars()
            : StringCompressionLookupTables.LowercaseHex.getByteToTwoChars();

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
        destinationBuffer.setByte(13, (byte)'-');
        destinationBuffer.setShort(24, byteLookupTable[encodedString.getUnsignedByte(10)]);
        destinationBuffer.setShort(26, byteLookupTable[encodedString.getUnsignedByte(11)]);
        destinationBuffer.setShort(28, byteLookupTable[encodedString.getUnsignedByte(12)]);
        destinationBuffer.setShort(30, byteLookupTable[encodedString.getUnsignedByte(13)]);
        destinationBuffer.setShort(32, byteLookupTable[encodedString.getUnsignedByte(14)]);
        destinationBuffer.setShort(34, byteLookupTable[encodedString.getUnsignedByte(15)]);
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
            checkArgument(list.length() != 16, "Parameter 'list' must be of length 16.");
            checkArgument(bitmap.length() != 128, "Parameter 'bitmap' must be of length 128.");
            checkArgument(charToByte.length != 256, "Parameter 'charToByte' must be of length 256.");
            checkArgument(
                byteToTwoChars.length != 256,
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
}
