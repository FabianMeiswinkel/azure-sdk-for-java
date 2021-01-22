package com.azure.cosmos;

import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class CosmosAsyncReliableItemStore {
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
    public abstract <T> Mono<CosmosItemResponse<T>> readItem(String itemId, PartitionKey partitionKey, Class<T> itemType);

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
        String itemId, PartitionKey partitionKey,
        CosmosItemRequestOptions options, Class<T> itemType);

    /**
     * Query for items in the current container.
     * <p>
     * After subscription the operation will be performed. The {@link CosmosPagedFlux} will
     * contain one or several feed response of the obtained items. In case of
     * failure the {@link CosmosPagedFlux} will error.
     *
     * @param <T> the type parameter.
     * @param query the query.
     * @param classType the class type.
     * @return a {@link CosmosPagedFlux} containing one or several feed response pages of the obtained items or an
     * error.
     */
    public abstract <T> CosmosPagedFlux<T> queryItems(String query, Class<T> classType);

    /**
     * Query for items in the current container using a string.
     * <p>
     * After subscription the operation will be performed. The {@link CosmosPagedFlux} will
     * contain one or several feed response of the obtained items. In case of
     * failure the {@link CosmosPagedFlux} will error.
     *
     * @param <T> the type parameter.
     * @param query the query.
     * @param options the query request options.
     * @param classType the class type.
     * @return a {@link CosmosPagedFlux} containing one or several feed response pages of the obtained items or an
     * error.
     */
    public abstract <T> CosmosPagedFlux<T> queryItems(String query, CosmosQueryRequestOptions options, Class<T> classType);

    /**
     * Query for items in the current container using a {@link SqlQuerySpec}.
     * <p>
     * After subscription the operation will be performed. The {@link CosmosPagedFlux} will
     * contain one or several feed response of the obtained items. In case of
     * failure the {@link CosmosPagedFlux} will error.
     *
     * @param <T> the type parameter.
     * @param querySpec the SQL query specification.
     * @param classType the class type.
     * @return a {@link CosmosPagedFlux} containing one or several feed response pages of the obtained items or an
     * error.
     */
    public abstract <T> CosmosPagedFlux<T> queryItems(SqlQuerySpec querySpec, Class<T> classType);

    /**
     * Query for items in the current container using a {@link SqlQuerySpec} and {@link CosmosQueryRequestOptions}.
     * <p>
     * After subscription the operation will be performed. The {@link Flux} will
     * contain one or several feed response of the obtained items. In case of
     * failure the {@link CosmosPagedFlux} will error.
     *
     * @param <T> the type parameter.
     * @param querySpec the SQL query specification.
     * @param options the query request options.
     * @param classType the class type.
     * @return a {@link CosmosPagedFlux} containing one or several feed response pages of the obtained items or an
     * error.
     */
    public abstract <T> CosmosPagedFlux<T> queryItems(SqlQuerySpec querySpec, CosmosQueryRequestOptions options, Class<T> classType) ;

    public <T> Mono<CosmosItemResponse<T>> createOrReplaceItem(
        T createTemplate,
        Function<T, T> replaceAction,
        Class<T> itemType) {

        return createOrReplaceItem(
            UUID.randomUUID().toString(),
            null,
            createTemplate,
            replaceAction,
            itemType
        );
    }

    public <T> Mono<CosmosItemResponse<T>> createOrReplaceItem(
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, T> replaceAction,
        Class<T> itemType) {

        return createOrReplaceItem(
            UUID.randomUUID().toString(),
            partitionKey,
            createTemplate,
            replaceAction,
            itemType
        );
    }

    public <T> Mono<CosmosItemResponse<T>> createOrReplaceItem(
        String transactionId,
        T createTemplate,
        Function<T, T> replaceAction,
        Class<T> itemType) {

        return createOrReplaceItem(
            transactionId,
            null,
            createTemplate,
            replaceAction,
            itemType
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> createOrReplaceItem(
        String transactionId,
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, T> replaceAction,
        Class<T> itemType);

    public <T> Mono<CosmosItemResponse<T>> createOrPatchItem(
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction,
        Class<T> itemType) {

        return this.createOrPatchItem(
            UUID.randomUUID().toString(),
            null,
            createTemplate,
            patchAction,
            itemType
        );
    }

    public <T> Mono<CosmosItemResponse<T>> createOrPatchItem(
        String transactionId,
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction,
        Class<T> itemType) {

        return this.createOrPatchItem(
            transactionId,
            null,
            createTemplate,
            patchAction,
            itemType
        );
    }

    public <T> Mono<CosmosItemResponse<T>> createOrPatchItem(
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction,
        Class<T> itemType) {

        return this.createOrPatchItem(
            UUID.randomUUID().toString(),
            partitionKey,
            createTemplate,
            patchAction,
            itemType
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> createOrPatchItem(
        String transactionId,
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction,
        Class<T> itemType);

    public <T> Mono<CosmosItemResponse<T>> replaceOrCreateItem(
        T createTemplate,
        Function<T, T> replaceAction,
        Class<T> itemType) {

        return this.replaceOrCreateItem(
            UUID.randomUUID().toString(),
            null,
            createTemplate,
            replaceAction,
            itemType
        );
    }

    public <T> Mono<CosmosItemResponse<T>> replaceOrCreateItem(
        String transactionId,
        T createTemplate,
        Function<T, T> replaceAction,
        Class<T> itemType) {

        return this.replaceOrCreateItem(
            transactionId,
            null,
            createTemplate,
            replaceAction,
            itemType
        );
    }


    public <T> Mono<CosmosItemResponse<T>> replaceOrCreateItem(
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, T> replaceAction,
        Class<T> itemType) {

        return this.replaceOrCreateItem(
            UUID.randomUUID().toString(),
            partitionKey,
            createTemplate,
            replaceAction,
            itemType
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> replaceOrCreateItem(
        String transactionId,
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, T> replaceAction,
        Class<T> itemType
    );

    public <T> Mono<CosmosItemResponse<T>> patchOrCreateItem(
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction,
        Class<T> itemType) {

        return this.patchOrCreateItem(
            UUID.randomUUID().toString(),
            null,
            createTemplate,
            patchAction,
            itemType
        );
    }

    public <T> Mono<CosmosItemResponse<T>> patchOrCreateItem(
        String transactionId,
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction,
        Class<T> itemType) {

        return this.patchOrCreateItem(
            transactionId,
            null,
            createTemplate,
            patchAction,
            itemType
        );
    }

    public <T> Mono<CosmosItemResponse<T>> patchOrCreateItem(
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction,
        Class<T> itemType) {

        return this.patchOrCreateItem(
            UUID.randomUUID().toString(),
            partitionKey,
            createTemplate,
            patchAction,
            itemType
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> patchOrCreateItem(
        String transactionId,
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction,
        Class<T> itemType
    );

    public <T> Mono<CosmosItemResponse<T>> createOrReadItem(
        T createTemplate,
        Class<T> itemType) {

        return this.createOrReadItem(
            UUID.randomUUID().toString(),
            null,
            createTemplate,
            itemType
        );
    }

    public <T> Mono<CosmosItemResponse<T>> createOrReadItem(
        String transactionId,
        T createTemplate,
        Class<T> itemType) {

        return this.createOrReadItem(
            transactionId,
            null,
            createTemplate,
            itemType
        );
    }

    public <T> Mono<CosmosItemResponse<T>> createOrReadItem(
        PartitionKey partitionKey,
        T createTemplate,
        Class<T> itemType) {

        return this.createOrReadItem(
            UUID.randomUUID().toString(),
            partitionKey,
            createTemplate,
            itemType
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> createOrReadItem(
        String transactionId,
        PartitionKey partitionKey,
        T createTemplate,
        Class<T> itemType
    );

    public <T> Mono<CosmosItemResponse<T>> readOrCreateItem(
        T createTemplate,
        Class<T> itemType) {

        return this.readOrCreateItem(
            UUID.randomUUID().toString(),
            null,
            createTemplate,
            itemType
        );
    }

    public <T> Mono<CosmosItemResponse<T>> readOrCreateItem(
        String transactionId,
        T createTemplate,
        Class<T> itemType) {

        return this.readOrCreateItem(
            transactionId,
            null,
            createTemplate,
            itemType
        );
    }

    public <T> Mono<CosmosItemResponse<T>> readOrCreateItem(
        PartitionKey partitionKey,
        T createTemplate,
        Class<T> itemType) {

        return this.readOrCreateItem(
            UUID.randomUUID().toString(),
            partitionKey,
            createTemplate,
            itemType
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> readOrCreateItem(
        String transactionId,
        PartitionKey partitionKey,
        T createTemplate,
        Class<T> itemType
    );

    public <T> Mono<CosmosItemResponse<T>> replaceItem(
        PartitionKey partitionKey,
        String itemId,
        Function<T, T> replaceAction,
        Class<T> itemType) {

        return this.replaceItem(
            UUID.randomUUID().toString(),
            partitionKey,
            itemId,
            replaceAction,
            itemType
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> replaceItem(
        String transactionId,
        PartitionKey partitionKey,
        String itemId,
        Function<T, T> replaceAction,
        Class<T> itemType
    );

    public <T> Mono<CosmosItemResponse<T>> patchItem(
        PartitionKey partitionKey,
        String itemId,
        Function<T, CosmosPatchOperations> patchAction,
        Class<T> itemType) {

        return this.patchItem(
            UUID.randomUUID().toString(),
            partitionKey,
            itemId,
            patchAction,
            itemType
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> patchItem(
        String transactionId,
        PartitionKey partitionKey,
        String itemId,
        Function<T, CosmosPatchOperations> patchAction,
        Class<T> itemType
    );

    public <TSummary, TNewItems> Mono<CosmosItemResponse<TSummary>> addItemsAndUpdateSummary(
        PartitionKey partitionKey,
        String summaryId,
        List<TNewItems> itemsToAdd,
        BiFunction<List<TNewItems>, TSummary, TSummary> summaryTransformationAction,
        Class<TSummary> summaryType,
        Class<TNewItems> newItemType) {

        return this.addItemsAndUpdateSummary(
            UUID.randomUUID().toString(),
            partitionKey,
            summaryId,
            itemsToAdd,
            summaryTransformationAction,
            summaryType,
            newItemType
        );
    }

    public abstract <TSummary, TNewItems> Mono<CosmosItemResponse<TSummary>> addItemsAndUpdateSummary(
        String transactionId,
        PartitionKey partitionKey,
        String summaryId,
        List<TNewItems> itemsToAdd,
        BiFunction<List<TNewItems>, TSummary, TSummary> summaryTransformationAction,
        Class<TSummary> summaryType,
        Class<TNewItems> newItemType
    );

    public <TSummary, TNewItems> Mono<CosmosItemResponse<TSummary>> addItemsAndPatchSummary(
        PartitionKey partitionKey,
        String summaryId,
        List<TNewItems> itemsToAdd,
        BiFunction<List<TNewItems>, TSummary, CosmosPatchOperations> summaryTransformationAction,
        Class<TSummary> summaryType,
        Class<TNewItems> newItemType) {

        return this.addItemsAndPatchSummary(
            UUID.randomUUID().toString(),
            partitionKey,
            summaryId,
            itemsToAdd,
            summaryTransformationAction,
            summaryType,
            newItemType
        );
    }

    public abstract <TSummary, TNewItems> Mono<CosmosItemResponse<TSummary>> addItemsAndPatchSummary(
        String transactionId,
        PartitionKey partitionKey,
        String summaryId,
        List<TNewItems> itemsToAdd,
        BiFunction<List<TNewItems>, TSummary, CosmosPatchOperations> summaryTransformationAction,
        Class<TSummary> summaryType,
        Class<TNewItems> newItemType
    );

    public abstract <T> Mono<CosmosItemResponse<T>> deleteItem();
}
