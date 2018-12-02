package cr0s.warpdrive.compat;

import blusunrize.immersiveengineering.api.energy.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.Connection;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.Collection;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fml.common.Optional;

public class CompatImmersiveEngineering implements IBlockTransformer {
	
	private static Class<?> classTileEntityIEBase;
	
	public static void register() {
		try {
			classTileEntityIEBase = Class.forName("blusunrize.immersiveengineering.common.blocks.TileEntityIEBase");
			WarpDriveConfig.registerBlockTransformer("immersiveengineering", new CompatImmersiveEngineering());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return tileEntity instanceof IImmersiveConnectable || classTileEntityIEBase.isInstance(tileEntity);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		return true;
	}
	
	@Override
	@Optional.Method(modid = "immersiveengineering")
	public NBTBase saveExternals(final World world, final int x, final int y, final int z, final Block block, final int blockMeta, final TileEntity tileEntity) {
		if (tileEntity instanceof IImmersiveConnectable) {
			final BlockPos node = tileEntity.getPos();
			final Collection<Connection> connections = ImmersiveNetHandler.INSTANCE.getConnections(tileEntity.getWorld(), node);
			if (connections != null) {
				final NBTTagList nbtImmersiveEngineering = new NBTTagList();
				for (final Connection connection : connections) {
					nbtImmersiveEngineering.appendTag(connection.writeToNBT());
				}
				ImmersiveNetHandler.INSTANCE.clearConnectionsOriginatingFrom(node, tileEntity.getWorld());
				return nbtImmersiveEngineering;
			}
		}
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
	}
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0 || !nbtTileEntity.hasKey("facing")) {
			return metadata;
		}
		
		final int facing = nbtTileEntity.getInteger("facing");
		final int[] mrot = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
		switch (rotationSteps) {
		case 1:
			nbtTileEntity.setInteger("facing", mrot[facing]);
			return metadata;
		case 2:
			nbtTileEntity.setInteger("facing", mrot[mrot[facing]]);
			return metadata;
		case 3:
			nbtTileEntity.setInteger("facing", mrot[mrot[mrot[facing]]]);
			return metadata;
		default:
			return metadata;
		}
	}
	
	@Override
	@Optional.Method(modid = "immersiveengineering")
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final IBlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		final NBTTagList nbtImmersiveEngineering = (NBTTagList) nbtBase;
		if (nbtImmersiveEngineering == null) {
			return;
		}
		final World targetWorld = transformation.getTargetWorld();
		
		// powerPathList
		for (int indexConnectionToAdd = 0; indexConnectionToAdd < nbtImmersiveEngineering.tagCount(); indexConnectionToAdd++) {
			final Connection connectionToAdd = Connection.readFromNBT(nbtImmersiveEngineering.getCompoundTagAt(indexConnectionToAdd));
			connectionToAdd.start = transformation.apply(connectionToAdd.start);
			connectionToAdd.end = transformation.apply(connectionToAdd.end);
			final BlockPos node = tileEntity.getPos();
			final Collection<Connection> connectionActuals = ImmersiveNetHandler.INSTANCE.getConnections(tileEntity.getWorld(), node);
			boolean existing = false;
			if (connectionActuals != null) {
				for (final Connection connectionActual : connectionActuals) {
					if ( connectionActual.start.equals(connectionToAdd.start)
					  && connectionActual.end.equals(connectionToAdd.end) ) {
						existing = true;
						break;
					} else if (
					     connectionActual.start.equals(connectionToAdd.end)
					  && connectionActual.end.equals(connectionToAdd.start) ) {
						existing = true;
						break;
					}
				}
			}
			if (!existing) {
				ImmersiveNetHandler.INSTANCE.addConnection(targetWorld, connectionToAdd.start, connectionToAdd);
		}
		}
	}
}
