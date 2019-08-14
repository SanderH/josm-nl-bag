package org.openstreetmap.josm.plugins.nl_bag.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.TestError.Builder;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.plugins.nl_bag.BagObjectOsmPrimitive;
import org.openstreetmap.josm.plugins.nl_bag.BagUtils;
import org.openstreetmap.josm.plugins.nl_bag.RefBagKey;
import org.openstreetmap.josm.plugins.nl_bag.BagValidationData;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.*;

public class DuplicateBag extends Test {
    public static final int DUPLICATE_BAG = 13702;

    private final BagValidationData data;

    public DuplicateBag() {
        super(tr("Duplicate BAG objects"), tr("Checks for duplicate BAG objects."));
        data = new BagValidationData();
    }

    public BagValidationData getValidationData() {
        return data;
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        data.clear();
    }

    @Override
    public void visit(Node n) {
        if (BagUtils.isTaggedAsBagObject(n)) {
        	BagObjectOsmPrimitive bagObjectNode = new BagObjectOsmPrimitive(n);
            data.add(bagObjectNode);
        }
    }
    
    public void visit(Way w) {
        if (BagUtils.isTaggedAsBagObject(w)) {
        	BagObjectOsmPrimitive bagObjectWay = new BagObjectOsmPrimitive(w);
            data.add(bagObjectWay);
        }
    }

    public void visit(Relation r) {
        if (BagUtils.isTaggedAsBagObject(r)) {
        	BagObjectOsmPrimitive bagObjectRelation = new BagObjectOsmPrimitive(r);
            data.add(bagObjectRelation);
        }
    }

    @Override
    public void endTest() {
        for (Entry<RefBagKey, Set<BagObjectOsmPrimitive>> entry :
            data.getDuplicateRefBagOsmPrimitives().entrySet()) {
            errors.addAll(buildTestErrorsRefBag(this, entry));
        }
        super.endTest();
        data.clear();
    }

    private static Collection<? extends TestError> buildTestErrorsRefBag(
            DuplicateBag tester, Entry<RefBagKey, Set<BagObjectOsmPrimitive>> entry) {
        List<OsmPrimitive> osmprimitives = new ArrayList<>(entry.getValue().size());
        RefBagKey key = entry.getKey();
        for (BagObjectOsmPrimitive n : entry.getValue()) {
        	osmprimitives.add(n.getOsmPrimitive());
        }

        Builder builder = TestError
                .builder(tester, Severity.ERROR, DUPLICATE_BAG)
                .message("Duplicate BAG object",
                        I18n.tr("Duplicate BAG object for {0}",
                                key.getRefBAG()))
                .primitives(osmprimitives)
                .fix(new DuplicateBAGObjectFixer(osmprimitives));
        return Collections.singletonList(builder.build());
    }

    static class DuplicateBAGObjectFixer implements Supplier<Command> {
        private final Collection<OsmPrimitive> osmprimitives;

        public DuplicateBAGObjectFixer(Collection<OsmPrimitive> osmprimitives) {
            this.osmprimitives = osmprimitives;
        }

        @Override
        public Command get() {
            if (osmprimitives.size() != 2)
                return null;
            Iterator<? extends OsmPrimitive> it = osmprimitives.iterator();
            OsmPrimitive n1 = it.next();
            OsmPrimitive n2 = it.next();
            if (n1.isDeleted() || n1.isDeleted())
                return null;
            
            OsmPrimitive originalPrimitive;
            OsmPrimitive newPrimitive;
            
            // determine original and new primitive
            if (n1.isNew() && !n2.isNew())
            {
            	originalPrimitive = n2;
            	newPrimitive = n1;
            }
            else if (!n1.isNew() && n2.isNew())
            {
            	originalPrimitive = n1;
            	newPrimitive = n2;
            }
            else
            {
            	// both new or original, don't fix
            	return null;
            }
            
        	// phase 1
        	// update BAG-related fields
            // set source date to newest value
            try
            {
            	SequenceCommand updateTagsCommand = getUpdatedBAGObjectCommands(originalPrimitive, newPrimitive);
            	
                if (updateTagsCommand != null)
                {                
	        		Logging.trace("BAGObject fixer: Update Tags");
	                return updateTagsCommand;
                }
            } catch (Exception ex)
            {
                new Notification(
                        ex.getMessage()
                        ).setIcon(JOptionPane.WARNING_MESSAGE).show();
            }            
            
        	// phase 2
        	// try to merge old and new objects
        	try {
                ReplaceGeometryCommand replaceCommand = ReplaceGeometryUtils.buildReplaceWithNewCommand(n1, n2);

                // action was canceled
                if (replaceCommand == null)
                    return null;

        		Logging.trace("BAGObject fixer: Replace geometry");
                return replaceCommand;
            } catch (IllegalArgumentException ex) {
                new Notification(
                        ex.getMessage()
                        ).setIcon(JOptionPane.WARNING_MESSAGE).show();
            } catch (ReplaceGeometryException ex) {
                new Notification(
                        ex.getMessage()
                        ).setIcon(JOptionPane.WARNING_MESSAGE).show();
            }

            return null;
        }
        
       protected static SequenceCommand getUpdatedBAGObjectCommands(OsmPrimitive originalPrimitive, OsmPrimitive newPrimitive) 
        {
        	List<Command> commands = new ArrayList<>();
        	
        	// update ref:bag
        	if (BagUtils.normalizeRefBag(originalPrimitive).equals(BagUtils.normalizeRefBag(newPrimitive)) && 
        			!originalPrimitive.get(BagUtils.REF_BAG).equals(newPrimitive.get(BagUtils.REF_BAG)))
        	{
        		// check both original and new
        		if (originalPrimitive.get(BagUtils.REF_BAG).length() < 16)
        		{
        			commands.add(new ChangePropertyCommand(originalPrimitive, BagUtils.REF_BAG, BagUtils.normalizeRefBag(originalPrimitive)));        		
    			}
        		if (newPrimitive.get(BagUtils.REF_BAG).length() < 16)
        		{
        			commands.add(new ChangePropertyCommand(newPrimitive, BagUtils.REF_BAG, BagUtils.normalizeRefBag(newPrimitive)));        		
    			}
        	}
        	
        	// fix source:date
            if (hasSourceDate(originalPrimitive) && hasSourceDate(newPrimitive))
            {
            	Date d1 = getSourceDate(originalPrimitive);
            	Date d2 = getSourceDate(newPrimitive);
            	
            	if (d2.before(d1))
            	{
            		Logging.trace("BAGObject fixer: Update source date, will need 2nd pass");
                	commands.add(new ChangePropertyCommand(newPrimitive, SOURCE_DATE, originalPrimitive.get(SOURCE_DATE)));
            	}
            	else if (d1.before(d2))
            	{
            		Logging.trace("BAGObject fixer: Update source date, will need 2nd pass");
            		commands.add(new ChangePropertyCommand(originalPrimitive, SOURCE_DATE, newPrimitive.get(SOURCE_DATE)));
            	}
            	else
            	{
            		// identical dates, no need to update
            	}
            }
            
            // cleanup construction tag if needed
        	String buildingValueTag = BUILDING;
            if (isConstruction(originalPrimitive) && !isConstruction(newPrimitive))
            {
            	// remove tag
            	commands.add(new ChangePropertyCommand(originalPrimitive, CONSTRUCTION, null));
            	
            	// set property source for next test
        		buildingValueTag = CONSTRUCTION;
            }
            
            // fix (retain/update) building tag
            if (isBuilding(originalPrimitive) && isBuilding(newPrimitive))
            {
        		if (!originalPrimitive.get(buildingValueTag).equals(newPrimitive.get(BUILDING)))
        		{
	            	switch (originalPrimitive.get(buildingValueTag))
	            	{
		            	case "greenhouse":
		            	case "shed":
		            	case "barn":
		            	case "stable":
		            	case "garage":
		            	case "garages":
		            	case "farm":
		            	case "farm_auxiliary":
		            	case "church":
		            	case "chapel":
		            	case "mosque":
		            	case "warehouse":
		            	case "university":
		            	case "college":
		            	case "school":
		            	case "storage_tank":
		            	case "hospital":
		            	case "hotel":
		            	case "hangar":
		            		// this custom value is probably better than BAG, so retain value
		            		if (buildingValueTag.equals(CONSTRUCTION))
		            		{
		            			// promote construction value to building
		            			commands.add(new ChangePropertyCommand(originalPrimitive, BUILDING, originalPrimitive.get(CONSTRUCTION)));
		            			commands.add(new ChangePropertyCommand(newPrimitive, BUILDING, originalPrimitive.get(CONSTRUCTION)));
		            		}
		            		else
		            		{
		            			// use existing value
		            			commands.add(new ChangePropertyCommand(newPrimitive, BUILDING, originalPrimitive.get(BUILDING)));
		            		}
		            		break;
		            	default:
		            		// use new value
	            			commands.add(new ChangePropertyCommand(originalPrimitive, BUILDING, newPrimitive.get(BUILDING)));
		            		break;
	            	}
        		}
            }
            
            // fix start_date tag
            if (hasStartDate(originalPrimitive) && hasStartDate(newPrimitive))
            {
            	if (!originalPrimitive.get(START_DATE).equals(newPrimitive.get(START_DATE)))
            	{
            		commands.add(new ChangePropertyCommand(originalPrimitive, START_DATE, newPrimitive.get(START_DATE)));
            	}
            }
            
            // fix source tag
            if (hasSource(originalPrimitive) && hasSource(newPrimitive))
            {
            	if (!originalPrimitive.get(SOURCE).equals(newPrimitive.get(SOURCE)))
            	{
            		commands.add(new ChangePropertyCommand(originalPrimitive, SOURCE, newPrimitive.get(SOURCE)));
            	}
            }
            
            if (!commands.isEmpty())
            {
            	return new SequenceCommand(tr("Updating BAG Object tags"), commands);
            }
            else
            	return null;
        }
        
        final static String SOURCE_DATE = "source:date";
        public static boolean hasSourceDate(OsmPrimitive osm) {
       		return osm.hasKey(SOURCE_DATE) && isDateValid(osm.get(SOURCE_DATE));
        }
        
        final static String DATE_FORMAT = "yyyy-MM-dd";
        public static boolean isDateValid(String date) 
        {
            try {
                DateFormat df = new SimpleDateFormat(DATE_FORMAT);
                df.setLenient(false);
                df.parse(date);
                return true;
            } catch (ParseException e) {
                return false;
            }
        }
        
        public static Date getSourceDate(OsmPrimitive osm)
        {
        	return getDate(osm.get(SOURCE_DATE));
        }
        
        public static Date getDate(String date) 
        {
            try {
                DateFormat df = new SimpleDateFormat(DATE_FORMAT);
                df.setLenient(false);
                return df.parse(date);
            } catch (ParseException e) {
                return null;
            }
        }
        
        final static String CONSTRUCTION = "construction";
        public static boolean isConstruction(OsmPrimitive osm) {
        	return osm.hasKey(CONSTRUCTION);
        }
        
        final static String BUILDING = "building";
        public static boolean isBuilding(OsmPrimitive osm) {
        	return osm.hasKey(BUILDING);
        }
        
        final static String START_DATE = "start_date";
        public static boolean hasStartDate(OsmPrimitive osm) {
        	return osm.hasKey(START_DATE);
        }
        
        
        final static String SOURCE = "source";
        public static boolean hasSource(OsmPrimitive osm) {
        	return osm.hasKey(SOURCE);
        }

    }
}
