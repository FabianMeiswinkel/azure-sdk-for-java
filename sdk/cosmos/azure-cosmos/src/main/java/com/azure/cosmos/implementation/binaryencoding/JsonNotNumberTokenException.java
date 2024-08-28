package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonParseException;

public class JsonNotNumberTokenException extends JsonParseException {
    public JsonNotNumberTokenException() {
        super("Encountered a value that was not a JSON number.");
    }
}
