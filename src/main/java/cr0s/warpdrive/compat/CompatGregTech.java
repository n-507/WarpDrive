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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CompatGregTech implements IBlockTransformer {
	
	private static Class<?> classMetaTileEntityHolder;
	private static Class<?> classTileEntityPipeBase;
	
	public static void register() {
		try {
			classMetaTileEntityHolder = Class.forName("gregtech.api.metatileentity.MetaTileEntityHolder");
			classTileEntityPipeBase   = Class.forName("gregtech.api.pipenet.tile.TileEntityPipeBase");
			
			WarpDriveConfig.registerBlockTransformer("gregtech", new CompatGregTech());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classMetaTileEntityHolder.isInstance(tileEntity)
			|| classTileEntityPipeBase.isInstance(tileEntity);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		return true;
	}
	
	@Override
	public NBTBase saveExternals(final World world, final int x, final int y, final int z,
	                             final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
	}
	
	/*
	As of gregtech-1.12.2-1.0.124
	gregtech.api.pipenet.tile.TileEntityPipeBase (cables and fluid pipes)
	    BlockedConnections int (1 << side.func_176745_a())
	gregtech.api.metatileentity.MetaTileEntityHolder (machine)
		MetaTileEntity.FrontFacing int 2 5 3 4
	*/
	
	// -----------------------------------------    {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   rotFacing        = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0 || nbtTileEntity == null) {
			return metadata;
		}
		
		// BlockedConnections int (1 << side.func_176745_a())
		if (nbtTileEntity.hasKey("BlockedConnections")) {// @TODO: to be tested once GTCE implements it for players
			final int blockedConnectionsOld = nbtTileEntity.getInteger("BlockedConnections");
			int blockedConnectionsNew = blockedConnectionsOld & 0x3; // keep bottom and top
			for (final EnumFacing enumFacing : EnumFacing.HORIZONTALS) {
				// get state for current face
				final int indexFacingOld = enumFacing.getIndex();
				final boolean isBlocked = (blockedConnectionsOld & (1 << indexFacingOld)) != 0;
				if (!isBlocked) {
					continue;
				}
				// get new face
				final int indexFacingNew;
				switch (rotationSteps) {
				case 1:
					indexFacingNew = rotFacing[indexFacingOld];
					break;
				case 2:
					indexFacingNew = rotFacing[rotFacing[indexFacingOld]];
					break;
				case 3:
					indexFacingNew = rotFacing[rotFacing[rotFacing[indexFacingOld]]];
					break;
				default:
					indexFacingNew = indexFacingOld;
					break;
				}
				// apply state on new face
				blockedConnectionsNew |= (1 << indexFacingNew);
			}
			nbtTileEntity.setInteger("BlockedConnections", blockedConnectionsNew);
		}
		
		// MetaTileEntity.FrontFacing int 2 5 3 4
		if (nbtTileEntity.hasKey("MetaTileEntity")) {
			final NBTTagCompound tagCompoundMetaTileEntity = nbtTileEntity.getCompoundTag("MetaTileEntity");
			if (tagCompoundMetaTileEntity.hasKey("FrontFacing")) {
				final int frontFacing = tagCompoundMetaTileEntity.getInteger("FrontFacing");
				switch (rotationSteps) {
				case 1:
					tagCompoundMetaTileEntity.setInteger("FrontFacing", rotFacing[frontFacing]);
					break;
				case 2:
					tagCompoundMetaTileEntity.setInteger("FrontFacing", rotFacing[rotFacing[frontFacing]]);
					break;
				case 3:
					tagCompoundMetaTileEntity.setInteger("FrontFacing", rotFacing[rotFacing[rotFacing[frontFacing]]]);
					break;
				default:
					break;
				}
			}
		}
		
		return metadata;
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final IBlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		// refresh client @TODO
		// (not working in SSP) world.notifyBlockUpdate(blockPos, blockState, blockState, 2);
	}
}
