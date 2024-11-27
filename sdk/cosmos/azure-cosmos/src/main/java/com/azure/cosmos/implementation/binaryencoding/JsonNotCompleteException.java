// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonParseException;

public class JsonNotCompleteException extends JsonParseException {
    public JsonNotCompleteException() {
        super("Encountered a JSON property name without a corresponding property value.");
    }
}
