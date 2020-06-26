package com.azure.cosmos.implementation.feedranges;

public final class FeedRangeVisitor {
    // TODO fabianm - Implement ctor and additional overloads
    public FeedRangeVisitor() {

    }

    public void visit(FeedRangeEPKImpl feedRange) {
        // No-op since the range is defined by the composite continuation token
    }

    public void visit(FeedRangePartitionKeyRangeImpl feedRange) {
        // TODO fabianm - Implement
    }

    public void visit(FeedRangePartitionKeyImpl feedRange) {
        // TODO fabianm - Implement
    }
}