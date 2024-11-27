// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonParseException;

public class JsonUnexpectedEndArrayException extends JsonParseException {
    public JsonUnexpectedEndArrayException() {
        super("Read a JSON end array (\"]\") symbol without a matching JSON start array symbol (\"[\").");
    }
}
