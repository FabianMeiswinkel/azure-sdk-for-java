// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.ai.formrecognizer.models;

import com.azure.core.annotation.Immutable;

import java.util.Collections;
import java.util.Map;

/**
 * The CustomFormSubmodel model.
 */
@Immutable
public final class CustomFormSubmodel {

    /*
     * Estimated extraction accuracy for this model.
     */
    private final Float accuracy;

    /*
     * A map of the fields recognized from the input document.
     * For models trained with labels, this is the training-time label of the field. For models trained with forms
     * only, a unique name is generated for each field.
     */
    private final Map<String, CustomFormModelField> fields;

    /*
     * The form type.
     */
    private final String formType;

    /**
     * Constructs a CustomFormSubmodel object.
     *
     * @param accuracy The estimated extraction accuracy for this model.
     * @param fields The Map of fields used to train the model.
     * @param formType The recognized form type.
     */
    public CustomFormSubmodel(final Float accuracy, final Map<String, CustomFormModelField> fields,
        final String formType) {
        this.accuracy = accuracy;
        this.fields = fields == null ? null : Collections.unmodifiableMap(fields);
        this.formType = formType;
    }

    /**
     * Get the estimated extraction accuracy for this model.
     *
     * @return the accuracy value.
     */
    public Float getAccuracy() {
        return this.accuracy;
    }

    /**
     * Gets the recognized form type for the model.
     *
     * @return the form type for the model.
     */
    public String getFormType() {
        return this.formType;
    }

    /**
     * A map of the fields recognized from the input document.
     * For models trained with labels, this is the training-time label of the field. For models trained with forms
     * only, a unique name is generated for each field.
     *
     * @return the unmodifiable map of recognized fields.
     */
    public Map<String, CustomFormModelField> getFields() {
        return this.fields;
    }
}
