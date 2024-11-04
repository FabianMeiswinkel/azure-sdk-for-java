// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.implementation.faultinjection;

import com.azure.cosmos.implementation.RxDocumentServiceRequest;
import com.azure.cosmos.implementation.apachecommons.lang.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public class AddressInjector {
    private List<IServerErrorInjector> faultInjectors = new ArrayList<>();

    public void registerServerErrorInjector(IServerErrorInjector serverErrorInjector) {
        checkNotNull(serverErrorInjector, "Argument 'serverErrorInjector' can not be null");
        this.faultInjectors.add(serverErrorInjector);
    }

    public Pair<Boolean, Boolean> shouldScrambleAddresses(RxDocumentServiceRequest request) {
        FaultInjectionRequestArgs faultInjectionRequestArgs = this.createFaultInjectionRequestArgs(request);
        for (IServerErrorInjector injector : this.faultInjectors) {
            Pair<Boolean, Boolean> result = injector.scrambleAddresses(faultInjectionRequestArgs);
            if (result.getLeft()) {
                return result;
            }
        }

        return Pair.of(false, false);
    }

    FaultInjectionRequestArgs createFaultInjectionRequestArgs(RxDocumentServiceRequest rxDocumentServiceRequest) {
        boolean isPrimary = rxDocumentServiceRequest.isReadOnlyRequest() ? false : true;
        return new FaultInjectionRequestArgs(0, null, isPrimary, rxDocumentServiceRequest) {
            @Override
            public List<String> getPartitionKeyRangeIds() {
                if(rxDocumentServiceRequest.getPartitionKeyRangeIdentity() != null) {
                    return Arrays.asList(rxDocumentServiceRequest.getPartitionKeyRangeIdentity().getPartitionKeyRangeId());
                }

                return new ArrayList<>();
            }

            @Override
            public String getCollectionRid() {
                return rxDocumentServiceRequest.requestContext.resolvedCollectionRid;
            }
        };
    }
}
