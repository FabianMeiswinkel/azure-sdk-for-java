// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonParseException;

public class JsonMaxNestingDepthExceededException extends JsonParseException {
    public JsonMaxNestingDepthExceededException() {
        super("Exceeded the maximum level of nesting for JSON.");
    }
}

