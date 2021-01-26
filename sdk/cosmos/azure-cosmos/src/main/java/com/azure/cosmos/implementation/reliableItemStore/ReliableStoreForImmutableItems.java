package com.azure.cosmos.implementation.reliableItemStore;

import com.azure.core.util.Context;
import com.azure.cosmos.BridgeInternal;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosAsyncReliableItemStore;
import com.azure.cosmos.CosmosBridgeInternal;
import com.azure.cosmos.CosmosPatchOperations;
import com.azure.cosmos.implementation.AsyncDocumentClient;
import com.azure.cosmos.implementation.Document;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.implementation.OperationType;
import com.azure.cosmos.implementation.Paths;
import com.azure.cosmos.implementation.RequestOptions;
import com.azure.cosmos.implementation.ResourceType;
import com.azure.cosmos.implementation.TracerProvider;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.ModelBridgeInternal;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedFlux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.azure.core.util.FluxUtil.withContext;
import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkArgument;
import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public class ReliableStoreForImmutableItems extends ReliableItemStore {

    public ReliableStoreForImmutableItems(
        CosmosAsyncClient client,
        CosmosAsyncDatabase database,
        CosmosAsyncContainer container,
        Duration defaultTtlForSoftDeletes) {

       super(client, database, container, defaultTtlForSoftDeletes);
    }



    @Override
    public <T> Mono<CosmosItemResponse<T>> createOrReplaceItem(String transactionId,
                                                               String id,
                                                               T createTemplate,
                                                               PartitionKey partitionKey,
                                                               Function<T, T> replaceAction) {
        return null;
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> createOrPatchItem(String transactionId,
                                                             PartitionKey partitionKey,
                                                             T createTemplate, Function<T,
        CosmosPatchOperations> patchAction) {
        return null;
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> replaceOrCreateItem(String transactionId,
                                                               PartitionKey partitionKey,
                                                               T createTemplate,
                                                               Function<T, T> replaceAction) {
        return null;
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> patchOrCreateItem(String transactionId,
                                                             PartitionKey partitionKey,
                                                             T createTemplate, Function<T,
        CosmosPatchOperations> patchAction) {
        return null;
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> createOrReadItem(String transactionId,
                                                            PartitionKey partitionKey,
                                                            T createTemplate) {
        return null;
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> readOrCreateItem(String transactionId,
                                                            PartitionKey partitionKey,
                                                            T createTemplate) {
        return null;
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> replaceItem(String transactionId,
                                                       PartitionKey partitionKey, String itemId,
                                                       Function<T, T> replaceAction,
                                                       Class<T> itemType) {
        return null;
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> patchItem(String transactionId,
                                                     PartitionKey partitionKey, String itemId,
                                                     Function<T, CosmosPatchOperations> patchAction, Class<T> itemType) {
        return null;
    }

    @Override
    public <TSummary, TNewItems> Mono<CosmosItemResponse<TSummary>> addItemsAndUpdateSummary(String transactionId, PartitionKey partitionKey, String summaryId, List<TNewItems> itemsToAdd, BiFunction<List<TNewItems>, TSummary, TSummary> summaryTransformationAction, Class<TSummary> summaryType, Class<TNewItems> newItemType) {
        return null;
    }

    @Override
    public <TSummary, TNewItems> Mono<CosmosItemResponse<TSummary>> addItemsAndPatchSummary(String transactionId, PartitionKey partitionKey, String summaryId, List<TNewItems> itemsToAdd, BiFunction<List<TNewItems>, TSummary, CosmosPatchOperations> summaryTransformationAction, Class<TSummary> summaryType, Class<TNewItems> newItemType) {
        return null;
    }

    @Override
    public Mono<CosmosItemResponse<Object>> deleteItem(String transactionId,
                                                       PartitionKey partitionKey, String itemId) {
        return null;
    }


}
