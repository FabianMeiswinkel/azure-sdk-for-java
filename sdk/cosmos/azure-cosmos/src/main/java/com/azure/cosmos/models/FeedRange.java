package com.azure.cosmos.models;

import com.azure.cosmos.implementation.feedranges.FeedRangeInternal;

public interface FeedRange {
    public abstract String toJsonString();

    /**
     * Creates a range from a previously obtained string representation.
     * @param toStringValue A string representation obtained from {@Link toJsonString}
     * @return A {@Link FeedRange}
     */
    public static FeedRange fromJsonString(String toStringValue) {
        FeedRange parsedRange = FeedRangeInternal.tryParse(toStringValue);
        if (parsedRange == null)
        {
            throw new IllegalArgumentException(
                String.format(
                    "The provided string '%s' does not represent any known format.",
                    toStringValue));
        }

        return parsedRange;
    }
}
