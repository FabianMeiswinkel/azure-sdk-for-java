// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonParseException;

public class JsonPropertyArrayOrObjectNotStartedException extends JsonParseException {
    public JsonPropertyArrayOrObjectNotStartedException() {
        super("Either a JSON property array or object was not started.");
    }
}
