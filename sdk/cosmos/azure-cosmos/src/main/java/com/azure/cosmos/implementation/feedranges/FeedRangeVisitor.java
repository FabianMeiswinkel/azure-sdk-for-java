package com.azure.cosmos.implementation.feedranges;

public abstract class FeedRangeVisitor {
    public abstract void visit(FeedRangeEPKImpl feedRange);

    public abstract void visit(FeedRangePartitionKeyRangeImpl feedRange);

    public abstract void visit(FeedRangePartitionKeyImpl feedRange);
}
