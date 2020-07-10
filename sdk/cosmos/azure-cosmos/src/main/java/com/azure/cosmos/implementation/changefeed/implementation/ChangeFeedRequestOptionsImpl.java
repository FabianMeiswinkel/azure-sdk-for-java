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
        // TODO fabianm - Implement
    }
}
