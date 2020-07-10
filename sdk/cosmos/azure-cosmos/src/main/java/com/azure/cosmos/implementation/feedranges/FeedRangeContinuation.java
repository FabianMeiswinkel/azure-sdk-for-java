package com.azure.cosmos.implementation.feedranges;

import com.azure.cosmos.implementation.RxDocumentClientImpl;
import com.azure.cosmos.implementation.RxDocumentServiceResponse;
import com.azure.cosmos.implementation.ShouldRetryResult;
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
        return FeedRangeCompositeContinuationImpl.tryParse(toStringValue);
    }

    public abstract void validateContainer(final String containerRid) throws IllegalArgumentException;
}