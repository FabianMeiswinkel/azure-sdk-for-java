package com.azure.cosmos.models;

import com.azure.cosmos.implementation.feedranges.FeedRangeInternal;
import com.azure.cosmos.util.Beta;

import java.io.IOException;

@Beta(Beta.SinceVersion.V4_3_0)
public interface FeedRange {
    /**
     * Creates a range from a previously obtained string representation.
     *
     * @param json A string representation of a feed range
     * @return A feed range
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
