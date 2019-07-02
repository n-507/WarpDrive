package cr0s.warpdrive.block.breathing;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class BlockAbstractAir extends BlockAbstractBase {
	
	public static final PropertyInteger CONCENTRATION = PropertyInteger.create("concentration", 0, 15);
	
	BlockAbstractAir(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.AIR);
		
		setHardness(0.0F);
		setTranslationKey("warpdrive.breathing.air");
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean causesSuffocation(final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullBlock(final IBlockState blockState) {
		return true;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullCube(final IBlockState state) {
		return false;
	}
	
	@Override
	public boolean isAir(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos pos) {
		return true;
	}
	
	@SuppressWarnings("deprecation")
	@Nullable
	@Override
	public AxisAlignedBB getCollisionBoundingBox(final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return NULL_AABB;
	}
	
	@Override
	public boolean isReplaceable(final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return true;
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public boolean canPlaceBlockAt(final World world, @Nonnull final BlockPos blockPos) {
		return true;
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public boolean canCollideCheck(final IBlockState blockState, final boolean hitIfLiquid) {
		return false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(final CreativeTabs creativeTab, final NonNullList<ItemStack> list) {
		// hide in NEI
		for (int i = 0; i < 16; i++) {
			Commons.hideItemStack(new ItemStack(this, 1, i));
		}
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public EnumPushReaction getPushReaction(final IBlockState blockState) {
		return EnumPushReaction.DESTROY;
	}
	
	@Nonnull
	@Override
	public Item getItemDropped(final IBlockState blockState, final Random rand, final int fortune) {
		return Items.AIR;
	}
	
	@Override
	public int quantityDropped(final Random random) {
		return 0;
	}
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.TRANSLUCENT;
	}
	
	@SuppressWarnings("deprecation")
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing) {
		if (WarpDriveConfig.BREATHING_AIR_BLOCK_DEBUG) {
			return facing == EnumFacing.DOWN || facing == EnumFacing.UP;
		}
		
		final BlockPos blockPosSide = blockPos.offset(facing);
		final Block blockSide = blockAccess.getBlockState(blockPosSide).getBlock();
		if (blockSide instanceof BlockAbstractAir) {
			return false;
		}
		
		return blockSide == Blocks.AIR;
	}
	
	@Override
	public boolean isCollidable() {
		return false;
	}
}