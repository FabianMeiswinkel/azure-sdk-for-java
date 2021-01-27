package com.azure.cosmos.implementation.reliableItemStore;

import com.azure.cosmos.CosmosAsyncConcurrentItemStore;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.CosmosPatchOperations;
import com.azure.cosmos.implementation.HttpConstants;
import com.azure.cosmos.implementation.InternalObjectNode;
import com.azure.cosmos.implementation.ObservableHelper;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.ModelBridgeInternal;
import com.azure.cosmos.models.PartitionKey;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ConcurrentItemStore extends CosmosAsyncConcurrentItemStore {

    private final ReliableItemStore reliableStore;

    public ConcurrentItemStore(
        CosmosAsyncContainer container,
        long ttlForTransactionLogInSeconds,
        long transactionLogCapacity,
        boolean isSoftDeleteEnabled,
        long ttlForSoftDeletesInSeconds,
        int maxNumberOfRetriesOnTransientErrors) {

        this.reliableStore = new ReliableItemStore(
            container,
            ttlForTransactionLogInSeconds,
            transactionLogCapacity,
            isSoftDeleteEnabled,
            ttlForSoftDeletesInSeconds,
            maxNumberOfRetriesOnTransientErrors);
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> createOrPatchItem(String transactionId,
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
    public <T> Mono<CosmosItemResponse<T>> createOrReplaceItem(String transactionId,
                                                               PartitionKey partitionKey,
                                                               T createTemplate,
                                                               Function<T, T> replaceAction) {
        @SuppressWarnings("unchecked") final Class<T> itemType =
            (Class<T>)createTemplate.getClass();

        final TransactionContext txCtx = this
            .reliableStore
            .createTransactionContext(transactionId);

        Mono<CosmosItemResponse<T>> observable = this
            .createOrReplaceItemInternal(
                txCtx,
                createTemplate,
                partitionKey,
                replaceAction,
                itemType)
            .map(r -> txCtx.convertResponseAndAddTelemetry(r, itemType));

        return ObservableHelper.inlineIfPossible(
            () -> observable,
            new TransientRetryPolicy(txCtx, this.reliableStore.GetMaxTransientErrorRetryCount()));
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> patchOrCreateItem(String transactionId,
                                                             PartitionKey partitionKey,
                                                             T createTemplate, Function<T,
        CosmosPatchOperations> patchAction) {
        return null;
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> readOrCreateItem(String transactionId,
                                                            PartitionKey partitionKey,
                                                            T createTemplate) {
        return null;
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> replaceOrCreateItem(String transactionId,
                                                               PartitionKey partitionKey,
                                                               T createTemplate,
                                                               Function<T, T> replaceAction) {
        return null;
    }

    public <T> Mono<CosmosItemResponse<InternalObjectNode>> createOrReplaceItemInternal(
        TransactionContext txCtx,
        T createTemplate,
        PartitionKey partitionKey,
        Function<T, T> replaceAction,
        Class<T> itemType) {

        final CosmosItemRequestOptions requestOptions =
            ModelBridgeInternal.createCosmosItemRequestOptions(partitionKey);

        InternalObjectNode createTemplateNode =
            InternalObjectNode.fromObjectToInternalObjectNode(createTemplate);

        final String id = createTemplateNode.getId();
        txCtx.ensureTransactionId(createTemplateNode);

        return this
            .reliableStore
            .getContainer()
            .createItem(
                createTemplateNode,
                requestOptions)
            .onErrorResume(exception -> {
                final Throwable unwrappedException = Exceptions.unwrap(exception);
                if (unwrappedException instanceof CosmosException) {
                    final CosmosException cosmosException = (CosmosException)unwrappedException;
                    if (cosmosException.getStatusCode() == HttpConstants.StatusCodes.CONFLICT) {
                        txCtx.recordCosmosException(cosmosException);
                        Mono<CosmosItemResponse<InternalObjectNode>> observable = this
                            .reliableStore
                            .readItemInternal(
                                id,
                                partitionKey,
                                requestOptions,
                                false)
                            .flatMap(currentPayloadResponse -> {
                                txCtx.recordItemResponse(currentPayloadResponse);
                                InternalObjectNode currentPayload =
                                    currentPayloadResponse.getItem();

                                if (txCtx.containsTransactionId(currentPayload)) {
                                    this.reliableStore.throwIfSoftDeleted(
                                        currentPayload,
                                        this.reliableStore.getItemLink(id),
                                        currentPayloadResponse.getResponseHeaders());

                                    return Mono.just(currentPayloadResponse);
                                }

                                return this
                                    .reliableStore
                                    .replaceItemCoreInternal(
                                        txCtx,
                                        id,
                                        partitionKey,
                                        currentPayload,
                                        currentPayloadResponse.getETag(),
                                        replaceAction,
                                        itemType);
                            });

                        return ObservableHelper.inlineIfPossible(
                            () -> observable,
                            new PreconditionFailedRetryPolicy(
                                txCtx,
                                this.reliableStore.GetMaxTransientErrorRetryCount()));
                    }
                }
                return Mono.error(unwrappedException);
            });
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> readItem(String itemId, PartitionKey partitionKey,
                                                    CosmosItemRequestOptions options,
                                                    Class<T> itemType) {
        return this.reliableStore.readItem(itemId, partitionKey, options, itemType);
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> createItem(String transactionId,
                                                      PartitionKey partitionKey, T item) {
        return this.reliableStore.createItem(transactionId, partitionKey, item);
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> replaceItem(String transactionId, String itemId,
                                                       PartitionKey partitionKey,
                                                       Function<T, T> replaceAction,
                                                       Class<T> itemType) {

        return this.reliableStore.replaceItem(transactionId, itemId, partitionKey, replaceAction, itemType);
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> patchItem(String transactionId, String itemId,
                                                     PartitionKey partitionKey, Function<T,
        CosmosPatchOperations> patchAction, Class<T> itemType) {
        return null;
    }

    @Override
    public <TSummary, TNewItems> Mono<CosmosItemResponse<TSummary>> addItemsAndReplaceSummary(String transactionId, String summaryItemId, PartitionKey partitionKey, List<TNewItems> itemsToAdd, BiFunction<List<TNewItems>, TSummary, TSummary> summaryTransformationAction, Class<TSummary> summaryType, Class<TNewItems> newItemType) {
        return null;
    }

    @Override
    public <TSummary, TNewItems> Mono<CosmosItemResponse<TSummary>> addItemsAndPatchSummary(String transactionId, String summaryItemId, PartitionKey partitionKey, List<TNewItems> itemsToAdd, BiFunction<List<TNewItems>, TSummary, CosmosPatchOperations> summaryTransformationAction, Class<TSummary> summaryType, Class<TNewItems> newItemType) {
        return null;
    }

    @Override
    public Mono<CosmosItemResponse<Object>> deleteItem(String transactionId, String itemId,
                                                       PartitionKey partitionKey) {
        return null;
    }
}
