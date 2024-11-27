// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonParseException;

public class JsonMissingEndArrayException extends JsonParseException {
    public JsonMissingEndArrayException() {
        super("Missing an end array (\"]\") symbol in JSON.");
    }
}

