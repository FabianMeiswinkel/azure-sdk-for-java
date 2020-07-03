package com.azure.cosmos.implementation.feedranges;

import com.azure.cosmos.implementation.IRoutingMapProvider;
import com.azure.cosmos.implementation.PartitionKeyRange;
import com.azure.cosmos.implementation.apachecommons.collections.list.UnmodifiableList;
import com.azure.cosmos.implementation.routing.PartitionKeyInternal;
import com.azure.cosmos.implementation.routing.Range;
import com.azure.cosmos.models.ModelBridgeInternal;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.PartitionKeyDefinition;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;

public final class FeedRangePartitionKeyImpl extends FeedRangeInternal {
    private final PartitionKey partitionKey;
    private final UnmodifiableList<String> partitionKeyRangesList = null;

    public FeedRangePartitionKeyImpl(PartitionKey partitionKey) {
        if (partitionKey == null) {
            throw new NullPointerException("partitionKey");
        }

        this.partitionKey = partitionKey;
    }

    public PartitionKey getPartitionKey() {
        return this.partitionKey;
    }

    @Override
    public void accept(FeedRangeVisitor visitor) {
        if (visitor == null) {
            throw new NullPointerException("visitor");
        }

        visitor.visit(this);
    }

    @Override
    public <T> Mono<T> acceptAsync(FeedRangeAsyncVisitor<T> visitor) {
        if (visitor == null) {
            throw new NullPointerException("visitor");
        }

        return visitor.visitAsync(this);
    }

    @Override
    public Mono<UnmodifiableList<Range<String>>> getEffectiveRangesAsync(
        IRoutingMapProvider routingMapProvider,
        String containerRid,
        PartitionKeyDefinition partitionKeyDefinition) {

        PartitionKeyInternal pkInternal =
            ModelBridgeInternal.getPartitionKeyInternal(this.partitionKey);
        String effectivePartitionKey = pkInternal.getEffectivePartitionKeyString(pkInternal,
            partitionKeyDefinition);
        Range<String> range = Range.getPointRange(effectivePartitionKey);
        ArrayList<Range<String>> rangeList = new ArrayList<Range<String>>();
        rangeList.add(range);

        return Mono.just((UnmodifiableList<Range<String>>)Collections.unmodifiableList(rangeList));
    }

    @Override
    public Mono<UnmodifiableList<String>> getPartitionKeyRangesAsync(
        IRoutingMapProvider routingMapProvider,
        String containerRid,
        PartitionKeyDefinition partitionKeyDefinition) {

        PartitionKeyInternal pkInternal =
            ModelBridgeInternal.getPartitionKeyInternal(this.partitionKey);
        String effectivePartitionKey = pkInternal.getEffectivePartitionKeyString(pkInternal,
            partitionKeyDefinition);
        return routingMapProvider
            .tryGetOverlappingRangesAsync(
                null,
                containerRid,
                Range.getPointRange(effectivePartitionKey),
                false,
                null)
            .flatMap(pkRangeHolder -> {
                if (pkRangeHolder == null) {
                    // TODO fabianm - throw exception
                }

                String rangeId = pkRangeHolder.v.get(0).getId();
                ArrayList<String> rangeList = new ArrayList<String>();
                rangeList.add(rangeId);

                return Mono.just((UnmodifiableList<String>)Collections.unmodifiableList(rangeList));
            });
    }

    @Override
    public String toJsonString() {
        PartitionKeyInternal pkInternal =
            ModelBridgeInternal.getPartitionKeyInternal(this.partitionKey);
        return pkInternal.toJson();
    }

    @Override
    public String toString() {
        PartitionKeyInternal pkInternal =
            ModelBridgeInternal.getPartitionKeyInternal(this.partitionKey);
        return pkInternal.toJson();
    }

    private static Mono<PartitionKeyRange> tryGetRangeByEffectivePartitionKey(
        IRoutingMapProvider routingMapProvider,
        String containerRid,
        String effectivePartitionKey) {

        return routingMapProvider
            .tryGetOverlappingRangesAsync(
                null,
                containerRid,
                Range.getPointRange(effectivePartitionKey),
                false,
                null)
            .flatMap((pkRangeHolder) -> {
                if (pkRangeHolder == null) {
                    return Mono.empty();
                }

                return Mono.just(pkRangeHolder.v.get(0));
            });
    }
}
