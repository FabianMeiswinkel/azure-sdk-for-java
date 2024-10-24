// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.resourcemanager.network.fluent.inner;

import com.azure.core.annotation.Fluent;
import com.azure.core.annotation.JsonFlatten;
import com.azure.core.management.Resource;
import com.azure.core.management.SubResource;
import com.azure.core.util.logging.ClientLogger;
import com.azure.resourcemanager.network.models.AzureFirewallThreatIntelMode;
import com.azure.resourcemanager.network.models.ProvisioningState;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** The FirewallPolicy model. */
@JsonFlatten
@Fluent
public class FirewallPolicyInner extends Resource {
    @JsonIgnore private final ClientLogger logger = new ClientLogger(FirewallPolicyInner.class);

    /*
     * A unique read-only string that changes whenever the resource is updated.
     */
    @JsonProperty(value = "etag", access = JsonProperty.Access.WRITE_ONLY)
    private String etag;

    /*
     * List of references to FirewallPolicyRuleGroups.
     */
    @JsonProperty(value = "properties.ruleGroups", access = JsonProperty.Access.WRITE_ONLY)
    private List<SubResource> ruleGroups;

    /*
     * The provisioning state of the firewall policy resource.
     */
    @JsonProperty(value = "properties.provisioningState", access = JsonProperty.Access.WRITE_ONLY)
    private ProvisioningState provisioningState;

    /*
     * The parent firewall policy from which rules are inherited.
     */
    @JsonProperty(value = "properties.basePolicy")
    private SubResource basePolicy;

    /*
     * List of references to Azure Firewalls that this Firewall Policy is
     * associated with.
     */
    @JsonProperty(value = "properties.firewalls", access = JsonProperty.Access.WRITE_ONLY)
    private List<SubResource> firewalls;

    /*
     * List of references to Child Firewall Policies.
     */
    @JsonProperty(value = "properties.childPolicies", access = JsonProperty.Access.WRITE_ONLY)
    private List<SubResource> childPolicies;

    /*
     * The operation mode for Threat Intelligence.
     */
    @JsonProperty(value = "properties.threatIntelMode")
    private AzureFirewallThreatIntelMode threatIntelMode;

    /*
     * Resource ID.
     */
    @JsonProperty(value = "id")
    private String id;

    /**
     * Get the etag property: A unique read-only string that changes whenever the resource is updated.
     *
     * @return the etag value.
     */
    public String etag() {
        return this.etag;
    }

    /**
     * Get the ruleGroups property: List of references to FirewallPolicyRuleGroups.
     *
     * @return the ruleGroups value.
     */
    public List<SubResource> ruleGroups() {
        return this.ruleGroups;
    }

    /**
     * Get the provisioningState property: The provisioning state of the firewall policy resource.
     *
     * @return the provisioningState value.
     */
    public ProvisioningState provisioningState() {
        return this.provisioningState;
    }

    /**
     * Get the basePolicy property: The parent firewall policy from which rules are inherited.
     *
     * @return the basePolicy value.
     */
    public SubResource basePolicy() {
        return this.basePolicy;
    }

    /**
     * Set the basePolicy property: The parent firewall policy from which rules are inherited.
     *
     * @param basePolicy the basePolicy value to set.
     * @return the FirewallPolicyInner object itself.
     */
    public FirewallPolicyInner withBasePolicy(SubResource basePolicy) {
        this.basePolicy = basePolicy;
        return this;
    }

    /**
     * Get the firewalls property: List of references to Azure Firewalls that this Firewall Policy is associated with.
     *
     * @return the firewalls value.
     */
    public List<SubResource> firewalls() {
        return this.firewalls;
    }

    /**
     * Get the childPolicies property: List of references to Child Firewall Policies.
     *
     * @return the childPolicies value.
     */
    public List<SubResource> childPolicies() {
        return this.childPolicies;
    }

    /**
     * Get the threatIntelMode property: The operation mode for Threat Intelligence.
     *
     * @return the threatIntelMode value.
     */
    public AzureFirewallThreatIntelMode threatIntelMode() {
        return this.threatIntelMode;
    }

    /**
     * Set the threatIntelMode property: The operation mode for Threat Intelligence.
     *
     * @param threatIntelMode the threatIntelMode value to set.
     * @return the FirewallPolicyInner object itself.
     */
    public FirewallPolicyInner withThreatIntelMode(AzureFirewallThreatIntelMode threatIntelMode) {
        this.threatIntelMode = threatIntelMode;
        return this;
    }

    /**
     * Get the id property: Resource ID.
     *
     * @return the id value.
     */
    public String id() {
        return this.id;
    }

    /**
     * Set the id property: Resource ID.
     *
     * @param id the id value to set.
     * @return the FirewallPolicyInner object itself.
     */
    public FirewallPolicyInner withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Validates the instance.
     *
     * @throws IllegalArgumentException thrown if the instance is not valid.
     */
    public void validate() {
    }
}
