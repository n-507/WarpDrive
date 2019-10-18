package cr0s.warpdrive.block.collection;

import cr0s.warpdrive.CommonProxy;
import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityAbstractLaser;
import cr0s.warpdrive.data.FluidWrapper;
import cr0s.warpdrive.data.InventoryWrapper;
import cr0s.warpdrive.data.Vector3;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

public abstract class TileEntityAbstractMiner extends TileEntityAbstractLaser {
	
	// machine type
	protected EnumFacing laserOutputSide = EnumFacing.NORTH;
	
	// machine state
	protected boolean		 enableSilktouch = false;
	
	// pre-computation
	protected Vector3        laserOutput = null;
	
	public TileEntityAbstractMiner() {
		super();
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		laserOutput = new Vector3(this).translate(0.5D).translate(laserOutputSide, 0.5D);
	}
	
	protected void harvestBlock(@Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		if (blockState.getBlock().isAir(blockState, world, blockPos)) {
			return;
		}
		if (FluidWrapper.isFluid(blockState)) {
			// Evaporate fluid
			world.playSound(null, blockPos, net.minecraft.init.SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
					2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
			
			// remove without updating neighbours
			world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 2);
			
		} else {
			final List<ItemStack> itemStackDrops = getItemStackFromBlock(blockPos, blockState);
			
			final EntityPlayer entityPlayer = CommonProxy.getFakePlayer(null, (WorldServer) world, blockPos);
			net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(itemStackDrops, getWorld(), blockPos, blockState,
			                                                               0, 1.0f, true, entityPlayer);
			
			if (InventoryWrapper.addToConnectedInventories(world, pos, itemStackDrops)) {
				setIsEnabled(false);
			}
			// standard harvest block effect
			world.playEvent(2001, blockPos, Block.getStateId(blockState));
			
			// remove while updating neighbours
			world.setBlockToAir(blockPos); // setBlockState(blockPos, Blocks.AIR.getDefaultState(), 3);
		}
	}
	
	@Nullable
	private NonNullList<ItemStack> getItemStackFromBlock(final BlockPos blockPos, final IBlockState blockState) {
		if (blockState == null) {
			WarpDrive.logger.error(String.format("%s Invalid block %s",
			                                     this, Commons.format(world, blockPos)));
			return null;
		}
		final NonNullList<ItemStack> itemStackDrops = NonNullList.create();
		boolean isHarvested = false;
		if (enableSilktouch) {
			boolean isSilkHarvestable = false;
			try {
				isSilkHarvestable = blockState.getBlock().canSilkHarvest(world, blockPos, blockState, null);
			} catch (final Exception exception) {// protect in case the mined block is corrupted
				exception.printStackTrace(WarpDrive.printStreamError);
			}
			if (isSilkHarvestable) {
				// intended code if AccessTransformer was working with gradlew:
				// itemStackDrops.add(blockState.getBlock().getSilkTouchDrop(blockState));
				
				final ItemStack itemStackDrop;
				try {
					itemStackDrop = (ItemStack) WarpDrive.methodBlock_getSilkTouch.invoke(blockState.getBlock(), blockState);
				} catch (final IllegalAccessException | InvocationTargetException exception) {
					throw new RuntimeException(exception);
				}
				if (!itemStackDrop.isEmpty()) {
					itemStackDrops.add(itemStackDrop);
				}
				isHarvested = true;
			}
		}
		
		try {
			if (!isHarvested) {
				blockState.getBlock().getDrops(itemStackDrops, world, blockPos, blockState, 0);
			}
		} catch (final Exception exception) {// protect in case the mined block is corrupted
			exception.printStackTrace(WarpDrive.printStreamError);
			return null;
		}
		
		return itemStackDrops;
	}
	
	// NBT DATA
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		enableSilktouch = tagCompound.getBoolean("enableSilktouch");
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		tagCompound.setBoolean("enableSilktouch", enableSilktouch);
		return tagCompound;
	}
}
