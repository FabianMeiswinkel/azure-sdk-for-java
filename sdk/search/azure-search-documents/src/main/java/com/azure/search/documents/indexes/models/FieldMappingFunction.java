// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.search.documents.indexes.models;

import com.azure.core.annotation.Fluent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Represents a function that transforms a value from a data source before
 * indexing.
 */
@Fluent
public final class FieldMappingFunction {
    /*
     * The name of the field mapping function.
     */
    @JsonProperty(value = "name", required = true)
    private String name;

    /*
     * A dictionary of parameter name/value pairs to pass to the function. Each
     * value must be of a primitive type.
     */
    @JsonProperty(value = "parameters")
    private Map<String, Object> parameters;

    /**
     * Constructor of {@link FieldMappingFunction}.
     *
     * @param name The name of the field mapping function.
     */
    @JsonCreator
    public FieldMappingFunction(@JsonProperty(value = "name", required = true) String name) {
        this.name = name;
    }

    /**
     * Get the name property: The name of the field mapping function.
     *
     * @return the name value.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the parameters property: A dictionary of parameter name/value pairs
     * to pass to the function. Each value must be of a primitive type.
     *
     * @return the parameters value.
     */
    public Map<String, Object> getParameters() {
        return this.parameters;
    }

    /**
     * Set the parameters property: A dictionary of parameter name/value pairs
     * to pass to the function. Each value must be of a primitive type.
     *
     * @param parameters the parameters value to set.
     * @return the FieldMappingFunction object itself.
     */
    public FieldMappingFunction setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
        return this;
    }
}
