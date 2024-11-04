// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.implementation.directconnectivity;

import com.azure.cosmos.CosmosContainerProactiveInitConfig;
import com.azure.cosmos.implementation.GoneException;
import com.azure.cosmos.implementation.RxDocumentServiceRequest;
import com.azure.cosmos.implementation.Strings;
import com.azure.cosmos.implementation.apachecommons.lang.tuple.Pair;
import com.azure.cosmos.implementation.faultinjection.AddressInjector;
import com.azure.cosmos.implementation.faultinjection.IFaultInjectorProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class AddressSelector {
    private final IAddressResolver addressResolver;
    private final Protocol protocol;
    private final Random random = new Random();
    private final AddressInjector addressInjector = new AddressInjector();

    public AddressSelector(IAddressResolver addressResolver, Protocol protocol) {
        this.addressResolver = addressResolver;
        this.protocol = protocol;
    }

    public void configureFaultInjectionProvider(IFaultInjectorProvider injectorProvider) {
        this.addressInjector
            .registerServerErrorInjector(injectorProvider.getServerErrorInjector());
    }

    public Mono<List<Uri>> resolveAllUriAsync(
        RxDocumentServiceRequest request,
        boolean includePrimary,
        boolean forceRefresh) {
        Mono<List<AddressInformation>> allReplicaAddressesObs = this.resolveAddressesAsync(request, forceRefresh);
        return allReplicaAddressesObs.map(allReplicaAddresses -> allReplicaAddresses.stream().filter(a -> includePrimary || !a.isPrimary())
            .map(a -> a.getPhysicalUri()).collect(Collectors.toList()));
    }

    public Mono<List<Uri>> resolveAllUriAsync(
        RxDocumentServiceRequest request,
        boolean includePrimary,
        boolean forceRefresh,
        List<Uri> allReplicas) {

        Mono<List<AddressInformation>> allReplicaAddressesObs = this.resolveAddressesAsync(request, forceRefresh);

        // if there already has address being refreshed, then stop checking scramble address rule
        Pair<Boolean, Boolean> shouldScrambleAddresses =
            request.faultInjectionRequestContext.getAddressForceRefreshed() ?
                Pair.of(false, false) : this.addressInjector.shouldScrambleAddresses(request);

        return allReplicaAddressesObs
            .map(allAddresses -> {
                if (shouldScrambleAddresses.getLeft()) {
                    return scrambleAddresses(allAddresses, shouldScrambleAddresses.getRight());
                } else {
                    return allAddresses;
                }})
            .map(allReplicaAddresses ->
                allReplicaAddresses
                    .stream()
                    .map(a -> {
                        allReplicas.add(a.getPhysicalUri());
                        return a;
                    })
                    .filter(a -> includePrimary || !a.isPrimary())
                .map(a -> a.getPhysicalUri()).collect(Collectors.toList()));
    }

    private List<AddressInformation> scrambleAddresses(
        List<AddressInformation> realAddresses,
        boolean shouldReduceReplicaCount) {
        AddressInformation[] scrambledAddresses =
            new AddressInformation[realAddresses.size()];

        int secondaryToBeMadePrimary = random.nextInt(realAddresses.size() - 2);
        int secondariesProcessed = 0;
        int nonScrambledSecondaryIndex = 2;
        for (AddressInformation a : realAddresses) {
            String addressAsString = a.getPhysicalUri().getURIAsString();
            if (a.isPrimary()) {
                scrambledAddresses[1] = new AddressInformation(
                    a.isPublic(),
                    false,
                    addressAsString.substring(0, addressAsString.length() - 2) + "s/",
                    a.getProtocol()
                );
            } else {
                if (secondariesProcessed == secondaryToBeMadePrimary) {
                    scrambledAddresses[0] = new AddressInformation(
                        a.isPublic(),
                        true,
                        addressAsString.substring(0, addressAsString.length() - 2) + "p/",
                        a.getProtocol()
                    );
                } else {
                    scrambledAddresses[nonScrambledSecondaryIndex] = a;
                    nonScrambledSecondaryIndex++;
                }

                secondariesProcessed++;
//                if (secondariesProcessed == realAddresses.size() - 2) {
//                    break;
//                }
            }
        }

        if (shouldReduceReplicaCount) {
            return Arrays
                .stream(
                    Arrays.copyOfRange(scrambledAddresses, 0, scrambledAddresses.length - 1))
                .collect(Collectors.toList());
        } else {
            return Arrays.stream(scrambledAddresses).collect(Collectors.toList());
        }
    }

    public Mono<Uri> resolvePrimaryUriAsync(RxDocumentServiceRequest request, boolean forceAddressRefresh) {
        Mono<List<AddressInformation>> replicaAddressesObs = this.resolveAddressesAsync(request, forceAddressRefresh);
        return replicaAddressesObs.flatMap(replicaAddresses -> {
            try {
                return Mono.just(AddressSelector.getPrimaryUri(request, replicaAddresses));
            } catch (Exception e) {
                return Mono.error(e);
            }
        });
    }

    public Mono<Uri> resolvePrimaryUriAsync(RxDocumentServiceRequest request, boolean forceAddressRefresh, Set<String> replicaStatuses) {
        Mono<List<AddressInformation>> replicaAddressesObs = this.resolveAddressesAsync(request, forceAddressRefresh);
        return replicaAddressesObs.flatMap(replicaAddresses -> {
            try {
                replicaAddresses.stream().filter(replica -> !replica.isPrimary()).forEach(replica ->
                    replicaStatuses.add(replica.getPhysicalUri().getHealthStatusDiagnosticString()));
                return Mono.just(AddressSelector.getPrimaryUri(request, replicaAddresses));
            } catch (Exception e) {
                return Mono.error(e);
            }
        });
    }

    public static Uri getPrimaryUri(RxDocumentServiceRequest request, List<AddressInformation> replicaAddresses) throws GoneException {
        AddressInformation primaryAddress = null;

        if (request.getDefaultReplicaIndex() != null) {
            int defaultReplicaIndex = request.getDefaultReplicaIndex();
            if (defaultReplicaIndex >= 0 && defaultReplicaIndex < replicaAddresses.size()) {
                primaryAddress = replicaAddresses.get(defaultReplicaIndex);
            }
        } else {
            primaryAddress = replicaAddresses.stream().filter(address -> address.isPrimary() && !address.getPhysicalUri().getURIAsString().contains("["))
                .findAny().orElse(null);
        }

        if (primaryAddress == null) {
            // Primary endpoint (of the desired protocol) was not found.
            throw new GoneException(String.format("The requested resource is no longer available at the server. Returned addresses are {%s}",
                                                  String.join(",", replicaAddresses.stream()
                                                      .map(address -> address.getPhysicalUri().getURIAsString()).collect(Collectors.toList()))));
        }

        return primaryAddress.getPhysicalUri();
    }

    public Mono<List<AddressInformation>> resolveAddressesAsync(RxDocumentServiceRequest request, boolean forceAddressRefresh) {
        // if there already has address being refreshed, then stop checking scramble address rule
        Pair<Boolean, Boolean> shouldScrambleAddresses =
            request.faultInjectionRequestContext.getAddressForceRefreshed() ?
                Pair.of(false, false) : this.addressInjector.shouldScrambleAddresses(request);

        Mono<List<AddressInformation>> resolvedAddressesObs =
            (this.addressResolver.resolveAsync(request, forceAddressRefresh))
                .map(addresses -> {
                    if (shouldScrambleAddresses.getLeft()) {
                        return scrambleAddresses(
                                Arrays.stream(addresses).collect(Collectors.toList()),
                                shouldScrambleAddresses.getRight())
                            .toArray(new AddressInformation[0]);
                    }

                    return addresses;
                })
                .map(addresses -> Arrays.stream(addresses)
                    .filter(address -> !Strings.isNullOrEmpty(address.getPhysicalUri().getURIAsString()) && Strings.areEqualIgnoreCase(address.getProtocolScheme(), this.protocol.scheme()))
                    .collect(Collectors.toList()));

        return resolvedAddressesObs.map(
            resolvedAddresses -> {
                List<AddressInformation> r = resolvedAddresses.stream().filter(address -> !address.isPublic()).collect(Collectors.toList());
                if (r.size() > 0) {
                    return r;
                } else {
                    return resolvedAddresses.stream().filter(AddressInformation::isPublic).collect(Collectors.toList());
                }
            }
        );
    }

    public Flux<Void> submitOpenConnectionTasksAndInitCaches(CosmosContainerProactiveInitConfig proactiveContainerInitConfig) {
        return this.addressResolver.submitOpenConnectionTasksAndInitCaches(proactiveContainerInitConfig);
    }
}
