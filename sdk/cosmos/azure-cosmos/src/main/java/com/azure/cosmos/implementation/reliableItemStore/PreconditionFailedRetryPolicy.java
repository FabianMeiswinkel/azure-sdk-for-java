package com.azure.cosmos.implementation.reliableItemStore;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.HttpConstants;
import com.azure.cosmos.implementation.ShouldRetryResult;
import reactor.core.publisher.Mono;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

final class PreconditionFailedRetryPolicy extends TransientRetryPolicy {
    public PreconditionFailedRetryPolicy(TransactionContext txCtx,int maxNumberOfRetriesOnTransientErrors) {
        super(txCtx, maxNumberOfRetriesOnTransientErrors);
    }

    @Override
    public Mono<ShouldRetryResult> shouldRetry(Exception e) {
        return this
            .shouldRetryInternal(e)
            .flatMap(result -> !result.shouldRetry ? super.shouldRetry(e) : Mono.just(result));
    }

    private Mono<ShouldRetryResult> shouldRetryInternal(Exception e) {
        checkNotNull(e, "Argument 'e' must not be null.");

        if (!(e instanceof CosmosException)) {
            return Mono.just(ShouldRetryResult.NO_RETRY);
        }

        CosmosException cosmosException = (CosmosException)e;
        txCtx.recordCosmosException(cosmosException);
        int statusCode = cosmosException.getStatusCode();

        if (statusCode == HttpConstants.StatusCodes.PRECONDITION_FAILED) {
            return Mono.just(ShouldRetryResult.RETRY_NOW);
        }

        return Mono.just(ShouldRetryResult.NO_RETRY);
    }
}
