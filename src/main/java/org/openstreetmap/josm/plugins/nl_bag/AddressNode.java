package org.openstreetmap.josm.plugins.nl_addresses;

import org.openstreetmap.josm.data.osm.Node;

public class AddressNode {
    private final Node node;
    private final Address address;

    public AddressNode(Node node) {
        this.node = node;
        this.address = new Address(node);
    }

    public Node getNode() {
        return node;
    }

    public Address getAddress() {
        return address;
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AddressNode)) {
            return false;
        }
        return node.equals(((AddressNode)obj).node);
    }
}
