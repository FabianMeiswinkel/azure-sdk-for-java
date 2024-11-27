// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonParseException;

public class JsonMissingEndObjectException extends JsonParseException {
    public JsonMissingEndObjectException() {
        super("Missing an end object (\"}\") symbol in JSON.");
    }
}

