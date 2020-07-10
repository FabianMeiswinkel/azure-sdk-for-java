package com.azure.cosmos.implementation.changefeed.implementation;

import com.azure.cosmos.implementation.HttpConstants;
import com.azure.cosmos.implementation.RxDocumentServiceRequest;
import com.azure.cosmos.implementation.feedranges.FeedRangeInternal;
import com.azure.cosmos.implementation.feedranges.FeedRangeRequestMessagePopulatorVisitor;
import com.azure.cosmos.models.CosmosChangeFeedRequestOptions;

public final class ChangeFeedRequestOptionsImpl {
    public static void populateRequestOptions(
        CosmosChangeFeedRequestOptions requestOptions,
        RxDocumentServiceRequest request,
        ChangeFeedStartFromInternal startFromInternal,
        FeedRangeInternal feedRange) {
        if (requestOptions == null) {
            throw new NullPointerException("requestOptions");
        }

        if (request == null) {
            throw new NullPointerException("request");
        }

        if (startFromInternal == null) {
            throw new NullPointerException("startFromInternal");
        }

        final PopulateStartFromRequestOptionVisitor populateRequestOptionsVisitor =
            new PopulateStartFromRequestOptionVisitor(request);
        startFromInternal.accept(populateRequestOptionsVisitor);

        Integer maxItemCount = requestOptions.getMaxItemCount();
        if (maxItemCount != null) {
            request.getHeaders().put(
                HttpConstants.HttpHeaders.PAGE_SIZE,
                maxItemCount.toString());
        }

        if (feedRange != null) {
            final FeedRangeRequestMessagePopulatorVisitor feedRangeVisitor =
                new FeedRangeRequestMessagePopulatorVisitor(request);
            feedRange.accept(feedRangeVisitor);
        }

        request.getHeaders().put(
            HttpConstants.HttpHeaders.A_IM,
            HttpConstants.A_IMHeaderValues.INCREMENTAL_FEED);
    }
}
