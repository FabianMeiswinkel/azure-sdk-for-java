package com.azure.cosmos;

import com.azure.cosmos.implementation.reliableItemStore.ConcurrentItemStore;

import java.time.Duration;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkArgument;
import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public final class CosmosAsyncConcurrentItemStoreBuilder {
    private static final int MIN_MAX_TRANSIENT_RETRY_COUNT = 50;
    private static final long MIN_TTL_FOR_TRANSACTION_LOG_IN_SECONDS= 60 * 60;
    private static final int MIN_TRANSACTION_LOG_CAPACITY = 50;

    private final CosmosAsyncContainer container;
    private volatile int maxNumberOfRetriesOnTransientErrors;
    private volatile long ttlForSoftDeletesInSeconds;
    private volatile long ttlForTransactionLogInSeconds;
    private volatile long transactionLogCapacity;
    private volatile boolean useSoftDeletes;


    CosmosAsyncConcurrentItemStoreBuilder(CosmosAsyncContainer container) {
        checkNotNull(container, "Argument 'container' must not be null.");
        this.container = container;
        this.ttlForSoftDeletesInSeconds = -1;
        this.ttlForTransactionLogInSeconds = MIN_TTL_FOR_TRANSACTION_LOG_IN_SECONDS;
        this.transactionLogCapacity = MIN_TRANSACTION_LOG_CAPACITY;
        this.useSoftDeletes = false;
        this.maxNumberOfRetriesOnTransientErrors = Integer.MAX_VALUE;
    }

    /**
     * Enables soft-deletes without TTL - meaning all delete operations of the
     * {@link CosmosAsyncConcurrentItemStore} would result in a soft-delete and the document
     * would not automatically be physically deleted.
     * @return the Store builder with the updated configuration
     */
    public CosmosAsyncConcurrentItemStoreBuilder withSoftDeletes() {
        return this.withSoftDeletes(null);
    }

    /**
     * Enables soft-deletes without TTL - meaning all delete operations of the
     * {@link CosmosAsyncConcurrentItemStore} would result in a soft-delete
     * @param ttlForSoftDeletes - the TTL that should be set when soft-deleting documents. If null
     *                            no TTL will be applied and soft-deleted documents will not be
     *                            physically deleted automatically.
     * @return the Store builder with the updated configuration
     */
    public CosmosAsyncConcurrentItemStoreBuilder withSoftDeletes(Duration ttlForSoftDeletes) {
        checkArgument(
            ttlForSoftDeletes == null || ttlForSoftDeletes.getNano() == 0,
            "Argument 'ttlForSoftDeletes' must only have time periods up-to the second-level. " +
                "No periods with more granularity - like millisecond or nanosecond are allowed.");

        this.useSoftDeletes = true;
        this.ttlForSoftDeletesInSeconds = ttlForSoftDeletes.getSeconds();
        return this;
    }

    /**
     * Changes the retry policy for transient errors - like network connectivity issues
     * @param maxRetryCount - the maximum number of retries for transient errors. Default value is
     *                        Integer.MAX_VALUE
     * @return the Store builder with the updated configuration
     */
    public CosmosAsyncConcurrentItemStoreBuilder withTransientErrorRetryPolicy(int maxRetryCount) {
        checkArgument(
            maxRetryCount >= MIN_MAX_TRANSIENT_RETRY_COUNT,
            String.format(
                "Argument 'maxRetryCount' must have at least a value of %d.",
                MIN_MAX_TRANSIENT_RETRY_COUNT));

        this.maxNumberOfRetriesOnTransientErrors = maxRetryCount;
        return this;
    }

    /**
     * To be able to reach idempotency for write operations the concurrent reliable item store
     * will use logical transaction ids and persist them in a system property within the
     * stored documents. To limit the size this transaction log can consume the transaction logs
     * are removed based on when the transaction got committed and the total number
     * of transaction og entries.
     * @param capacity             - the maximum number of transaction log entries
     *                               persisted in each document. The default value (also minimum allowed value) is 50
     * @param ttlForTransactionLog - the duration transaction log records will be maintained. Default
     *                               duration (and minimum allowed duration) is one hour -
     *                               which would guarantee idempotency as long as
     *                               logical transactions finish within one hour. The smallest granularity
     *                               allowed are seconds (no ms or ns)
     * @return the Store builder with the updated configuration
     */
    public CosmosAsyncConcurrentItemStoreBuilder withTransactionLogPolicy(
        int capacity,
        Duration ttlForTransactionLog) {

        checkNotNull(ttlForTransactionLog, "Argument 'ttlForTransactionLog' must not be null.");
        checkArgument(
            capacity >= MIN_TRANSACTION_LOG_CAPACITY,
            String.format(
                "Argument 'capacity' must have at least a value of %d.",
                MIN_TRANSACTION_LOG_CAPACITY));
        checkArgument(
            ttlForTransactionLog.getSeconds() >= MIN_TTL_FOR_TRANSACTION_LOG_IN_SECONDS,
            String.format(
                "Argument 'ttlForTransactionLog' must have at least a value of %d seconds.",
                MIN_TTL_FOR_TRANSACTION_LOG_IN_SECONDS));
        checkArgument(
            ttlForTransactionLog.getNano() == 0,
            "Argument 'ttlForTransactionLog' must only have time periods up-to the second-level. " +
                "No periods with more granularity - like millisecond or nanosecond are allowed.");

        this.ttlForTransactionLogInSeconds = ttlForTransactionLog.getSeconds();
        this.transactionLogCapacity = capacity;
        return this;
    }

    /**
     * Creates a {@link CosmosAsyncConcurrentItemStore} instance that can be used
     * to reliably execute poin operations
     * @return an instance of {@link CosmosAsyncConcurrentItemStore}
     */
    public CosmosAsyncConcurrentItemStore build() {
        return new ConcurrentItemStore(
            this.container,
            this.ttlForTransactionLogInSeconds,
            this.transactionLogCapacity,
            this.useSoftDeletes,
            this.ttlForSoftDeletesInSeconds,
            this.maxNumberOfRetriesOnTransientErrors);
    }
}
