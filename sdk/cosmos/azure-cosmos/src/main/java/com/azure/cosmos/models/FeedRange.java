package com.azure.cosmos.models;

import com.azure.cosmos.implementation.feedranges.FeedRangeInternal;

import java.io.IOException;

public interface FeedRange {
    /**
     * Creates a range from a previously obtained string representation.
     *
     * @param json A string representation obtained from {@link json}
     * @return A {@link FeedRange}
     */
    public static FeedRange fromJsonString(String json) {
        FeedRange parsedRange = null;

        try {
            parsedRange = FeedRangeInternal.tryParse(json);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                String.format("Unable to parse JSON %s", json), e);
        }

        if (parsedRange == null) {
            throw new IllegalArgumentException(
                String.format(
                    "The provided string '%s' does not represent any known format.",
                    json));
        }

        return parsedRange;
    }

    public String toJsonString();
}
