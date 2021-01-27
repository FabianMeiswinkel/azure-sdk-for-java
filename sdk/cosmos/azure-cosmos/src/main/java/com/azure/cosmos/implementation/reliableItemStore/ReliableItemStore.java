package com.azure.cosmos.implementation.reliableItemStore;

import com.azure.cosmos.BridgeInternal;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncReliableItemStore;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.CosmosPatchOperations;
import com.azure.cosmos.implementation.HttpConstants;
import com.azure.cosmos.implementation.InternalObjectNode;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.implementation.ObservableHelper;
import com.azure.cosmos.implementation.Paths;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.ModelBridgeInternal;
import com.azure.cosmos.models.PartitionKey;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public class ReliableItemStore extends CosmosAsyncReliableItemStore {
    private final CosmosAsyncContainer container;
    private final boolean isSoftDeleteEnabled;
    private final String link;
    private final int maxNumberOfRetriesOnTransientErrors;
    private final long transactionLogCapacity;
    private final long ttlForSoftDeletesInSeconds;
    private final long ttlForTransactionLogInSeconds;

    public ReliableItemStore(
        CosmosAsyncContainer container,
        long ttlForTransactionLogInSeconds,
        long transactionLogCapacity,
        boolean isSoftDeleteEnabled,
        long ttlForSoftDeletesInSeconds,
        int maxNumberOfRetriesOnTransientErrors) {

        checkNotNull(container, "Argument 'container' must not be null.");

        this.container = container;
        this.ttlForTransactionLogInSeconds = ttlForTransactionLogInSeconds;
        this.transactionLogCapacity = transactionLogCapacity;
        this.isSoftDeleteEnabled = isSoftDeleteEnabled;
        this.ttlForSoftDeletesInSeconds = ttlForSoftDeletesInSeconds;
        this.maxNumberOfRetriesOnTransientErrors = maxNumberOfRetriesOnTransientErrors;
        this.link = BridgeInternal.extractContainerSelfLink(container);
    }

    CosmosAsyncContainer getContainer() {
        return this.container;
    }

    int GetMaxTransientErrorRetryCount() {
        return this.maxNumberOfRetriesOnTransientErrors;
    }

    TransactionContext createTransactionContext(String transactionId) {
        checkNotNull(transactionId, "Argument 'transactionId' must not be null.");

        return new TransactionContext(
            transactionId,
            this.ttlForTransactionLogInSeconds,
            this.transactionLogCapacity);
    }

    String getItemLink(String itemId) {
        StringBuilder builder = new StringBuilder();
        builder.append(this.link);
        builder.append("/");
        builder.append(Paths.DOCUMENTS_PATH_SEGMENT);
        builder.append("/");
        builder.append(itemId);
        return builder.toString();
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> readItem(String itemId, PartitionKey partitionKey,
                                                    CosmosItemRequestOptions options,
                                                    Class<T> itemType) {
        TransactionContext txCtx = this.createTransactionContext(UUID.randomUUID().toString());

        Mono<CosmosItemResponse<T>> observable = this
            .readItemInternal(
                itemId,
                partitionKey,
                options,
                true)
            .map(r -> txCtx.convertResponseAndAddTelemetry(r, itemType));

        return ObservableHelper.inlineIfPossible(
            () -> observable,
            new TransientRetryPolicy(txCtx, this.maxNumberOfRetriesOnTransientErrors));
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> createItem(
        String transactionId,
        PartitionKey partitionKey,
        T item) {

        @SuppressWarnings("unchecked") final Class<T> itemType =
            (Class<T>)item.getClass();

        final TransactionContext txCtx = this
            .createTransactionContext(transactionId);

        Mono<CosmosItemResponse<T>> observable = this
            .createItemInternal(
                txCtx,
                item,
                partitionKey,
                itemType)
            .map(r -> txCtx.convertResponseAndAddTelemetry(r, itemType));

        return ObservableHelper.inlineIfPossible(
            () -> observable,
            new TransientRetryPolicy(txCtx, this.GetMaxTransientErrorRetryCount()));
    }

    public <T> Mono<CosmosItemResponse<InternalObjectNode>> createItemInternal(
        TransactionContext txCtx,
        T createTemplate,
        PartitionKey partitionKey,
        Class<T> itemType) {

        final CosmosItemRequestOptions requestOptions =
            ModelBridgeInternal.createCosmosItemRequestOptions(partitionKey);

        InternalObjectNode createTemplateNode =
            InternalObjectNode.fromObjectToInternalObjectNode(createTemplate);

        final String id = createTemplateNode.getId();
        txCtx.ensureTransactionId(createTemplateNode);

        return this
            .container
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
                                    this.throwIfSoftDeleted(
                                        currentPayload,
                                        this.getItemLink(id),
                                        currentPayloadResponse.getResponseHeaders());

                                    return Mono.just(currentPayloadResponse);
                                }

                                return Mono.error(cosmosException);
                            });

                        return ObservableHelper.inlineIfPossible(
                            () -> observable,
                            new PreconditionFailedRetryPolicy(
                                txCtx,
                                this.GetMaxTransientErrorRetryCount()));
                    }
                }
                return Mono.error(unwrappedException);
            });
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> replaceItem(String transactionId, String itemId,
                                                       PartitionKey partitionKey,
                                                       Function<T, T> replaceAction,
                                                       Class<T> itemType) {

        final TransactionContext txCtx = this
            .createTransactionContext(transactionId);

        Mono<CosmosItemResponse<T>> observable = this
            .readItemInternal(
                itemId,
                partitionKey,
                ModelBridgeInternal.createCosmosItemRequestOptions(partitionKey),
                true)
            .flatMap(r -> {
                txCtx.convertResponseAndAddTelemetry(r, itemType);

                InternalObjectNode currentPayload = r.getItem();

                if (txCtx.containsTransactionId(currentPayload)) {
                    return Mono.just(r);
                }

                return this
                    .replaceItemCoreInternal(
                        txCtx,
                        itemId,
                        partitionKey,
                        currentPayload,
                        r.getETag(),
                        replaceAction,
                        itemType);
            })
            .map(r -> txCtx.convertResponseAndAddTelemetry(r, itemType));

        return ObservableHelper.inlineIfPossible(
            () -> observable,
            new PreconditionFailedRetryPolicy(txCtx, this.GetMaxTransientErrorRetryCount()));
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

    Mono<CosmosItemResponse<InternalObjectNode>> readItemInternal(
        String itemId,
        PartitionKey partitionKey,
        CosmosItemRequestOptions options,
        boolean throwIfSoftDeleted) {

        return this
            .container
            .readItem(itemId, partitionKey, options, InternalObjectNode.class)
            .map(response -> {

                if (!this.isSoftDeleteEnabled ||
                    !throwIfSoftDeleted) {

                    return response;
                }

                this.throwIfSoftDeleted(
                    response.getItem(),
                    this.getItemLink(itemId),
                    response.getResponseHeaders());

                return response;
            });
    }

    <T> Mono<CosmosItemResponse<InternalObjectNode>> replaceItemCoreInternal(
        TransactionContext txCtx,
        String id,
        PartitionKey partitionKey,
        InternalObjectNode inputItemNode,
        String ifMatchETag,
        Function<T, T> replaceAction,
        Class<T> itemType) {

        T input = inputItemNode.toObject(itemType);
        T output = replaceAction.apply(input);

        InternalObjectNode outputNode = InternalObjectNode.fromObjectToInternalObjectNode(output);
        txCtx.ensureTransactionId(outputNode);

        CosmosItemRequestOptions requestOptions =
            ModelBridgeInternal.createCosmosItemRequestOptions(partitionKey);
        requestOptions.setIfMatchETag(ifMatchETag);

        return this
            .container
            .replaceItem(outputNode, id, partitionKey, requestOptions);
    }

    void throwIfSoftDeleted(InternalObjectNode node,
                            String itemLink,
                            Map<String, String> responseHeaders) {
        Integer isDeletedFlag = node != null ?
            node.getInt(SYSTEM_PROPERTY_NAME_IS_DELETED)
            : null;

        if (isDeletedFlag != null &&
            isDeletedFlag.equals(1)) {

            throw new NotFoundException(
                String.format(
                    "The document '%s' has been soft deleted.",
                    itemLink),
                responseHeaders,
                null
            );
        }
    }
}
