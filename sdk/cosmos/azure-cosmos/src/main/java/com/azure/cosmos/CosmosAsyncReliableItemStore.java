package com.azure.cosmos;

import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.ModelBridgeInternal;
import com.azure.cosmos.models.PartitionKey;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public abstract class CosmosAsyncReliableItemStore {
    public static final String SYSTEM_PROPERTY_NAME_IS_DELETED = "_del";

    /**
     * Reads an item.
     * <p>
     * After subscription the operation will be performed.
     * The {@link Mono} upon successful completion will contain an item response with the read item.
     *
     * @param <T> the type parameter.
     * @param itemId the item id.
     * @param partitionKey the partition key.
     * @param itemType the item type.
     * @return an {@link Mono} containing the Cosmos item response with the read item or an error.
     */
    public <T> Mono<CosmosItemResponse<T>> readItem(
        String itemId,
        PartitionKey partitionKey,
        Class<T> itemType) {

        return readItem(
            itemId,
            partitionKey,
            ModelBridgeInternal.createCosmosItemRequestOptions(partitionKey),
            itemType);
    }

    /**
     * Reads an item using a configured {@link CosmosItemRequestOptions}.
     * <p>
     * After subscription the operation will be performed.
     * The {@link Mono} upon successful completion will contain a Cosmos item response with the read item.
     *
     * @param <T> the type parameter.
     * @param itemId the item id.
     * @param partitionKey the partition key.
     * @param options the request {@link CosmosItemRequestOptions}.
     * @param itemType the item type.
     * @return an {@link Mono} containing the Cosmos item response with the read item or an error.
     */
    public abstract <T> Mono<CosmosItemResponse<T>> readItem(
        String itemId,
        PartitionKey partitionKey,
        CosmosItemRequestOptions options,
        Class<T> itemType);

    public <T> Mono<CosmosItemResponse<T>> createItem(
        T item) {

        return this.createItem(
            UUID.randomUUID().toString(),
            null,
            item
        );
    }

    public <T> Mono<CosmosItemResponse<T>> createItem(
        PartitionKey partitionKey,
        T item) {

        return this.createItem(
            UUID.randomUUID().toString(),
            partitionKey,
            item
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> createItem(
        String transactionId,
        PartitionKey partitionKey,
        T item
    );

    public <T> Mono<CosmosItemResponse<T>> replaceItem(
        String itemId,
        PartitionKey partitionKey,
        Function<T, T> replaceAction,
        Class<T> itemType) {

        return this.replaceItem(
            UUID.randomUUID().toString(),
            itemId,
            partitionKey,
            replaceAction,
            itemType
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> replaceItem(
        String transactionId,
        String itemId,
        PartitionKey partitionKey,
        Function<T, T> replaceAction,
        Class<T> itemType
    );

    public <T> Mono<CosmosItemResponse<T>> patchItem(
        String itemId,
        PartitionKey partitionKey,
        Function<T, CosmosPatchOperations> patchAction,
        Class<T> itemType) {

        return this.patchItem(
            UUID.randomUUID().toString(),
            itemId,
            partitionKey,
            patchAction,
            itemType
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> patchItem(
        String transactionId,
        String itemId,
        PartitionKey partitionKey,
        Function<T, CosmosPatchOperations> patchAction,
        Class<T> itemType
    );

    public <TSummary, TNewItems> Mono<CosmosItemResponse<TSummary>> addItemsAndReplaceSummary(
        String summaryItemId,
        PartitionKey partitionKey,
        List<TNewItems> itemsToAdd,
        BiFunction<List<TNewItems>, TSummary, TSummary> summaryTransformationAction,
        Class<TSummary> summaryType,
        Class<TNewItems> newItemType) {

        return this.addItemsAndReplaceSummary(
            UUID.randomUUID().toString(),
            summaryItemId,
            partitionKey,
            itemsToAdd,
            summaryTransformationAction,
            summaryType,
            newItemType
        );
    }

    public abstract <TSummary, TNewItems> Mono<CosmosItemResponse<TSummary>> addItemsAndReplaceSummary(
        String transactionId,
        String summaryItemId,
        PartitionKey partitionKey,
        List<TNewItems> itemsToAdd,
        BiFunction<List<TNewItems>, TSummary, TSummary> summaryTransformationAction,
        Class<TSummary> summaryType,
        Class<TNewItems> newItemType
    );

    public <TSummary, TNewItems> Mono<CosmosItemResponse<TSummary>> addItemsAndPatchSummary(
        String summaryItemId,
        PartitionKey partitionKey,
        List<TNewItems> itemsToAdd,
        BiFunction<List<TNewItems>, TSummary, CosmosPatchOperations> summaryTransformationAction,
        Class<TSummary> summaryType,
        Class<TNewItems> newItemType) {

        return this.addItemsAndPatchSummary(
            UUID.randomUUID().toString(),
            summaryItemId,
            partitionKey,
            itemsToAdd,
            summaryTransformationAction,
            summaryType,
            newItemType
        );
    }

    public abstract <TSummary, TNewItems> Mono<CosmosItemResponse<TSummary>> addItemsAndPatchSummary(
        String transactionId,
        String summaryItemId,
        PartitionKey partitionKey,
        List<TNewItems> itemsToAdd,
        BiFunction<List<TNewItems>, TSummary, CosmosPatchOperations> summaryTransformationAction,
        Class<TSummary> summaryType,
        Class<TNewItems> newItemType
    );

    public Mono<CosmosItemResponse<Object>> deleteItem(
        String itemId,
        PartitionKey partitionKey) {

        return this.deleteItem(
            UUID.randomUUID().toString(),
            itemId,
            partitionKey
        );
    }

    public abstract Mono<CosmosItemResponse<Object>> deleteItem(
        String transactionId,
        String itemId,
        PartitionKey partitionKey);
}
