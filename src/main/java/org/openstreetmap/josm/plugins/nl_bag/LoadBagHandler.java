package org.openstreetmap.josm.plugins.nl_bag;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.AutoScaleAction.AutoScaleMode;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler;
import org.openstreetmap.josm.plugins.ods.OdsModule;
import org.openstreetmap.josm.plugins.ods.OpenDataServicesPlugin;
import org.openstreetmap.josm.plugins.ods.bag.BagImportModule;
import org.openstreetmap.josm.plugins.ods.io.DownloadRequest;
import org.openstreetmap.josm.plugins.ods.io.MainDownloader;
import org.openstreetmap.josm.plugins.ods.jts.Boundary;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * Handler to load data directly from the URL.
 * @since 7636
 */
public class LoadBagHandler extends RequestHandler {

    private MainDownloader downloader;
    private LocalDateTime startDate;
    private Boundary boundary;
    private boolean downloadOsm;
    private boolean downloadOpenData;
    private OdsModule module;

    /**
     * The remote control command name used to import BAG.
     */
    public static final String command = "load_bag";

    // Mandatory arguments
    private double minlat;
    private double maxlat;
    private double minlon;
    private double maxlon;

    // Optional argument 'select'
    private final Set<SimplePrimitiveId> toSelect = new HashSet<>();

    @Override
    public String getPermissionMessage() {
        String msg = tr("Remote Control has been asked to load data from the API.") +
                "<br>" + tr("Bounding box: ") + new BBox(minlon, minlat, maxlon, maxlat).toStringCSV(", ");
        if (args.containsKey("select") && !toSelect.isEmpty()) {
            msg += "<br>" + tr("Selection: {0}", toSelect.size());
        }
        return msg;
    }

    @Override
    public String[] getMandatoryParams() {
        return new String[] {"bottom", "top", "left", "right"};
    }

    @Override
    public String[] getOptionalParams() {
        return new String[] {};
    }

    @Override
    public String getUsage() {
        return "download a bounding box from the OSM and BAG API, zoom to the downloaded area";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[] {"/load_and_zoom?left=5.4980045&top=52.3603626&right=5.5000045&bottom=52.3583626"};
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException {
        try {
        	
        	this.module = OpenDataServicesPlugin.INSTANCE.getActiveModule();
        	if ((module == null) || (!module.getClass().equals(BagImportModule.class))) 
        	{
                throw new RequestHandlerErrorException(new Throwable("BAG plugin is not active"));
        	}
        	
            DownloadParams settings = getDownloadParams();

            if (command.equals(myCommand)) {
                if (!PermissionPrefWithDefault.LOAD_DATA.isAllowed()) {
                    Logging.info("RemoteControl: download forbidden by preferences");
                } else {
                    Area toDownload = null;
                    if (!settings.isNewLayer()) {
                        // find out whether some data has already been downloaded
                        Area present = null;
                        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
                        if (ds != null) {
                            present = ds.getDataSourceArea();
                        }
                        if (present != null && !present.isEmpty()) {
                            toDownload = new Area(new Rectangle2D.Double(minlon, minlat, maxlon-minlon, maxlat-minlat));
                            toDownload.subtract(present);
                            if (!toDownload.isEmpty()) {
                                // the result might not be a rectangle (L shaped etc)
                                Rectangle2D downloadBounds = toDownload.getBounds2D();
                                minlat = downloadBounds.getMinY();
                                minlon = downloadBounds.getMinX();
                                maxlat = downloadBounds.getMaxY();
                                maxlon = downloadBounds.getMaxX();
                            }
                        }
                    }
                    //if (toDownload != null && toDownload.isEmpty()) {
                    //    Logging.info("RemoteControl: no download necessary");
                    //} else {
						final Bounds bbox = new Bounds(minlat, minlon, maxlat, maxlon);
						// after downloading, zoom to downloaded area.
						zoom(Collections.<OsmPrimitive>emptySet(), bbox);

						//MainApplication.worker.submit(new PostDownloadHandler(osmTask, future));
                        //BagDownloader downloader = new BagDownloader(module);
                        //Boundary bounds = new Boundary(new Bounds(minlat, minlon, maxlat, maxlon));
                        //DownloadRequest req = new DownloadRequest(LocalDateTime.now(), bounds, true, true);
                        //ProgressMonitor progress = new ProgressMonitor();
                        //downloader.run(progress, req);
					    this.downloader = module.getDownloader();
					    startDate = LocalDateTime.now();
					    boundary = new Boundary(new Bounds(minlat, minlon, maxlat, maxlon));
					    downloadOsm = true;
					    downloadOpenData = true;
						
                        DownloadTask task = new DownloadTask();
                        MainApplication.worker.submit(task);
                    //}
                }
            }
        } catch (RuntimeException ex) { // NOPMD
            Logging.warn("RemoteControl: Error parsing load_bag remote control request:");
            Logging.error(ex);
            throw new RequestHandlerErrorException(ex);
        }

    }

    protected void zoom(Collection<OsmPrimitive> primitives, final Bounds bbox) {
        if (!PermissionPrefWithDefault.CHANGE_VIEWPORT.isAllowed()) {
            return;
        }
        // zoom_mode=(download|selection), defaults to selection
        if (!"download".equals(args.get("zoom_mode")) && !primitives.isEmpty()) {
            AutoScaleAction.autoScale(AutoScaleMode.SELECTION);
        } else if (MainApplication.isDisplayingMapView()) {
            // make sure this isn't called unless there *is* a MapView
            GuiHelper.executeByMainWorkerInEDT(() -> {
                BoundingXYVisitor bbox1 = new BoundingXYVisitor();
                bbox1.visit(bbox);
                MainApplication.getMap().mapView.zoomTo(bbox1);
            });
        }
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return null;
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        validateDownloadParams();
        // Process mandatory arguments
        minlat = 0;
        maxlat = 0;
        minlon = 0;
        maxlon = 0;
        try {
            minlat = LatLon.roundToOsmPrecision(Double.parseDouble(args != null ? args.get("bottom") : ""));
            maxlat = LatLon.roundToOsmPrecision(Double.parseDouble(args != null ? args.get("top") : ""));
            minlon = LatLon.roundToOsmPrecision(Double.parseDouble(args != null ? args.get("left") : ""));
            maxlon = LatLon.roundToOsmPrecision(Double.parseDouble(args != null ? args.get("right") : ""));
        } catch (NumberFormatException e) {
            throw new RequestHandlerBadRequestException("NumberFormatException ("+e.getMessage()+')', e);
        }

        // Current API 0.6 check: "The latitudes must be between -90 and 90"
        if (!LatLon.isValidLat(minlat) || !LatLon.isValidLat(maxlat)) {
            throw new RequestHandlerBadRequestException(tr("The latitudes must be between {0} and {1}", -90d, 90d));
        }
        // Current API 0.6 check: "longitudes between -180 and 180"
        if (!LatLon.isValidLon(minlon) || !LatLon.isValidLon(maxlon)) {
            throw new RequestHandlerBadRequestException(tr("The longitudes must be between {0} and {1}", -180d, 180d));
        }
        // Current API 0.6 check: "the minima must be less than the maxima"
        if (minlat > maxlat || minlon > maxlon) {
            throw new RequestHandlerBadRequestException(tr("The minima must be less than the maxima"));
        }
    }
    
    private class DownloadTask extends PleaseWaitRunnable {

        public DownloadTask() {
            super(tr("Downloading data"));
        }

        @SuppressWarnings("synthetic-access")
        @Override
        protected void cancel() {
            downloader.cancel();
        }

        @SuppressWarnings("synthetic-access")
        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            DownloadRequest request = new DownloadRequest(startDate, boundary, downloadOsm, downloadOpenData);
            downloader.run(getProgressMonitor(), request);
        }

        @SuppressWarnings("synthetic-access")
        @Override
        protected void finish() {
            if (downloadOpenData) {
                MainApplication.getLayerManager().setActiveLayer(module.getOpenDataLayerManager().getOsmDataLayer());
            }
            else {
                MainApplication.getLayerManager().setActiveLayer(module.getOsmLayerManager().getOsmDataLayer());
            }
        }
    }

}
