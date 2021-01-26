package com.azure.cosmos.implementation.reliableItemStore;

import com.azure.core.util.Context;
import com.azure.cosmos.BridgeInternal;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosAsyncReliableItemStore;
import com.azure.cosmos.CosmosBridgeInternal;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.CosmosPatchOperations;
import com.azure.cosmos.implementation.AsyncDocumentClient;
import com.azure.cosmos.implementation.CosmosPagedFluxOptions;
import com.azure.cosmos.implementation.Document;
import com.azure.cosmos.implementation.HttpConstants;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.implementation.OperationType;
import com.azure.cosmos.implementation.Paths;
import com.azure.cosmos.implementation.RequestOptions;
import com.azure.cosmos.implementation.ResourceType;
import com.azure.cosmos.implementation.TracerProvider;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.ModelBridgeInternal;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedFlux;
import com.azure.cosmos.util.UtilBridgeInternal;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.azure.core.util.FluxUtil.withContext;
import static com.azure.cosmos.implementation.Utils.setContinuationTokenAndMaxItemCount;
import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkArgument;
import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public class ReliableItemStore extends CosmosAsyncReliableItemStore {
    protected final AsyncDocumentClient asyncDocumentClient;
    protected final CosmosAsyncClient client;
    protected final CosmosAsyncContainer container;
    protected final String containerId;
    protected final String databaseId;
    protected final boolean isSoftDeleteEnabled;
    protected final String link;
    protected final TracerProvider tracerProvider;
    protected final long ttlForSoftDeletesInSeconds;

    protected final String createItemSpanName;
    protected final String readItemSpanName;

    public ReliableItemStore(
        CosmosAsyncClient client,
        CosmosAsyncDatabase database,
        CosmosAsyncContainer container,
        Duration defaultTtlForSoftDeletes) {

        checkNotNull(client, "Argument 'client' must not be null.");
        checkNotNull(database, "Argument 'database' must not be null.");
        checkNotNull(container, "Argument 'container' must not be null.");
        checkArgument(
            defaultTtlForSoftDeletes == null || defaultTtlForSoftDeletes.getNano() == 0,
            "Argument 'ttlForSoftDeletes' must only have time periods up-to the second-level. " +
                "No periods with more granularity - like millisecond or nanosecond are allowed.");

        this.container = container;
        this.asyncDocumentClient = CosmosBridgeInternal.getAsyncDocumentClient(database);
        this.client = client;
        this.tracerProvider = CosmosBridgeInternal.getTracerProvider(container);
        this.link = BridgeInternal.extractContainerSelfLink(container);
        this.containerId = container.getId();
        this.databaseId = database.getId();
        this.isSoftDeleteEnabled = defaultTtlForSoftDeletes != null &&
            !defaultTtlForSoftDeletes.isNegative();
        this.ttlForSoftDeletesInSeconds = this.isSoftDeleteEnabled ?
            defaultTtlForSoftDeletes.getSeconds() : -1;

        this.createItemSpanName = "reliableStore.createItem." + container.getId();
        this.readItemSpanName = "reliableStore.readItem." + container.getId();
    }

    @Override
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

    protected <T> Mono<CosmosItemResponse<T>> readItemInternal(
        String itemId,
        PartitionKey partitionKey,
        CosmosItemRequestOptions options,
        Class<T> itemType,
        boolean throwIfSoftDeleted) {

        return this
            .container
            .readItem(itemId, partitionKey, options, itemType)
            .map(response -> {

                if (!this.isSoftDeleteEnabled ||
                    !throwIfSoftDeleted) {

                    return response;
                }

                final String itemLink = getItemLink(itemId);
                Document document = response.getResource();
                Integer isDeletedFlag = document != null ?
                    document.getInt(SYSTEM_PROPERTY_NAME_IS_DELETED)
                    : null;

                if (document != null &&
                    isDeletedFlag != null &&
                    isDeletedFlag.equals(1)) {

                    throw new NotFoundException(
                        String.format(
                            "The document '%s' has been soft deleted.",
                            itemLink),
                        response.getResponseHeaders(),
                        null
                    );
                }

                return response;
            });
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> readItem(
        String itemId,
        PartitionKey partitionKey,
        CosmosItemRequestOptions options,
        Class<T> itemType) {


    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> createOrReplaceItem(
        String transactionId,
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
    public <T> Mono<CosmosItemResponse<T>> createOrReadItem(String transactionId, PartitionKey partitionKey, T createTemplate) {
        return null;
    }

    @Override
    public <T> Mono<CosmosItemResponse<T>> readOrCreateItem(String transactionId,
                                                            PartitionKey partitionKey, T createTemplate) {
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

    private String getItemLink(String itemId) {
        StringBuilder builder = new StringBuilder();
        builder.append(this.link);
        builder.append("/");
        builder.append(Paths.DOCUMENTS_PATH_SEGMENT);
        builder.append("/");
        builder.append(itemId);
        return builder.toString();
    }

    private <T> Mono<CosmosItemResponse<T>> createOrReplaceItemInternal(
        String transactionId,
        PartitionKey partitionKey,
        String id,
        T createTemplate,
        Function<T, T> replaceAction,
        Context context) {

        checkNotNull(transactionId, "Argument 'transactionId' must not be null");
        checkNotNull(id, "Argument 'id' must not be null");
        checkNotNull(createTemplate, "Argument 'createTemplate' must not be null");
        checkNotNull(context, "Argument 'context' must not be null");

        CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        ModelBridgeInternal.setPartitionKey(options, partitionKey);

        Mono<CosmosItemResponse<T>> responseMono = createOrReplaceItem(
            transactionId,
            partitionKey,
            id,
            createTemplate,
            replaceAction,
            options);

        return this.tracerProvider.traceEnabledCosmosItemResponsePublisher(responseMono,
            context,
            this.createOrReplaceItemSpanName,
            this.containerId,
            this.databaseId,
            this.client,
            null, // Intentionally forcing default consistency - for writes the only option anyway
            OperationType.,
            ResourceType.Document);
    }

    private <T> Mono<CosmosItemResponse<T>> createOrReplaceItem(
        String transactionId,
        PartitionKey partitionKey,
        String id,
        T createTemplate,
        Function<T, T> replaceAction,
        CosmosItemRequestOptions options) {

        @SuppressWarnings("unchecked")
        Class<T> itemType = (Class<T>) createTemplate.getClass();
        RequestOptions requestOptions = ModelBridgeInternal.toRequestOptions(options);
        return this.asyncDocumentClient
                       .createDocument(
                           this.link,
                           createTemplate,
                           requestOptions,
                           true)
                       .onErrorResume(exception -> {
                           final Throwable unwrappedException = Exceptions.unwrap(exception);
                           if (unwrappedException instanceof CosmosException) {
                               final CosmosException cosmosException = (CosmosException) unwrappedException;
                               if (cosmosException.getStatusCode() == HttpConstants.StatusCodes.CONFLICT) {
                                   final String itemLink = getItemLink(itemId);

                                   this.asyncDocumentClient
                                       .readDocument(
                                           this.
                                       )
                               }
                           }
                           return Mono.error(unwrappedException);
                       })
                       .map(response -> ModelBridgeInternal.createCosmosAsyncItemResponse(
                           response,
                           itemType,
                           this.asyncDocumentClient.getItemDeserializer()))
                       .single();
    }

    private <T> Mono<CosmosItemResponse<T>> readItemInternal(
        String itemId,
        RequestOptions requestOptions, Class<T> itemType,
        Context context,
        boolean throwIfSoftDeleted) {

        final String itemLink = getItemLink(itemId);
        Mono<CosmosItemResponse<T>> responseMono =
            this.asyncDocumentClient
                .readDocument(itemLink, requestOptions)
                .map(response -> {

                    if (!this.isSoftDeleteEnabled ||
                        !throwIfSoftDeleted) {

                        return response;
                    }

                    Document document = response.getResource();
                    Integer isDeletedFlag = document != null ?
                        document.getInt(SYSTEM_PROPERTY_NAME_IS_DELETED)
                        : null;

                    if (document != null &&
                        isDeletedFlag != null &&
                        isDeletedFlag.equals(1)) {

                        throw new NotFoundException(
                            String.format(
                                "The document '%s' has been soft deleted.",
                                itemLink),
                            response.getResponseHeaders(),
                            null
                        );
                    }

                    return response;
                })
                .map(response -> ModelBridgeInternal.createCosmosAsyncItemResponse(
                    response,
                    itemType,
                    this.asyncDocumentClient.getItemDeserializer()))
                .single();
        return this.tracerProvider.traceEnabledCosmosItemResponsePublisher(responseMono,
            context,
            this.readItemSpanName,
            this.containerId,
            this.databaseId,
            this.client,
            requestOptions.getConsistencyLevel(),
            OperationType.Read,
            ResourceType.Document);
    }
}
