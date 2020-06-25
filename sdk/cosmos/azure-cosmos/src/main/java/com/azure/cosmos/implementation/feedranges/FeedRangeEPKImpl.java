package com.azure.cosmos.implementation.feedranges;

import java.io.IOException;

import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.implementation.apachecommons.collections.list.UnmodifiableList;
import com.azure.cosmos.implementation.routing.PartitionKeyInternalHelper;
import com.azure.cosmos.implementation.routing.Range;
import com.azure.cosmos.implementation.routing.RoutingMapProvider;
import com.azure.cosmos.models.PartitionKeyDefinition;

import reactor.core.publisher.Mono;

public final class FeedRangeEPKImpl extends FeedRangeInternal {
    private static final FeedRangeEPKImpl fullRangeEPK = new FeedRangeEPKImpl(PartitionKeyInternalHelper.FullRange);

    private final Range<String> range;

    public FeedRangeEPKImpl(Range<String> range) {
        if (range == null) {
            throw new NullPointerException("range");
        }

        this.range = range;
    }

    public static FeedRangeEPKImpl ForFullRange() {
        return fullRangeEPK;
    }

    @Override
    public String toJsonString() {
        try {
            return Utils.getSimpleObjectMapper().writeValueAsString(this);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Unable serialize the feedrange token for an extended partition key into a JSON string", e);
        }
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
        // TODO FABIANM Auto-generated method stub
        return null;
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
