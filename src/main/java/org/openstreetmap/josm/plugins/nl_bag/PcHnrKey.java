package org.openstreetmap.josm.plugins.nl_bag;

import java.util.Objects;

public class PcHnrKey {
    private final String postcode;
    private final String fullHouseNumber;

    public PcHnrKey(AddressNode addressNode) {
        this(addressNode.getAddress());
    }

    public PcHnrKey(Address address) {
        this(address.getPostCode(), address.getFullHouseNumber());
    }

    public PcHnrKey(String postcode, String fullHouseNumber) {
        super();
        this.postcode = postcode;
        this.fullHouseNumber = fullHouseNumber;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getFullHouseNumber() {
        return fullHouseNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(postcode, fullHouseNumber);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof PcHnrKey) &&
                equals((PcHnrKey)obj);
    }

    public boolean equals(PcHnrKey other) {
        return Objects.equals(postcode,  other.postcode) &&
                Objects.equals(fullHouseNumber,  other.fullHouseNumber);
    }
}
