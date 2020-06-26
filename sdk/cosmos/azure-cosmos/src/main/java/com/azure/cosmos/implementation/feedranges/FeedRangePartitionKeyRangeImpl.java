package com.azure.cosmos.implementation.feedranges;

import java.io.IOException;
import java.util.Collections;

import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.implementation.apachecommons.collections.list.UnmodifiableList;
import com.azure.cosmos.implementation.routing.PartitionKeyInternalHelper;
import com.azure.cosmos.implementation.routing.Range;
import com.azure.cosmos.implementation.routing.RoutingMapProvider;
import com.azure.cosmos.models.PartitionKeyDefinition;

import reactor.core.publisher.Mono;

public final class FeedRangePartitionKeyRange extends FeedRangeInternal {
    private final String partitionKeyRangeId;
    private UnmodifiableList<String> partitionKeyRangesList = null;

    public FeedRangePartitionKeyRange(String partitionKeyRangeId) {
        if (partitionKeyRangeId == null) {
            throw new NullPointerException("partitionKeyRangeId");
        }

        this.partitionKeyRangeId = partitionKeyRangeId;
    }

    @Override
    public String toJsonString() {
        return this.partitionKeyRangeId;
    }

    @Override
    public Mono<UnmodifiableList<Range<String>>> getEffectiveRangesAsync(RoutingMapProvider routingMapProvider,
                                                                         String containerRid, PartitionKeyDefinition partitionKeyDefinition) {
        // TODO FABIANM Auto-generated method stub
        return null;
    }

    @Override
    public Mono<UnmodifiableList<String>> getPartitionKeyRangesAsync(RoutingMapProvider routingMapProvider,
                                                                     String containerRid, PartitionKeyDefinition partitionKeyDefinition) {
        if (this.partitionKeyRangesList != null)
        {
            return Mono.just(this.partitionKeyRangesList);
        }

        synchronized(this.partitionKeyRangeId) {
            if (this.partitionKeyRangesList == null) {


                ArrayList<String> temp = new ArrayList<String>();
                temp.Add(this.partitionKeyRangeId);
                this.partitionKeyRangeId = Collections.unmodifiableList(temp);
            }

            return Mono.just(this.partitionKeyRangesList);
        }
   }

    @Override
    public void accept(FeedRangeVisitor visitor) {
        // TODO FABIANM Auto-generated method stub

    }

    @Override
    public String toString() {
        return this.range.toString();
    }
}
