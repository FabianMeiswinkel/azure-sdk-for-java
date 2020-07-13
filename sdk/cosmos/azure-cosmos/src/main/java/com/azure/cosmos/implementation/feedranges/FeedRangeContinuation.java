package com.azure.cosmos.implementation.feedranges;

import java.io.IOException;

import com.azure.cosmos.implementation.RxDocumentClientImpl;
import com.azure.cosmos.implementation.RxDocumentServiceResponse;
import com.azure.cosmos.implementation.ShouldRetryResult;
import com.azure.cosmos.implementation.Strings;
import com.azure.cosmos.models.FeedRange;
import reactor.core.publisher.Mono;

public abstract class FeedRangeContinuation {
    private final String containerRid;
    private final FeedRangeInternal feedRangeInternal;

    public FeedRangeContinuation(
        final String containerRid,
        final FeedRangeInternal feedRangeInternal) {
        if (containerRid == null) {
            throw new NullPointerException("containerRid");
        }

        if (feedRangeInternal == null) {
            throw new NullPointerException("feedRangeInternal");
        }

        this.feedRangeInternal = feedRangeInternal;
        this.containerRid = containerRid;
    }

    public String getContainerRid() {
        return this.containerRid;
    }

    public abstract String getContinuation();

    public abstract FeedRange getFeedRange();

    public FeedRangeInternal getFeedRangeInternal() {
        return this.feedRangeInternal;
    }

    public abstract void accept(final FeedRangeContinuationVisitor visitor);

    public abstract ShouldRetryResult handleChangeFeedNotModified(final RxDocumentServiceResponse response);

    public abstract Mono<ShouldRetryResult> handleSplitAsync(
        final RxDocumentClientImpl client,
        final RxDocumentServiceResponse response);

    public abstract Boolean isDone();

    public abstract void replaceContinuation(final String continuationToken);

    public static FeedRangeContinuation tryParse(
        String toStringValue) {
        
        try
        {
            return FeedRangeCompositeContinuationImpl.parse(toStringValue);
        }
        catch (final IOException ioError) {
            return null;
        }
    }

    public static FeedRangeContinuation convert(final String continuationToken) {
        if (Strings.isNullOrWhiteSpace(continuationToken)) {
            throw new NullPointerException("continuationToken");
        }

        try {
            FeedRangeContinuation returnValue = FeedRangeCompositeContinuationImpl.parse(continuationToken);
            if (returnValue != null) {
                return returnValue;
            }

            throw new IllegalArgumentException(
                String.format(
                    "Invalid Feed range continuation token '%s'",
                    continuationToken));
        } catch (final IOException ioError) {
            throw new IllegalArgumentException(
                String.format(
                    "Invalid Feed range continuation token '%s'",
                    continuationToken),
                 ioError);
        }
    }

    public abstract void validateContainer(final String containerRid) throws IllegalArgumentException;
}