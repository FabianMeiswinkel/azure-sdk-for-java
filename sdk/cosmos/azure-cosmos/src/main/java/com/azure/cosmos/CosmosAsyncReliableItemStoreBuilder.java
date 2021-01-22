package com.azure.cosmos;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public class CosmosAsyncReliableItemStoreBuilder {
    private final CosmosAsyncContainer container;
    private ReliableStoreMode storeMode;

    CosmosAsyncReliableItemStoreBuilder(CosmosAsyncContainer container) {
        checkNotNull(container, "Argument 'container' must not be null.");
        this.container = container;
        this.storeMode = ReliableStoreMode.MUTABLE_ITEMS;
    }

    public CosmosAsyncReliableItemStoreBuilder forImmutableItems() {
        this.storeMode = ReliableStoreMode.IMMUTABLE_ITEMS;

        return this;
    }

    public CosmosAsyncReliableItemStore build() {
        switch (this.storeMode) {
            case MUTABLE_ITEMS:
            case IMMUTABLE_ITEMS:
                return null;
            default:
                throw new IllegalStateException(
                    String.format(
                        "Unknown store model %s",
                        this.storeMode));
        }
    }

    private enum ReliableStoreMode {
        MUTABLE_ITEMS,
        IMMUTABLE_ITEMS
    }
}
