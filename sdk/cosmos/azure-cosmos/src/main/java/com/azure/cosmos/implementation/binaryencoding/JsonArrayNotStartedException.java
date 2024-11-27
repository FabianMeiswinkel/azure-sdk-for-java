// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonParseException;

public class JsonArrayNotStartedException extends JsonParseException {
    public JsonArrayNotStartedException() {
        super("Tried to write a JSON end array (“]“) symbol without a matching array start symbol (“[“).");
    }
}
