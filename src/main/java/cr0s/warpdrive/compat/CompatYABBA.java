package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CompatYABBA implements IBlockTransformer {
	
	private static Class<?> classBlockAdvancedBarrelBase;
	private static Class<?> classBlockDecorativeBlock;
	
	public static void register() {
		try {
			classBlockAdvancedBarrelBase = Class.forName("com.latmod.yabba.block.BlockAdvancedBarrelBase");
			classBlockDecorativeBlock = Class.forName("com.latmod.yabba.block.BlockDecorativeBlock");
			
			WarpDriveConfig.registerBlockTransformer("YABBA", new CompatYABBA());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockAdvancedBarrelBase.isInstance(block)
		    || classBlockDecorativeBlock.isInstance(block);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		return true;
	}
	
	@Override
	public NBTBase saveExternals(final World world, final int x, final int y, final int z, final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
	}
	
	//                                               0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final byte[] rotHorizontal   = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		// barrels
		switch (rotationSteps) {
		case 1:
			return rotHorizontal[metadata];
		case 2:
			return rotHorizontal[rotHorizontal[metadata]];
		case 3:
			return rotHorizontal[rotHorizontal[rotHorizontal[metadata]]];
		default:
			return metadata;
		}
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final IBlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		// nothing to do
	}
}
