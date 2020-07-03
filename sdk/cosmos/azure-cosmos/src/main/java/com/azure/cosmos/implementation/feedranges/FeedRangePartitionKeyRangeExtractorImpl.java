package com.azure.cosmos.implementation.feedranges;

import com.azure.cosmos.BridgeInternal;
import com.azure.cosmos.implementation.ChangeFeedOptions;
import com.azure.cosmos.implementation.DocumentCollection;
import com.azure.cosmos.implementation.PartitionKeyRange;
import com.azure.cosmos.implementation.ResourceResponse;
import com.azure.cosmos.implementation.RxDocumentClientImpl;
import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.implementation.apachecommons.collections.list.UnmodifiableList;
import com.azure.cosmos.implementation.caches.RxPartitionKeyRangeCache;
import com.azure.cosmos.implementation.routing.Range;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FeedRangePartitionKeyRangeExtractorImpl
    extends FeedRangeAsyncVisitor<UnmodifiableList<Range<String>>> {

    private final RxDocumentClientImpl client;
    private final String collectionLink;

    public FeedRangePartitionKeyRangeExtractorImpl(RxDocumentClientImpl client,
                                                   String collectionLink,
                                                   ChangeFeedOptions changeFeedOptions) {
        this.client = client;
        this.collectionLink = collectionLink;
    }

    @Override
    public Mono<UnmodifiableList<Range<String>>> visitAsync(FeedRangePartitionKeyImpl feedRange) {
        RxPartitionKeyRangeCache partitionKeyRangeCache = this.client.getPartitionKeyRangeCache();
        Mono<ResourceResponse<DocumentCollection>> collectionResponseObservable = this.client
            .readCollection(this.collectionLink, null);

        return collectionResponseObservable.flatMap(collectionResponse -> {
            DocumentCollection collection = collectionResponse.getResource();
            return feedRange.getEffectiveRangesAsync(
                partitionKeyRangeCache,
                collection.getResourceId(),
                collection.getPartitionKey());
        });
    }

    @Override
    public Mono<UnmodifiableList<Range<String>>> visitAsync(FeedRangePartitionKeyRangeImpl feedRange) {
        RxPartitionKeyRangeCache partitionKeyRangeCache = this.client.getPartitionKeyRangeCache();
        Mono<ResourceResponse<DocumentCollection>> collectionResponseObservable = this.client
            .readCollection(this.collectionLink, null);

        return collectionResponseObservable.flatMap(collectionResponse -> {
            DocumentCollection collection = collectionResponse.getResource();
            return feedRange.getEffectiveRangesAsync(
                partitionKeyRangeCache,
                collection.getResourceId(),
                null);
        });
    }

    @Override
    public Mono<UnmodifiableList<Range<String>>> visitAsync(FeedRangeEPKImpl feedRange) {
        RxPartitionKeyRangeCache partitionKeyRangeCache = this.client.getPartitionKeyRangeCache();
        Mono<ResourceResponse<DocumentCollection>> collectionResponseObservable = this.client
            .readCollection(this.collectionLink, null);

        Mono<Utils.ValueHolder<List<PartitionKeyRange>>> valueHolderMono =
            collectionResponseObservable.flatMap(collectionResponse -> {
                    DocumentCollection collection = collectionResponse.getResource();
                    return partitionKeyRangeCache
                        .tryGetOverlappingRangesAsync(
                            BridgeInternal.getMetaDataDiagnosticContext(null),
                            collection.getResourceId(),
                            feedRange.getRange(),
                            false,
                            null);
                }
            );

        return valueHolderMono.map(partitionKeyRangeListResponse -> {
            return toFeedRanges(partitionKeyRangeListResponse);
        });
    }

    private static UnmodifiableList<Range<String>> toFeedRanges(
        Utils.ValueHolder<List<PartitionKeyRange>> partitionKeyRangeListValueHolder) {
        final List<PartitionKeyRange> partitionKeyRangeList = partitionKeyRangeListValueHolder.v;
        if (partitionKeyRangeList == null) {
            throw new IllegalStateException("PartitionKeyRange list cannot be null");
        }

        List<Range<String>> feedRanges = new ArrayList<Range<String>>();
        partitionKeyRangeList.forEach(pkRange -> {
            feedRanges.add(pkRange.toRange());
        });

        UnmodifiableList<Range<String>> feedRangesResult =
            (UnmodifiableList<Range<String>>)Collections.unmodifiableList(feedRanges);

        return feedRangesResult;
    }
}
