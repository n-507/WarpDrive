package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IBlockUpdateDetector;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.client.ClientProxy;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.render.ClientCameraHandler;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

import net.minecraftforge.common.IRarity;
import net.minecraftforge.fml.common.Optional;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ConcurrentModificationException;
import java.util.Random;

@Optional.InterfaceList({
	@Optional.Interface(iface = "defense.api.IEMPBlock", modid = "DefenseTech"),
})
public abstract class BlockAbstractContainer extends BlockContainer implements IBlockBase, defense.api.IEMPBlock {
	
	private static boolean isInvalidEMPreported = false;
	
	protected EnumTier enumTier;
	protected boolean hasSubBlocks = false;
	protected boolean ignoreFacingOnPlacement = false;
	
	protected BlockAbstractContainer(final String registryName, final EnumTier enumTier, final Material material) {
		super(material);
		
		this.enumTier = enumTier;
		setHardness(5.0F);
		setResistance(6.0F * 5 / 3);
		setSoundType(SoundType.METAL);
		setCreativeTab(WarpDrive.creativeTabMain);
		setRegistryName(registryName);
		WarpDrive.register(this);
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockAbstractBase(this, false, true);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void modelInitialisation() {
		final Item item = Item.getItemFromBlock(this);
		ClientProxy.modelInitialisation(item);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public EnumBlockRenderType getRenderType(final IBlockState blockState) {
		return EnumBlockRenderType.MODEL;
	}
	
	@Override
	public void onBlockAdded(final World world, final BlockPos pos, final IBlockState blockState) {
		super.onBlockAdded(world, pos, blockState);
		final TileEntity tileEntity = world.getTileEntity(pos);
		if (tileEntity instanceof IBlockUpdateDetector) {
			((IBlockUpdateDetector) tileEntity).onBlockUpdateDetected();
		}
	}
	
	@Nonnull
	@Override
	public IBlockState getStateForPlacement(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing,
	                                        final float hitX, final float hitY, final float hitZ, final int metadata,
	                                        @Nonnull final EntityLivingBase entityLivingBase, final EnumHand enumHand) {
		final IBlockState blockState = super.getStateForPlacement(world, blockPos, facing, hitX, hitY, hitZ, metadata, entityLivingBase, enumHand);
		final boolean isRotating = !ignoreFacingOnPlacement
		                        && blockState.getProperties().containsKey(BlockProperties.FACING);
		if (isRotating) {
			if (blockState.isFullBlock()) {
				final EnumFacing enumFacing = Commons.getFacingFromEntity(entityLivingBase);
				return blockState.withProperty(BlockProperties.FACING, enumFacing);
			} else {
				return blockState.withProperty(BlockProperties.FACING, facing);
			}
		}
		return blockState;
	}
	
	@Override
	public void onBlockPlacedBy(final World world, final BlockPos blockPos, final IBlockState blockState,
	                            final EntityLivingBase entityLivingBase, final ItemStack itemStack) {
		super.onBlockPlacedBy(world, blockPos, blockState, entityLivingBase, itemStack);
		
		// set inherited properties
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		assert tileEntity instanceof TileEntityAbstractBase;
		if (itemStack.getTagCompound() != null) {
			final NBTTagCompound tagCompound = itemStack.getTagCompound().copy();
			tagCompound.setInteger("x", blockPos.getX());
			tagCompound.setInteger("y", blockPos.getY());
			tagCompound.setInteger("z", blockPos.getZ());
			tileEntity.readFromNBT(tagCompound);
			tileEntity.markDirty();
			world.notifyBlockUpdate(blockPos, blockState, blockState, 3);
		}
	}
	
	@Override
	public boolean removedByPlayer(@Nonnull final IBlockState blockState, final World world, @Nonnull final BlockPos blockPos,
	                               @Nonnull final EntityPlayer entityPlayer, final boolean willHarvest) {
		final boolean bResult;
		if (willHarvest) {// harvestBlock will be called later on, we'll need the TileEntity at that time, so we don't call ancestor here so we don't set block to air
			this.onBlockHarvested(world, blockPos, blockState, entityPlayer);
			bResult = true;
		} else {
			bResult = super.removedByPlayer(blockState, world, blockPos, entityPlayer, false);
		}
		return bResult;
	}
	
	@Override
	public void dropBlockAsItemWithChance(final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                                      final float chance, final int fortune) {
		super.dropBlockAsItemWithChance(world, blockPos, blockState, chance, fortune); // calls getDrops() here below
		if ( !world.isRemote
		  && !world.restoringBlockSnapshots) {
			world.setBlockToAir(blockPos);
		}
	}
	
	// willHarvest was true during the call to removedPlayer so TileEntity is still there when drops will be computed hereafter
	@Override
	public void getDrops(@Nonnull final NonNullList<ItemStack> drops,
	                     final IBlockAccess blockAccess, final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                     final int fortune) {
		final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityAbstractBase)) {
			WarpDrive.logger.error(String.format("Missing tile entity for %s %s, reverting to vanilla getDrops logic",
			                                     this, Commons.format(blockAccess, blockPos)));
			super.getDrops(drops, blockAccess, blockPos, blockState, fortune);
			return;
		}
		
		final Random rand = blockAccess instanceof World ? ((World) blockAccess).rand : RANDOM;
		final int count = quantityDropped(blockState, fortune, rand);
		for (int i = 0; i < count; i++) {
			final Item item = this.getItemDropped(blockState, rand, fortune);
			if (item != Items.AIR) {
				final ItemStack itemStack = new ItemStack(item, 1, damageDropped(blockState));
				final NBTTagCompound tagCompound = new NBTTagCompound();
				((TileEntityAbstractBase) tileEntity).writeItemDropNBT(tagCompound);
				itemStack.setTagCompound(tagCompound);
				drops.add(itemStack);
			}
		}
	}
	
	@Nonnull
	@Override
	public ItemStack getPickBlock(@Nonnull final IBlockState blockState, final RayTraceResult target, @Nonnull final World world, @Nonnull final BlockPos blockPos, final EntityPlayer entityPlayer) {
		final ItemStack itemStack = super.getPickBlock(blockState, target, world, blockPos, entityPlayer);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		final NBTTagCompound tagCompound = new NBTTagCompound();
		if (tileEntity instanceof TileEntityAbstractBase) {
			((TileEntityAbstractBase) tileEntity).writeItemDropNBT(tagCompound);
			itemStack.setTagCompound(tagCompound);
		}
		return itemStack;
	}
	
	@Override
	public boolean rotateBlock(final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing axis) {
		// already handled by vanilla
		return super.rotateBlock(world, blockPos, axis);
	}
	
	// Triggers on server side when placing a comparator compatible block
	// May trigger twice for the same placement action (placing a vanilla chest)
	// Triggers on server side when removing a comparator compatible block
	// Triggers on both sides when removing a TileEntity
	// (by extension, it'll trigger twice for the same removal of a TileEntity with comparator output)
	@Override
	public void onNeighborChange(final IBlockAccess blockAccess, final BlockPos blockPos, final BlockPos blockPosNeighbor) {
		super.onNeighborChange(blockAccess, blockPos, blockPosNeighbor);
		if (!Commons.isSafeThread()) {
			if (WarpDriveConfig.LOGGING_PROFILING_THREAD_SAFETY) {
				final Block blockNeighbor = blockAccess.getBlockState(blockPosNeighbor).getBlock();
				final ResourceLocation registryName = blockNeighbor.getRegistryName();
				WarpDrive.logger.error(String.format("Bad multithreading detected from mod %s %s, please report to mod author",
				                                     registryName == null ? blockNeighbor : registryName.getNamespace(),
				                                     Commons.format(blockAccess, blockPosNeighbor) ));
				new ConcurrentModificationException().printStackTrace();
			}
			return;
		}
		final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
		if ( tileEntity == null
		  || tileEntity.getWorld().isRemote ) {
			return;
		}
		if (tileEntity instanceof IBlockUpdateDetector) {
			((IBlockUpdateDetector) tileEntity).onBlockUpdateDetected();
		}
	}
	
	@Override
	@Optional.Method(modid = "DefenseTech")
	public void onEMP(final World world, final int x, final int y, final int z, final defense.api.IExplosion explosiveEMP) {
		if (WarpDriveConfig.LOGGING_WEAPON) {
			WarpDrive.logger.info(String.format("EMP received %s from %s with energy %d and radius %.1f",
			                                    Commons.format(world, x, y, z),
			                                    explosiveEMP, explosiveEMP.getEnergy(), explosiveEMP.getRadius()));
		}
		// EMP tower = 3k Energy, 60 radius
		// EMP explosive = 3k Energy, 50 radius
		if (explosiveEMP.getRadius() == 60.0F) {// compensate tower stacking effect
			onEMP(world, new BlockPos(x, y, z), 0.02F);
		} else if (explosiveEMP.getRadius() == 50.0F) {
			onEMP(world, new BlockPos(x, y, z), 0.70F);
		} else {
			if (!isInvalidEMPreported) {
				isInvalidEMPreported = true;
				WarpDrive.logger.warn(String.format("EMP received %s from %s with energy %d and unsupported radius %.1f",
				                                    Commons.format(world, x, y, z),
				                                    explosiveEMP, explosiveEMP.getEnergy(), explosiveEMP.getRadius()));
				Commons.dumpAllThreads();
			}
			onEMP(world, new BlockPos(x, y, z), 0.02F);
		}
	}
	
	public void onEMP(final World world, final BlockPos blockPos, final float efficiency) {
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityAbstractEnergy) {
			final TileEntityAbstractEnergy tileEntityAbstractEnergy = (TileEntityAbstractEnergy) tileEntity;
			if (tileEntityAbstractEnergy.energy_getMaxStorage() > 0) {
				tileEntityAbstractEnergy.energy_consume(Math.round(tileEntityAbstractEnergy.energy_getEnergyStored() * efficiency), false);
			}
		}
	}
	
	@Nonnull
	@Override
	public EnumTier getTier(final ItemStack itemStack) {
		return enumTier;
	}
	
	@Nonnull
	@Override
	public IRarity getForgeRarity(@Nonnull final ItemStack itemStack) {
		return getTier(itemStack).getForgeRarity();
	}
	
	@Override
	public boolean onBlockActivated(final World world, final BlockPos blockPos, final IBlockState blockState,
	                                final EntityPlayer entityPlayer, final EnumHand enumHand,
	                                final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		if (enumHand != EnumHand.MAIN_HAND) {
			return true;
		}
		if ( world.isRemote
		  && ClientCameraHandler.isOverlayEnabled ) {
			return true;
		}
		
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(enumHand);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityAbstractBase)) {
			return false;
		}
		final TileEntityAbstractBase tileEntityAbstractBase = (TileEntityAbstractBase) tileEntity;
		final boolean hasVideoChannel = tileEntity instanceof IVideoChannel;
		
		// video channel is reported client side, everything else is reported server side
		if ( world.isRemote
		  && !hasVideoChannel ) {
			return false;
		}
		
		EnumComponentType enumComponentType = null;
		if ( !itemStackHeld.isEmpty()
		  && itemStackHeld.getItem() instanceof ItemComponent ) {
			enumComponentType = EnumComponentType.get(itemStackHeld.getItemDamage());
		}
		
		// sneaking with an empty hand or an upgrade item in hand to dismount current upgrade
		if ( !world.isRemote
		  && entityPlayer.isSneaking() ) {
			// using an upgrade item or an empty hand means dismount upgrade
			if ( tileEntityAbstractBase.isUpgradeable()
			  && ( itemStackHeld.isEmpty()
			    || enumComponentType != null ) ) {
				// find a valid upgrade to dismount
				if ( itemStackHeld.isEmpty()
				  || !tileEntityAbstractBase.hasUpgrade(enumComponentType) ) {
					enumComponentType = (EnumComponentType) tileEntityAbstractBase.getFirstUpgradeOfType(EnumComponentType.class, null);
				}
				
				if (enumComponentType == null) {
					// no more upgrades to dismount
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.styleWarning, "warpdrive.upgrade.result.no_upgrade_to_dismount"));
					return true;
				}
				
				if (!entityPlayer.capabilities.isCreativeMode) {
					// dismount the current upgrade item
					final ItemStack itemStackDrop = ItemComponent.getItemStackNoCache(enumComponentType, 1);
					final EntityItem entityItem = new EntityItem(world, entityPlayer.posX, entityPlayer.posY + 0.5D, entityPlayer.posZ, itemStackDrop);
					entityItem.setNoPickupDelay();
					world.spawnEntity(entityItem);
				}
				
				tileEntityAbstractBase.dismountUpgrade(enumComponentType);
				// upgrade dismounted
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.styleCorrect, "warpdrive.upgrade.result.dismounted",
				                                                       enumComponentType.name()));
				return true;
			}
			
		} else if ( !entityPlayer.isSneaking()
		         && itemStackHeld.isEmpty() ) {// no sneaking and no item in hand => show status
			Commons.addChatMessage(entityPlayer, tileEntityAbstractBase.getStatus());
			return true;
			
		} else if ( !world.isRemote
		         && tileEntityAbstractBase.isUpgradeable()
		         && enumComponentType != null ) {// no sneaking and an upgrade in hand => mounting an upgrade
			// validate type
			if (tileEntityAbstractBase.getUpgradeMaxCount(enumComponentType) <= 0) {
				// invalid upgrade type
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.styleWarning,"warpdrive.upgrade.result.invalid_upgrade"));
				return true;
			}
			if (!tileEntityAbstractBase.canUpgrade(enumComponentType)) {
				// too many upgrades
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.styleWarning,"warpdrive.upgrade.result.too_many_upgrades",
				                                                       tileEntityAbstractBase.getUpgradeMaxCount(enumComponentType)));
				return true;
			}
			
			if (!entityPlayer.capabilities.isCreativeMode) {
				// validate quantity
				if (itemStackHeld.getCount() < 1) {
					// not enough upgrade items
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.styleWarning, "warpdrive.upgrade.result.not_enough_upgrades"));
					return true;
				}
				
				// update player inventory
				itemStackHeld.shrink(1);
			}
			
			// mount the new upgrade item
			tileEntityAbstractBase.mountUpgrade(enumComponentType);
			// upgrade mounted
			Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.styleCorrect, "warpdrive.upgrade.result.mounted",
			                                                       enumComponentType.name()));
			return true;
		}
		
		return false;
	}
}
