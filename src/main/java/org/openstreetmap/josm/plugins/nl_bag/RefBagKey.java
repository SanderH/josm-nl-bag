package org.openstreetmap.josm.plugins.nl_bag;

import java.util.Objects;

public class RefBagKey {
    private final String refbag;

    public RefBagKey(String refbag) {
        super();
        this.refbag = refbag;
    }

    public RefBagKey(BagObjectOsmPrimitive bagobjectOsmPrimitive) {
        this(bagobjectOsmPrimitive.getBAGObject());
    }

    public RefBagKey(BagObject bagobject) {
        this(bagobject.getRefBAG());
    }

    public String getRefBAG() {
        return refbag;
    }

    @Override
    public int hashCode() {
        return Objects.hash(refbag);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof RefBagKey) && equals((RefBagKey) obj);
    }

    public boolean equals(RefBagKey obj) {
        return Objects.equals(obj.refbag, refbag);
    }
}
