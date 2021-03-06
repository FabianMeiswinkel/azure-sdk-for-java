// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.search.documents.test.environment.models;

import com.azure.search.documents.indexes.SearchableFieldProperty;

public class HotelAnalyzerException {
    @SearchableFieldProperty(analyzerName = "en.microsoft", indexAnalyzer = "whitespce")
    private String tag;

    /**
     * Gets the tag.
     *
     * @return The tag of hotel.
     */
    public String getTag() {
        return tag;
    }

    /**
     * Sets the pattern.
     *
     * @param tag The tag of hotel.
     * @return the {@link HotelAnalyzerException} object itself.
     */
    public HotelAnalyzerException setTag(String tag) {
        this.tag = tag;
        return this;
    }


}
