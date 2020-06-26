package com.azure.cosmos.implementation.feedranges;

import java.io.IOException;

import com.azure.cosmos.implementation.IRoutingMapProvider;
import com.azure.cosmos.implementation.JsonSerializable;
import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.implementation.apachecommons.collections.list.UnmodifiableList;
import com.azure.cosmos.implementation.routing.Range;
import com.azure.cosmos.models.FeedRange;
import com.azure.cosmos.models.PartitionKeyDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

public abstract class FeedRangeInternal extends JsonSerializable implements FeedRange {
    public abstract void accept(FeedRangeVisitor visitor);

    public abstract Mono<UnmodifiableList<Range<String>>> getEffectiveRangesAsync(IRoutingMapProvider routingMapProvider,
                                                                                  String containerRid, PartitionKeyDefinition partitionKeyDefinition);

    public abstract Mono<UnmodifiableList<String>> getPartitionKeyRangesAsync(IRoutingMapProvider routingMapProvider,
                                                                              String containerRid
        , PartitionKeyDefinition partitionKeyDefinition);

    @Override
    public String toJsonString() {
        return this.toJson();
    }

    @Override
    public abstract String toString();

    public static FeedRangeInternal tryParse(String jsonString) throws IOException {
        final ObjectMapper mapper = Utils.getSimpleObjectMapper();
        final FeedRangeInternal feedRange = mapper.readValue(jsonString, FeedRangeInternal.class);
        return feedRange;
    }
}
