package org.openstreetmap.josm.plugins.nl_bag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ValidationData {
    private final Map<PcHnrKey, AddressNode> pcHnrIndex = new HashMap<>();
    private final Map<PcHnrKey, Set<AddressNode>> duplicatesByPcHnr = new HashMap<>();
    private final Map<StreetHnrKey, AddressNode> streetHnrIndex = new HashMap<>();
    private final Map<StreetHnrKey, Set<AddressNode>> duplicatesByStreetHnr = new HashMap<>();

    public void clear() {
        pcHnrIndex.clear();
        duplicatesByPcHnr.clear();
        streetHnrIndex.clear();
        duplicatesByStreetHnr.clear();
    }

    public void add(AddressNode addressNode) {
        PcHnrKey pcHnrKey = new PcHnrKey(addressNode);
        if (addressNode.getAddress().getPostCode() != null) {
            AddressNode existing = pcHnrIndex.put(pcHnrKey, addressNode);
            if (existing != null && existing != addressNode) {
                Set<AddressNode> duplicates = duplicatesByPcHnr.get(pcHnrKey);
                if (duplicates == null) {
                    duplicates = new HashSet<>();
                    duplicates.add(existing);
                    duplicatesByPcHnr.put(pcHnrKey, duplicates);
                }
                duplicates.add(addressNode);
            }
        }
        StreetHnrKey streetHnrKey = new StreetHnrKey(addressNode);
        AddressNode existing = streetHnrIndex.put(streetHnrKey, addressNode);
        if (existing != null && existing != addressNode &&
                !Objects.equals(existing.getAddress().getPostCode(), pcHnrKey.getPostcode())) {
            Set<AddressNode> duplicates = duplicatesByStreetHnr.get(streetHnrKey);
            if (duplicates == null) {
                duplicates = new HashSet<>();
                duplicates.add(existing);
                duplicatesByStreetHnr.put(streetHnrKey, duplicates);
            }
            duplicates.add(addressNode);
        }

    }

    public Map<PcHnrKey, Set<AddressNode>> getDuplicatePcHnrNodes() {
        return duplicatesByPcHnr;
    }

    public Map<StreetHnrKey, Set<AddressNode>> getDuplicateStreetHnrNodes() {
        return duplicatesByStreetHnr;
    }
}
