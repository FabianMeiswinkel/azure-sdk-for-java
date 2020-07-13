package com.azure.cosmos.implementation.feedranges;

import com.azure.cosmos.BridgeInternal;
import com.azure.cosmos.implementation.DocumentCollection;
import com.azure.cosmos.implementation.PartitionKeyRange;
import com.azure.cosmos.implementation.ResourceResponse;
import com.azure.cosmos.implementation.RxDocumentClientImpl;
import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.implementation.apachecommons.collections.list.UnmodifiableList;
import com.azure.cosmos.implementation.caches.RxPartitionKeyRangeCache;
import com.azure.cosmos.implementation.routing.Range;
import com.azure.cosmos.models.CosmosChangeFeedRequestOptions;

import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FeedRangePartitionKeyRangeExtractorImpl
    extends FeedRangeAsyncVisitor<UnmodifiableList<Range<String>>> {

    private final RxDocumentClientImpl client;
    private final String collectionLink;

    public FeedRangePartitionKeyRangeExtractorImpl(
        final RxDocumentClientImpl client,
        final String collectionLink) {

        this.client = client;
        this.collectionLink = collectionLink;
    }

    @Override
    public Mono<UnmodifiableList<Range<String>>> visitAsync(final FeedRangePartitionKeyImpl feedRange) {
        final RxPartitionKeyRangeCache partitionKeyRangeCache =
            this.client.getPartitionKeyRangeCache();
        final Mono<ResourceResponse<DocumentCollection>> collectionResponseObservable = this.client
            .readCollection(this.collectionLink, null);

        return collectionResponseObservable.flatMap(collectionResponse -> {
            final DocumentCollection collection = collectionResponse.getResource();
            return feedRange.getEffectiveRangesAsync(partitionKeyRangeCache,
                collection.getResourceId(),
                collection.getPartitionKey());
        });
    }

    @Override
    public Mono<UnmodifiableList<Range<String>>> visitAsync(final FeedRangePartitionKeyRangeImpl feedRange) {
        final RxPartitionKeyRangeCache partitionKeyRangeCache =
            this.client.getPartitionKeyRangeCache();
        final Mono<ResourceResponse<DocumentCollection>> collectionResponseObservable = this.client
            .readCollection(this.collectionLink, null);

        return collectionResponseObservable.flatMap(collectionResponse -> {
            final DocumentCollection collection = collectionResponse.getResource();
            return feedRange.getEffectiveRangesAsync(partitionKeyRangeCache,
                collection.getResourceId(), null);
        });
    }

    @Override
    public Mono<UnmodifiableList<Range<String>>> visitAsync(final FeedRangeEPKImpl feedRange) {
        final RxPartitionKeyRangeCache partitionKeyRangeCache =
            this.client.getPartitionKeyRangeCache();
        final Mono<ResourceResponse<DocumentCollection>> collectionResponseObservable = this.client
            .readCollection(this.collectionLink, null);

        final Mono<Utils.ValueHolder<List<PartitionKeyRange>>> valueHolderMono =
            collectionResponseObservable
            .flatMap(collectionResponse -> {
                final DocumentCollection collection = collectionResponse.getResource();
                return partitionKeyRangeCache.tryGetOverlappingRangesAsync(
                    BridgeInternal.getMetaDataDiagnosticContext(null), collection.getResourceId(),
                    feedRange.getRange(), false, null);
            });

        return valueHolderMono.map(partitionKeyRangeListResponse -> {
            return toFeedRanges(partitionKeyRangeListResponse);
        });
    }

    private static UnmodifiableList<Range<String>> toFeedRanges(
        final Utils.ValueHolder<List<PartitionKeyRange>> partitionKeyRangeListValueHolder) {
        final List<PartitionKeyRange> partitionKeyRangeList = partitionKeyRangeListValueHolder.v;
        if (partitionKeyRangeList == null) {
            throw new IllegalStateException("PartitionKeyRange list cannot be null");
        }

        final List<Range<String>> feedRanges = new ArrayList<Range<String>>();
        partitionKeyRangeList.forEach(pkRange -> {
            feedRanges.add(pkRange.toRange());
        });

        final UnmodifiableList<Range<String>> feedRangesResult =
            (UnmodifiableList<Range<String>>)UnmodifiableList.unmodifiableList(feedRanges);

        return feedRangesResult;
    }
}
