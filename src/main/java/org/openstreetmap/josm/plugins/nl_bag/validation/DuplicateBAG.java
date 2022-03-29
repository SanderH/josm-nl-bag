package org.openstreetmap.josm.plugins.nl_bag.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

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
        Boolean hideFixer = false;
        for (BagObjectOsmPrimitive n : entry.getValue()) {
        	hideFixer = hideFixer || BagUtils.isStaticCaravan(n.getOsmPrimitive()) || BagUtils.hasNoteBag(n.getOsmPrimitive());
        	osmprimitives.add(n.getOsmPrimitive());
        }

        if (hideFixer)
        {
            Builder builder = TestError
                    .builder(tester, Severity.ERROR, DUPLICATE_BAG)
                    .message("Duplicate BAG object",
                            I18n.tr("Duplicate BAG object for {0}",
                                    key.getRefBAG()))
                    .primitives(osmprimitives);
            return Collections.singletonList(builder.build());
        }
        else {
            Builder builder = TestError
                    .builder(tester, Severity.ERROR, DUPLICATE_BAG)
                    .message("Duplicate BAG object",
                            I18n.tr("Duplicate BAG object for {0}",
                                    key.getRefBAG()))
                    .primitives(osmprimitives)
                    .fix(new DuplicateBAGObjectFixer(osmprimitives));
            return Collections.singletonList(builder.build());
        }
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
            
    		if (BagUtils.isStaticCaravan(originalPrimitive) || 
    			BagUtils.isStaticCaravan(newPrimitive) || 
    			BagUtils.hasNoteBag(originalPrimitive))
    		{
    			// not going to touch objects with static_caravan and note:bag
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
                ReplaceGeometryCommand replaceCommand = ReplaceGeometryUtils.buildReplaceWithNewCommand(originalPrimitive, newPrimitive);

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
            if (BagUtils.hasSourceDate(originalPrimitive) && BagUtils.hasSourceDate(newPrimitive))
            {
            	Date d1 = BagUtils.getSourceDate(originalPrimitive);
            	Date d2 = BagUtils.getSourceDate(newPrimitive);
            	
            	if (d2.before(d1))
            	{
            		Logging.trace("BAGObject fixer: Update source date, will need 2nd pass");
                	commands.add(new ChangePropertyCommand(newPrimitive, BagUtils.SOURCE_DATE, originalPrimitive.get(BagUtils.SOURCE_DATE)));
            	}
            	else if (d1.before(d2))
            	{
            		Logging.trace("BAGObject fixer: Update source date, will need 2nd pass");
            		commands.add(new ChangePropertyCommand(originalPrimitive, BagUtils.SOURCE_DATE, newPrimitive.get(BagUtils.SOURCE_DATE)));
            	}
            	else
            	{
            		// identical dates, no need to update
            	}
            }
            
            // cleanup construction tag if needed
        	String buildingValueTag = BagUtils.BUILDING;
            if (BagUtils.isConstruction(originalPrimitive) && !BagUtils.isConstruction(newPrimitive))
            {
            	// remove tag
            	commands.add(new ChangePropertyCommand(originalPrimitive, BagUtils.CONSTRUCTION, null));
            	
            	// set property source for next test
        		buildingValueTag = BagUtils.CONSTRUCTION;
            }
            
            // fix (retain/update) building tag
            if (isBuilding(originalPrimitive) && isBuilding(newPrimitive))
            {
        		if (!originalPrimitive.get(buildingValueTag).equals(newPrimitive.get(BagUtils.BUILDING)))
        		{
        			if (newPrimitive.get(BagUtils.BUILDING).equals(BagUtils.YES))
        			{
        				// always use existing value if new value is 'yes'
            			commands.add(new ChangePropertyCommand(newPrimitive, BagUtils.BUILDING, originalPrimitive.get(BagUtils.BUILDING)));
        			}
        			else 
        			{
		            	switch (originalPrimitive.get(buildingValueTag))
		            	{
			            	case "barn":
			            	case "bungalow":
			            	case "bunker":
			            	case "castle":
			            	case "cathedral":
			            	case "chapel":
			            	case "church":
			            	case "civic":
			            	case "college":
			            	case "dormitory":
			            	case "farm":
			            	case "farm_auxiliary":
			            	case "fire_station":
			            	case "garage":
			            	case "garages":
			            	case "government":
			            	case "greenhouse":
			            	case "hangar":
			            	case "hospital":
			            	case "hotel":
			            	case "hut":
			            	case "kindergarten":
			            	case "monastery":
			            	case "mosque":
			            	case "prison":
			            	case "school":
			            	case "service":
			            	case "shed":
			            	case "stable":
			            	case "stadium":
			            	case "storage_tank":
			            	case "supermarket":
			            	case "synagogue":
			            	case "temple":
			            	case "train_station":
			            	case "university":
			            	case "warehouse":
		            		// this custom value is probably better than BAG, so retain value
			            		if (buildingValueTag.equals(BagUtils.CONSTRUCTION))
			            		{
			            			// promote construction value to building
			            			commands.add(new ChangePropertyCommand(originalPrimitive, BagUtils.BUILDING, originalPrimitive.get(BagUtils.CONSTRUCTION)));
			            			commands.add(new ChangePropertyCommand(newPrimitive, BagUtils.BUILDING, originalPrimitive.get(BagUtils.CONSTRUCTION)));
			            		}
			            		else
			            		{
			            			// use existing value
			            			commands.add(new ChangePropertyCommand(newPrimitive, BagUtils.BUILDING, originalPrimitive.get(BagUtils.BUILDING)));
			            		}
			            		break;
			            	case "construction":
			            	case "yes":
			            	case "house":
			            	case "apartments":
			            	case "office":
			            	case "industrial":
			            	case "retail":
			            		if (buildingValueTag.equals(BagUtils.CONSTRUCTION))
			            		{
			            			// promote construction value to building
			            			commands.add(new ChangePropertyCommand(originalPrimitive, BagUtils.BUILDING, originalPrimitive.get(BagUtils.CONSTRUCTION)));
			            			commands.add(new ChangePropertyCommand(newPrimitive, BagUtils.BUILDING, originalPrimitive.get(BagUtils.CONSTRUCTION)));
			            		}
			            		else
			            		{
				            		// use new value
			            			commands.add(new ChangePropertyCommand(originalPrimitive, BagUtils.BUILDING, newPrimitive.get(BagUtils.BUILDING)));
			            		}
			            		break;
			            	default:
			            		// handle difference manually
		            			//commands.add(new ChangePropertyCommand(originalPrimitive, BagUtils.BUILDING, newPrimitive.get(BagUtils.BUILDING)));
			            		break;
		            	}
        			}
        		}
            }
            
            // fix start_date tag
            if (BagUtils.hasStartDate(originalPrimitive) && BagUtils.hasStartDate(newPrimitive))
            {
            	if (!originalPrimitive.get(BagUtils.START_DATE).equals(newPrimitive.get(BagUtils.START_DATE)))
            	{
            		commands.add(new ChangePropertyCommand(originalPrimitive, BagUtils.START_DATE, newPrimitive.get(BagUtils.START_DATE)));
            	}
            }
            
            // fix source tag
            if (BagUtils.hasSource(originalPrimitive) && BagUtils.hasSource(newPrimitive))
            {
            	if (!originalPrimitive.get(BagUtils.SOURCE).equals(newPrimitive.get(BagUtils.SOURCE)))
            	{
            		commands.add(new ChangePropertyCommand(originalPrimitive, BagUtils.SOURCE, newPrimitive.get(BagUtils.SOURCE)));
            	}
            }
            
            if (!commands.isEmpty())
            {
            	return new SequenceCommand(tr("Updating BAG Object tags"), commands);
            }
            else
            	return null;
        }

    }
}
