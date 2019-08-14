package org.openstreetmap.josm.plugins.nl_bag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BagValidationData {
    private final Map<RefBagKey, BagObjectOsmPrimitive> refBagIndex = new HashMap<>();
    private final Map<RefBagKey, Set<BagObjectOsmPrimitive>> duplicatesByRefBag = new HashMap<>();

    public void clear() {
    	refBagIndex.clear();
    	duplicatesByRefBag.clear();
    }

    public void add(BagObjectOsmPrimitive bagobjectOsmPrimitive) {
        RefBagKey refBagKey = new RefBagKey(bagobjectOsmPrimitive);
        BagObjectOsmPrimitive existing = refBagIndex.put(refBagKey, bagobjectOsmPrimitive);
        if (existing != null && existing != bagobjectOsmPrimitive) {
            Set<BagObjectOsmPrimitive> duplicates = duplicatesByRefBag.get(refBagKey);
            if (duplicates == null) {
                duplicates = new HashSet<>();
                duplicates.add(existing);
                duplicatesByRefBag.put(refBagKey, duplicates);
            }
            duplicates.add(bagobjectOsmPrimitive);
        }
    }

    public Map<RefBagKey, Set<BagObjectOsmPrimitive>> getDuplicateRefBagOsmPrimitives() {
        return duplicatesByRefBag;
    }
}
