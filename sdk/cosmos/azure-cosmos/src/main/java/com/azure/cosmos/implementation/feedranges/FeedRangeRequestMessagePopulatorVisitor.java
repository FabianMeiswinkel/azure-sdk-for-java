package com.azure.cosmos.implementation.feedranges;

import com.azure.cosmos.implementation.HttpConstants;
import com.azure.cosmos.implementation.RxDocumentServiceRequest;

import java.util.Map;

public class FeedRangeRequestMessagePopulatorVisitor extends FeedRangeVisitor {
    private final RxDocumentServiceRequest request;

    public FeedRangeRequestMessagePopulatorVisitor(final RxDocumentServiceRequest request) {
        if (request == null) {
            throw new NullPointerException("request");
        }

        this.request = request;
    }

    @Override
    public void visit(final FeedRangeEPKImpl feedRange) {
        if (feedRange == null) {
            throw new NullPointerException("feedRange");
        }

        final Map<String, Object> properties = this.request.getProperties();

        // In case EPK has already been set by compute
        if (properties.containsKey(EpkRequestPropertyConstants.START_EPK_STRING)) {
            return;
        }

        properties.put(EpkRequestPropertyConstants.END_EPK_STRING, feedRange.getRange().getMax());
        properties.put(EpkRequestPropertyConstants.START_EPK_STRING, feedRange.getRange().getMin());
    }

    @Override
    public void visit(final FeedRangePartitionKeyRangeImpl feedRange) {
        if (feedRange == null) {
            throw new NullPointerException("feedRange");
        }

        this.request.routeTo(feedRange.getPartitionKeyRangeIdentity());
    }

    @Override
    public void visit(final FeedRangePartitionKeyImpl feedRange) {
        if (feedRange == null) {
            throw new NullPointerException("feedRange");
        }

        this.request.getHeaders().put(
            HttpConstants.HttpHeaders.PARTITION_KEY,
            feedRange.getPartitionKeyInternal().toJson());
        this.request.setPartitionKeyInternal(feedRange.getPartitionKeyInternal());
    }
}