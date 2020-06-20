package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public abstract class BlockAbstractSpinRotatingContainer extends BlockAbstractContainer {
	
	protected BlockAbstractSpinRotatingContainer(final String registryName, final EnumTier enumTier, final Material material) {
		super(registryName, enumTier, material);
		
		setDefaultState(getDefaultState()
				                .withProperty(BlockProperties.ACTIVE, false)
				                .withProperty(BlockProperties.FACING, EnumFacing.DOWN)
				                .withProperty(BlockProperties.SPINNING, EnumFacing.NORTH)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockProperties.ACTIVE, BlockProperties.FACING, BlockProperties.SPINNING);
	}
	
	// Metadata Facing  Spinning
	// 0        NORTH   NORTH
	// 1        SOUTH   SOUTH
	// 2        WEST    WEST
	// 3        EAST    EAST
	// 4        DOWN    NORTH
	// 5        DOWN    SOUTH
	// 6        DOWN    WEST
	// 7        DOWN    EAST
	// 8        UP      NORTH
	// 9        UP      SOUTH
	// 10       UP      WEST
	// 11       UP      EAST
	// 12 to 15 ?       ?
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				       .withProperty(BlockProperties.FACING, EnumFacing.byIndex(metadata < 4 ? 2 + metadata : metadata < 8 ? 0 : 1))
				       .withProperty(BlockProperties.SPINNING, EnumFacing.byIndex(2 + (metadata % 4)));
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		final EnumFacing enumFacing = blockState.getValue(BlockProperties.FACING);
		switch (enumFacing) {
		case DOWN: return 4 + blockState.getValue(BlockProperties.SPINNING).getIndex();
		case UP: return 8 + blockState.getValue(BlockProperties.SPINNING).getIndex();
		default: return enumFacing.getIndex() - 2;
		}
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getActualState(@Nonnull final IBlockState blockState, final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityAbstractMachine) {
			return blockState
					       .withProperty(BlockProperties.ACTIVE, ((TileEntityAbstractMachine) tileEntity).isEnabled);
		} else {
			return blockState;
		}
	}
	
	@Nonnull
	@Override
	public IBlockState getStateForPlacement(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing, final float hitX, final float hitY, final float hitZ, final int metadata, @Nonnull final EntityLivingBase entityLivingBase, @Nonnull final EnumHand enumHand) {
		final IBlockState blockState = super.getStateForPlacement(world, blockPos, facing, hitX, hitY, hitZ, metadata, entityLivingBase, enumHand);
		if (!ignoreFacingOnPlacement) {
			if (blockState.getProperties().containsKey(BlockProperties.FACING)) {
				if (blockState.isFullBlock()) {
					final EnumFacing enumFacing = Commons.getFacingFromEntity(entityLivingBase);
					return blockState.withProperty(BlockProperties.FACING, enumFacing);
				} else {
					return blockState.withProperty(BlockProperties.FACING, facing);
				}
			}
			if (blockState.getProperties().containsKey(BlockProperties.FACING_HORIZONTAL)) {
				final EnumFacing enumFacing = Commons.getHorizontalDirectionFromEntity(entityLivingBase);
				if (blockState.isFullBlock()) {
					return blockState.withProperty(BlockProperties.FACING_HORIZONTAL, enumFacing.getOpposite());
				} else {
					return blockState.withProperty(BlockProperties.FACING_HORIZONTAL, enumFacing);
				}
			}
		}
		return blockState;
	}
}
