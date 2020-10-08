package com.azure.cosmos;

import com.azure.cosmos.models.PartitionKey;

public interface CosmosItemOperation {
    public String getId();

    public PartitionKey getPartitionKeyValue();

    public CosmosItemOperationType getOperationType();

    public <T> T getItem();
}
