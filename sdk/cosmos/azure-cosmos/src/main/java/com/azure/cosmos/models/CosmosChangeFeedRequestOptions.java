package com.azure.cosmos.models;

import com.azure.cosmos.implementation.RxDocumentServiceRequest;
import com.azure.cosmos.implementation.changefeed.implementation.ChangeFeedRequestOptionsImpl;
import com.azure.cosmos.implementation.changefeed.implementation.ChangeFeedStartFromInternal;
import com.azure.cosmos.implementation.feedranges.FeedRangeContinuation;
import com.azure.cosmos.implementation.feedranges.FeedRangeInternal;

import java.time.Instant;

public final class CosmosChangeFeedRequestOptions {
    private static final Integer DEFAULT_MAX_ITEM_COUNT = 1000;
    private FeedRangeInternal feedRangeInternal;
    private Integer maxItemCount;
    private ChangeFeedStartFromInternal startFromInternal;

    private CosmosChangeFeedRequestOptions(
        FeedRangeInternal feedRange,
        ChangeFeedStartFromInternal startFromInternal) {

        super();
        this.maxItemCount = DEFAULT_MAX_ITEM_COUNT;
    }

    public FeedRange getFeedRange() {
        return this.feedRangeInternal;
    }

    /**
     * Gets the maximum number of items to be returned in the enumeration
     * operation.
     *
     * @return the max number of items.
     */
    public Integer getMaxItemCount() {
        return this.maxItemCount;
    }

    /**
     * Sets the maximum number of items to be returned in the enumeration
     * operation.
     *
     * @param maxItemCount the max number of items.
     * @return the FeedOptionsBase.
     */
    public CosmosChangeFeedRequestOptions setMaxItemCount(Integer maxItemCount) {
        this.maxItemCount = maxItemCount;
        return this;
    }

    public static CosmosChangeFeedRequestOptions createForProcessingFromBeginning(FeedRange feedRange) {
        if (feedRange == null) {
            throw new NullPointerException("feedRange");
        }

        return new CosmosChangeFeedRequestOptions(
            FeedRangeInternal.convert(feedRange),
            ChangeFeedStartFromInternal.createFromBeginning());
    }

    public static CosmosChangeFeedRequestOptions createForProcessingFromContinuation(
        String continuation) {

        if (continuation == null) {
            throw new NullPointerException("continuation");
        }

        final FeedRangeContinuation feedRangeContinuation =
            FeedRangeContinuation.tryParse(continuation);

        if (feedRangeContinuation == null) {
            final String message = String.format(
                "The provided string '%s' does not represent any known format.",
                continuation);
            throw new IllegalArgumentException(message);
        }

        final FeedRangeInternal feedRange = feedRangeContinuation.getFeedRangeInternal();
        final String continuationToken = feedRangeContinuation.getContinuation();
        if (continuation != null) {
            return new CosmosChangeFeedRequestOptions(
                feedRange,
                ChangeFeedStartFromInternal.createFromContinuation(continuationToken));
        }

        return new CosmosChangeFeedRequestOptions(
            feedRange,
            ChangeFeedStartFromInternal.createFromBeginning());
    }

    public static CosmosChangeFeedRequestOptions createForProcessingFromNow(FeedRange feedRange) {
        if (feedRange == null) {
            throw new NullPointerException("feedRange");
        }

        return new CosmosChangeFeedRequestOptions(
            FeedRangeInternal.convert(feedRange),
            ChangeFeedStartFromInternal.createFromNow());
    }

    public static CosmosChangeFeedRequestOptions createForProcessingFromPointInTime(
        Instant pointInTime,
        FeedRange feedRange) {

        if (pointInTime == null) {
            throw new NullPointerException("pointInTime");
        }

        if (feedRange == null) {
            throw new NullPointerException("feedRange");
        }

        return new CosmosChangeFeedRequestOptions(
            FeedRangeInternal.convert(feedRange),
            ChangeFeedStartFromInternal.createFromPointInTime(pointInTime));
    }

    void populateRequestOptions(RxDocumentServiceRequest request) {
        ChangeFeedRequestOptionsImpl.populateRequestOptions(
            this,
            request,
            this.startFromInternal,
            this.feedRangeInternal
        );
    }
}
