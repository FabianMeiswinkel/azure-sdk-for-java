package com.azure.cosmos.implementation.feedranges;

import com.azure.cosmos.implementation.HttpConstants;
import com.azure.cosmos.implementation.Integers;
import com.azure.cosmos.implementation.PartitionKeyRange;
import com.azure.cosmos.implementation.RxDocumentClientImpl;
import com.azure.cosmos.implementation.RxDocumentServiceResponse;
import com.azure.cosmos.implementation.ShouldRetryResult;
import com.azure.cosmos.implementation.Strings;
import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.implementation.Utils.ValueHolder;
import com.azure.cosmos.implementation.caches.RxPartitionKeyRangeCache;
import com.azure.cosmos.implementation.query.CompositeContinuationToken;
import com.azure.cosmos.implementation.routing.Range;
import com.azure.cosmos.models.FeedRange;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FeedRangeCompositeContinuationImpl extends FeedRangeContinuation {
    private static final ShouldRetryResult NO_RETRY = ShouldRetryResult.noRetry();
    private static final ShouldRetryResult RETRY = ShouldRetryResult.retryAfter(Duration.ZERO);
    private final Queue<CompositeContinuationToken> compositeContinuationTokens;
    private CompositeContinuationToken currentToken;
    private String initialNoResultsRange;

    public FeedRangeCompositeContinuationImpl(final String containerRid,
                                              final FeedRangeInternal feedRangeInternal) {
        super(containerRid, feedRangeInternal);

        this.compositeContinuationTokens = new LinkedList<CompositeContinuationToken>();
    }

    public FeedRangeCompositeContinuationImpl(final String containerRid,
                                              final FeedRangeInternal feedRangeInternal,
                                              final List<Range<String>> ranges,
                                              final String continuation) {

        this(containerRid, feedRangeInternal);

        if (ranges == null) {
            throw new NullPointerException("ranges");
        }

        if (ranges.size() == 0) {
            throw new IllegalArgumentException("At least one range expected.");
        }

        for (final Range<String> range : ranges) {
            this.compositeContinuationTokens
                .add(createCompositeContinuationTokenForRange(range.getMin(), range.getMax(),
                    continuation));
        }

        this.currentToken = this.compositeContinuationTokens.peek();
    }

    // Used for deserialization only
    public FeedRangeCompositeContinuationImpl(final String containerRid,
                                              final FeedRangeInternal feedRange,
                                              final List<CompositeContinuationToken> tokens) {

        this(containerRid, feedRange);

        if (tokens == null) {
            throw new NullPointerException("tokens");
        }

        if (tokens.size() == 0) {
            throw new IllegalArgumentException("At least one deserialized token expected.");
        }

        for (final CompositeContinuationToken token : tokens) {
            this.compositeContinuationTokens.add(token);
        }

        this.currentToken = this.compositeContinuationTokens.peek();
    }

    public Queue<CompositeContinuationToken> getCompositeContinuationTokens() {
        return this.compositeContinuationTokens;
    }

    @Override
    public String getContinuation() {
        final CompositeContinuationToken continuationTokenSnapshot = this.currentToken;

        if (continuationTokenSnapshot == null) {
            return null;
        }

        return continuationTokenSnapshot.getToken();
    }

    @Override
    public FeedRange getFeedRange() {
        final FeedRangeInternal feedRangeInternal = this.getFeedRangeInternal();

        if (feedRangeInternal instanceof FeedRangePartitionKeyRangeImpl) {
            return feedRangeInternal;
        }

        final CompositeContinuationToken continuationTokenSnapshot = this.currentToken;
        if (continuationTokenSnapshot != null) {
            return new FeedRangeEPKImpl(continuationTokenSnapshot.getRange());
        }

        return null;
    }

    @Override
    public void replaceContinuation(final String continuationToken) {
        final CompositeContinuationToken continuationTokenSnapshot = this.currentToken;

        if (continuationTokenSnapshot == null) {
            return;
        }

        continuationTokenSnapshot.setToken(continuationToken);
        this.moveToNextToken();
    }

    @Override
    public Boolean isDone() {
        return this.compositeContinuationTokens.size() == 0;
    }

    @Override
    public void validateContainer(final String containerRid) throws IllegalArgumentException {
        if (Strings.isNullOrEmpty(containerRid) || !containerRid.equals(this.getContainerRid())) {

            final String message = String.format(
                "The continuation was generated for container %s but current container is %s.",
                this.getContainerRid(), containerRid);
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public ShouldRetryResult handleChangeFeedNotModified(final RxDocumentServiceResponse response) {
        if (response == null) {
            throw new NullPointerException("response");
        }

        final int statusCode = response.getStatusCode();
        if (statusCode >= HttpConstants.StatusCodes.MINIMUM_SUCCESS_STATUSCODE
            && statusCode <= HttpConstants.StatusCodes.MAXIMUM_SUCCESS_STATUSCODE) {

            this.initialNoResultsRange = null;
            return NO_RETRY;
        }

        if (statusCode == HttpConstants.StatusCodes.NOT_MODIFIED && this.compositeContinuationTokens.size() > 1) {

            final String eTag = response.getResponseHeaders().get(HttpConstants.HttpHeaders.E_TAG);
            if (this.initialNoResultsRange == null) {

                this.initialNoResultsRange = this.currentToken.getRange().getMin();
                this.replaceContinuation(eTag);
                return RETRY;
            }

            if (!this.initialNoResultsRange.equalsIgnoreCase(this.currentToken.getRange().getMin())) {
                this.replaceContinuation(eTag);
                return RETRY;
            }
        }

        return NO_RETRY;
    }

    @Override
    public Mono<ShouldRetryResult> handleSplitAsync(final RxDocumentClientImpl client,
                                                    final RxDocumentServiceResponse response) {

        if (client == null) {
            throw new NullPointerException("container");
        }

        if (response == null) {
            throw new NullPointerException("response");
        }

        Integer nSubStatus = 0;
        final String valueSubStatus =
            response.getResponseHeaders().get(HttpConstants.HttpHeaders.SUB_STATUS);
        if (!Strings.isNullOrEmpty(valueSubStatus)) {
            nSubStatus = Integers.tryParse(valueSubStatus);
        }

        final Boolean partitionSplit =
            response.getStatusCode() == HttpConstants.StatusCodes.GONE && nSubStatus != null
                && (nSubStatus.intValue() == HttpConstants.SubStatusCodes.PARTITION_KEY_RANGE_GONE
                || nSubStatus.intValue() == HttpConstants.SubStatusCodes.COMPLETING_SPLIT);

        if (!partitionSplit) {
            return Mono.just(NO_RETRY);
        }

        final RxPartitionKeyRangeCache partitionKeyRangeCache = client.getPartitionKeyRangeCache();
        final Mono<ValueHolder<List<PartitionKeyRange>>> resolvedRangesTask =
            this.tryGetOverlappingRangesAsync(
                partitionKeyRangeCache, this.currentToken.getRange().getMin(),
                this.currentToken.getRange().getMax(),
                true);

        final Mono<ShouldRetryResult> result = resolvedRangesTask.flatMap(resolvedRanges -> {
            if (resolvedRanges.v != null && resolvedRanges.v.size() > 0) {
                this.createChildRanges(resolvedRanges.v);
            }

            return Mono.just(RETRY);
        });

        return result;
    }

    @Override
    public void accept(final FeedRangeContinuationVisitor visitor) {
        if (visitor == null) {
            throw new NullPointerException("visiotr");
        }

        visitor.visit(this);
    }

    public CompositeContinuationToken getCurrentToken() {
        return this.currentToken;
    }

    public void setCurrentToken(final CompositeContinuationToken token) {
        this.currentToken = token;
    }

    @Override
    public String toString() {
        try {
            return Utils.getSimpleObjectMapper().writeValueAsString(this);
        } catch (final IOException e) {
            throw new IllegalArgumentException(
                "Unable serialize the composite FeedRange continuation token into a JSON string",
                e);
        }
    }

    public static FeedRangeContinuation tryParse(final String jsonString) {
        if (jsonString == null) {
            throw new NullPointerException("jsonString");
        }

        final ObjectMapper mapper = Utils.getSimpleObjectMapper();

        try {
            return mapper.readValue(jsonString, FeedRangeCompositeContinuationImpl.class);
        } catch (final IOException ioException) {
            return null;
        }
    }

    private void createChildRanges(final List<PartitionKeyRange> keyRanges) {
        final PartitionKeyRange firstRange = keyRanges.get(0);
        this.currentToken
            .setRange(new Range<String>(firstRange.getMinInclusive(),
                firstRange.getMaxExclusive(), true, false));

        final CompositeContinuationToken continuationAsComposite =
            tryParseAsCompositeContinuationToken(
                this.currentToken.getToken());

        if (continuationAsComposite != null) {
            // Update the internal composite continuation
            continuationAsComposite.setRange(this.currentToken.getRange());
            this.currentToken.setToken(continuationAsComposite.toJson());
            // Add children
            final int size = keyRanges.size();
            for (int i = 1; i < size; i++) {
                final PartitionKeyRange keyRange = keyRanges.get(i);
                continuationAsComposite.setRange(keyRange.toRange());
                this.compositeContinuationTokens.add(createCompositeContinuationTokenForRange(
                    keyRange.getMinInclusive(), keyRange.getMaxExclusive(),
                    continuationAsComposite.toJson()));
            }
        } else {
            // Add children
            final int size = keyRanges.size();
            for (int i = 1; i < size; i++) {
                final PartitionKeyRange keyRange = keyRanges.get(i);
                this.compositeContinuationTokens.add(createCompositeContinuationTokenForRange(
                    keyRange.getMinInclusive(), keyRange.getMaxExclusive(),
                    this.currentToken.getToken()));
            }
        }
    }

    private static CompositeContinuationToken createCompositeContinuationTokenForRange(
        final String minInclusive,
        final String maxExclusive, final String token) {

        final Range<String> range = new Range<String>(minInclusive, maxExclusive, true, false);
        return new CompositeContinuationToken(token, range);
    }

    private void moveToNextToken() {
        final CompositeContinuationToken recentToken = this.compositeContinuationTokens.poll();
        if (recentToken.getToken() != null) {
            // Normal ReadFeed can signal termination by CT null, not NotModified
            // Change Feed never lands here, as it always provides a CT
            // Consider current range done, if this FeedToken contains multiple ranges due
            // to splits,
            // all of them need to be considered done
            this.compositeContinuationTokens.add(recentToken);
        }

        if (this.compositeContinuationTokens.size() > 0) {
            this.currentToken = this.compositeContinuationTokens.peek();
        } else {
            this.currentToken = null;
        }
    }

    private Mono<ValueHolder<List<PartitionKeyRange>>> tryGetOverlappingRangesAsync(
        final RxPartitionKeyRangeCache partitionKeyRangeCache, final String min, final String max,
        final Boolean forceRefresh) {

        return partitionKeyRangeCache.tryGetOverlappingRangesAsync(null, this.getContainerRid(),
            new Range<String>(min, max, false, true), forceRefresh, null);
    }

    private static CompositeContinuationToken tryParseAsCompositeContinuationToken(
        final String providedContinuation) {

        try {
            final ObjectMapper mapper = Utils.getSimpleObjectMapper();

            if (providedContinuation.trim().startsWith("[")) {
                final List<CompositeContinuationToken> compositeContinuationTokens = Arrays
                    .asList(mapper.readValue(providedContinuation,
                        CompositeContinuationToken[].class));

                if (compositeContinuationTokens != null && compositeContinuationTokens.size() > 0) {
                    return compositeContinuationTokens.get(0);
                }

                return null;
            } else if (providedContinuation.trim().startsWith("{")) {
                return mapper.readValue(providedContinuation, CompositeContinuationToken.class);
            }

            return null;
        } catch (final IOException ioError) {
            return null;
        }
    }
}