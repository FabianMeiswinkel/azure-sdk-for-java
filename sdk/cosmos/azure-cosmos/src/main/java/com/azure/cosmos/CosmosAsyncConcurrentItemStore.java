package com.azure.cosmos;

import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public abstract class CosmosAsyncConcurrentItemStore extends CosmosAsyncReliableItemStore {

    public <T> Mono<CosmosItemResponse<T>> createOrPatchItem(
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction) {

        return this.createOrPatchItem(
            UUID.randomUUID().toString(),
            null,
            createTemplate,
            patchAction
        );
    }

    public <T> Mono<CosmosItemResponse<T>> createOrPatchItem(
        String transactionId,
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction) {

        return this.createOrPatchItem(
            transactionId,
            null,
            createTemplate,
            patchAction
        );
    }

    public <T> Mono<CosmosItemResponse<T>> createOrPatchItem(
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction) {

        return this.createOrPatchItem(
            UUID.randomUUID().toString(),
            partitionKey,
            createTemplate,
            patchAction
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> createOrPatchItem(
        String transactionId,
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction);

    public <T> Mono<CosmosItemResponse<T>> createOrReadItem(
        T createTemplate) {

        return this.createOrReadItem(
            UUID.randomUUID().toString(),
            null,
            createTemplate
        );
    }

    public <T> Mono<CosmosItemResponse<T>> createOrReadItem(
        String transactionId,
        T createTemplate) {

        return this.createOrReadItem(
            transactionId,
            null,
            createTemplate
        );
    }

    public <T> Mono<CosmosItemResponse<T>> createOrReadItem(
        PartitionKey partitionKey,
        T createTemplate) {

        return this.createOrReadItem(
            UUID.randomUUID().toString(),
            partitionKey,
            createTemplate
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> createOrReadItem(
        String transactionId,
        PartitionKey partitionKey,
        T createTemplate
    );

    public <T> Mono<CosmosItemResponse<T>> createOrReplaceItem(
        T createTemplate,
        Function<T, T> replaceAction) {

        return createOrReplaceItem(
            UUID.randomUUID().toString(),
            createTemplate,
            replaceAction
        );
    }

    public <T> Mono<CosmosItemResponse<T>> createOrReplaceItem(
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, T> replaceAction) {

        return createOrReplaceItem(
            UUID.randomUUID().toString(),
            partitionKey,
            createTemplate,
            replaceAction
        );
    }

    public <T> Mono<CosmosItemResponse<T>> createOrReplaceItem(
        String transactionId,
        T createTemplate,
        Function<T, T> replaceAction) {

        checkNotNull(createTemplate, "Argument 'createTemplate' must not be null.");

        return createOrReplaceItem(
            transactionId,
            null,
            createTemplate,
            replaceAction
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> createOrReplaceItem(
        String transactionId,
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, T> replaceAction);

    public <T> Mono<CosmosItemResponse<T>> patchOrCreateItem(
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction) {

        return this.patchOrCreateItem(
            UUID.randomUUID().toString(),
            null,
            createTemplate,
            patchAction
        );
    }

    public <T> Mono<CosmosItemResponse<T>> patchOrCreateItem(
        String transactionId,
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction) {

        return this.patchOrCreateItem(
            transactionId,
            null,
            createTemplate,
            patchAction
        );
    }

    public <T> Mono<CosmosItemResponse<T>> patchOrCreateItem(
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction) {

        return this.patchOrCreateItem(
            UUID.randomUUID().toString(),
            partitionKey,
            createTemplate,
            patchAction
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> patchOrCreateItem(
        String transactionId,
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, CosmosPatchOperations> patchAction
    );

    public <T> Mono<CosmosItemResponse<T>> readOrCreateItem(
        T createTemplate) {

        return this.readOrCreateItem(
            UUID.randomUUID().toString(),
            null,
            createTemplate
        );
    }

    public <T> Mono<CosmosItemResponse<T>> readOrCreateItem(
        String transactionId,
        T createTemplate) {

        return this.readOrCreateItem(
            transactionId,
            null,
            createTemplate
        );
    }

    public <T> Mono<CosmosItemResponse<T>> readOrCreateItem(
        PartitionKey partitionKey,
        T createTemplate) {

        return this.readOrCreateItem(
            UUID.randomUUID().toString(),
            partitionKey,
            createTemplate
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> readOrCreateItem(
        String transactionId,
        PartitionKey partitionKey,
        T createTemplate
    );

    public <T> Mono<CosmosItemResponse<T>> replaceOrCreateItem(
        T createTemplate,
        Function<T, T> replaceAction) {

        return this.replaceOrCreateItem(
            UUID.randomUUID().toString(),
            null,
            createTemplate,
            replaceAction
        );
    }

    public <T> Mono<CosmosItemResponse<T>> replaceOrCreateItem(
        String transactionId,
        T createTemplate,
        Function<T, T> replaceAction) {

        return this.replaceOrCreateItem(
            transactionId,
            null,
            createTemplate,
            replaceAction
        );
    }

    public <T> Mono<CosmosItemResponse<T>> replaceOrCreateItem(
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, T> replaceAction) {

        return this.replaceOrCreateItem(
            UUID.randomUUID().toString(),
            partitionKey,
            createTemplate,
            replaceAction
        );
    }

    public abstract <T> Mono<CosmosItemResponse<T>> replaceOrCreateItem(
        String transactionId,
        PartitionKey partitionKey,
        T createTemplate,
        Function<T, T> replaceAction
    );
}
