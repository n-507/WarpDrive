package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockCloakingCoil extends BlockAbstractBase {
	
	// Metadata values
	// 0 = not linked
	// 1 = inner coil passive
	// 2-7 = outer coil passive
	// 8 = (not used)
	// 9 = inner coil active
	// 10-15 = outer coil active
	
	public static final PropertyBool OUTER = PropertyBool.create("outer");
	
	public BlockCloakingCoil(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setHardness(3.5F);
		setTranslationKey("warpdrive.detection.cloaking_coil");
		
		setDefaultState(getDefaultState()
				                .withProperty(BlockProperties.ACTIVE, false)
				                .withProperty(OUTER, false)
				                .withProperty(BlockProperties.FACING, EnumFacing.DOWN)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockProperties.ACTIVE, OUTER, BlockProperties.FACING);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		// 15 = not used
		// 9-14 = active, outer facing
		// 8 = active, inner
		// 7 = not used
		// 1-6 = outer facing
		// 0 = inner
		final boolean isActive = (metadata & 0x8) != 0;
		final boolean isOuter = (metadata & 0x7) > 0;
		return getDefaultState()
				.withProperty(BlockProperties.ACTIVE, isActive)
				.withProperty(OUTER, isOuter)
				.withProperty(BlockProperties.FACING, isOuter ? EnumFacing.byIndex((metadata & 0x7) - 1) : EnumFacing.DOWN);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return (blockState.getValue(BlockProperties.ACTIVE) ? 8 : 0)
		     + (blockState.getValue(OUTER) ? 1 + blockState.getValue(BlockProperties.FACING).ordinal() : 0);
	}
	
	public static void setBlockState(@Nonnull final World world, @Nonnull final BlockPos blockPos, final boolean isActive, final boolean isOuter, final EnumFacing enumFacing) {
		final IBlockState blockStateActual = world.getBlockState(blockPos);
		IBlockState blockStateNew = blockStateActual.withProperty(BlockProperties.ACTIVE, isActive).withProperty(OUTER, isOuter);
		if (enumFacing != null) {
			blockStateNew = blockStateNew.withProperty(BlockProperties.FACING, enumFacing);
		}
		if (blockStateActual.getBlock().getMetaFromState(blockStateActual) != blockStateActual.getBlock().getMetaFromState(blockStateNew)) {
			world.setBlockState(blockPos, blockStateNew);
		}
	}
}
