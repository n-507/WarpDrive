package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Filler;
import cr0s.warpdrive.config.GenericSet;
import cr0s.warpdrive.config.InvalidXmlException;
import cr0s.warpdrive.config.Loot;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.XmlFileManager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.w3c.dom.Element;

public class Schematic extends AbstractStructure {
	
	protected String filename;
	protected Replacement[] replacements;
	protected Insertion[] insertions;
	
	public Schematic(final String group, final String name) {
		super(group, name);
	}
	
	@Override
	public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
		super.loadFromXmlElement(element);
		
		// load all replacement elements
		final List<Element> listReplacements = XmlFileManager.getChildrenElementByTagName(element, "replacement");
		replacements = new Replacement[listReplacements.size()];
		int replacementIndexOut = 0;
		for (final Element elementReplacement : listReplacements) {
			final String blockState = elementReplacement.getAttribute("blockState");
			
			replacements[replacementIndexOut] = new Replacement(getFullName(), blockState);
			try {
				replacements[replacementIndexOut].loadFromXmlElement(elementReplacement);
				replacementIndexOut++;
			} catch (final InvalidXmlException exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Skipping invalid replacement %s",
				                                     blockState));
			}
		}
		
		// load all insertion elements
		final List<Element> listInsertions = XmlFileManager.getChildrenElementByTagName(element, "insertion");
		insertions = new Insertion[listInsertions.size()];
		int insertionIndexOut = 0;
		for (final Element elementInsertion : listInsertions) {
			final String blockState = elementInsertion.getAttribute("blockState");
			
			insertions[insertionIndexOut] = new Insertion(getFullName(), blockState);
			try {
				insertions[insertionIndexOut].loadFromXmlElement(elementInsertion);
				insertionIndexOut++;
			} catch (final InvalidXmlException exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Skipping invalid insertion %s",
				                                     blockState));
			}
		}
		
		return true;
	}
	
	@Override
	public boolean generate(@Nonnull final World world, @Nonnull final Random random, @Nonnull final BlockPos blockPos) {
		return instantiate(random).generate(world, random, blockPos);
	}

	@Override
	public AbstractStructureInstance instantiate(final Random random) {
		return new SchematicInstance(this, random);
	}
	
	public class Replacement extends GenericSet<Filler> {
		
		private final String parentFullName;
		protected Block block;
		protected IBlockState blockState;
		
		public Replacement(final String parentFullName, final String name) {
			super(null, name, Filler.DEFAULT, "filler");
			this.parentFullName = parentFullName;
		}
		
		@Override
		public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
			if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
				WarpDrive.logger.info(String.format("  + found replacement %s",
				                                    element.getAttribute("name")));
			}
			
			super.loadFromXmlElement(element);
			
			// resolve static imports
			for (final String importGroupName : getImportGroupNames()) {
				final GenericSet<Filler> fillerSet = WarpDriveConfig.FillerManager.getGenericSet(importGroupName);
				if (fillerSet == null) {
					WarpDrive.logger.warn(String.format("Skipping missing FillerSet %s in replacement %s of structure %s",
					                                    importGroupName, name, parentFullName));
				} else {
					loadFrom(fillerSet);
				}
			}
			
			// validate dynamic imports
			for (final String importGroup : getImportGroups()) {
				if (!WarpDriveConfig.FillerManager.doesGroupExist(importGroup)) {
					WarpDrive.logger.warn(String.format("An invalid FillerSet group %s is referenced in replacement %s of structure %s",
					                                    importGroup, name, parentFullName));
				}
			}
			
			return true;
		}
		
		public Replacement instantiate(final Random random) {
			final Replacement replacement = new Replacement(parentFullName, name);
			replacement.block = block;
			replacement.blockState = blockState;
			try {
				replacement.loadFrom(this);
				for (final String importGroup : getImportGroups()) {
					final GenericSet<Filler> fillerSet = WarpDriveConfig.FillerManager.getRandomSetFromGroup(random, importGroup);
					if (fillerSet == null) {
						WarpDrive.logger.warn(String.format("Ignoring invalid group %s in replacement %s of structure %s",
						                                    importGroup, name, parentFullName));
						continue;
					}
					if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
						WarpDrive.logger.info(String.format("Filling %s:%s with %s:%s",
						                                    parentFullName, name, importGroup, fillerSet.getName()));
					}
					replacement.loadFrom(fillerSet);
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Failed to instantiate replacement %s from structure %s",
				                                     name, parentFullName));
			}
			if (replacement.isEmpty()) {
				if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
					WarpDrive.logger.info(String.format("Ignoring empty replacement %s in structure %s",
					                                    name, parentFullName));
				}
				return null;
			}
			return replacement;
		}
		
		public boolean isMatching(final IBlockState blockStateIn) {
			return (block != null && block == blockStateIn.getBlock())
			    || blockState.equals(blockStateIn);
		}
	}
	
	public class Insertion extends GenericSet<Loot> {
		
		private final String parentFullName;
		private int minQuantity;
		private int maxQuantity;
		protected Block block;
		protected IBlockState blockState;
		
		public Insertion(final String parentFullName, final String name) {
			super(null, name, Loot.DEFAULT, "loot");
			this.parentFullName = parentFullName;
		}
		
		@Override
		public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
			if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
				WarpDrive.logger.info(String.format("  + found insertion %s",
				                                    element.getAttribute("name")));
			}
			
			super.loadFromXmlElement(element);
			
			// get optional minQuantity attribute, defaulting to 0
			minQuantity = 0;
			final String stringMinQuantity = element.getAttribute("minQuantity");
			if (!stringMinQuantity.isEmpty()) {
				minQuantity = Integer.parseInt(stringMinQuantity);
			}
			
			// get optional maxQuantity attribute, defaulting to 7
			maxQuantity = 7;
			final String stringMaxQuantity = element.getAttribute("minQuantity");
			if (!stringMaxQuantity.isEmpty()) {
				maxQuantity = Integer.parseInt(stringMaxQuantity);
			}
			
			// resolve static imports
			for (final String importGroupName : getImportGroupNames()) {
				final GenericSet<Loot> lootSet = WarpDriveConfig.LootManager.getGenericSet(importGroupName);
				if (lootSet == null) {
					WarpDrive.logger.warn(String.format("Skipping missing LootSet %s in insertion %s of structure %s",
					                                    importGroupName, name, parentFullName));
				} else {
					loadFrom(lootSet);
				}
			}
			
			// validate dynamic imports
			for (final String importGroup : getImportGroups()) {
				if (!WarpDriveConfig.LootManager.doesGroupExist(importGroup)) {
					WarpDrive.logger.warn(String.format("An invalid LootSet group %s is referenced in insertion %s of structure %s",
					                                    importGroup, name, parentFullName));
				}
			}
			
			return true;
		}
		
		public Insertion instantiate(final Random random) {
			final Insertion insertion = new Insertion(parentFullName, name);
			insertion.minQuantity = minQuantity;
			insertion.maxQuantity = maxQuantity;
			insertion.block = block;
			insertion.blockState = blockState;
			try {
				insertion.loadFrom(this);
				for (final String importGroup : getImportGroups()) {
					final GenericSet<Loot> lootSet = WarpDriveConfig.LootManager.getRandomSetFromGroup(random, importGroup);
					if (lootSet == null) {
						WarpDrive.logger.warn(String.format("Ignoring invalid group %s in insertion %s of structure %s",
						                                    importGroup, name, parentFullName));
						continue;
					}
					if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
						WarpDrive.logger.info(String.format("Filling %s:%s with %s:%s",
						                                    parentFullName, name, importGroup, lootSet.getName()));
					}
					insertion.loadFrom(lootSet);
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Failed to instantiate insertion %s from structure %s",
				                                     name, parentFullName));
			}
			if (insertion.isEmpty()) {
				if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
					WarpDrive.logger.info(String.format("Ignoring empty insertion %s in structure %s",
					                                    name, parentFullName));
				}
				return null;
			}
			return insertion;
		}
		
		public boolean isMatching(final IBlockState blockStateIn) {
			return (block != null && block == blockStateIn.getBlock())
			    || blockState.equals(blockStateIn);
		}
	}
}
