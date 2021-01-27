// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos;

import com.azure.cosmos.implementation.DiagnosticsClientContext;
import com.azure.cosmos.implementation.FeedResponseDiagnostics;
import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.util.Beta;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * This class represents response diagnostic statistics associated with a request to Azure Cosmos DB
 */
public final class CosmosDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger(CosmosDiagnostics.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String COSMOS_DIAGNOSTICS_KEY = "cosmosDiagnostics";

    static final String USER_AGENT = Utils.getUserAgent();
    static final String USER_AGENT_KEY = "userAgent";

    private final CosmosDiagnosticsBase implementation;

    CosmosDiagnostics(DiagnosticsClientContext diagnosticsClientContext) {
        this.implementation = new SimpleCosmosDiagnosticsImpl(diagnosticsClientContext);
    }

    CosmosDiagnostics(FeedResponseDiagnostics feedResponseDiagnostics) {
        this.implementation = new SimpleCosmosDiagnosticsImpl(feedResponseDiagnostics);
    }

    CosmosDiagnostics(List<CosmosDiagnostics> diagnostics) {
        this.implementation = new AggregatedCosmosDiagnostics(diagnostics);
    }

    ClientSideRequestStatistics clientSideRequestStatistics() {
        return this.implementation.clientSideRequestStatistics();
    }

    /**
     * Retrieves Response Diagnostic String
     *
     * @return Response Diagnostic String
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        fillCosmosDiagnostics(null, stringBuilder);
        return stringBuilder.toString();
    }

    /**
     * Retrieves duration related to the completion of the request.
     * This represents end to end duration of an operation including all the retries.
     * This is meant for point operation only, for query please use toString() to get full query diagnostics.
     *
     * @return request completion duration
     */
    public Duration getDuration() {
        return this.implementation.getDuration();
    }

    /**
     * Regions contacted for this request
     * @return set of regions contacted for this request
     */
    @Beta(value = Beta.SinceVersion.V4_9_0, warningText = Beta.PREVIEW_SUBJECT_TO_CHANGE_WARNING)
    public Set<URI> getRegionsContacted() {
        return this.implementation.getRegionsContacted();
    }

    FeedResponseDiagnostics getFeedResponseDiagnostics() {
        return this.implementation.getFeedResponseDiagnostics();
    }

    void fillCosmosDiagnostics(ObjectNode parentNode, StringBuilder stringBuilder) {
        this.implementation.fillCosmosDiagnostics(parentNode, stringBuilder);
    }

    private interface CosmosDiagnosticsBase {
        ClientSideRequestStatistics clientSideRequestStatistics();
        Set<URI> getRegionsContacted();
        Duration getDuration();
        void fillCosmosDiagnostics(ObjectNode parentNode, StringBuilder stringBuilder);
        FeedResponseDiagnostics getFeedResponseDiagnostics();
    }

    private static final class SimpleCosmosDiagnosticsImpl implements CosmosDiagnosticsBase {

        private ClientSideRequestStatistics clientSideRequestStatistics;
        private FeedResponseDiagnostics feedResponseDiagnostics;

        SimpleCosmosDiagnosticsImpl(DiagnosticsClientContext diagnosticsClientContext) {
            this.clientSideRequestStatistics = new ClientSideRequestStatistics(diagnosticsClientContext);
        }

        SimpleCosmosDiagnosticsImpl(FeedResponseDiagnostics feedResponseDiagnostics) {
            this.feedResponseDiagnostics = feedResponseDiagnostics;
        }

        @Override
        public ClientSideRequestStatistics clientSideRequestStatistics() {
            return this.clientSideRequestStatistics;
        }

        @Override
        public Set<URI> getRegionsContacted() {
            return this.clientSideRequestStatistics.getRegionsContacted();
        }

        @Override
        public Duration getDuration() {
            if (this.feedResponseDiagnostics != null) {
                return null;
            }

            return this.clientSideRequestStatistics.getDuration();
        }

        @Override
        public void fillCosmosDiagnostics(ObjectNode parentNode, StringBuilder stringBuilder) {
            if (this.feedResponseDiagnostics != null) {
                if (parentNode != null) {
                    parentNode.put(USER_AGENT_KEY, USER_AGENT);
                    parentNode.putPOJO(COSMOS_DIAGNOSTICS_KEY, feedResponseDiagnostics);
                }

                if (stringBuilder != null) {
                    stringBuilder.append(USER_AGENT_KEY +"=").append(USER_AGENT).append(System.lineSeparator());
                    stringBuilder.append(feedResponseDiagnostics);
                }
            } else {
                if (parentNode != null) {
                    parentNode.putPOJO(COSMOS_DIAGNOSTICS_KEY, clientSideRequestStatistics);
                }

                if (stringBuilder != null) {
                    try {
                        stringBuilder.append(OBJECT_MAPPER.writeValueAsString(this.clientSideRequestStatistics));
                    } catch (JsonProcessingException e) {
                        LOGGER.error("Error while parsing diagnostics ", e);
                    }
                }
            }
        }

        @Override
        public FeedResponseDiagnostics getFeedResponseDiagnostics() {
            return feedResponseDiagnostics;
        }
    }

    private static final class AggregatedCosmosDiagnostics implements CosmosDiagnosticsBase {

        private final CosmosDiagnostics[] diagnostics;

        public AggregatedCosmosDiagnostics(List<CosmosDiagnostics> diagnosticsList) {
            if (diagnosticsList != null) {
                diagnostics = new CosmosDiagnostics[diagnosticsList.size()];
                diagnosticsList.toArray(this.diagnostics);
            } else {
                diagnostics = null;
            }
        }

        @Override
        public ClientSideRequestStatistics clientSideRequestStatistics() {
            return null;
        }

        @Override
        public Set<URI> getRegionsContacted() {
            return null;
        }

        @Override
        public Duration getDuration() {
            return null;
        }

        @Override
        public void fillCosmosDiagnostics(ObjectNode parentNode, StringBuilder stringBuilder) {
            if (this.diagnostics == null) {
                return;
            }


            if (parentNode != null) {
                ArrayNode diagnosticsRootNode = parentNode.putArray(COSMOS_DIAGNOSTICS_KEY);
                for (CosmosDiagnostics inner : this.diagnostics) {
                    ClientSideRequestStatistics clientSideRequestStatistics = inner.clientSideRequestStatistics();
                    if (clientSideRequestStatistics != null) {
                        diagnosticsRootNode.addPOJO(clientSideRequestStatistics);
                    }
                }
            }

            if (stringBuilder != null) {
                try {
                    stringBuilder.append("[");
                    boolean isFirst = true;
                    for (CosmosDiagnostics inner : this.diagnostics) {
                        ClientSideRequestStatistics clientSideRequestStatistics = inner.clientSideRequestStatistics();
                        if (clientSideRequestStatistics != null) {
                            if (isFirst) {
                                isFirst = false;
                            } else {
                                stringBuilder.append(", ");
                            }
                            stringBuilder.append(
                                OBJECT_MAPPER.writeValueAsString(clientSideRequestStatistics));
                        }
                    }
                    stringBuilder.append("]");
                } catch (JsonProcessingException e) {
                    LOGGER.error("Error while parsing diagnostics ", e);
                }
            }
        }

        @Override
        public FeedResponseDiagnostics getFeedResponseDiagnostics() {
            throw new UnsupportedOperationException();
        }
    }
}
