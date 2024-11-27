// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonParseException;

public class JsonUnexpectedTokenException extends JsonParseException {
    public JsonUnexpectedTokenException() {
        super("Encountered an unexpected JSON token.");
    }
}
