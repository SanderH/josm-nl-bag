package org.openstreetmap.josm.plugins.nl_bag;

import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.io.remotecontrol.RequestProcessor;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.nl_bag.validation.DuplicateBag;

public class NLBagPlugin extends Plugin {

    public NLBagPlugin(PluginInformation info) {
        super(info);
        OsmValidator.addTest(DuplicateBag.class);
        UploadAction.registerUploadHook(new UpdateBagTagsHook());
        RequestProcessor.addRequestHandlerClass(LoadBagHandler.command, LoadBagHandler.class);
    }
}
