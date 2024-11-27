// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonParseException;

public class JsonUnexpectedEndObjectException extends JsonParseException {
    public JsonUnexpectedEndObjectException() {
        super("Read a JSON end object (\"}\") symbol without a matching JSON start object symbol (\"{\").");
    }
}
