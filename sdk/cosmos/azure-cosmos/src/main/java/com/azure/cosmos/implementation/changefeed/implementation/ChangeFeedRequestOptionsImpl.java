package com.azure.cosmos.implementation.changefeed.implementation;

import com.azure.cosmos.implementation.RxDocumentServiceRequest;
import com.azure.cosmos.implementation.feedranges.FeedRangeInternal;
import com.azure.cosmos.models.CosmosChangeFeedRequestOptions;
import com.azure.cosmos.models.FeedRange;

import java.time.Instant;

public final class ChangeFeedRequestOptionsImpl {
    public static void populateRequestOptions(
        RxDocumentServiceRequest request,
        ChangeFeedStartFromInternal startFromInternal,
        FeedRangeInternal feedRange)
    {
    }

    public static CosmosChangeFeedRequestOptions createForProcessingFromBeginning(FeedRange feedRange) {
        // TODO fabianm - Implement
        return null;
    }

    public static CosmosChangeFeedRequestOptions createForProcessingFromNow(FeedRange feedRange) {
        // TODO fabianm - Implement
        return null;
    }

    public static CosmosChangeFeedRequestOptions createForProcessingFromPointInTime(
        Instant pointInTime,
        FeedRange feedRange) {

        // TODO fabianm - Implement
        return null;
    }

    public static CosmosChangeFeedRequestOptions createForProcessingFromContinuation(String continuationToken) {
        // TODO fabianm - Implement
        return null;
    }
}
