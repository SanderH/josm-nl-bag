package org.openstreetmap.josm.plugins.nl_bag;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class BagObjectOsmPrimitive {
    private final OsmPrimitive osmprimitive;
    private final BagObject bagobject;

    public BagObjectOsmPrimitive(OsmPrimitive osmprimitive) {
        this.osmprimitive = osmprimitive;
        this.bagobject = new BagObject(osmprimitive);
    }

    public OsmPrimitive getOsmPrimitive() {
        return osmprimitive;
    }

    public BagObject getBAGObject() {
        return bagobject;
    }

    @Override
    public int hashCode() {
        return osmprimitive.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BagObjectOsmPrimitive)) {
            return false;
        }
        return osmprimitive.equals(((BagObjectOsmPrimitive)obj).osmprimitive);
    }
}
