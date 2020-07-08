package com.azure.cosmos.implementation.feedranges;

import com.azure.cosmos.implementation.IRoutingMapProvider;
import com.azure.cosmos.implementation.PartitionKeyRange;
import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.implementation.apachecommons.collections.CollectionUtils;
import com.azure.cosmos.implementation.apachecommons.collections.list.UnmodifiableList;
import com.azure.cosmos.implementation.routing.PartitionKeyInternalHelper;
import com.azure.cosmos.implementation.routing.Range;
import com.azure.cosmos.models.PartitionKeyDefinition;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FeedRangeEPKImpl extends FeedRangeInternal {
    private static final FeedRangeEPKImpl fullRangeEPK =
        new FeedRangeEPKImpl(PartitionKeyInternalHelper.FullRange);

    private final Range<String> range;
    private final UnmodifiableList<Range<String>> rangeList;

    public FeedRangeEPKImpl(Range<String> range) {
        if (range == null) {
            throw new NullPointerException("range");
        }

        this.range = range;
        ArrayList<Range<String>> temp = new ArrayList<Range<String>>();
        temp.add(range);

        this.rangeList = (UnmodifiableList<Range<String>>)Collections.unmodifiableList(temp);
    }

    public Range<String> getRange() {
        return this.range;
    }

    public static FeedRangeEPKImpl ForFullRange() {
        return fullRangeEPK;
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

        return Mono.just(this.rangeList);
    }

    @Override
    public Mono<UnmodifiableList<String>> getPartitionKeyRangesAsync(
        IRoutingMapProvider routingMapProvider,
        String containerRid,
        PartitionKeyDefinition partitionKeyDefinition) {

        return routingMapProvider
            .tryGetOverlappingRangesAsync(
                null,
                containerRid,
                this.range,
                false,
                null)
            .flatMap(pkRangeHolder -> {
                ArrayList<String> rangeList = new ArrayList<String>();

                if (pkRangeHolder != null) {
                    List<PartitionKeyRange> pkRanges = pkRangeHolder.v;
                    for(PartitionKeyRange pkRange : pkRanges) {
                        rangeList.add(pkRange.getId());
                    }
                }

                return Mono.just((UnmodifiableList<String>)Collections.unmodifiableList(rangeList));
            });
    }

    @Override
    public String toJsonString() {
        try {
            return Utils.getSimpleObjectMapper().writeValueAsString(this);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "Unable serialize the feedrange token for an extended partition key into a JSON " +
                    "string", e);
        }
    }

    @Override
    public String toString() {
        return this.range.toString();
    }
}
