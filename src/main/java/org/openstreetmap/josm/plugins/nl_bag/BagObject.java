package org.openstreetmap.josm.plugins.nl_bag;

import java.util.Objects;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class BagObject {
    private final String refBAG;
    private final String refBAGold;

    public BagObject(OsmPrimitive p) {
        this.refBAG = BagUtils.normalizeRefBag(p);
        this.refBAGold = BagUtils.normalizeRefBagOld(p);
    }

    public String getRefBAG() {
        return refBAG;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof BagObject)) {
            return false;
        }
        BagObject a = (BagObject) other;
        return Objects.equals(a.refBAG, refBAG) || 
        		Objects.equals(a.refBAGold, refBAG) || 
        		Objects.equals(a.refBAG, refBAGold) ||
        		Objects.equals(a.refBAGold, refBAGold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(refBAG);
    }
}
