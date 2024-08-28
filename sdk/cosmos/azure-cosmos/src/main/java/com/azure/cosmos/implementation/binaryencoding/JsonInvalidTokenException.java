// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonParseException;

public class JsonInvalidTokenException extends JsonParseException {

    public JsonInvalidTokenException() {
        super("Encountered an element that is not a valid JSON value (false / null / true / object / array / number / string)");
    }
}
