package com.azure.cosmos.implementation.feedranges;

import com.azure.cosmos.implementation.IRoutingMapProvider;
import com.azure.cosmos.implementation.PartitionKeyRange;
import com.azure.cosmos.implementation.Utils;
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

    public FeedRangeEPKImpl(final Range<String> range) {
        if (range == null) {
            throw new NullPointerException("range");
        }

        this.range = range;
        final ArrayList<Range<String>> temp = new ArrayList<Range<String>>();
        temp.add(range);

        this.rangeList = (UnmodifiableList<Range<String>>)UnmodifiableList.unmodifiableList(temp);
    }

    public Range<String> getRange() {
        return this.range;
    }

    public static FeedRangeEPKImpl ForFullRange() {
        return fullRangeEPK;
    }

    @Override
    public void accept(final FeedRangeVisitor visitor) {
        if (visitor == null) {
            throw new NullPointerException("visitor");
        }

        visitor.visit(this);
    }

    @Override
    public <T> Mono<T> acceptAsync(final FeedRangeAsyncVisitor<T> visitor) {
        if (visitor == null) {
            throw new NullPointerException("visitor");
        }

        return visitor.visitAsync(this);
    }

    @Override
    public Mono<UnmodifiableList<Range<String>>> getEffectiveRangesAsync(
        final IRoutingMapProvider routingMapProvider,
        final String containerRid,
        final PartitionKeyDefinition partitionKeyDefinition) {

        return Mono.just(this.rangeList);
    }

    @Override
    public Mono<UnmodifiableList<String>> getPartitionKeyRangesAsync(
        final IRoutingMapProvider routingMapProvider,
        final String containerRid,
        final PartitionKeyDefinition partitionKeyDefinition) {

        return routingMapProvider.tryGetOverlappingRangesAsync(
            null,
            containerRid,
            this.range,
            false,
            null)
                 .flatMap(pkRangeHolder -> {
                     final ArrayList<String> rangeList = new ArrayList<String>();

                     if (pkRangeHolder != null) {
                         final List<PartitionKeyRange> pkRanges = pkRangeHolder.v;
                         for (final PartitionKeyRange pkRange : pkRanges) {
                             rangeList.add(pkRange.getId());
                         }
                     }

                     return Mono.just((UnmodifiableList<String>)UnmodifiableList.unmodifiableList(rangeList));
                 });
    }

    @Override
    public String toJsonString() {
        try {
            return Utils.getSimpleObjectMapper().writeValueAsString(this);
        } catch (final IOException e) {
            throw new IllegalArgumentException(
                "Unable serialize the feed range token for an extended partition key into a JSON string",
                e);
        }
    }

    @Override
    public String toString() {
        return this.range.toString();
    }
}
