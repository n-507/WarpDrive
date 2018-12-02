package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.block.breathing.BlockAirFlow;
import cr0s.warpdrive.block.breathing.BlockAirGeneratorTiered;
import cr0s.warpdrive.block.breathing.BlockAirSource;
import cr0s.warpdrive.block.decoration.BlockAbstractLamp;
import cr0s.warpdrive.block.detection.BlockMonitor;
import cr0s.warpdrive.block.forcefield.BlockForceFieldProjector;
import cr0s.warpdrive.block.hull.BlockHullSlab;
import cr0s.warpdrive.block.movement.BlockShipCore;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.ChunkData;
import cr0s.warpdrive.data.StateAir;
import cr0s.warpdrive.event.ChunkHandler;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CompatWarpDrive implements IBlockTransformer {
	
	public static void register() {
		WarpDriveConfig.registerBlockTransformer("WarpDrive", new CompatWarpDrive());
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return block instanceof BlockHullSlab
		    || block instanceof BlockAbstractBase
		    || block instanceof BlockAbstractContainer;
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		return true;
	}
	
	@Override
	public NBTBase saveExternals(final World world, final int x, final int y, final int z,
	                             final Block block, final int blockMeta, final TileEntity tileEntity) {
		if (block instanceof BlockAirFlow || block instanceof BlockAirSource) {
			final ChunkData chunkData = ChunkHandler.getChunkData(world, x, y, z);
			if (chunkData == null) {
				// chunk isn't loaded, skip it
				return null;
			}
			final int dataAir = chunkData.getDataAir(x, y, z);
			if (dataAir == StateAir.AIR_DEFAULT) {
				return null;
			}
			final NBTTagCompound tagCompound = new NBTTagCompound();
			tagCompound.setInteger("dataAir", dataAir);
			return tagCompound;
		}
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		if (block instanceof BlockAirFlow || block instanceof BlockAirSource) {
			final ChunkData chunkData = ChunkHandler.getChunkData(world, x, y, z);
			if (chunkData == null) {
				// chunk isn't loaded, skip it
				return;
			}
			chunkData.setDataAir(x, y, z, StateAir.AIR_DEFAULT);
		}
	}
	
	private static final short[] mrotDirection = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final short[] mrotHullSlab  = {  0,  1,  5,  4,  2,  3,  6,  7, 11, 10,  8,  9, 12, 13, 15, 14 };
	// cloaking core will refresh itself
	
	private byte[] rotate_byteArray(final byte rotationSteps, final byte[] data) {
		final byte[] newData = data.clone();
		for (int index = 0; index < data.length; index++) {
			switch (rotationSteps) {
			case 1:
				newData[mrotDirection[index]] = data[index];
				break;
			case 2:
				newData[mrotDirection[mrotDirection[index]]] = data[index];
				break;
			case 3:
				newData[mrotDirection[mrotDirection[mrotDirection[index]]]] = data[index];
				break;
			default:
				break;
			}
		}
		return newData;
	}
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		
		// Hull slabs
		if (block instanceof BlockHullSlab) {
			switch (rotationSteps) {
			case 1:
				return mrotHullSlab[metadata];
			case 2:
				return mrotHullSlab[mrotHullSlab[metadata]];
			case 3:
				return mrotHullSlab[mrotHullSlab[mrotHullSlab[metadata]]];
			default:
				return metadata;
			}
		}
		
		// Monitor and lamps
		if ( block instanceof BlockAirSource
		  || block instanceof BlockMonitor
		  || block instanceof BlockAbstractLamp ) {
			switch (rotationSteps) {
			case 1:
				return mrotDirection[metadata];
			case 2:
				return mrotDirection[mrotDirection[metadata]];
			case 3:
				return mrotDirection[mrotDirection[mrotDirection[metadata]]];
			default:
				return metadata;
			}
		}
		
		// Force field projector
		if ( block instanceof BlockAirGeneratorTiered
		  || block instanceof BlockForceFieldProjector
		  || block instanceof BlockShipCore ) {
			switch (rotationSteps) {
			case 1:
				return mrotDirection[metadata & 0x7] | (metadata & 0x8);
			case 2:
				return mrotDirection[mrotDirection[metadata & 0x7]] | (metadata & 0x8);
			case 3:
				return mrotDirection[mrotDirection[mrotDirection[metadata & 0x7]]] | (metadata & 0x8);
			default:
				return metadata;
			}
		}
		
		// subspace capacitor sides
		if (nbtTileEntity != null && nbtTileEntity.hasKey("modeSide")) {
			nbtTileEntity.setByteArray("modeSide", rotate_byteArray(rotationSteps, nbtTileEntity.getByteArray("modeSide")));
		}
		return metadata;
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final IBlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		if (nbtBase == null) {
			return;
		}
		if (!(nbtBase instanceof NBTTagCompound)) {
			return;
		}
		if (((NBTTagCompound) nbtBase).hasKey("dataAir")) {
			final byte rotationSteps = transformation.getRotationSteps();
			final int dataAir = ((NBTTagCompound) nbtBase).getInteger("dataAir");
			final ChunkData chunkData = ChunkHandler.getChunkData(world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
			if (chunkData == null) {
				// chunk isn't loaded, skip it
				return;
			}
			final int dataAirRotated = StateAir.rotate(dataAir, rotationSteps);
			chunkData.setDataAir(blockPos.getX(), blockPos.getY(), blockPos.getZ(), dataAirRotated);
		}
	}
}
