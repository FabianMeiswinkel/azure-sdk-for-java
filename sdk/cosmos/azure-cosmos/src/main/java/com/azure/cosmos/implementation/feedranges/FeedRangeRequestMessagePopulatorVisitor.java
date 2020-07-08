package com.azure.cosmos.implementation.feedranges;

import java.util.Map;

import com.azure.cosmos.implementation.HttpConstants;
import com.azure.cosmos.implementation.RxDocumentServiceRequest;

public class FeedRangeRequestMessagePopulatorVisitor extends FeedRangeVisitor {
    private final RxDocumentServiceRequest request;

    public FeedRangeRequestMessagePopulatorVisitor(RxDocumentServiceRequest request) {
        if(request == null) {
            throw new NullPointerException("request");
        }

        this.request = request;
    }

    @Override
    public void visit(FeedRangeEPKImpl feedRange) {
        if (feedRange == null) {
            throw new NullPointerException("feedRange");
        }

        Map<String, Object> properties = this.request.getProperties();

        // In case EPK has already been set by compute
        if (properties.containsKey(EpkRequestPropertyConstants.START_EPK_STRING))
        {
            return;
        }

        properties.put(EpkRequestPropertyConstants.END_EPK_STRING, feedRange.getRange().getMax());
        properties.put(EpkRequestPropertyConstants.START_EPK_STRING, feedRange.getRange().getMin());
    }

    @Override
    public void visit(FeedRangePartitionKeyRangeImpl feedRange) {
        if (feedRange == null) {
            throw new NullPointerException("feedRange");
        }

        this.request.routeTo(
            feedRange.getPartitionKeyRangeIdentity());
    }

    @Override
    public void visit(FeedRangePartitionKeyImpl feedRange) {
        if (feedRange == null) {
            throw new NullPointerException("feedRange");
        }

        this.request.getHeaders().put(
            HttpConstants.HttpHeaders.PARTITION_KEY,
            feedRange.getPartitionKeyInternal().toJson());
        this.request.setPartitionKeyInternal(feedRange.getPartitionKeyInternal());
    }
}