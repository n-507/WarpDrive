package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.FastSetBlockState;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.computer.ICoreSignature;
import cr0s.warpdrive.block.energy.BlockCapacitor;
import cr0s.warpdrive.block.movement.BlockShipCore;
import cr0s.warpdrive.compat.CompatForgeMultipart;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.Filler;
import cr0s.warpdrive.config.WarpDriveDataFixer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockVine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.Constants.NBT;

public class JumpBlock {
	
	public Block block;
	public int blockMeta;
	public WeakReference<TileEntity> weakTileEntity;
	public NBTTagCompound blockNBT;
	public int x;
	public int y;
	public int z;
	public HashMap<String, NBTBase> externals;
	
	public JumpBlock() {
	}
	
	public JumpBlock(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState, @Nullable final TileEntity tileEntity) {
		this.x = blockPos.getX();
		this.y = blockPos.getY();
		this.z = blockPos.getZ();
		this.block = blockState.getBlock();
		this.blockMeta = blockState.getBlock().getMetaFromState(blockState);
		if (tileEntity == null) {
			weakTileEntity = null;
			blockNBT = null;
		} else {
			weakTileEntity = new WeakReference<>(tileEntity);
			blockNBT = new NBTTagCompound();
			tileEntity.writeToNBT(blockNBT);
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("Saving from (%d %d %d) with TileEntity %s",
				                                    x, y, z, blockNBT));
			}
		}
		
		// save externals
		for (final Entry<String, IBlockTransformer> entryBlockTransformer : WarpDriveConfig.blockTransformers.entrySet()) {
			if (entryBlockTransformer.getValue().isApplicable(block, blockMeta, tileEntity)) {
				final NBTBase nbtBase = entryBlockTransformer.getValue().saveExternals(world, x, y, z, block, blockMeta, tileEntity);
				// (we always save, even if null as a reminder on which transformer applies to this block)
				setExternal(entryBlockTransformer.getKey(), nbtBase);
			}
		}
	}
	
	public JumpBlock(@Nonnull final Filler filler, final int x, final int y, final int z) {
		if (filler.block == null) {
			WarpDrive.logger.info(String.format("Forcing glass for invalid filler with null block at (%d %d %d)",
			                                    x, y, z));
			filler.block = Blocks.GLASS;
		}
		block = filler.block;
		blockMeta = filler.metadata;
		weakTileEntity = null;
		blockNBT = (filler.tagCompound != null) ? filler.tagCompound.copy() : null;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public TileEntity getTileEntity(@Nonnull final World worldSource) {
		if (weakTileEntity == null) {
			return null;
		}
		final TileEntity tileEntity = weakTileEntity.get();
		if (tileEntity != null) {
			return tileEntity;
		}
		WarpDrive.logger.error(String.format("Tile entity lost in %s",
		                                     this));
		return worldSource.getTileEntity(new BlockPos(x, y, z));
	}
	
	private NBTTagCompound getBlockNBT(@Nonnull final World worldSource) {
		if (weakTileEntity == null) {
			return blockNBT == null ? null : blockNBT.copy();
		}
		TileEntity tileEntity = weakTileEntity.get();
		if (tileEntity == null) {
			tileEntity = worldSource.getTileEntity(new BlockPos(x, y, z));
			if (tileEntity == null) {
				WarpDrive.logger.error(String.format("Tile entity lost in %s",
				                                     this));
				return blockNBT == null ? null : blockNBT.copy();
			}
		}
		final NBTTagCompound tagCompound = new NBTTagCompound();
		tileEntity.writeToNBT(tagCompound);
		return tagCompound;
	}
	
	public NBTBase getExternal(final String modId) {
		if (externals == null) {
			return null;
		}
		final NBTBase nbtExternal = externals.get(modId);
		if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
			WarpDrive.logger.info(String.format("Returning externals from (%d %d %d) of %s: %s",
			                                    x, y, z, modId, nbtExternal));
		}
		if (nbtExternal == null) {
			return null;
		}
		return nbtExternal.copy();
	}
	
	private void setExternal(final String modId, final NBTBase nbtExternal) {
		if (externals == null) {
			externals = new HashMap<>();
		}
		externals.put(modId, nbtExternal);
		if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
			WarpDrive.logger.info(String.format("Saved externals from (%d %d %d) of %s: %s",
			                                    x, y, z, modId, nbtExternal));
		}
	}
	
	private static final byte[] mrotNone           = {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotRail           = {  1,  0,  5,  4,  2,  3,  7,  8,  9,  6, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotAnvil          = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 12, 13, 14, 15 };
	private static final byte[] mrotFenceGate      = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 13, 14, 15, 12 };
	private static final byte[] mrotPumpkin        = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };	// Tripwire hook, Pumpkin, Jack-o-lantern
	private static final byte[] mrotEndPortalFrame = {  1,  2,  3,  0,  5,  6,  7,  4,  8,  9, 10, 11, 12, 13, 14, 15 };	// EndPortal, doors (open/closed, base/top)
	private static final byte[] mrotCocoa          = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 12, 13, 14, 15 };
	private static final byte[] mrotRepeater       = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 13, 14, 15, 12 };	// Repeater (normal/lit), Comparator
	private static final byte[] mrotBed            = {  1,  2,  3,  0,  4,  5,  6,  7,  9, 10, 11,  8, 12, 13, 14, 15 };
	private static final byte[] mrotStair          = {  2,  3,  1,  0,  6,  7,  5,  4,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotSign           = {  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15,  0,  1,  2,  3 };	// Sign, Skull
	private static final byte[] mrotTrapDoor       = {  3,  2,  0,  1,  7,  6,  4,  5, 11, 10,  8,  9, 15, 14, 12, 13 };
	private static final byte[] mrotLever          = {  7,  3,  4,  2,  1,  6,  5,  0, 15, 11, 12, 10,  9, 14, 13,  8 };
	private static final byte[] mrotNetherPortal   = {  0,  2,  1,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotVine           = {  0,  2,  4,  6,  8, 10, 12, 14,  1,  3,  5,  7,  9, 11, 13, 15 };
	private static final byte[] mrotButton         = {  0,  3,  4,  2,  1,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };	// Button, torch (normal, redstone lit/unlit)
	private static final byte[] mrotMushroom       = {  0,  3,  6,  9,  2,  5,  8,  1,  4,  7, 10, 11, 12, 13, 14, 15 };	// Red/brown mushroom block
	private static final byte[] mrotForgeDirection = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };	// Furnace (lit/normal), Dispenser/Dropper, Enderchest, Chest (normal/trapped), Hopper, Ladder, Wall sign
	private static final byte[] mrotPiston         = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 13, 12, 10, 11, 14, 15 };	// Pistons (sticky/normal, base/head)
	private static final byte[] mrotWoodLog        = {  0,  1,  2,  3,  8,  9, 10, 11,  4,  5,  6,  7, 12, 13, 14, 15 };
	
	// Return updated metadata from rotating a vanilla block
	private int getMetadataRotation(final NBTTagCompound nbtTileEntity, final byte rotationSteps) {
		if (rotationSteps == 0) {
			return blockMeta;
		}
		
		byte[] mrot = mrotNone;
		if (block instanceof BlockRailBase) {
			mrot = mrotRail;
		} else if (block instanceof BlockAnvil) {
			mrot = mrotAnvil;
		} else if (block instanceof BlockFenceGate) {
			mrot = mrotFenceGate;
		} else if (block instanceof BlockPumpkin || block instanceof BlockTripWireHook) {
			mrot = mrotPumpkin;
		} else if (block instanceof BlockEndPortalFrame || block instanceof BlockDoor) {
			mrot = mrotEndPortalFrame;
		} else if (block instanceof BlockCocoa) {
			mrot = mrotCocoa;
		} else if (block instanceof BlockRedstoneDiode) {
			mrot = mrotRepeater;
		} else if (block instanceof BlockBed) {
			mrot = mrotBed;
		} else if (block instanceof BlockStairs) {
			mrot = mrotStair;
		} else if (block instanceof BlockSign) {
			if (block == Blocks.WALL_SIGN) {
				mrot = mrotForgeDirection;
			} else {
				mrot = mrotSign;
			}
		} else if (block instanceof BlockTrapDoor) {
			mrot = mrotTrapDoor;
		} else if (block instanceof BlockLever) {
			mrot = mrotLever;
		} else if (block instanceof BlockPortal) {
			mrot = mrotNetherPortal;
		} else if (block instanceof BlockVine) {
			mrot = mrotVine;
		} else if (block instanceof BlockButton || block instanceof BlockTorch) {
			mrot = mrotButton;
		} else if (block instanceof BlockHugeMushroom) {
			mrot = mrotMushroom;
		} else if (block instanceof BlockFurnace || block instanceof BlockDispenser || block instanceof BlockHopper
				|| block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockLadder) {
			mrot = mrotForgeDirection;
		} else if (block instanceof BlockPistonBase || block instanceof BlockPistonExtension || block instanceof BlockPistonMoving) {
			mrot = mrotPiston;
		} else if (block instanceof BlockLog) {
			mrot = mrotWoodLog;
		} else if (block instanceof BlockSkull) {
			// mrot = mrotNone;
			final short facing = nbtTileEntity.getShort("Rot");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setShort("Rot", mrotSign[facing]);
				break;
			case 2:
				nbtTileEntity.setShort("Rot", mrotSign[mrotSign[facing]]);
				break;
			case 3:
				nbtTileEntity.setShort("Rot", mrotSign[mrotSign[mrotSign[facing]]]);
				break;
			default:
				break;
			}
		} else {
			// apply default transformer
			return IBlockTransformer.rotateFirstEnumFacingProperty(block, blockMeta, rotationSteps);
		}
		
		switch (rotationSteps) {
		case 1:
			return mrot[blockMeta];
		case 2:
			return mrot[mrot[blockMeta]];
		case 3:
			return mrot[mrot[mrot[blockMeta]]];
		default:
			return blockMeta;
		}
	}
	
	public BlockPos deploy(final World worldSource, final World worldTarget, final ITransformation transformation) {
		try {
			final NBTTagCompound nbtToDeploy = getBlockNBT(worldSource);
			int newBlockMeta = blockMeta;
			if (externals != null) {
				for (final Entry<String, NBTBase> external : externals.entrySet()) {
					final IBlockTransformer blockTransformer = WarpDriveConfig.blockTransformers.get(external.getKey());
					if (blockTransformer != null) {
						newBlockMeta = blockTransformer.rotate(block, blockMeta, nbtToDeploy, transformation);
					}
				}
			} else {
				newBlockMeta = getMetadataRotation(nbtToDeploy, transformation.getRotationSteps());
			}
			final BlockPos target = transformation.apply(x, y, z);
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("Deploying to (%d %d %d) of %s@%d: %s",
				                                    target.getX(), target.getY(), target.getZ(),
				                                    block, newBlockMeta, nbtToDeploy));
			}
			final IBlockState blockState = block.getStateFromMeta(newBlockMeta);
			FastSetBlockState.setBlockStateNoLight(worldTarget, target, blockState, 2);
			
			if (nbtToDeploy != null) {
				nbtToDeploy.setInteger("x", target.getX());
				nbtToDeploy.setInteger("y", target.getY());
				nbtToDeploy.setInteger("z", target.getZ());
				
				if (nbtToDeploy.hasKey("screenData")) {// IC2NuclearControl 2.2.5a
					final NBTTagCompound nbtScreenData = nbtToDeploy.getCompoundTag("screenData");
					if ( nbtScreenData.hasKey("minX") && nbtScreenData.hasKey("minY") && nbtScreenData.hasKey("minZ")
					  && nbtScreenData.hasKey("maxX") && nbtScreenData.hasKey("maxY") && nbtScreenData.hasKey("maxZ") ) {
						if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
							WarpDrive.logger.info(String.format("%s deploy: TileEntity has screenData.min/maxXYZ", this));
						}
						final BlockPos minTarget = transformation.apply(nbtScreenData.getInteger("minX"), nbtScreenData.getInteger("minY"), nbtScreenData.getInteger("minZ"));
						nbtScreenData.setInteger("minX", minTarget.getX());
						nbtScreenData.setInteger("minY", minTarget.getY());
						nbtScreenData.setInteger("minZ", minTarget.getZ());
						final BlockPos maxTarget = transformation.apply(nbtScreenData.getInteger("maxX"), nbtScreenData.getInteger("maxY"), nbtScreenData.getInteger("maxZ"));
						nbtScreenData.setInteger("maxX", maxTarget.getX());
						nbtScreenData.setInteger("maxY", maxTarget.getY());
						nbtScreenData.setInteger("maxZ", maxTarget.getZ());
						nbtToDeploy.setTag("screenData", nbtScreenData);
					}
				}
				
				TileEntity newTileEntity = null;
				boolean isForgeMultipart = false;
				if ( WarpDriveConfig.isForgeMultipartLoaded
				  && nbtToDeploy.hasKey("id")
				  && nbtToDeploy.getString("id").equals("savedMultipart") ) {
					isForgeMultipart = true;
					newTileEntity = (TileEntity) CompatForgeMultipart.methodMultipartHelper_createTileFromNBT.invoke(null, worldTarget, nbtToDeploy);
				}
				
				if (newTileEntity == null) {
					newTileEntity = TileEntity.create(worldTarget, nbtToDeploy);
					if (newTileEntity == null) {
						WarpDrive.logger.error(String.format("%s deploy failed to create new tile entity %s block %s:%d",
						                                     this, Commons.format(worldTarget, x, y, z), block, blockMeta));
						WarpDrive.logger.error(String.format("NBT data was %s",
						                                     nbtToDeploy));
					}
				}
				
				if (newTileEntity != null) {
					worldTarget.setTileEntity(target, newTileEntity);
					if (isForgeMultipart) {
						CompatForgeMultipart.methodTileMultipart_onChunkLoad.invoke(newTileEntity);
						CompatForgeMultipart.methodMultipartHelper_sendDescPacket.invoke(null, worldTarget, newTileEntity);
					}
					
					newTileEntity.markDirty();
				}
			}
			return target;
			
		} catch (final Exception exception) {
			exception.printStackTrace();
			WarpDrive.logger.error(String.format("Deploy failed from (%d %d %d) of %s:%d",
			                                     x, y, z, block, blockMeta));
		}
		return null;
	}
	
	public static void refreshBlockStateOnClient(@Nonnull final World world, @Nonnull final BlockPos blockPos) {
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity != null) {
			final Class<?> teClass = tileEntity.getClass();
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("Refreshing clients %s with %s derived from %s",
				                                    Commons.format(world, blockPos),
				                                    teClass,
				                                    teClass.getSuperclass()));
			}
			final String className = teClass.getName();
			try {
				final String superClassName = teClass.getSuperclass().getName();
				final boolean isIC2 = superClassName.contains("ic2.core.block");
				if (isIC2 || superClassName.contains("advsolar.common.tiles")) {// IC2
					final Method onUnloaded = teClass.getMethod("onUnloaded");
					final Method onLoaded = teClass.getMethod("onLoaded");
					if (onUnloaded != null && onLoaded != null) {
						onUnloaded.invoke(tileEntity);
						onLoaded.invoke(tileEntity);
					} else {
						WarpDrive.logger.error(String.format("Missing IC2 (un)loaded events for TileEntity %s %s. Please report this issue!",
						                                     className,
						                                     Commons.format(world, blockPos)));
					}
					
					tileEntity.updateContainingBlockInfo();
				}
				
				if (isIC2) {// IC2
					// required in SSP during same dimension jump to update client with rotation data
					if (className.equals("ic2.core.block.wiring.TileEntityCable")) {
						NetworkHelper_updateTileEntityField(tileEntity, "color");
						NetworkHelper_updateTileEntityField(tileEntity, "foamColor");
						NetworkHelper_updateTileEntityField(tileEntity, "foamed");
					} else {
						NetworkHelper_updateTileEntityField(tileEntity, "active");
						NetworkHelper_updateTileEntityField(tileEntity, "facing");
						if (className.equals("ic2.core.block.reactor.TileEntityNuclearReactorElectric")) {
							NetworkHelper_updateTileEntityField(tileEntity, "heat");	// not working, probably an IC2 bug here...
						}
						// not needed: if ic2.core.block.machine.tileentity.TileEntityMatter then updated "state"
					}
				} else if ( !className.contains("$")
				         && !superClassName.contains("$")
				         && !superClassName.startsWith("mekanism.")
				         && !superClassName.startsWith("micdoodle8.") ) {// IC2 extensions without network optimization (transferring all fields)
					try {
						final Method getNetworkedFields = teClass.getMethod("getNetworkedFields");
						@SuppressWarnings("unchecked")
						final List<String> fields = (List<String>) getNetworkedFields.invoke(tileEntity);
						if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
							WarpDrive.logger.info(String.format("Tile has %d networked fields: %s",
							                                    fields.size(), fields));
						}
						for (final String field : fields) {
							NetworkHelper_updateTileEntityField(tileEntity, field);
						}
					} catch (final NoSuchMethodException exception) {
						// WarpDrive.logger.info("Tile has no getNetworkedFields method");
					} catch (final NoClassDefFoundError exception) {
						if (WarpDriveConfig.LOGGING_JUMP) {
							WarpDrive.logger.info(String.format("TileEntity %s %s is missing a class definition",
							                                    className, Commons.format(world, blockPos)));
							if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
								exception.printStackTrace();
							}
						}
					}
				}
			} catch (final Exception exception) {
				WarpDrive.logger.info(String.format("Exception involving TileEntity %s %s",
				                                    className, Commons.format(world, blockPos)));
				exception.printStackTrace();
			}
		}
	}
	
	public void readFromNBT(final NBTTagCompound tagCompound) {
		final String blockName = tagCompound.getString("block");
		blockMeta = tagCompound.getByte("blockMeta");
		final String stringBlockState = String.format("%s@%d", blockName, blockMeta);
		final IBlockState blockState = WarpDriveDataFixer.getFixedBlockState(stringBlockState);
		if (blockState == null) {
			if (WarpDriveConfig.LOGGING_BUILDING) {
				WarpDrive.logger.warn(String.format("Ignoring unknown blockstate %s from tag %s, consider updating your warpdrive/dataFixer.yml",
				                                    stringBlockState, tagCompound));
			}
			block = Blocks.AIR;
			return;
		}
		block = blockState.getBlock();
		blockMeta = blockState.getBlock().getMetaFromState(blockState);
		
		weakTileEntity = null;
		if (tagCompound.hasKey("blockNBT")) {
			blockNBT = tagCompound.getCompoundTag("blockNBT");
			
			// Clear computer IDs
			if (blockNBT.hasKey("computerID")) {
				blockNBT.removeTag("computerID");
			}
			if (blockNBT.hasKey("oc:computer")) {
				final NBTTagCompound tagComputer = blockNBT.getCompoundTag("oc:computer");
				tagComputer.removeTag("components");
				tagComputer.removeTag("node");
				blockNBT.setTag("oc:computer", tagComputer);
			}
		} else {
			blockNBT = null;
		}
		x = tagCompound.getInteger("x");
		y = tagCompound.getInteger("y");
		z = tagCompound.getInteger("z");
		if (tagCompound.hasKey("externals")) {
			final NBTTagCompound tagCompoundExternals = tagCompound.getCompoundTag("externals");
			externals = new HashMap<>();
			for (final Object key : tagCompoundExternals.getKeySet()) {
				assert key instanceof String;
				externals.put((String) key, tagCompoundExternals.getTag((String) key));
			}
		} else {
			externals = null;
		}
	}
	
	public void writeToNBT(final World worldSource, final NBTTagCompound tagCompound) {
		tagCompound.setString("block", Block.REGISTRY.getNameForObject(block).toString());
		tagCompound.setByte("blockMeta", (byte) blockMeta);
		final NBTTagCompound nbtTileEntity = getBlockNBT(worldSource);
		if (nbtTileEntity != null) {
			tagCompound.setTag("blockNBT", nbtTileEntity);
		}
		tagCompound.setInteger("x", x);
		tagCompound.setInteger("y", y);
		tagCompound.setInteger("z", z);
		if (externals != null && !externals.isEmpty()) {
			final NBTTagCompound tagCompoundExternals = new NBTTagCompound();
			for (final Entry<String, NBTBase> entry : externals.entrySet()) {
				if (entry.getValue() == null) {
					tagCompoundExternals.setString(entry.getKey(), "");
				} else {
					tagCompoundExternals.setTag(entry.getKey(), entry.getValue());
				}
			}
			tagCompound.setTag("externals", tagCompoundExternals);
		}
	}
	
	public void removeUniqueIDs() {
		removeUniqueIDs(blockNBT);
	}
	
	public static void removeUniqueIDs(final NBTTagCompound tagCompound) {
		if (tagCompound == null) {
			return;
		}
		
		// ComputerCraft computer
		if (tagCompound.hasKey("computerID")) {
			tagCompound.removeTag("computerID");
			tagCompound.removeTag("label");
		}
		
		// WarpDrive UUID
		if (tagCompound.hasKey(ICoreSignature.UUID_MOST_TAG)) {
			tagCompound.removeTag(ICoreSignature.UUID_MOST_TAG);
			tagCompound.removeTag(ICoreSignature.UUID_LEAST_TAG);
		}
		
		// WarpDrive any OC connected tile
		if (tagCompound.hasKey("oc:node")) {
			tagCompound.removeTag("oc:node");
		}
		
		// OpenComputers case
		if (tagCompound.hasKey("oc:computer")) {
			final NBTTagCompound tagComputer = tagCompound.getCompoundTag("oc:computer");
			tagComputer.removeTag("chunkX");
			tagComputer.removeTag("chunkZ");
			tagComputer.removeTag("components");
			tagComputer.removeTag("dimension");
			tagComputer.removeTag("node");
			tagCompound.setTag("oc:computer", tagComputer);
		}
		
		// OpenComputers case
		if (tagCompound.hasKey("oc:items")) {
			final NBTTagList tagListItems = tagCompound.getTagList("oc:items", Constants.NBT.TAG_COMPOUND);
			for (int indexItemSlot = 0; indexItemSlot < tagListItems.tagCount(); indexItemSlot++) {
				final NBTTagCompound tagCompoundItemSlot = tagListItems.getCompoundTagAt(indexItemSlot);
				final NBTTagCompound tagCompoundItem = tagCompoundItemSlot.getCompoundTag("item");
				final NBTTagCompound tagCompoundTag = tagCompoundItem.getCompoundTag("tag");
				final NBTTagCompound tagCompoundOCData = tagCompoundTag.getCompoundTag("oc:data");
				final NBTTagCompound tagCompoundNode = tagCompoundOCData.getCompoundTag("node");
				if (tagCompoundNode.hasKey("address")) {
					tagCompoundNode.removeTag("address");
				}
			}
		}
		
		// OpenComputers keyboard
		if (tagCompound.hasKey("oc:keyboard")) {
			final NBTTagCompound tagCompoundKeyboard = tagCompound.getCompoundTag("oc:keyboard");
			tagCompoundKeyboard.removeTag("node");
		}
		
		// OpenComputers screen
		if (tagCompound.hasKey("oc:hasPower")) {
			tagCompound.removeTag("node");
		}
		
		// Immersive Engineering & Thermal Expansion
		if (tagCompound.hasKey("Owner")) {
			tagCompound.setString("Owner", "None");
		}
		
		// Mekanism
		if (tagCompound.hasKey("owner")) {
			tagCompound.setString("owner", "None");
		}
	}
	
	public static void emptyEnergyStorage(@Nonnull final NBTTagCompound tagCompound) {
		// BuildCraft
		if (tagCompound.hasKey("battery", NBT.TAG_COMPOUND)) {
			final NBTTagCompound tagCompoundBattery = tagCompound.getCompoundTag("battery");
			if (tagCompoundBattery.hasKey("energy", NBT.TAG_INT)) {
				tagCompoundBattery.setInteger("energy", 0);
			}
		}
		
		// Gregtech
		if (tagCompound.hasKey("mStoredEnergy", NBT.TAG_INT)) {
			tagCompound.setInteger("mStoredEnergy", 0);
		}
		
		// IC2
		if (tagCompound.hasKey("energy", NBT.TAG_DOUBLE)) {
			// energy_consume((int)Math.round(blockNBT.getDouble("energy")), true);
			tagCompound.setDouble("energy", 0);
		}
		
		// Immersive Engineering & Thermal Expansion
		if (tagCompound.hasKey("Energy", NBT.TAG_INT)) {
			// energy_consume(blockNBT.getInteger("Energy"), true);
			tagCompound.setInteger("Energy", 0);
		}
		
		// Mekanism
		if (tagCompound.hasKey("electricityStored", NBT.TAG_DOUBLE)) {
			tagCompound.setDouble("electricityStored", 0);
		}
		
		// WarpDrive
		if (tagCompound.hasKey("energy", NBT.TAG_LONG)) {
			tagCompound.setLong("energy", 0L);
		}
	}
	
	public void fillEnergyStorage() {
		if (block instanceof IBlockBase) {
			final EnumTier enumTier = ((IBlockBase) block).getTier(null);
			if (enumTier != EnumTier.CREATIVE) {
				if (block instanceof BlockShipCore) {
					blockNBT.setLong(EnergyWrapper.TAG_ENERGY, WarpDriveConfig.SHIP_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()]);
				}
				if (block instanceof BlockCapacitor) {
					blockNBT.setLong(EnergyWrapper.TAG_ENERGY, WarpDriveConfig.CAPACITOR_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()]);
				}
			}
		}
	}
	
	// IC2 support for updating tile entity fields
	private static Object NetworkManager_instance;
	private static Method NetworkManager_updateTileEntityField;
	
	private static void NetworkHelper_init() {
		try {
			NetworkManager_updateTileEntityField = Class.forName("ic2.core.network.NetworkManager").getMethod("updateTileEntityField", TileEntity.class, String.class);
			
			NetworkManager_instance = Class.forName("ic2.core.IC2").getDeclaredField("network").get(null);
		} catch (final Exception exception) {
			throw new RuntimeException(exception);
		}
	}
	
	private static void NetworkHelper_updateTileEntityField(final TileEntity tileEntity, final String field) {
		try {
			if (NetworkManager_instance == null) {
				NetworkHelper_init();
			}
			NetworkManager_updateTileEntityField.invoke(NetworkManager_instance, tileEntity, field);
		} catch (final Exception exception) {
			throw new RuntimeException(exception);
		}
	}
	// IC2 support ends here
	
	@Override
	public String toString() {
		return String.format("%s @ (%d %d %d) %s:%d %s nbt %s",
		                     getClass().getSimpleName(),
		                     x, y, z,
		                     block.getRegistryName(), blockMeta,
		                     weakTileEntity == null ? null : weakTileEntity.get(),
		                     blockNBT);
	}
}
