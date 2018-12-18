package cr0s.warpdrive.event;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

public class TooltipHandler {
	
	private static final BlockPos blockPosDummy = new BlockPos(0, -1, 0);
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onTooltipEvent_first(final ItemTooltipEvent event) {
		if (event.getEntityPlayer() == null) {
			return;
		}
		
		// add dictionary information
		if (Dictionary.ITEMS_BREATHING_HELMET.contains(event.getItemStack().getItem())) {
			Commons.addTooltip(event.getToolTip(), new TextComponentTranslation("warpdrive.tooltip.item_tag.breathing_helmet").getFormattedText());
		}
		if (Dictionary.ITEMS_FLYINSPACE.contains(event.getItemStack().getItem())) {
			Commons.addTooltip(event.getToolTip(), new TextComponentTranslation("warpdrive.tooltip.item_tag.fly_in_space").getFormattedText());
		}
		if (Dictionary.ITEMS_NOFALLDAMAGE.contains(event.getItemStack().getItem())) {
			Commons.addTooltip(event.getToolTip(), new TextComponentTranslation("warpdrive.tooltip.item_tag.no_fall_damage").getFormattedText());
		}
	}
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onTooltipEvent_last(final ItemTooltipEvent event) {
		if (event.getEntityPlayer() == null) {
			return;
		}
		
		final boolean isSneaking = event.getEntityPlayer().isSneaking();
		final boolean isCreativeMode = event.getEntityPlayer().capabilities.isCreativeMode;
		
		// cleanup the mess every mods add (notably the registry name)
		cleanupTooltip(event.getToolTip(), isSneaking, isCreativeMode);
		
		// add block/items details
		final Block block = Block.getBlockFromItem(event.getItemStack().getItem());
		if (block != Blocks.AIR) {
			addBlockDetails(event, isSneaking, isCreativeMode, block);
		} else {
			addItemDetails(event, isSneaking, isCreativeMode, event.getItemStack());
		}
		
		// add burn time details
		if (WarpDriveConfig.TOOLTIP_ADD_BURN_TIME.isEnabled(isSneaking, isCreativeMode)) {
			final int fuelEvent = ForgeEventFactory.getItemBurnTime(event.getItemStack());
			final int fuelFurnace = Math.round(TileEntityFurnace.getItemBurnTime(event.getItemStack()));
			final int fuelValue = fuelEvent >= 0 ? 0 : fuelFurnace;
			if (fuelValue > 0) {
				Commons.addTooltip(event.getToolTip(), String.format("§8Fuel to burn %.1f ores", fuelValue / 200.0F));
			}
		}
		
		// add ore dictionary names
		if (WarpDriveConfig.TOOLTIP_ADD_ORE_DICTIONARY_NAME.isEnabled(isSneaking, isCreativeMode)) {
			final int[] idOres = OreDictionary.getOreIDs(event.getItemStack());
			if (idOres.length != 0) {
				Commons.addTooltip(event.getToolTip(), "Ore dictionary names:");
				for (final int idOre : idOres) {
					final String nameOre = OreDictionary.getOreName(idOre);
					Commons.addTooltip(event.getToolTip(), "- " + nameOre);
				}
			}
		}
	}
	
	// remove redundant information in tooltips
	private static void cleanupTooltip(@Nonnull final List<String> list, final boolean isSneaking, final boolean isCreativeMode) {
		// skip empty tooltip
		if (list.isEmpty()) {
			return;
		}
		
		// skip if disabled
		if (!WarpDriveConfig.TOOLTIP_ENABLE_DEDUPLICATION.isEnabled(isSneaking, isCreativeMode)) {
			return;
		}
		
		// remove duplicates
		final HashSet<String> setClean = new HashSet<>(list.size());
		Iterator<String> iterator = list.iterator();
		while (iterator.hasNext()) {
			final String original = iterator.next();
			final String clean = Commons.removeFormatting(original).trim().toLowerCase();
			if (clean.isEmpty()) {
				continue;
			}
			
			boolean doRemove = setClean.contains(clean);
			for (final String key : WarpDriveConfig.TOOLTIP_CLEANUP_LIST) {
				if (clean.contains(key)) {
					doRemove = true;
					break;
				}
			}
			if (doRemove) {
				iterator.remove();
			} else {
				setClean.add(clean);
			}
		}
		
		// remove extra separator lines that might be resulting from the cleanup (i.e. 2 consecutive empty lines or a final empty line)
		boolean wasEmpty = false;
		iterator = list.iterator();
		while (iterator.hasNext()) {
			final String original = iterator.next();
			final String clean = Commons.removeFormatting(original).trim();
			// keep line with content or at least 4 spaces (for mods adding image overlays)
			if ( !clean.isEmpty()
			     || original.length() > 4 ) {
				wasEmpty = false;
				continue;
			}
			// only keep first empty line in a sequence
			// always remove the last line when it's empty
			if ( wasEmpty
			     || !iterator.hasNext() ) {
				iterator.remove();
			}
			wasEmpty = true;
		}
	}
	
	@SideOnly(Side.CLIENT)
	@SuppressWarnings("deprecation")
	private static void addBlockDetails(@Nonnull final ItemTooltipEvent event, final boolean isSneaking, final boolean isCreativeMode, final Block block) {
		// item registry name
		final ResourceLocation registryNameItem = event.getItemStack().getItem().getRegistryName();
		if (registryNameItem == null) {
			Commons.addTooltip(event.getToolTip(), "§4Invalid item with no registry name!");
			return;
		}
		
		// registry name
		if (WarpDriveConfig.TOOLTIP_ADD_REGISTRY_NAME.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final ResourceLocation registryNameBlock = Block.REGISTRY.getNameForObject(block);
				// noinspection ConstantConditions
				if (registryNameBlock != null) {
					Commons.addTooltip(event.getToolTip(), "§8" + registryNameBlock);
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// tool related stats
		final IBlockState blockState = getStateForPlacement(block,
		                                                    null, null,
		                                                    EnumFacing.DOWN, 0.0F, 0.0F, 0.0F, event.getItemStack().getMetadata(),
		                                                    null, EnumHand.MAIN_HAND);
		if (WarpDriveConfig.TOOLTIP_ADD_HARVESTING.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final String harvestTool = block.getHarvestTool(blockState);
				if (harvestTool != null) {
					Commons.addTooltip(event.getToolTip(), String.format("Harvest with %s (%d)",
					                                                     harvestTool, 
					                                                     block.getHarvestLevel(blockState)));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// generic properties
		if ( WarpDriveConfig.TOOLTIP_ADD_OPACITY.isEnabled(isSneaking, isCreativeMode)
		  && blockState != null ) {
			try {
				Commons.addTooltip(event.getToolTip(), String.format("§8Light opacity is %d",
				                                                     block.getLightOpacity(blockState)));
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		if (WarpDriveConfig.TOOLTIP_ADD_HARDNESS.isEnabled(isSneaking, isCreativeMode)) {
			try {
				Commons.addTooltip(event.getToolTip(), String.format("§8Hardness is %.1f",
				                                                     (float) WarpDrive.fieldBlockHardness.get(block)));
			} catch (final Exception exception) {
				// no operation
			}
			Commons.addTooltip(event.getToolTip(), String.format("§8Explosion resistance is %.1f",
			                                                     block.getExplosionResistance(null)));
		}
		
		// flammability
		if (WarpDriveConfig.TOOLTIP_ADD_FLAMMABILITY.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final int flammability = Blocks.FIRE.getFlammability(block);
				final int fireSpread = Blocks.FIRE.getEncouragement(block);
				if (flammability > 0) {
					Commons.addTooltip(event.getToolTip(), String.format("§8Flammability is %d, spread %d",
					                                                     flammability, fireSpread));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// fluid stats
		if (WarpDriveConfig.TOOLTIP_ADD_FLUID.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final Fluid fluid = FluidRegistry.lookupFluidForBlock(block);
				if (fluid != null) {
					if (fluid.isGaseous()) {
						Commons.addTooltip(event.getToolTip(), String.format("Gaz viscosity is %d",
						                                                     fluid.getViscosity()));
						Commons.addTooltip(event.getToolTip(), String.format("Gaz density is %d",
						                                                     fluid.getDensity()));
					} else {
						Commons.addTooltip(event.getToolTip(), String.format("Liquid viscosity is %d",
						                                                     fluid.getViscosity()));
						Commons.addTooltip(event.getToolTip(), String.format("Liquid density is %d",
						                                                     fluid.getDensity()));
					}
					Commons.addTooltip(event.getToolTip(), String.format("Temperature is %d K",
					                                                     fluid.getTemperature()));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
	}
	
	// Some mods read tooltips server side for searching through them.
	// However, world & placer are unknown on server side, so we're defaulting to null which doesn't always work (see stairs for example).
	// IC2 is throwing assertions due to a bug somewhere...
	public static IBlockState getStateForPlacement(final Block block,
	                                               final World world, final BlockPos blockPos, @Nonnull final EnumFacing facing,
	                                               final float hitX, final float hitY, final float hitZ, final int metadata,
	                                               final EntityLivingBase entityLivingBase, @Nonnull final EnumHand enumHand) {
		final BlockPos blockPosToUse = blockPos != null ? blockPos : blockPosDummy;
		final IBlockState blockState;
		if ( (world != null && world.isRemote)
		  || Thread.currentThread().getName().equals("Client thread") ) {// we're on client side, we can fallback to current player
			blockState = getStateForPlacement_client(block,
			                                         world, blockPosToUse, facing,
			                                         hitX, hitY, hitZ, metadata,
			                                         entityLivingBase, enumHand);
		} else {// we're server side, we try to be more 'flexible'
			blockState = getStateForPlacement_safely(block,
			                                         world, blockPosToUse, facing,
			                                         hitX, hitY, hitZ, metadata,
			                                         entityLivingBase, enumHand);
		}
		return blockState;
	}
	
	@SideOnly(Side.CLIENT)
	private static IBlockState getStateForPlacement_client(final Block block,
	                                                       final World world, final BlockPos blockPos, @Nonnull final EnumFacing facing,
	                                                       final float hitX, final float hitY, final float hitZ, final int metadata,
	                                                       final EntityLivingBase entityLivingBase, @Nonnull final EnumHand enumHand) {
		final EntityLivingBase entityLivingBaseToUse = entityLivingBase != null ? entityLivingBase : Minecraft.getMinecraft().player;
		final World worldToUse = world != null ? world : entityLivingBase != null ? entityLivingBaseToUse.getEntityWorld() : null;
		// during boot, even on client, player isn't defined yet, so we fallback to the server 'flexible' approach
		return getStateForPlacement_safely(block,
		                                   worldToUse, blockPos, facing,
		                                   hitX, hitY, hitZ, metadata,
		                                   entityLivingBaseToUse, enumHand);
	}
	
	@SuppressWarnings("deprecation")
	private static IBlockState getStateForPlacement_safely(final Block block,
	                                                       final World world, final BlockPos blockPos, @Nonnull final EnumFacing facing,
	                                                       final float hitX, final float hitY, final float hitZ, final int metadata,
	                                                       final EntityLivingBase entityLivingBase, @Nonnull final EnumHand enumHand) {
		IBlockState blockState;
		try {
			// world or entity might be null at this point
			blockState = block.getStateForPlacement(world, blockPos, facing,
			                                        hitX, hitY, hitZ, metadata,
			                                        entityLivingBase, enumHand);
		} catch (final Exception | AssertionError throwable1) {
			try {
				// first fallback in case world was really needed...
				blockState = block.getStateFromMeta(metadata);
				
				// just report in dev if a mandatory value was null
				if (WarpDrive.isDev && (world == null || entityLivingBase == null)) {
					WarpDrive.logger.error(String.format("Exception getting block state from block %s (%s %d): %s",
					                                     block,
					                                     block.getRegistryName(),
					                                     metadata,
					                                     throwable1));
				}
			} catch (final Exception | AssertionError throwable2) {
				WarpDrive.logger.error(String.format("Exception getting block state from item stack %s (%s %d): %s",
				                                     block,
				                                     block.getRegistryName(),
				                                     metadata,
				                                     throwable1 ));
				throwable1.printStackTrace();
				WarpDrive.logger.error(String.format("followed by %s",
				                                     throwable2 ));
				throwable2.printStackTrace();
				// Final fallback to default
				blockState = block.getDefaultState();
			}
		}
		return blockState;
	}
	
	@SideOnly(Side.CLIENT)
	private static void addItemDetails(final ItemTooltipEvent event, final boolean isSneaking, final boolean isCreativeMode, final ItemStack itemStack) {
		final Item item = itemStack.getItem();
		
		// registry name
		if (WarpDriveConfig.TOOLTIP_ADD_REGISTRY_NAME.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final ResourceLocation registryNameItem = Item.REGISTRY.getNameForObject(itemStack.getItem());
				if (registryNameItem == null) {
					Commons.addTooltip(event.getToolTip(), "§4Invalid item with no registry name!");
					return;
				}
				Commons.addTooltip(event.getToolTip(), "§8" + registryNameItem);
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// (item duration can't be directly understood => out)
		
		// durability
		if (WarpDriveConfig.TOOLTIP_ADD_DURABILITY.isEnabled(isSneaking, isCreativeMode)) {
			try {
				if (event.getItemStack().isItemStackDamageable()) {
					Commons.addTooltip(event.getToolTip(), String.format("Durability: %d / %d",
					                                                     event.getItemStack().getMaxDamage() - event.getItemStack().getItemDamage(),
					                                                     event.getItemStack().getMaxDamage()));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// armor stats
		if (WarpDriveConfig.TOOLTIP_ADD_ARMOR.isEnabled(isSneaking, isCreativeMode)) {
			try {
				if (item instanceof ItemArmor) {
					if (WarpDrive.isDev) {// Armor points is already shown by vanilla tooltips
						Commons.addTooltip(event.getToolTip(), String.format("Armor points: %d",
						                                                     ((ItemArmor) item).damageReduceAmount));
					}
					final ArmorMaterial armorMaterial = ((ItemArmor) item).getArmorMaterial();
					Commons.addTooltip(event.getToolTip(), String.format("Enchantability: %d",
					                                                     armorMaterial.getEnchantability()));
					
					if (WarpDriveConfig.TOOLTIP_ADD_REPAIR_WITH.isEnabled(isSneaking, isCreativeMode)) {
						final ItemStack itemStackRepair = armorMaterial.getRepairItemStack();
						if (!itemStackRepair.isEmpty()) {
							Commons.addTooltip(event.getToolTip(), String.format("Repair with %s",
							                                                     itemStackRepair.getTranslationKey()));
						}
					}
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// entity data
		if (item instanceof ItemMonsterPlacer) {
			if (WarpDriveConfig.TOOLTIP_ADD_ENTITY_ID.isEnabled(isSneaking, isCreativeMode)) {
				final ResourceLocation entityId = ItemMonsterPlacer.getNamedIdFrom(itemStack);
				if (entityId != null) {
					Commons.addTooltip(event.getToolTip(), String.format("Entity is %s",
					                                                     entityId));
				} else {
					Commons.addTooltip(event.getToolTip(), "Entity is §4-undefined");
				}
			}
		}
	}
}
