package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.InvalidXmlException;
import cr0s.warpdrive.config.XmlRandomCollection;
import cr0s.warpdrive.config.XmlFileManager;
import cr0s.warpdrive.data.EnumStructureGroup;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import java.io.File;
import java.util.HashMap;
import java.util.Random;

public class StructureManager extends XmlFileManager {
	
	private static final StructureManager INSTANCE = new StructureManager();
	
	private static HashMap<String, XmlRandomCollection<AbstractStructure>> structuresByGroup;
	
	public static void load(final File dir) {
		structuresByGroup = new HashMap<>();
		INSTANCE.load(dir, "structure", "structure");
		
		for (final EnumStructureGroup group : EnumStructureGroup.values()) {
			if (!group.isRequired()) {
				continue;
			}
			if (!structuresByGroup.containsKey(group.getName())) {
				WarpDrive.logger.error(String.format("Error: no structure defined for mandatory group %s",
				                                     group.getName()));
			}
		}
	}
	
	@Override
	protected void parseRootElement(final String location, final Element element) throws InvalidXmlException {
		final String group = element.getAttribute("group");
		if (group.isEmpty()) {
			throw new InvalidXmlException(String.format("%s is missing a group attribute!",
			                                            location));
		}
		
		final String name = element.getAttribute("name");
		if (name.isEmpty()) {
			throw new InvalidXmlException(String.format("%s is missing a name attribute!",
			                                            location));
		}
		
		if (!element.getTagName().equals("structure")) {
			throw new InvalidXmlException(String.format("%s contains invalid element %s, expecting structure!",
			                                            location, element.getTagName()));
		}
		WarpDrive.logger.info(String.format("- found %s %s:%s",
		                                    element.getTagName(), group, name));
		
		final XmlRandomCollection<AbstractStructure> xmlRandomCollection = structuresByGroup.computeIfAbsent(group, k -> new XmlRandomCollection<>());
		
		AbstractStructure abstractStructure = xmlRandomCollection.getNamedEntry(name);
		if (abstractStructure == null) {
			final EnumStructureGroup enumStructureGroup = EnumStructureGroup.byName(group);
			switch (enumStructureGroup) {
			case STARS:
				abstractStructure = new Star(group, name);
				break;
				
			case MOONS:
				abstractStructure = new Orb(group, name);
				break;
			
			case SCHEMATIC:
				abstractStructure = new Schematic(group, name);
				break;
				
			default:
				abstractStructure = new MetaOrb(group, name);
				break;
			}
		}
		xmlRandomCollection.loadFromXML(abstractStructure, element);
	}
	
	public static AbstractStructure getStructure(final Random random, final String group, final String name) {
		if (group == null || group.isEmpty()) {
			return null;
		}
		
		// @TODO XML configuration for Asteroids Fields
		if (EnumStructureGroup.byName(group) == EnumStructureGroup.ASTEROIDS_FIELDS) {
			return new AsteroidField(null, null);
		}
		
		final XmlRandomCollection<AbstractStructure> xmlRandomCollection = structuresByGroup.get(group);
		if (xmlRandomCollection == null) {
			return null;
		}
		
		if (name == null || name.isEmpty()) {
			return xmlRandomCollection.getRandomEntry(random);
		} else {
			return xmlRandomCollection.getNamedEntry(name);
		}
	}
	
	public static String getStructureNames(final String group) {
		if (group != null && !group.isEmpty()) {
			final XmlRandomCollection<AbstractStructure> xmlRandomCollection = structuresByGroup.get(group);
			if (xmlRandomCollection != null) {
				return xmlRandomCollection.getNames();
			}
		}
		return "Error: group '" + group + "' isn't defined. Try one of: " + StringUtils.join(structuresByGroup.keySet(), ", ");
	}
	
	public static String getGroups() {
		return Commons.format(structuresByGroup.keySet().toArray());
	}
}
