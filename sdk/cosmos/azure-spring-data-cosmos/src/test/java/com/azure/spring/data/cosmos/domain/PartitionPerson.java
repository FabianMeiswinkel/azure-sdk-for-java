// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.spring.data.cosmos.domain;

import com.azure.spring.data.cosmos.core.mapping.Document;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;

import java.util.List;
import java.util.Objects;

@Document()
public class PartitionPerson {

    private String id;

    private String firstName;

    @PartitionKey
    private String lastName;

    private List<String> hobbies;

    private List<Address> shippingAddresses;

    public PartitionPerson() {
    }

    public PartitionPerson(String id, String firstName, String lastName, List<String> hobbies, List<Address> shippingAddresses) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.hobbies = hobbies;
        this.shippingAddresses = shippingAddresses;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List<String> getHobbies() {
        return hobbies;
    }

    public void setHobbies(List<String> hobbies) {
        this.hobbies = hobbies;
    }

    public List<Address> getShippingAddresses() {
        return shippingAddresses;
    }

    public void setShippingAddresses(List<Address> shippingAddresses) {
        this.shippingAddresses = shippingAddresses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PartitionPerson that = (PartitionPerson) o;
        return Objects.equals(id, that.id)
            && Objects.equals(firstName, that.firstName)
            && Objects.equals(lastName, that.lastName)
            && Objects.equals(hobbies, that.hobbies)
            && Objects.equals(shippingAddresses, that.shippingAddresses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, hobbies, shippingAddresses);
    }

    @Override
    public String toString() {
        return "PartitionPerson{"
            + "id='"
            + id
            + '\''
            + ", firstName='"
            + firstName
            + '\''
            + ", lastName='"
            + lastName
            + '\''
            + ", hobbies="
            + hobbies
            + ", shippingAddresses="
            + shippingAddresses
            + '}';
    }
}
