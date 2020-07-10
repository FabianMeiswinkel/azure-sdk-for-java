package com.azure.cosmos.implementation.feedranges;

interface FeedRangeContinuationVisitor {
    void visit(final FeedRangeCompositeContinuationImpl feedRangeCompositeContinuation);
}