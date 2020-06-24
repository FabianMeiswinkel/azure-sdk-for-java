package com.azure.cosmos.implementation.feedranges;

import com.azure.cosmos.implementation.JsonSerializable;
import com.azure.cosmos.implementation.apachecommons.collections.list.UnmodifiableList;
import com.azure.cosmos.implementation.routing.Range;
import com.azure.cosmos.implementation.routing.RoutingMapProvider;
import com.azure.cosmos.models.PartitionKeyDefinition;
import com.azure.cosmos.models.FeedRange;

import reactor.core.publisher.Mono;

public abstract class FeedRangeInternal extends JsonSerializable implements FeedRange {
    public abstract Mono<UnmodifiableList<Range<String>>> getEffectiveRangesAsync(
            RoutingMapProvider routingMapProvider,
            String containerRid,
            PartitionKeyDefinition partitionKeyDefinition);

        public abstract Mono<UnmodifiableList<String>> getPartitionKeyRangesAsync(
            RoutingMapProvider routingMapProvider,
            String containerRid,
            PartitionKeyDefinition partitionKeyDefinition);

        public abstract void accept(FeedRangeVisitor visitor);

        @Override
        public abstract String toString();

        @Override
        public String toJsonString() {
            return this.toJson();
        }

    public static FeedRangeInternal tryParse(String jsonString) {
        return null;
    }
}
