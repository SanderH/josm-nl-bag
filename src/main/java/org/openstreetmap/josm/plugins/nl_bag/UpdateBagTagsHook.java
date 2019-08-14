package org.openstreetmap.josm.plugins.nl_bag;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.actions.upload.UploadHook;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Update BAG Reference tags for all modified objects before upload.
 */
class UpdateBagTagsHook implements UploadHook {

    @Override
    public final boolean checkUpload(APIDataSet apiDataSet) {
        List<OsmPrimitive> objectsToUpload = apiDataSet.getPrimitives();
        
        // Find old-style BAG references and add leading "0" if updating, leave untouched otherwise to prevent unneeded changing primitives
        List<Command> commands = new ArrayList<>();
        for (OsmPrimitive osm : objectsToUpload) {
        	if (BagUtils.isTaggedAsBagObject(osm))
        	{
        		if (osm.get(BagUtils.REF_BAG).length() < 16)
        		{
        			commands.add(new ChangePropertyCommand(osm, BagUtils.REF_BAG, BagUtils.normalizeRefBag(osm)));        		
    			}
        	}
        }

        if (!commands.isEmpty())
        {
            UndoRedoHandler.getInstance().add(new SequenceCommand(tr("Updating BAG Reference tag"), commands));
        }
        return true;
    }
}