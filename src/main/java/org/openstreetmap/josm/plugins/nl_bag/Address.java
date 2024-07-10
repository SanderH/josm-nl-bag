package org.openstreetmap.josm.plugins.nl_bag;

import java.util.Objects;

import org.openstreetmap.josm.data.osm.Node;

public class Address {
    private final String fullHouseNumber;
    private final Integer houseNumber;
    private final String streetName;
    private final String postCode;
    private final String city;

    public Address(Node n) {
        this.fullHouseNumber = n.get("addr:housenumber");
        this.houseNumber = parseNumber(fullHouseNumber);
        this.streetName = n.get("addr:street");
        this.postCode = normalizePostcode(n.get("addr:postcode"));
        this.city = n.get("addr:city");
    }

    public String getFullHouseNumber() {
        return fullHouseNumber;
    }

    public Integer getHouseNumber() {
        return houseNumber;
    }

    public String getStreetName() {
        return streetName;
    }

    public String getPostCode() {
        return postCode;
    }

    public String getCity() {
        return city;
    }

    public static String normalizePostcode(String pc) {
        return pc == null ? null : pc.replace(" ", "");
    }
    private static Integer parseNumber(String full) {
        if (full == null) return null;
        int i = 0;
        while (i < full.length() && Character.isDigit(full.charAt(i))) {
            i++;
        }
        if (i == 0) return null;
        return Integer.valueOf(full.substring(0, i));
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Address)) {
            return false;
        }
        Address a = (Address) other;
        return Objects.equals(a.fullHouseNumber, fullHouseNumber) &&
                Objects.equals(a.postCode, postCode) &&
                Objects.equals(a.streetName, streetName) &&
                Objects.equals(a.city, city);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullHouseNumber, postCode, streetName, city);
    }
}
