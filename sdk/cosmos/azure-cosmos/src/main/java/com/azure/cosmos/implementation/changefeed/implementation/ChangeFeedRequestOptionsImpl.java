package com.azure.cosmos.implementation.changefeed.implementation;

import com.azure.cosmos.implementation.HttpConstants;
import com.azure.cosmos.implementation.RxDocumentServiceRequest;
import com.azure.cosmos.implementation.Strings;
import com.azure.cosmos.implementation.feedranges.FeedRangeContinuation;
import com.azure.cosmos.implementation.feedranges.FeedRangeInternal;
import com.azure.cosmos.implementation.feedranges.FeedRangeRequestMessagePopulatorVisitor;
import com.azure.cosmos.models.CosmosChangeFeedRequestOptions;

public final class ChangeFeedRequestOptionsImpl {
    public static void populateRequestOptions(
        CosmosChangeFeedRequestOptions requestOptions,
        RxDocumentServiceRequest request,
        ChangeFeedStartFromInternal startFromInternal,
        FeedRangeInternal feedRange,
        String continuation) {
        if (requestOptions == null) {
            throw new NullPointerException("requestOptions");
        }

        if (request == null) {
            throw new NullPointerException("request");
        }

        if (startFromInternal == null) {
            throw new NullPointerException("startFromInternal");
        }

        // Page size
        Integer maxItemCount = requestOptions.getMaxItemCount();
        if (maxItemCount != null) {
            request.getHeaders().put(
                HttpConstants.HttpHeaders.PAGE_SIZE,
                maxItemCount.toString());
        }

        final FeedRangeInternal effectiveFeedRange;
        final ChangeFeedStartFromInternal effectiveStartFrom;
        if (Strings.isNullOrWhiteSpace(continuation)) {
           effectiveFeedRange = feedRange;
           effectiveStartFrom = startFromInternal;
        }
        else {
            final FeedRangeContinuation feedRangeContinuation = FeedRangeContinuation.convert(continuation);
            effectiveFeedRange = feedRangeContinuation.getFeedRangeInternal();
            effectiveStartFrom = ChangeFeedStartFromInternal.createFromContinuation(feedRangeContinuation.getContinuation());
        }

        final PopulateStartFromRequestOptionVisitor populateRequestOptionsVisitor =
            new PopulateStartFromRequestOptionVisitor(request);
        effectiveStartFrom.accept(populateRequestOptionsVisitor);

        if (effectiveFeedRange != null) {
            final FeedRangeRequestMessagePopulatorVisitor feedRangeVisitor =
                new FeedRangeRequestMessagePopulatorVisitor(request);
            effectiveFeedRange.accept(feedRangeVisitor);
        }

        request.getHeaders().put(
            HttpConstants.HttpHeaders.A_IM,
            HttpConstants.A_IMHeaderValues.INCREMENTAL_FEED);
    }
}
