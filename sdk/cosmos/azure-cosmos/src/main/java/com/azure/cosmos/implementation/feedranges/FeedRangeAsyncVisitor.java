package com.azure.cosmos.implementation.feedranges;

import reactor.core.publisher.Mono;

public abstract class FeedRangeAsyncVisitor<TResult> {
    public abstract Mono<TResult> visitAsync(FeedRangePartitionKeyImpl feedRange);

    public abstract Mono<TResult> visitAsync(FeedRangePartitionKeyRangeImpl feedRange);

    public abstract Mono<TResult> visitAsync(FeedRangeEPKImpl feedRange);
}
