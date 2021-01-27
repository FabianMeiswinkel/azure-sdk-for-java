package com.azure.cosmos.implementation.reliableItemStore;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.HttpConstants;
import com.azure.cosmos.implementation.RetryPolicyWithDiagnostics;
import com.azure.cosmos.implementation.ShouldRetryResult;
import reactor.core.publisher.Mono;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

class TransientRetryPolicy extends RetryPolicyWithDiagnostics {
    protected final TransactionContext txCtx;
    private final int maxNumberOfRetriesOnTransientErrors;

    public TransientRetryPolicy(
        TransactionContext txCtx,
        int maxNumberOfRetriesOnTransientErrors) {
        checkNotNull(txCtx, "Argument 'txCtx' must not be null.");
        this.txCtx = txCtx;
        this.maxNumberOfRetriesOnTransientErrors = maxNumberOfRetriesOnTransientErrors;
    }

    @Override
    public Mono<ShouldRetryResult> shouldRetry(Exception e) {
        checkNotNull(e, "Argument 'e' must not be null.");

        if (!(e instanceof CosmosException)) {
            return Mono.just(ShouldRetryResult.NO_RETRY);
        }

        if (this.getRetryCount() > this.maxNumberOfRetriesOnTransientErrors) {
            Mono.just(ShouldRetryResult.NO_RETRY);
        }

        CosmosException cosmosException = (CosmosException)e;
        txCtx.recordCosmosException(cosmosException);
        int statusCode = cosmosException.getStatusCode();

        if (statusCode >= HttpConstants.StatusCodes.INTERNAL_SERVER_ERROR ||
            statusCode == HttpConstants.StatusCodes.GONE ||
            statusCode == HttpConstants.StatusCodes.REQUEST_TIMEOUT ||
            statusCode == HttpConstants.StatusCodes.RETRY_WITH ||
            (statusCode == HttpConstants.StatusCodes.NOTFOUND && cosmosException.getSubStatusCode() != 0)) {

            // TODO fabianm add back-off
            return Mono.just(ShouldRetryResult.RETRY_NOW);
        }

        return Mono.just(ShouldRetryResult.NO_RETRY);
    }
}
