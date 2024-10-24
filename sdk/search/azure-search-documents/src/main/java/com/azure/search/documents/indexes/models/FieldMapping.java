// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.search.documents.indexes.models;

import com.azure.core.annotation.Fluent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines a mapping between a field in a data source and a target field in an
 * index.
 */
@Fluent
public final class FieldMapping {
    /*
     * The name of the field in the data source.
     */
    @JsonProperty(value = "sourceFieldName", required = true)
    private String sourceFieldName;

    /*
     * The name of the target field in the index. Same as the source field name
     * by default.
     */
    @JsonProperty(value = "targetFieldName")
    private String targetFieldName;

    /*
     * A function to apply to each source field value before indexing.
     */
    @JsonProperty(value = "mappingFunction")
    private FieldMappingFunction mappingFunction;

    /**
     * Constructor of {@link FieldMapping}.
     *
     * @param sourceFieldName The name of the field in the data source.
     */
    @JsonCreator
    public FieldMapping(@JsonProperty(value = "sourceFieldName") String sourceFieldName) {
        this.sourceFieldName = sourceFieldName;
    }

    /**
     * Get the sourceFieldName property: The name of the field in the data
     * source.
     *
     * @return the sourceFieldName value.
     */
    public String getSourceFieldName() {
        return this.sourceFieldName;
    }

    /**
     * Get the targetFieldName property: The name of the target field in the
     * index. Same as the source field name by default.
     *
     * @return the targetFieldName value.
     */
    public String getTargetFieldName() {
        return this.targetFieldName;
    }

    /**
     * Set the targetFieldName property: The name of the target field in the
     * index. Same as the source field name by default.
     *
     * @param targetFieldName the targetFieldName value to set.
     * @return the FieldMapping object itself.
     */
    public FieldMapping setTargetFieldName(String targetFieldName) {
        this.targetFieldName = targetFieldName;
        return this;
    }

    /**
     * Get the mappingFunction property: A function to apply to each source
     * field value before indexing.
     *
     * @return the mappingFunction value.
     */
    public FieldMappingFunction getMappingFunction() {
        return this.mappingFunction;
    }

    /**
     * Set the mappingFunction property: A function to apply to each source
     * field value before indexing.
     *
     * @param mappingFunction the mappingFunction value to set.
     * @return the FieldMapping object itself.
     */
    public FieldMapping setMappingFunction(FieldMappingFunction mappingFunction) {
        this.mappingFunction = mappingFunction;
        return this;
    }
}
