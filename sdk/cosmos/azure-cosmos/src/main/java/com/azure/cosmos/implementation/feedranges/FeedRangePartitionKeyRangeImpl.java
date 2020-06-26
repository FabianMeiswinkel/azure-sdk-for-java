package com.azure.cosmos.implementation.feedranges;

import com.azure.cosmos.implementation.IRoutingMapProvider;
import com.azure.cosmos.implementation.PartitionKeyRange;
import com.azure.cosmos.implementation.Utils.ValueHolder;
import com.azure.cosmos.implementation.apachecommons.collections.list.UnmodifiableList;
import com.azure.cosmos.implementation.routing.Range;
import com.azure.cosmos.implementation.routing.RoutingMapProvider;
import com.azure.cosmos.models.PartitionKeyDefinition;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;

public final class FeedRangePartitionKeyRangeImpl extends FeedRangeInternal {
    private final String partitionKeyRangeId;

    public FeedRangePartitionKeyRangeImpl(String partitionKeyRangeId) {
        if (partitionKeyRangeId == null) {
            throw new NullPointerException("partitionKeyRangeId");
        }

        this.partitionKeyRangeId = partitionKeyRangeId;
    }

    @Override
    public void accept(FeedRangeVisitor visitor) {
        if (visitor == null) {
            throw new NullPointerException("visitor");
        }

        visitor.visit(this);
    }

    @Override
    public Mono<UnmodifiableList<Range<String>>> getEffectiveRangesAsync(
        IRoutingMapProvider routingMapProvider,
        String containerRid,
        PartitionKeyDefinition partitionKeyDefinition) {

        Mono<ValueHolder<PartitionKeyRange>> getPkRangeTask = routingMapProvider.tryGetPartitionKeyRangeByIdAsync(null, containerRid, partitionKeyRangeId, false, null)
            .flatMap((pkRangeHolder) -> {
                if (pkRangeHolder == null) {
                    return routingMapProvider.tryGetPartitionKeyRangeByIdAsync(null, containerRid, partitionKeyRangeId, true, null);
                }
                else {
                    return Mono.just(pkRangeHolder);
                }
            });

        return getPkRangeTask
            .flatMap((pkRangeHolder) -> {
                if (pkRangeHolder == null) {
                    // TODO fabianm throw exception --> see FeedRangePartitionKeyRange.cs
                }
                
                ArrayList<Range<String>> temp = new ArrayList<Range<String>>();
                temp.add(pkRangeHolder.v.toRange());

                return Mono.just((UnmodifiableList<Range<String>>)Collections.unmodifiableList(temp));
            });
    }

    @Override
    public Mono<UnmodifiableList<String>> getPartitionKeyRangesAsync(IRoutingMapProvider routingMapProvider,
                                                                     String containerRid,
                                                                     PartitionKeyDefinition partitionKeyDefinition) {
        ArrayList<String> temp = new ArrayList<String>();
        temp.add(this.partitionKeyRangeId);

        return Mono.just((UnmodifiableList<String>)Collections.unmodifiableList(temp));
    }

    @Override
    public String toJsonString() {
        return this.partitionKeyRangeId;
    }

    @Override
    public String toString() {
        return this.partitionKeyRangeId;
    }
}
