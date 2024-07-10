package org.openstreetmap.josm.plugins.nl_bag;

import java.util.Objects;

public class StreetHnrKey {
    private final String street;
    private final String fullHouseNumber;

    public StreetHnrKey(String street, String fullHouseNumber) {
        super();
        this.street = street;
        this.fullHouseNumber = fullHouseNumber;
    }

    public StreetHnrKey(AddressNode addressNode) {
        this(addressNode.getAddress());
    }

    public StreetHnrKey(Address address) {
        this(address.getStreetName(), address.getFullHouseNumber());
    }

    public String getStreet() {
        return street;
    }

    public String getFullHouseNumber() {
        return fullHouseNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, fullHouseNumber);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof StreetHnrKey) && equals((StreetHnrKey) obj);
    }

    public boolean equals(StreetHnrKey obj) {
        return Objects.equals(obj.fullHouseNumber, fullHouseNumber)
                && Objects.equals(obj.street, street);
    }
}
