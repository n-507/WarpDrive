package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;

import net.minecraft.block.BlockGlass;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockEnanReactorFrameGlass extends BlockAbstractBase {
	
	public BlockEnanReactorFrameGlass(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.GLASS);
		
		setHardness(WarpDriveConfig.HULL_HARDNESS[enumTier.getIndex()] / 3.0F);
		setResistance(WarpDriveConfig.HULL_BLAST_RESISTANCE[enumTier.getIndex()] / 3.0F * 5.0F / 3.0F);
		setTranslationKey("warpdrive.energy.enan_reactor_frame." + enumTier.getName());
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isTranslucent(final IBlockState blockState) {
		return true;
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
	public boolean shouldSideBeRendered(final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, final EnumFacing facing) {
		final BlockPos blockPosSide = blockPos.offset(facing);
		if (blockAccess.isAirBlock(blockPosSide)) {
			return true;
		}
		final EnumFacing opposite = facing.getOpposite();
		final IBlockState blockStateSide = blockAccess.getBlockState(blockPosSide);
		if ( blockStateSide.getBlock() instanceof BlockGlass
		  || blockStateSide.getBlock() instanceof BlockEnanReactorFrameGlass ) {
			return blockState.getBlock().getMetaFromState(blockState)
			    != blockStateSide.getBlock().getMetaFromState(blockStateSide);
		}
		return !blockAccess.isSideSolid(blockPosSide, opposite, false);
	}
}