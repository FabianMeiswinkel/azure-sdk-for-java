// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkArgument;

/// <summary>
/// This class maintains the current state of a JSON object/value while it is being read or written.
/// </summary>
public final class JsonObjectState
{
    /// <summary>
    /// This constant defines the maximum nesting depth that the parser supports.
    /// The JSON spec states that this is an implementation dependent thing, so we're just picking a value for now.
    /// FWIW .Net chose 100
    /// Note: This value needs to be a multiple of 8 and must be less than 2^15 (see asserts in the constructor)
    /// </summary>
    private final static int JsonMaxNestingDepth = 256;

    /// <summary>
    /// Flag for determining whether to throw exceptions that connote a context at the end or not started / complete.
    /// </summary>
    private final boolean readMode;

    /// <summary>
    /// Stores a bitmap for whether we are in an array or object context at a particular level (0 => array, 1 => object).
    /// </summary>
    private final byte[] nestingStackBitmap;

    /// <summary>
    /// The current nesting stack index.
    /// </summary>
    private int nestingStackIndex;

    /// <summary>
    /// The current JsonObjectContext.
    /// </summary>
    private JsonObjectContext currentContext;

    private JsonTokenType currentTokenType;

    /// <summary>
    /// Initializes a new instance of the JsonObjectState class.
    /// </summary>
    /// <param name="readMode">Flag for determining whether to throw exceptions that correspond to a JsonReader or JsonWriter.</param>
    public JsonObjectState(boolean readMode, int JsonMaxNestingDepth)
    {
        checkArgument(
            JsonMaxNestingDepth % 8 == 0,
            "JsonMaxNestingDepth must be multiple of 8");
        checkArgument(
            JsonMaxNestingDepth < (1 << 15),
            "JsonMaxNestingDepth must be less than 2^15");

        this.readMode = readMode;
        this.nestingStackBitmap = new byte[JsonMaxNestingDepth / 8];
        this.nestingStackIndex = -1;
        this.currentTokenType = JsonTokenType.NotStarted;
        this.currentContext = JsonObjectContext.None;
    }

    /// <summary>
    /// JsonObjectContext enum
    /// </summary>
    private enum JsonObjectContext
    {
        /// <summary>
        /// Context at the start of the object state.
        /// </summary>
        None,

        /// <summary>
        /// Context when state is in an array.
        /// </summary>
        Array,

        /// <summary>
        /// Context when state is in an object.
        /// </summary>
        Object,
    }

    /// <summary>
    /// Gets the current depth (level of nesting).
    /// </summary>
    public int getCurrentDepth() {
        return this.nestingStackIndex + 1;
    }

    /// <summary>
    /// Gets the current JsonTokenType.
    /// </summary>
    public JsonTokenType getCurrentTokenType()
    {
        return this.currentTokenType;
    }

    /// <summary>
    /// Gets a value indicating whether a property is expected.
    /// </summary>
    public boolean isPropertyExpected() {
        return this.currentTokenType != JsonTokenType.FieldName
            && this.currentContext == JsonObjectContext.Object;
    }

    /// <summary>
    /// Gets a value indicating whether the current context is an array.
    /// </summary>
    public boolean isInArrayContext() {
        return this.currentContext == JsonObjectContext.Array;
    }

    /// <summary>
    /// Gets a value indicating whether the current context in an object.
    /// </summary>
    public boolean isInObjectContext() {
        return this.currentContext == JsonObjectContext.Object;
    }

    /// <summary>
    /// Gets the current JsonObjectContext
    /// </summary>
    private JsonObjectContext retrieveCurrentContext() {
        if (this.nestingStackIndex < 0)
        {
            return JsonObjectContext.None;
        }

        return (this.nestingStackBitmap[this.nestingStackIndex / 8] & this.getMask()) == 0
            ? JsonObjectContext.Array
            : JsonObjectContext.Object;
    }

    /// <summary>
    /// Gets a mask to use to get the current context from the nesting stack
    /// </summary>
    private byte getMask() {
        return (byte) (1 << (this.nestingStackIndex % 8));
    }

    /// <summary>
    /// Registers a JsonTokenType.
    /// </summary>
    /// <param name="jsonTokenType">The JsonTokenType to register.</param>
    public void registerToken(JsonTokenType jsonTokenType) throws JsonParseException {
        switch (jsonTokenType)
        {
            case String:
            case Number:
            case True:
            case False:
            case Null:
            case Float32:
            case Float64:
            case Int8:
            case Int16:
            case Int32:
            case Int64:
            case UInt32:
            case Binary:
            case Guid:
                this.registerValue(jsonTokenType);
                break;
            case BeginArray:
                this.registerBeginArray();
                break;
            case EndArray:
                this.registerEndArray();
                break;
            case BeginObject:
                this.registerBeginObject();
                break;
            case EndObject:
                this.registerEndObject();
                break;
            case FieldName:
                this.registerFieldName();
                break;
            default:
                throw new IllegalArgumentException("Failed to register JsonTokenType: " + jsonTokenType);
        }
    }

    /// <summary>
    /// Pushes a JsonObjectContext onto the nesting stack.
    /// </summary>
    /// <param name="isArray">Whether the JsonObjectContext is an array.</param>
    private void push(boolean isArray) throws JsonMaxNestingDepthExceededException {
        if (this.nestingStackIndex + 1 >= JsonMaxNestingDepth) {
            throw new JsonMaxNestingDepthExceededException();
        }

        this.nestingStackIndex++;

        if (isArray)
        {
            this.nestingStackBitmap[this.nestingStackIndex / 8] &= (byte)~this.getMask();
            this.currentContext = JsonObjectContext.Array;
        }
        else
        {
            this.nestingStackBitmap[this.nestingStackIndex / 8] |= this.getMask();
            this.currentContext = JsonObjectContext.Object;
        }
    }

    /// <summary>
    /// Registers any json token type.
    /// </summary>
    /// <param name="jsonTokenType">The jsonTokenType to register</param>
    private void registerValue(JsonTokenType jsonTokenType) throws JsonParseException {
        if ((this.currentContext == JsonObjectContext.Object)
            && (this.getCurrentTokenType() != JsonTokenType.FieldName))
        {
            throw new JsonMissingPropertyException();
        }

        if ((this.currentContext == JsonObjectContext.None)
            && (this.getCurrentTokenType() != JsonTokenType.NotStarted))
        {
            throw new JsonPropertyArrayOrObjectNotStartedException();
        }

        this.currentTokenType = jsonTokenType;
    }

    /// <summary>
    /// Registers a beginning of a json array ('[')
    /// </summary>
    private void registerBeginArray() throws JsonParseException {
        // An array start is also a value
        this.registerValue(JsonTokenType.BeginArray);
        this.push(true);
    }

    /// <summary>
    /// Registers the end of a json array (']')
    /// </summary>
    public void registerEndArray() throws JsonParseException {
        if (this.currentContext != JsonObjectContext.Array)
        {
            if (this.readMode)
            {
                throw new JsonUnexpectedEndArrayException();
            }
            else
            {
                throw new JsonArrayNotStartedException();
            }
        }

        this.nestingStackIndex--;
        this.currentTokenType = JsonTokenType.EndArray;
        this.currentContext = this.retrieveCurrentContext();
    }

    /// <summary>
    /// Registers a beginning of a json object ('{')
    /// </summary>
    private void registerBeginObject() throws JsonParseException {
        // An object start is also a value
        this.registerValue(JsonTokenType.BeginObject);
        this.push(false);
    }

    /// <summary>
    /// Registers an end of a json object ('}')
    /// </summary>
    public void registerEndObject() throws JsonParseException {
        if (this.currentContext != JsonObjectContext.Object)
        {
            if (this.readMode)
            {
                throw new JsonUnexpectedEndObjectException();
            }
            else
            {
                throw new JsonObjectNotStartedException();
            }
        }

        // check if we have a property name but not a value
        if (this.currentTokenType == JsonTokenType.FieldName)
        {
            if (this.readMode)
            {
                throw new JsonUnexpectedEndObjectException();
            }
            else
            {
                throw new JsonNotCompleteException();
            }
        }

        this.nestingStackIndex--;
        this.currentTokenType = JsonTokenType.EndObject;
        this.currentContext = this.retrieveCurrentContext();
    }

    /// <summary>
    /// Register a Json FieldName
    /// </summary>
    public void registerFieldName() throws JsonParseException {
        if (this.currentContext != JsonObjectContext.Object)
        {
            throw new JsonObjectNotStartedException();
        }

        if (this.currentTokenType == JsonTokenType.FieldName)
        {
            throw new JsonPropertyAlreadyAddedException();
        }

        this.currentTokenType = JsonTokenType.FieldName;
    }

    public JsonToken getCurrentJsonToken() {
        JsonTokenType currentTokenTypeSnapshot = this.currentTokenType;
        switch (currentTokenTypeSnapshot) {
            case NotStarted:
                return JsonToken.NOT_AVAILABLE;

            case BeginArray:
                return JsonToken.START_ARRAY;

            case EndArray:
                return JsonToken.END_ARRAY;

            case BeginObject:
                return JsonToken.START_OBJECT;

            case EndObject:
                return JsonToken.END_OBJECT;
            case True:
                return JsonToken.VALUE_TRUE;

            case False:
                return JsonToken.VALUE_FALSE;

            case Null:
                return JsonToken.VALUE_NULL;

            case FieldName:
                return JsonToken.FIELD_NAME;

            case Int8:
            case Int16:
            case Int32:
            case Int64:
            case UInt8:
            case UInt32:
                return JsonToken.VALUE_NUMBER_INT;

            case Float32:
            case Float64:
                return JsonToken.VALUE_NUMBER_FLOAT;

            case Number:
                return JsonToken.VALUE_NUMBER_FLOAT;
            case String:
            case Guid:
            case Binary:
                return JsonToken.VALUE_STRING;

            default:
                throw new IllegalStateException(
                    "Unknown json token type '" + currentTokenTypeSnapshot + "'.");
        }
    }
}
