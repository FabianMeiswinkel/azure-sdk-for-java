// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.azure.cosmos.implementation.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Objects;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public final class UtfAllString {
    private final Utf8ByteBuffer utf8String;
    private final Utf8ByteBuffer utf8EscapedString;
    private final String utf16String;
    private final String utf16EscapedString;


    private UtfAllString(
        Utf8ByteBuffer utf8String,
        String utf16String,
        Utf8ByteBuffer utf8EscapedString,
        String utf16EscapedString) {
        this.utf8String = utf8String;
        this.utf16String = utf16String;
        this.utf8EscapedString = utf8EscapedString;
        this.utf16EscapedString = utf16EscapedString;
    }

    public Utf8ByteBuffer getUtf8String() {
        return this.utf8String;
    }

    public String getUtf16String() {
        return this.utf16String;
    }

    public Utf8ByteBuffer getUtf8EscapedString() {
        return this.utf8EscapedString;
    }

    public String getUtf16EscapedString() {
        return this.utf16EscapedString;
    }

    public static UtfAllString create(String utf16String) {
        checkNotNull(utf16String, "Parameter 'utf16String' MUST NOT be null.");

        Utf8ByteBuffer utf8String = Utf8ByteBuffer.create(utf16String);
        try {
            String utf16EscapedString = Utils.getSimpleObjectMapper().writeValueAsString(utf16String);
            utf16EscapedString = utf16EscapedString.substring(1, utf16EscapedString.length() - 2);

            Utf8ByteBuffer utf8EscapedString = Utf8ByteBuffer.create(utf16EscapedString);

            return new UtfAllString(utf8String, utf16String, utf8EscapedString, utf16EscapedString);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                "Parameter 'utf16String' is not a valid UTF-16 char sequence.",
                e);
        }
    }

    public static UtfAllString create(Utf8ByteBuffer utf8String) {
        String utf16String = utf8String.toString();
        try {
            String utf16EscapedString = Utils.getSimpleObjectMapper().writeValueAsString(utf16String);
            utf16EscapedString = utf16EscapedString.substring(1, utf16EscapedString.length() - 2);
            Utf8ByteBuffer utf8EscapedString = Utf8ByteBuffer.create(utf16EscapedString);


            return new UtfAllString(utf8String, utf16String, utf8EscapedString, utf16EscapedString);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                "Parameter 'utf8String' is not a valid UTF-8 byte sequence.",
                e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UtfAllString that = (UtfAllString) o;
        return Objects.equals(utf8String, that.utf8String);
    }

    @Override
    public int hashCode() {
        return Objects.hash(utf8String, utf16String);
    }
}
