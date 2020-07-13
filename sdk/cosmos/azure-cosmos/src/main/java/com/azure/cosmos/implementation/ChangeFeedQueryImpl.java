// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation;

import com.azure.cosmos.BridgeInternal;
import com.azure.cosmos.implementation.query.Paginator;
import com.azure.cosmos.models.CosmosChangeFeedRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.ModelBridgeInternal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

class ChangeFeedQueryImpl<T extends Resource> {
    private static final String INITIAL_EMPTY_CONTINUATION_TOKEN = null;
    private static final int INITIAL_TOP_VALUE = -1;

    private final RxDocumentClientImpl client;
    private final BiFunction<String, Integer, RxDocumentServiceRequest> createRequestFunc;
    private final String documentsLink;
    private final Function<RxDocumentServiceRequest, Mono<FeedResponse<T>>> executeFunc;
    private final Class<T> klass;
    private final CosmosChangeFeedRequestOptions options;
    private final ResourceType resourceType;

    public ChangeFeedQueryImpl(
        RxDocumentClientImpl client,
        ResourceType resourceType,
        Class<T> klass,
        String collectionLink,
        CosmosChangeFeedRequestOptions requestOptions) {

        if (client == null) {
            throw new NullPointerException("client");
        }

        if (resourceType == null) {
            throw new NullPointerException("resourceType");
        }

        if (klass == null) {
            throw new NullPointerException("klass");
        }

        if (requestOptions == null) {
            throw new NullPointerException("requestOptions");
        }

        if (Strings.isNullOrWhiteSpace(collectionLink)) {
            throw new NullPointerException("collectionLink");
        }

        this.createRequestFunc = this::createDocumentServiceRequest;
        this.executeFunc = this::executeRequestAsync;
        this.client = client;
        this.resourceType = resourceType;
        this.klass = klass;
        this.documentsLink = Utils.joinPath(collectionLink, Paths.DOCUMENTS_PATH_SEGMENT);
        this.options = requestOptions;
    }

    public Flux<FeedResponse<T>> executeAsync() {

        final int maxPageSize = this.options.getMaxItemCount() != null ?
            options.getMaxItemCount() : -1;

        return Paginator.getPaginatedQueryResultAsObservable(
            INITIAL_EMPTY_CONTINUATION_TOKEN,
            this.createRequestFunc,
            this.executeFunc,
            this.klass,
            INITIAL_TOP_VALUE,
            maxPageSize,
            true);
    }

    private RxDocumentServiceRequest createDocumentServiceRequest(String continuationToken,
                                                                  int pageSize) {
        Map<String, String> headers = new HashMap<>();
        RxDocumentServiceRequest req = RxDocumentServiceRequest.create(
            OperationType.ReadFeed,
            resourceType,
            documentsLink,
            headers,
            options);

        ModelBridgeInternal.populateChangeFeedRequestOptions(this.options, req, continuationToken);
        return req;
    }

    private Mono<FeedResponse<T>> executeRequestAsync(RxDocumentServiceRequest request) {
        return client.readFeed(request)
                     .map(rsp -> BridgeInternal.toChangeFeedResponsePage(rsp, klass));
    }
}
