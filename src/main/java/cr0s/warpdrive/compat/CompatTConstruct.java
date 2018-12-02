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

public class CompatTConstruct implements IBlockTransformer {
	
	private static Class<?> classBlockDryingRack;
	private static Class<?> classBlockFaucet;
	private static Class<?> classBlockMultiblockController;
	private static Class<?> classBlockSmelteryIO;
	private static Class<?> classTileEntityChannel;
	
	public static void register() {
		try {
			classBlockDryingRack = Class.forName("slimeknights.tconstruct.gadgets.block.BlockRack");
			classBlockFaucet = Class.forName("slimeknights.tconstruct.smeltery.block.BlockFaucet");
			classBlockMultiblockController = Class.forName("slimeknights.tconstruct.smeltery.block.BlockMultiblockController");
			classBlockSmelteryIO = Class.forName("slimeknights.tconstruct.smeltery.block.BlockSmelteryIO"); // Smeltery Drain
			classTileEntityChannel = Class.forName("slimeknights.tconstruct.smeltery.tileentity.TileChannel");
			WarpDriveConfig.registerBlockTransformer("tconstruct", new CompatTConstruct());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockDryingRack.isInstance(block)
		    || classBlockFaucet.isInstance(block)
			|| classBlockMultiblockController.isInstance(block)
		    || classBlockSmelteryIO.isInstance(block)
		    || classTileEntityChannel.isInstance(tileEntity);
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
	
	//                                              0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final int[]  mrotDryingRack = { 14,  1,  6,  3,  8,  5,  4,  7,  2,  9, 12, 11, 10, 13,  0, 15 };
	private static final int[]  mrotFacing     = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		// metadata = 2 5 3 4 Drying rack
		if ( classBlockDryingRack.isInstance(block) ) {
			if (nbtTileEntity.hasKey("ForgeData")) {
				final NBTTagCompound nbtForgeData = nbtTileEntity.getCompoundTag("ForgeData");
				if (nbtForgeData.hasKey("facing")) {
					final int facing = nbtForgeData.getInteger("facing");
					switch (rotationSteps) {
					case 1:
						nbtForgeData.setInteger("facing", mrotFacing[facing]);
						break;
					case 2:
						nbtForgeData.setInteger("facing", mrotFacing[mrotFacing[metadata]]);
						break;
					case 3:
						nbtForgeData.setInteger("facing", mrotFacing[mrotFacing[mrotFacing[metadata]]]);
						break;
					default:
						break;
					}
				}
			}
			switch (rotationSteps) {
			case 1:
				return mrotDryingRack[metadata];
			case 2:
				return mrotDryingRack[mrotDryingRack[metadata]];
			case 3:
				return mrotDryingRack[mrotDryingRack[mrotDryingRack[metadata]]];
			default:
				return metadata;
			}
		}
		
		// metadata = 2 5 3 4 Faucet, Multiblock controller, Smeltery drain
		if ( classBlockFaucet.isInstance(block)
		  || classBlockMultiblockController.isInstance(block)
		  || classBlockSmelteryIO.isInstance(block) ) {
			switch (rotationSteps) {
			case 1:
				return mrotFacing[metadata];
			case 2:
				return mrotFacing[mrotFacing[metadata]];
			case 3:
				return mrotFacing[mrotFacing[mrotFacing[metadata]]];
			default:
				return metadata;
			}
		}
		
		// Channel connections 0 1 2 3
		if (nbtTileEntity.hasKey("connections")) {
			final byte[] bytesOldConnections = nbtTileEntity.getByteArray("connections");
			final byte[] bytesNewConnections = bytesOldConnections.clone();
			for (int sideOld = 0; sideOld < 4; sideOld++) {
				final byte byteConnection = bytesOldConnections[sideOld];
				bytesNewConnections[(sideOld + rotationSteps) % 4] = byteConnection;
			}
			nbtTileEntity.setByteArray("connections", bytesNewConnections);
		}
		
		return metadata;
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final IBlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		// nothing to do
	}
}
