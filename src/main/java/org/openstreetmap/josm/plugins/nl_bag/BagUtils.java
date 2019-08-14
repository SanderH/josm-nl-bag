package org.openstreetmap.josm.plugins.nl_bag;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class BagUtils {
    public final static String REF_BAG = "ref:bag";
    public final static String REF_BAG_OLD = "ref:bag:old";

    public static boolean isTaggedAsBagObject(OsmPrimitive osm) {
        return osm.hasKey(REF_BAG);
    }

    public static String normalizeRefBag(OsmPrimitive osm) {
    	return normalizeRefBag(osm.get(REF_BAG));
    }
    
    public static String normalizeRefBagOld(OsmPrimitive osm) {
    	return normalizeRefBag(osm.get(REF_BAG_OLD));
    }
    
    public static String normalizeRefBag(String rb) {
        return rb == null ? null : StringUtils.leftPad(rb, 16, "0");
    }

}
