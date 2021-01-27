package com.azure.cosmos.implementation.reliableItemStore;

import com.azure.cosmos.CosmosBridgeInternal;
import com.azure.cosmos.CosmosDiagnostics;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.InternalObjectNode;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.ModelBridgeInternal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

final class TransactionContext {
    private static final String SYSTEM_PROPERTY_NAME_TRANSACTIONS = "_txs";
    private static final String SYSTEM_PROPERTY_NAME_TRANSACTION_ID = "id";
    private static final String SYSTEM_PROPERTY_NAME_TRANSACTION_EPOCH_MS = "ts";

    private final List<CosmosDiagnostics> diagnostics;
    private final AtomicDouble totalRequestCharge;
    private final String transactionId;
    private final long ttlForTransactionLogInSeconds;
    private final long transactionLogCapacity;
    private boolean hasOverride;

    public TransactionContext(
        String transactionId,
        long ttlForTransactionLogInSeconds,
        long transactionLogCapacity) {

        this.totalRequestCharge = new AtomicDouble();
        this.diagnostics = new ArrayList<>();
        this.transactionId = transactionId;
        this.hasOverride = false;
        this.transactionLogCapacity = transactionLogCapacity;
        this.ttlForTransactionLogInSeconds = ttlForTransactionLogInSeconds;
    }

    public <T> void recordItemResponse(CosmosItemResponse<T> response) {
        checkNotNull(response, "Argument 'response' must not be null.");

        this.hasOverride = true;
        totalRequestCharge.getAndAdd(response.getRequestCharge());
        CosmosDiagnostics responseDiagnostics = response.getDiagnostics();
        if (responseDiagnostics != null) {
            this.diagnostics.add(responseDiagnostics);
        }
    }

    public void recordCosmosException(CosmosException cosmosException) {
        checkNotNull(cosmosException, "Argument 'cosmosException' must not be null.");

        this.hasOverride = true;
        // TODO fabianm add other overrides (activityId etc.)
        totalRequestCharge.getAndAdd(cosmosException.getRequestCharge());
        CosmosDiagnostics responseDiagnostics = cosmosException.getDiagnostics();
        if (responseDiagnostics != null) {
            this.diagnostics.add(responseDiagnostics);
        }
    }

    public <T> CosmosItemResponse<T> convertResponseAndAddTelemetry(
        CosmosItemResponse<InternalObjectNode> response,
        Class<T> itemType) {

        if (!this.hasOverride) {
            return ModelBridgeInternal.cloneCosmosItemResponse(
                response,
                itemType,
                null,
                null,
                null,
                null);
        }

        return ModelBridgeInternal.cloneCosmosItemResponse(
            response,
            itemType,
            null,
            CosmosBridgeInternal.createAggregatedDiagnostics(this.diagnostics),
            null,
            this.totalRequestCharge.value.get());
    }

    public boolean containsTransactionId(InternalObjectNode node) {
        checkNotNull(node, "Argument 'node' must not be null.");

        ArrayNode txsNode = (ArrayNode)node
            .getPropertyBag()
            .get(SYSTEM_PROPERTY_NAME_TRANSACTIONS);

        if (txsNode == null) {
            return false;
        }

        for(int i = 0; i < txsNode.size(); i++) {
            JsonNode txIdNode = txsNode
                .get(i)
                .get(SYSTEM_PROPERTY_NAME_TRANSACTION_ID);
            if (txIdNode == null ||
                !txIdNode.isValueNode() ||
                !txIdNode.isTextual())
            {
                continue;
            }

            if (transactionId.equals(txIdNode.textValue())) {
                return true;
            }
        }

        return false;
    }

    public boolean ensureTransactionId(InternalObjectNode node) {

        checkNotNull(node, "Argument 'node' must not be null.");

        ArrayNode txsNode = (ArrayNode)node
            .getPropertyBag()
            .get(SYSTEM_PROPERTY_NAME_TRANSACTIONS);

        if (txsNode == null) {
            txsNode = node
                .getPropertyBag()
                .putArray(SYSTEM_PROPERTY_NAME_TRANSACTIONS);

            this.addNewTransactionNode(txsNode, transactionId);

            return false;
        }

        for(int i = 0; i < txsNode.size(); i++) {
            JsonNode txIdNode = txsNode
                .get(i)
                .get(SYSTEM_PROPERTY_NAME_TRANSACTION_ID);
            if (txIdNode == null ||
                !txIdNode.isValueNode() ||
                !txIdNode.isTextual())
            {
                continue;
            }

            if (transactionId.equals(txIdNode.textValue())) {
                this.trimTransactionNodes(txsNode, i);
                return true;
            }
        }

        this.trimTransactionNodes(txsNode, Integer.MAX_VALUE);
        this.addNewTransactionNode(txsNode, transactionId);
        return false;
    }

    private void addNewTransactionNode(ArrayNode txsNode, String transactionId) {
        checkNotNull(txsNode, "Argument 'txsNode' must not be null.");
        checkNotNull(transactionId, "Argument 'transactionId' must not be null.");

        ObjectNode txRecordNode = txsNode.addObject();
        txRecordNode.put(
            SYSTEM_PROPERTY_NAME_TRANSACTION_ID,
            Instant.now().toEpochMilli());
        txRecordNode.put(
            SYSTEM_PROPERTY_NAME_TRANSACTION_EPOCH_MS,
            Instant.now().toEpochMilli());
    }

    private void trimTransactionNodes(
        ArrayNode txsNode,
        int upToIndex) {

        checkNotNull(txsNode, "Argument 'txsNode' must not be null.");

        long threshold = Instant
            .now()
            .minusSeconds(this.ttlForTransactionLogInSeconds)
            .toEpochMilli();

        int i = 0;
        while (i < upToIndex && i < txsNode.size()) {
            JsonNode txTimeNode = txsNode
                .get(i)
                .get(SYSTEM_PROPERTY_NAME_TRANSACTION_EPOCH_MS);
            if (txTimeNode != null &&
                txTimeNode.isValueNode() &&
                txTimeNode.isLong() &&
                txTimeNode.asLong() >= threshold &&
                txsNode.size() <= this.transactionLogCapacity)
            {
                i++;
                continue;
            }

            txsNode.remove(0);
            upToIndex--;
        }
    }

    private static final class AtomicDouble {
        private final AtomicReference<Double> value = new AtomicReference<>(0.0);

        public double getAndAdd(double delta) {
            while (true) {
                Double currentValue = value.get();
                Double newValue = currentValue + delta;
                if (value.compareAndSet(currentValue, newValue)) {
                    return currentValue;
                }
            }
        }
    }
}
