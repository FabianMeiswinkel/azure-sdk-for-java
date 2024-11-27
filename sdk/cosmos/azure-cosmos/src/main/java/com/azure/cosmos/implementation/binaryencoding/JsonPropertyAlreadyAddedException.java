// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonParseException;

public class JsonPropertyAlreadyAddedException extends JsonParseException {
    public JsonPropertyAlreadyAddedException() {
        super("Encountered a JSON property name after another JSON property name.");
    }
}

