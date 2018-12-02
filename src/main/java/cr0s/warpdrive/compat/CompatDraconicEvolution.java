package cr0s.warpdrive.compat;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;

public class CompatDraconicEvolution implements IBlockTransformer {
	
	private static Class<?> classBlockBlockDE;
	private static Class<?> classBlockDraconiumBlock;
	private static Class<?> classBlockGenerator;
	private static Class<?> classBlockTeleporterStand;
	private static Class<?> classBlockPlacedItem;
	
	public static void register() {
		try {
			classBlockBlockDE = Class.forName("com.brandon3055.draconicevolution.common.blocks.BlockDE");
			classBlockDraconiumBlock = Class.forName("com.brandon3055.draconicevolution.common.blocks.DraconiumBlock");
			classBlockGenerator = Class.forName("com.brandon3055.draconicevolution.common.blocks.machine.Generator");
			classBlockPlacedItem = Class.forName("com.brandon3055.draconicevolution.common.blocks.PlacedItem");
			classBlockTeleporterStand = Class.forName("com.brandon3055.draconicevolution.common.blocks.TeleporterStand");
			WarpDriveConfig.registerBlockTransformer("DraconicEvolution", new CompatDraconicEvolution());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockBlockDE.isInstance(block);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		if ( classBlockDraconiumBlock.isInstance(block)
		  && metadata == 1) {
			reason.append(Commons.styleWarning, "warpdrive.compat.guide.ender_resurrection_ritual");
			return false;
		}
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
	
	/*
	Blacklisted for movement:
	DraconicEvolution:draconium@1
		TileEntityId = draconicevolution:TileEnderResurrection
		BlockClass = com.brandon3055.draconicevolution.common.blocks.DraconiumBlock
		Metadata = 0 is valid, consequently we can't use the dictionary for that one
	
	Whitelisted for movement:
	Generator
		metadata 1 2 3 0
	com.brandon3055.draconicevolution.common.tileentities.energynet.TileEnergyTransceiver
		int	Facing  0 1 5 3 4 2
		int	LinkCount
		int	X_LinkedDevice_0 Y_LinkedDevice_0 Z_LinkedDevice_0
	com.brandon3055.draconicevolution.common.tileentities.energynet.TileRemoteEnergyBase    (TileEnergyTransceiver, TileWirelessEnergyTransceiver & TileEnergyRelay)
		int	LinkCount
		int	X_LinkedDevice_0 Y_LinkedDevice_0 Z_LinkedDevice_0
	
	com.brandon3055.draconicevolution.common.tileentities.TilePlacedItem
		float Rotation  (x + 270.0F) % 360.0F only when metadata is 0 or 1
		metadata    0 1 5 3 4 2
	com.brandon3055.draconicevolution.common.tileentities.TileTeleporterStand
		int	Rotation (degrees?)
	com.brandon3055.draconicevolution.common.tileentities.TileDraconiumChest
		byte	facing  0 1 5 3 4 2
	com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorStabilizer
		int	Facing	0 1 5 3 4 2
		int	X_Master Y_Master Z_Master
	com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorEnergyInjector
		int	Facing	0 1 5 3 4 2
		int	X_Master Y_Master Z_Master
	com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore
		list	Stabilizers     (optional)
			int	X_tag	Y_tag	Z_tag
	com.brandon3055.draconicevolution.common.tileentities.TileParticleGenerator
		int	X_Key	Y_Key	Z_Key
	com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileInvisibleMultiblock
		int	X_Key	Y_Key	Z_Key
	com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileEnergyPylon
		int Cores
		int	X_Core0	Y_Core0	Z_Core0
	com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileEnergyStorageCore
		int	X_0	Y_0	Z_0 (optional)
		int	X_1	Y_1	Z_1 (optional)
		int	X_2	Y_2	Z_2 (optional)
		int	X_3	Y_3	Z_3 (optional)
	*/
	
	private static final int[]  rotRotation       = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]  rotGenerator      = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0 && nbtTileEntity == null) {
			return metadata;
		}
		
		// TileDraconiumChest rotation only has "facing" with no other field => return
		if (nbtTileEntity.hasKey("facing")) {
			final int facing = nbtTileEntity.getInteger("facing");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("facing", rotRotation[facing]);
				return metadata;
			case 2:
				nbtTileEntity.setInteger("facing", rotRotation[rotRotation[facing]]);
				return metadata;
			case 3:
				nbtTileEntity.setInteger("facing", rotRotation[rotRotation[rotRotation[facing]]]);
				return metadata;
			default:
				return metadata;
			}
		}
		
		// generator only has metadata with no other field => return
		if (classBlockGenerator.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return rotGenerator[metadata];
			case 2:
				return rotGenerator[rotGenerator[metadata]];
			case 3:
				return rotGenerator[rotGenerator[rotGenerator[metadata]]];
			default:
				return metadata;
			}
		}
		
		// generic tile entity rotations for TileEnergyTransceiver, TileReactorStabilizer, TileReactorEnergyInjector
		if (nbtTileEntity.hasKey("Facing")) {
			final int facing = nbtTileEntity.getInteger("Facing");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("Facing", rotRotation[facing]);
				break;
			case 2:
				nbtTileEntity.setInteger("Facing", rotRotation[rotRotation[facing]]);
				break;
			case 3:
				nbtTileEntity.setInteger("Facing", rotRotation[rotRotation[rotRotation[facing]]]);
				break;
			default:
				break;
			}
		}
		
		// generic linked devices for TileEnergyTransceiver and derived from TileRemoteEnergyBase (TileEnergyTransceiver, TileWirelessEnergyTransceiver & TileEnergyRelay)
		if (nbtTileEntity.hasKey("LinkCount")) {
			final int countLinks = nbtTileEntity.getInteger("LinkCount");
			if (countLinks > 0) {
				for (int indexLink = 0; indexLink < countLinks; indexLink++) {
					final BlockPos targetLink = transformation.apply(
						nbtTileEntity.getInteger(String.format("X_LinkedDevice_%d", indexLink)),
						nbtTileEntity.getInteger(String.format("Y_LinkedDevice_%d", indexLink)),
						nbtTileEntity.getInteger(String.format("Z_LinkedDevice_%d", indexLink)) );
					nbtTileEntity.setInteger(String.format("X_LinkedDevice_%d", indexLink), targetLink.getX());
					nbtTileEntity.setInteger(String.format("Y_LinkedDevice_%d", indexLink), targetLink.getY());
					nbtTileEntity.setInteger(String.format("Z_LinkedDevice_%d", indexLink), targetLink.getZ());
				}
			}
		}
		
		// generic link to master for TileReactorStabilizer and TileReactorEnergyInjector
		if (nbtTileEntity.hasKey("X_Master")) {
			final BlockPos targetLink = transformation.apply(
				nbtTileEntity.getInteger("X_Master"),
				nbtTileEntity.getInteger("Y_Master"),
				nbtTileEntity.getInteger("Z_Master") );
			nbtTileEntity.setInteger("X_Master", targetLink.getX());
			nbtTileEntity.setInteger("Y_Master", targetLink.getY());
			nbtTileEntity.setInteger("Z_Master", targetLink.getZ());
		}
		
		// generic link to master for TileParticleGenerator and TileInvisibleMultiblock
		if (nbtTileEntity.hasKey("X_Key")) {
			final BlockPos targetLink = transformation.apply(
				nbtTileEntity.getInteger("X_Key"),
				nbtTileEntity.getInteger("Y_Key"),
				nbtTileEntity.getInteger("Z_Key") );
			nbtTileEntity.setInteger("X_Key", targetLink.getX());
			nbtTileEntity.setInteger("Y_Key", targetLink.getY());
			nbtTileEntity.setInteger("Z_Key", targetLink.getZ());
		}
		
		// linked stabilizers for TileReactorCore
		if (nbtTileEntity.hasKey("Stabilizers")) {
			// we can't directly access it, hence removing then adding back later on
			final NBTTagList stabilizers = nbtTileEntity.getTagList("Stabilizers", Constants.NBT.TAG_COMPOUND);
			if (stabilizers != null) {
				for (int nodeIndex = 0; nodeIndex < stabilizers.tagCount(); nodeIndex++) {
					// remove
					final NBTTagCompound stabilizer = (NBTTagCompound) stabilizers.removeTag(0);
					// update coordinates
					final BlockPos target = transformation.apply(
						stabilizer.getInteger("X_tag"),
						stabilizer.getInteger("Y_tag"),
						stabilizer.getInteger("Z_tag"));
					stabilizer.setInteger("X_tag", target.getX());
					stabilizer.setInteger("Y_tag", target.getY());
					stabilizer.setInteger("Z_tag", target.getZ());
					// add
					stabilizers.appendTag(stabilizer);
				}
			}
		}
		
		// linked cores for TileEnergyPylon
		if (nbtTileEntity.hasKey("Cores")) {
			final int countCores = nbtTileEntity.getInteger("Cores");
			if (countCores > 0) {
				for (int indexCore = 0; indexCore < countCores; indexCore++) {
					final BlockPos targetLink = transformation.apply(
						nbtTileEntity.getInteger(String.format("X_Core%d", indexCore)),
						nbtTileEntity.getInteger(String.format("Y_Core%d", indexCore)),
						nbtTileEntity.getInteger(String.format("Z_Core%d", indexCore)) );
					nbtTileEntity.setInteger(String.format("X_Core%d", indexCore), targetLink.getX());
					nbtTileEntity.setInteger(String.format("Y_Core%d", indexCore), targetLink.getY());
					nbtTileEntity.setInteger(String.format("Z_Core%d", indexCore), targetLink.getZ());
				}
			}
		}
		
		// linked emitters for TileEnergyStorageCore
		if (nbtTileEntity.hasKey("X_0")) {
			final BlockPos targetLink = transformation.apply(
				nbtTileEntity.getInteger("X_0"),
				nbtTileEntity.getInteger("Y_0"),
				nbtTileEntity.getInteger("Z_0") );
			nbtTileEntity.setInteger("X_0", targetLink.getX());
			nbtTileEntity.setInteger("Y_0", targetLink.getY());
			nbtTileEntity.setInteger("Z_0", targetLink.getZ());
		}
		if (nbtTileEntity.hasKey("X_1")) {
			final BlockPos targetLink = transformation.apply(
				nbtTileEntity.getInteger("X_1"),
				nbtTileEntity.getInteger("Y_1"),
				nbtTileEntity.getInteger("Z_1") );
			nbtTileEntity.setInteger("X_1", targetLink.getX());
			nbtTileEntity.setInteger("Y_1", targetLink.getY());
			nbtTileEntity.setInteger("Z_1", targetLink.getZ());
		}
		if (nbtTileEntity.hasKey("X_2")) {
			final BlockPos targetLink = transformation.apply(
				nbtTileEntity.getInteger("X_2"),
				nbtTileEntity.getInteger("Y_2"),
				nbtTileEntity.getInteger("Z_2") );
			nbtTileEntity.setInteger("X_2", targetLink.getX());
			nbtTileEntity.setInteger("Y_2", targetLink.getY());
			nbtTileEntity.setInteger("Z_2", targetLink.getZ());
		}
		if (nbtTileEntity.hasKey("X_3")) {
			final BlockPos targetLink = transformation.apply(
				nbtTileEntity.getInteger("X_3"),
				nbtTileEntity.getInteger("Y_3"),
				nbtTileEntity.getInteger("Z_3") );
			nbtTileEntity.setInteger("X_3", targetLink.getX());
			nbtTileEntity.setInteger("Y_3", targetLink.getY());
			nbtTileEntity.setInteger("Z_3", targetLink.getZ());
		}
		
		// rotation for placed item only applies for top/bottom position
		if (classBlockPlacedItem.isInstance(block)) {
			if (rotationSteps > 0) {
				if (metadata == 0) {
					final float rotation = nbtTileEntity.getInteger("Rotation");
					nbtTileEntity.setFloat("Rotation", (rotation + rotationSteps * 90.0F) % 360.0F);
				} else if (metadata == 1) {
					final float rotation = nbtTileEntity.getInteger("Rotation");
					nbtTileEntity.setFloat("Rotation", (rotation + rotationSteps * 270.0F) % 360.0F);
				}
			}
			
			switch (rotationSteps) {
			case 1:
				return rotRotation[metadata];
			case 2:
				return rotRotation[rotRotation[metadata]];
			case 3:
				return rotRotation[rotRotation[rotRotation[metadata]]];
			default:
				return metadata;
			}
		}
		
		// rotation for dislocator stand / TileTeleporterStand
		if (classBlockTeleporterStand.isInstance(block)) {
			if (rotationSteps > 0) {
				final float rotation = nbtTileEntity.getInteger("Rotation");
				nbtTileEntity.setFloat("Rotation", (rotation + rotationSteps * 90.0F) % 360.0F);
			}
			return metadata;
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
