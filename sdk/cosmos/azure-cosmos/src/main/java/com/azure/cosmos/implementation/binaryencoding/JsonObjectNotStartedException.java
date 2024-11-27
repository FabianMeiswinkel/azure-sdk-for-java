// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonParseException;

public class JsonObjectNotStartedException extends JsonParseException {
    public JsonObjectNotStartedException() {
        super("Tried to write a JSON object end symbol (\"}\") without "
            + "first opening with a JSON object start symbol (\"{\").");
    }
}
