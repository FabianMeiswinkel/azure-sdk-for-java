package com.azure.cosmos.implementation.reliableItemStore;

import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncReliableItemStore;
import com.azure.cosmos.CosmosPatchOperations;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedFlux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ReliableStoreForImmutableItems extends CosmosAsyncReliableItemStore {
    public ReliableStoreForImmutableItems(CosmosAsyncContainer container) {

    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> readItem(String itemId, PartitionKey partitionKey,
                                                    Class<T> itemType) {
        return null;
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> readItem(String itemId, PartitionKey partitionKey, CosmosItemRequestOptions options, Class<T> itemType) {
        return null;
    }

    @Override
    public <T> CosmosPagedFlux<T> queryItems(String query, Class<T> classType) {
        return null;
    }

    @Override
    public <T> CosmosPagedFlux<T> queryItems(String query, CosmosQueryRequestOptions options,
                                             Class<T> classType) {
        return null;
    }

    @Override
    public <T> CosmosPagedFlux<T> queryItems(SqlQuerySpec querySpec, Class<T> classType) {
        return null;
    }

    @Override
    public <T> CosmosPagedFlux<T> queryItems(SqlQuerySpec querySpec,
                                             CosmosQueryRequestOptions options,
                                             Class<T> classType) {
        return null;
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> createOrReplaceItem(String transactionId, PartitionKey partitionKey, T createTemplate, Function<T, T> replaceAction) {
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
                                                            PartitionKey partitionKey, T createTemplate) {
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
    public <T> Mono<CosmosItemResponse<T>> patchItem(String transactionId, PartitionKey partitionKey, String itemId, Function<T, CosmosPatchOperations> patchAction, Class<T> itemType) {
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
    public Mono<CosmosItemResponse<Object>> deleteItem(String transactionId, PartitionKey partitionKey, String itemId) {
        return null;
    }
}
