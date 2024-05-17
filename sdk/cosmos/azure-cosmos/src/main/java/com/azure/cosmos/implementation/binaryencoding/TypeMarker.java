// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

/// <summary>
/// Defines the set of type-marker values that are used to encode JSON value
/// </summary>
public class TypeMarker {

    //region [0x00, 0x20): Encoded literal integer value (32 values)
    // ---------------------------------------------------------------------
    /// <summary>
    /// The first integer what can be encoded in the type marker itself.
    /// </summary>
    /// <example>1 can be encoded as LiterIntMin + 1.</example>
    public final static byte LiteralIntMin = 0x00;

    /// <summary>
    /// The last integer what can be encoded in the type marker itself.
    /// </summary>
    /// <example>1 can be encoded as LiterIntMin + 1.</example>
    public final static byte LiteralIntMax = LiteralIntMin + 32;
    //endregion

    //region [0x20, 0x40): Encoded 1-byte system string (32 values)
    // ---------------------------------------------------------------------
    /// <summary>
    /// The first type marker for a system string whose value can be encoded in a 1 byte type marker.
    /// </summary>
    public final static byte SystemString1ByteLengthMin = LiteralIntMax;

    /// <summary>
    /// The last type marker for a system string whose value can be encoded in a 1 byte type marker.
    /// </summary>
    public final static byte SystemString1ByteLengthMax = SystemString1ByteLengthMin + 32;
    //endregion

    //region [0x40, 0x60): Encoded 1-byte user string (32 values)
    // ---------------------------------------------------------------------
    /// <summary>
    /// The first type marker for a user string whose value can be encoded in a 1 byte type marker.
    /// </summary>
    public final static byte UserString1ByteLengthMin = SystemString1ByteLengthMax;

    /// <summary>
    /// The last type marker for a user string whose value can be encoded in a 1 byte type marker.
    /// </summary>
    public final static byte UserString1ByteLengthMax = UserString1ByteLengthMin + 32;
    //endregion

    //region [0x60, 0x68): 2-byte user string (8 values)
    // ---------------------------------------------------------------------
    /// <summary>
    /// The first type marker for a system string whose value can be encoded in a 2 byte type marker.
    /// </summary>
    public final static byte UserString2ByteLengthMin = UserString1ByteLengthMax;

    /// <summary>
    /// The last type marker for a system string whose value can be encoded in a 2 byte type marker.
    /// </summary>
    public final static byte UserString2ByteLengthMax = UserString2ByteLengthMin + 8;
    //endregion

    //region [0x68, 0x70): String Values (8 Values)
    // ---------------------------------------------------------------------
    // <empty> 0x68
    // <empty> 0x69
    // <empty> 0x6A
    // <empty> 0x6B
    // <empty> 0x6C
    // <empty> 0x6D
    // <empty> 0x6E
    // <empty> 0x6F
    //endregion

    //region [0x70, 0x78): String Values (8 Values)
    // ---------------------------------------------------------------------
    // <empty> 0x70
    // <empty> 0x71
    // <empty> 0x72
    // <empty> 0x73
    // <empty> 0x74

    /// <summary>
    /// The type marker for a guid string with only lowercase characters.
    /// </summary>
    public final static byte LowercaseGuidString = 0x75;

    /// <summary>
    /// The type marker for a guid string with only uppercase characaters.
    /// </summary>
    public final static byte UppercaseGuidString = 0x76;

    /// <summary>
    /// The type marker for a guid string that is double quoted (ETAG).
    /// </summary>
    public final static byte DoubleQuotedLowercaseGuidString = 0x77;
    //endregion

    //region [0x78, 0x80): Compressed String (8 Values)
    // ---------------------------------------------------------------------
    /// <summary>
    /// String 1-byte length - Lowercase hexadecimal digits encoded as 4-bit characters
    /// </summary>
    public final static byte CompressedLowercaseHexString = 0x78;

    /// <summary>
    /// String 1-byte length - Uppercase hexadecimal digits encoded as 4-bit characters
    /// </summary>
    public final static byte CompressedUppercaseHexString = 0x79;

    /// <summary>
    /// String 1-byte length - Date-time character set encoded as 4-bit characters
    /// </summary>
    public final static byte CompressedDateTimeString = 0x7A;

    /// <summary>
    /// String 1-byte Length - 4-bit packed characters relative to a base value
    /// </summary>
    public final static byte Packed4BitString = 0x7B;

    /// <summary>
    /// String 1-byte Length - 5-bit packed characters relative to a base value
    /// </summary>
    public final static byte Packed5BitString = 0x7C;

    /// <summary>
    /// String 1-byte Length - 6-bit packed characters relative to a base value
    /// </summary>
    public final static byte Packed6BitString = 0x7D;

    /// <summary>
    /// String 1-byte Length - 7-bit packed characters
    /// </summary>
    public final static byte Packed7BitStringLength1 = 0x7E;

    /// <summary>
    /// String 2-byte Length - 7-bit packed characters
    /// </summary>
    public final static byte Packed7BitStringLength2 = 0x7F;
    //endregion

    //region [0x80, 0xC0): Encoded string length (64 values)
    // ---------------------------------------------------------------------
    /// <summary>
    /// The first type marker for a string whose length is encoded.
    /// </summary>
    /// <example>EncodedStringLengthMin + 1 is a type marker for a string with length 1.</example>
    public final static byte EncodedStringLengthMin = (byte) 0x80;

    /// <summary>
    /// The last type marker for a string whose length is encoded.
    /// </summary>
    /// <example>EncodedStringLengthMin + 1 is a type marker for a string with length 1.</example>
    public final static byte EncodedStringLengthMax = EncodedStringLengthMin + 64;
    //endregion

    //region [0xC0, 0xC8): Variable Length Strings
    // ---------------------------------------------------------------------
    /// <summary>
    /// Type marker for a String of 1-byte length
    /// </summary>
    public final static byte String1ByteLength = (byte) 0xC0;

    /// <summary>
    /// Type marker for a String of 2-byte length
    /// </summary>
    public final static byte String2ByteLength = (byte) 0xC1;

    /// <summary>
    /// Type marker for a String of 4-byte length
    /// </summary>
    public final static byte String4ByteLength = (byte) 0xC2;

    /// <summary>
    /// Reference string of 1-byte offset
    /// </summary>
    public final static byte ReferenceString1ByteOffset = (byte) 0xC3;

    /// <summary>
    /// Reference string of 2-byte offset
    /// </summary>
    public final static byte ReferenceString2ByteOffset = (byte) 0xC4;

    /// <summary>
    /// Reference string of 3-byte offset
    /// </summary>
    public final static byte ReferenceString3ByteOffset = (byte) 0xC5;

    /// <summary>
    /// Reference string of 4-byte offset
    /// </summary>
    public final static byte ReferenceString4ByteOffset = (byte) 0xC6;
    // <empty> 0xC7
    //endregion

    //region [0xC8, 0xD0): Number Values
    // ---------------------------------------------------------------------
    /// <summary>
    /// Type marker for a 1-byte unsigned integer
    /// </summary>
    public final static byte NumberUInt8 = (byte) 0xC8;

    /// <summary>
    /// Type marker for a 2-byte singed integer
    /// </summary>
    public final static byte NumberInt16 = (byte) 0xC9;

    /// <summary>
    /// Type marker for a 4-byte singed integer
    /// </summary>
    public final static byte NumberInt32 = (byte) 0xCA;

    /// <summary>
    /// Type marker for a 8-byte singed integer
    /// </summary>
    public final static byte NumberInt64 = (byte) 0xCB;

    /// <summary>
    /// Type marker for a Double-precession floating point number
    /// </summary>
    public final static byte NumberDouble = (byte) 0xCC;

    /// <summary>
    /// Type marker for a single precision floating point number.
    /// </summary>
    public final static byte Float32 = (byte) 0xCD;

    /// <summary>
    /// Type marker for double precision floating point number.
    /// </summary>
    public final static byte Float64 = (byte) 0xCE;
    // <number reserved> 0xCF
    //endregion

    //region [0xDO, 0xE0): Other Value Types
    // ---------------------------------------------------------------------
    /// <summary>
    /// The type marker for a JSON null value.
    /// </summary>
    public final static byte Null = (byte) 0xD0;

    /// <summary>
    /// The type marker for a JSON false value.
    /// </summary>
    public final static byte False = (byte) 0xD1;

    /// <summary>
    /// The type marker for a JSON true value
    /// </summary>
    public final static byte True = (byte) 0xD2;

    /// <summary>
    /// The type marker for a GUID
    /// </summary>
    public final static byte Guid = (byte) 0xD3;

    // <other types empty> 0xD4
    // <other types empty> 0xD5
    // <other types empty> 0xD6
    // <other types empty> 0xD7

    /// <summary>
    /// The type marker for a 1-byte signed integer value.
    /// </summary>
    public final static byte Int8 = (byte) 0xD8;

    /// <summary>
    /// The type marker for a 2-byte signed integer value.
    /// </summary>
    public final static byte Int16 = (byte) 0xD9;

    /// <summary>
    /// The type marker for a 4-byte signed integer value.
    /// </summary>
    public final static byte Int32 = (byte) 0xDA;

    /// <summary>
    /// The type marker for a 8-byte signed integer value.
    /// </summary>
    public final static byte Int64 = (byte) 0xDB;

    /// <summary>
    /// The type marker for a 4-byte signed integer value.
    /// </summary>
    public final static byte UInt32 = (byte) 0xDC;

    /// <summary>
    /// Type marker for binary payloads with 1 byte length.
    /// </summary>
    public final static byte Binary1ByteLength = (byte) 0xDD;

    /// <summary>
    /// Type marker for binary payloads with 2 byte length.
    /// </summary>
    public final static byte Binary2ByteLength = (byte) 0xDE;

    /// <summary>
    /// Type marker for binary payloads with 4 byte length.
    /// </summary>
    public final static byte Binary4ByteLength = (byte) 0xDF;
    //endregion

    //region [0xE0, 0xE8): Array Type Markers
    // ---------------------------------------------------------------------
    /// <summary>
    /// Empty array type marker.
    /// </summary>
    public final static byte EmptyArray = (byte) 0xE0;

    /// <summary>
    /// Single-item array type marker.
    /// </summary>
    public final static byte SingleItemArray = (byte) 0xE1;

    /// <summary>
    /// Array of 1-byte length type marker.
    /// </summary>
    public final static byte Array1ByteLength = (byte) 0xE2;

    /// <summary>
    /// Array of 2-byte length type marker.
    /// </summary>
    public final static byte Array2ByteLength = (byte) 0xE3;

    /// <summary>
    /// Array of 4-byte length type marker.
    /// </summary>
    public final static byte Array4ByteLength = (byte) 0xE4;

    /// <summary>
    /// Array of 1-byte length and item count type marker.
    /// </summary>
    public final static byte Array1ByteLengthAndCount = (byte) 0xE5;

    /// <summary>
    /// Array of 2-byte length and item count type marker.
    /// </summary>
    public final static byte Array2ByteLengthAndCount = (byte) 0xE6;

    /// <summary>
    /// Array of 4-byte length and item count type marker.
    /// </summary>
    public final static byte Array4ByteLengthAndCount = (byte) 0xE7;
    //endregion

    //region [0xE8, 0xF0): Object Type Markers
    // ---------------------------------------------------------------------
    /// <summary>
    /// Empty object type marker.
    /// </summary>
    public final static byte EmptyObject = (byte) 0xE8;

    /// <summary>
    /// Single-property object type marker.
    /// </summary>
    public final static byte SinglePropertyObject = (byte) 0xE9;

    /// <summary>
    /// Object of 1-byte length type marker.
    /// </summary>
    public final static byte Object1ByteLength = (byte) 0xEA;

    /// <summary>
    /// Object of 2-byte length type marker.
    /// </summary>
    public final static byte Object2ByteLength = (byte) 0xEB;

    /// <summary>
    /// Object of 4-byte length type maker.
    /// </summary>
    public final static byte Object4ByteLength = (byte) 0xEC;

    /// <summary>
    /// Object of 1-byte length and property count type marker.
    /// </summary>
    public final static byte Object1ByteLengthAndCount = (byte) 0xED;

    /// <summary>
    /// Object of 2-byte length and property count type marker.
    /// </summary>
    public final static byte Object2ByteLengthAndCount = (byte) 0xEE;

    /// <summary>
    /// Object of 4-byte length and property count type marker.
    /// </summary>
    public final static byte Object4ByteLengthAndCount = (byte) 0xEF;
    //endregion

    //region [0xF0, 0xF8): Empty Range
    // ---------------------------------------------------------------------
    // <empty> 0xF0
    // <empty> 0xF1
    // <empty> 0xF2
    // <empty> 0xF3
    // <empty> 0xF4
    // <empty> 0xF5
    // <empty> 0xF6
    // <empty> 0xF7
    //endregion

    //region [0xF8, 0xFF]: Special Values
    // ---------------------------------------------------------------------
    // <special value reserved> 0xF8
    // <special value reserved> 0xF9
    // <special value reserved> 0xFA
    // <special value reserved> 0xFB
    // <special value reserved> 0xFC
    // <special value reserved> 0xFD
    // <special value reserved> 0xFE

    /// <summary>
    /// Type marker reserved to communicate an invalid type marker.
    /// </summary>
    public final static byte Invalid = (byte) 0xFF;
    //endregion

    //region Number Type Marker Utility Functions
    // ---------------------------------------------------------------------
    /// <summary>
    /// Gets whether an integer can be encoded as a literal.
    /// </summary>
    /// <param name="value">The input integer.</param>
    /// <returns>Whether an integer can be encoded as a literal.</returns>
    public static boolean IsEncodedNumberLiteral(long value) {
        return InRange(value, LiteralIntMin, LiteralIntMax);
    }

    /// <summary>
    /// Gets whether an integer is a fixed length integer.
    /// </summary>
    /// <param name="value">The input integer.</param>
    /// <returns>Whether an integer is a fixed length integer.</returns>
    public static boolean IsFixedLengthNumber(long value) {
        return InRange(value, NumberUInt8, NumberDouble + 1);
    }

    /// <summary>
    /// Gets whether an integer is a number.
    /// </summary>
    /// <param name="value">The input integer.</param>
    /// <returns>Whether an integer is a number.</returns>
    public static boolean IsNumber(long value) {
        return IsEncodedNumberLiteral(value) || IsFixedLengthNumber(value);
    }

    /// <summary>
    /// Encodes an integer as a literal.
    /// </summary>
    /// <param name="value">The input integer.</param>
    /// <returns>The integer encoded as a literal if it can; else Invalid</returns>
    public static byte EncodeIntegerLiteral(long value) {
        return IsEncodedNumberLiteral(value) ? (byte) (LiteralIntMin + value) : Invalid;
    }
    //endregion

    //region String Type Markers Utility Functions
    /// <summary>
    /// Gets whether a typeMarker is for a system string.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the typeMarker is for a system string.</returns>
    public static boolean IsSystemString(byte typeMarker) {
        return InRange(typeMarker, SystemString1ByteLengthMin, SystemString1ByteLengthMax);
    }

    /// <summary>
    /// Gets whether a typeMarker is for a one byte encoded user string.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the typeMarker is for a one byte encoded user string.</returns>
    public static boolean IsOneByteEncodedUserString(byte typeMarker) {
        return InRange(typeMarker, UserString1ByteLengthMin, UserString1ByteLengthMax);
    }

    /// <summary>
    /// Gets whether a typeMarker is for a two byte encoded user string.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the typeMarker is for a two byte encoded user string.</returns>
    public static boolean IsTwoByteEncodedUserString(byte typeMarker) {
        return InRange(typeMarker, UserString2ByteLengthMin, UserString2ByteLengthMax);
    }

    /// <summary>
    /// Gets whether a typeMarker is for a user string.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the typeMarker is for a user string.</returns>
    public static boolean IsUserString(byte typeMarker) {
        return IsOneByteEncodedUserString(typeMarker) || IsTwoByteEncodedUserString(typeMarker);
    }

    /// <summary>
    /// Gets whether a typeMarker is for a one byte encoded string.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the typeMarker is for a one byte encoded string.</returns>
    public static boolean IsOneByteEncodedString(byte typeMarker) {
        return InRange(typeMarker, SystemString1ByteLengthMin, UserString1ByteLengthMax);
    }

    /// <summary>
    /// Gets whether a typeMarker is for a two byte encoded string.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the typeMarker is for a two byte encoded string.</returns>
    public static boolean IsTwoByteEncodedString(byte typeMarker) {
        return IsTwoByteEncodedUserString(typeMarker);
    }

    /// <summary>
    /// Gets whether a typeMarker is for an encoded string.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the typeMarker is for an encoded string.</returns>
    public static boolean IsEncodedString(byte typeMarker) {
        return InRange(typeMarker, SystemString1ByteLengthMin, UserString2ByteLengthMax);
    }

    /// <summary>
    /// Gets whether a typeMarker is for an encoded length string.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the typeMarker is for an encoded string.</returns>
    public static boolean IsEncodedLengthString(byte typeMarker) {
        return InRange(typeMarker, EncodedStringLengthMin, EncodedStringLengthMax);
    }

    /// <summary>
    /// Gets whether a typeMarker is for a compressed string.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the typeMarker is for a compressed string.</returns>
    public static boolean IsCompressedString(byte typeMarker) {
        return InRange(typeMarker, CompressedLowercaseHexString, Packed7BitStringLength2 + 1);
    }

    /// <summary>
    /// Gets whether a typeMarker is for a variable length string.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the typeMarker is for a variable length string.</returns>
    public static boolean IsVariableLengthString(byte typeMarker) {
        return IsEncodedLengthString(typeMarker) || InRange(typeMarker, String1ByteLength, String4ByteLength + 1);
    }

    /// <summary>
    /// Gets whether a typeMarker is for a reference string.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the typeMarker is for a reference string.</returns>
    public static boolean IsReferenceString(byte typeMarker) {
        return InRange(typeMarker, ReferenceString1ByteOffset, ReferenceString4ByteOffset + 1);
    }

    /// <summary>
    /// Gets whether a typeMarker is for a GUID string.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the typeMarker is for a GUID string.</returns>
    public static boolean IsGuidString(byte typeMarker) {
        return InRange(typeMarker, LowercaseGuidString, DoubleQuotedLowercaseGuidString + 1);
    }

    /// <summary>
    /// Gets whether a typeMarker is for a hexadecimal string.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the typeMarker is for a hexadecimal string.</returns>
    public static boolean IsHexadecimalString(byte typeMarker) {
        return InRange(typeMarker, CompressedLowercaseHexString, CompressedUppercaseHexString + 1);
    }

    /// <summary>
    /// Gets whether a typeMarker is for a datetime string.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the typeMarker is for a datetime string.</returns>
    public static boolean IsDateTimeString(byte typeMarker) {
        return typeMarker == CompressedDateTimeString;
    }

    /// <summary>
    /// Gets whether a typeMarker is for a string.
    /// </summary>
    /// <param name="typeMarker">The type maker.</param>
    /// <returns>Whether the typeMarker is for a string.</returns>

    public static boolean IsString(byte typeMarker) {
        return InRange(typeMarker, SystemString1ByteLengthMin, UserString2ByteLengthMax)
            || InRange(typeMarker, LowercaseGuidString, ReferenceString4ByteOffset + 1);
    }

    /// <summary>
    /// Gets the length of a encoded string type marker.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>The length of the encoded string type marker.</returns>
    public static long GetEncodedStringLength(byte typeMarker) {
        return typeMarker & (EncodedStringLengthMin - 1);
    }

    /// <summary>
    /// Gets the type marker for an encoded string of a particular length.
    /// </summary>
    /// <param name="length">The length of the encoded string.</param>
    /// <param name="typeMarker">The type marker for the encoded string of particular length if valid.</param>
    /// <returns>Whether or not the there is a typemarker for the string of a particular length.</returns>
    public static Byte TryGetEncodedStringLengthTypeMarker(long length) {
        if (length >= (EncodedStringLengthMax - EncodedStringLengthMin)) {
            return null;
        }

        return (byte) (length | EncodedStringLengthMin);
    }
    //endregion

    //region Other Primitive Type Markers Utility Functions
    /// <summary>
    /// Gets whether a type maker is the null type marker.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the type maker is the null type marker.</returns>
    public static boolean IsNull(byte typeMarker) {
        return typeMarker == Null;
    }

    /// <summary>
    /// Gets whether a type maker is the false type marker.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the type maker is the false type marker.</returns>
    public static boolean IsFalse(byte typeMarker) {
        return typeMarker == False;
    }

    /// <summary>
    /// Gets whether a type maker is the true type marker.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the type maker is the true type marker.</returns>
    public static boolean IsTrue(byte typeMarker) {
        return typeMarker == True;
    }

    /// <summary>
    /// Gets whether a type maker is a boolean type marker.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the type maker is a boolean type marker.</returns>
    public static boolean IsBoolean(byte typeMarker) {
        return (typeMarker == False) || (typeMarker == True);
    }

    public static boolean IsGuid(byte typeMarker) {
        return typeMarker == Guid;
    }
    //endregion

    //region Array/Object Type Markers
    /// <summary>
    /// Gets whether a type marker is the empty array type marker.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the type marker is the empty array type marker.</returns>
    public static boolean IsEmptyArray(byte typeMarker) {
        return typeMarker == EmptyArray;
    }

    /// <summary>
    /// Gets whether a type marker is for an array.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the type marker is for an array.</returns>
    public static boolean IsArray(byte typeMarker) {
        return InRange(typeMarker, EmptyArray, Array4ByteLengthAndCount + 1);
    }

    /// <summary>
    /// Gets whether a type marker is the empty object type marker.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the type marker is the empty object type marker.</returns>
    public static boolean IsEmptyObject(byte typeMarker) {
        return typeMarker == EmptyObject;
    }

    /// <summary>
    /// Gets whether a type marker is for an object.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the type marker is for an object.</returns>
    public static boolean IsObject(byte typeMarker) {
        return InRange(typeMarker, EmptyObject, Object4ByteLengthAndCount + 1);
    }
    //endregion

    //region Common Utility Functions
    /// <summary>
    /// Gets whether a type marker is valid.
    /// </summary>
    /// <param name="typeMarker">The input type marker.</param>
    /// <returns>Whether the type marker is valid.</returns>
    public static boolean IsValid(byte typeMarker) {
        return typeMarker != Invalid;
    }

    public static boolean InRange(long value, long minInclusive, long maxExclusive) {
        return (value >= minInclusive) && (value < maxExclusive);
    }
    //endregion
}
