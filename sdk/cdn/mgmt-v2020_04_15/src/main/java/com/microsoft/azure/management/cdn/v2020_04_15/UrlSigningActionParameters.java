/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.cdn.v2020_04_15;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the parameters for the Url Signing action.
 */
public class UrlSigningActionParameters {
    /**
     * Id reference of the key to be used to verify the hash and should be
     * defined in UrlSigningKeys.
     */
    @JsonProperty(value = "keyId", required = true)
    private String keyId;

    /**
     * Algorithm to use for URL signing. Possible values include: 'SHA256'.
     */
    @JsonProperty(value = "algorithm")
    private String algorithm;

    /**
     * Defines which query string parameters in the url to be considered for
     * expires, key id etc.
     */
    @JsonProperty(value = "parameterNameOverride")
    private List<UrlSigningParamIdentifier> parameterNameOverride;

    /**
     * Match values to match against. Supports CIDR ranges (both IPv4 and
     * IPv6).
     */
    @JsonProperty(value = "ipSubnets")
    private List<String> ipSubnets;

    /**
     * Get id reference of the key to be used to verify the hash and should be defined in UrlSigningKeys.
     *
     * @return the keyId value
     */
    public String keyId() {
        return this.keyId;
    }

    /**
     * Set id reference of the key to be used to verify the hash and should be defined in UrlSigningKeys.
     *
     * @param keyId the keyId value to set
     * @return the UrlSigningActionParameters object itself.
     */
    public UrlSigningActionParameters withKeyId(String keyId) {
        this.keyId = keyId;
        return this;
    }

    /**
     * Get algorithm to use for URL signing. Possible values include: 'SHA256'.
     *
     * @return the algorithm value
     */
    public String algorithm() {
        return this.algorithm;
    }

    /**
     * Set algorithm to use for URL signing. Possible values include: 'SHA256'.
     *
     * @param algorithm the algorithm value to set
     * @return the UrlSigningActionParameters object itself.
     */
    public UrlSigningActionParameters withAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    /**
     * Get defines which query string parameters in the url to be considered for expires, key id etc.
     *
     * @return the parameterNameOverride value
     */
    public List<UrlSigningParamIdentifier> parameterNameOverride() {
        return this.parameterNameOverride;
    }

    /**
     * Set defines which query string parameters in the url to be considered for expires, key id etc.
     *
     * @param parameterNameOverride the parameterNameOverride value to set
     * @return the UrlSigningActionParameters object itself.
     */
    public UrlSigningActionParameters withParameterNameOverride(List<UrlSigningParamIdentifier> parameterNameOverride) {
        this.parameterNameOverride = parameterNameOverride;
        return this;
    }

    /**
     * Get match values to match against. Supports CIDR ranges (both IPv4 and IPv6).
     *
     * @return the ipSubnets value
     */
    public List<String> ipSubnets() {
        return this.ipSubnets;
    }

    /**
     * Set match values to match against. Supports CIDR ranges (both IPv4 and IPv6).
     *
     * @param ipSubnets the ipSubnets value to set
     * @return the UrlSigningActionParameters object itself.
     */
    public UrlSigningActionParameters withIpSubnets(List<String> ipSubnets) {
        this.ipSubnets = ipSubnets;
        return this;
    }

}
