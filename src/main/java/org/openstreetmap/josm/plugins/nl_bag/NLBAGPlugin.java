package org.openstreetmap.josm.plugins.nl_bag;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.remotecontrol.RequestProcessor;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.nl_bag.validation.DuplicateBag;
import org.openstreetmap.josm.tools.I18n;

public class NLBagPlugin extends Plugin {

    private final static String INFO_URL = "https://bag.tools4osm.nl/plugins/versions.json";
    private JsonObject metaInfo;
    private boolean isDebug = false;

    public NLBagPlugin(PluginInformation info) {
        super(info);
        OsmValidator.addTest(DuplicateBag.class);
        UploadAction.registerUploadHook(new UpdateBagTagsHook());
        RequestProcessor.addRequestHandlerClass(LoadBagHandler.command, LoadBagHandler.class);

        readInfo();
        checkVersion(info);
    }
    
    public void checkVersion(PluginInformation info) {
        if (metaInfo == null) return;
        String latestVersion = metaInfo.getJsonObject("version").getString("latest");
        String nextVersion = metaInfo.getJsonObject("version").getString("next");
        if (!info.version.equals(latestVersion) && !info.version.equals(nextVersion)) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), I18n.tr("Your NL-BAG version ({0}) is out of date.\n" +
                    "Please upgrade to the latest version: {1}", info.version, latestVersion), "Plug-in out of date", JOptionPane.WARNING_MESSAGE);
        }
        if (info.version.equals(nextVersion)) {
            isDebug = true;
        }
    }
    
    private void readInfo() {
        URL url;
        try {
            url = new URL(INFO_URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        try (
            InputStream is = url.openStream();
            JsonReader reader = Json.createReader(is);
            )  {
                metaInfo = reader.readObject().getJsonObject("nl-bag");
                if (metaInfo == null) {
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(), I18n.tr("No version information is available at the moment.\n" +
                            "Your NL-BAG version may be out of date"), "No version info", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), I18n.tr("No version information is available at the moment.\n" +
                    "Your NL-BAG version may be out of date"), "No version info", JOptionPane.WARNING_MESSAGE);

        }
    }

}
