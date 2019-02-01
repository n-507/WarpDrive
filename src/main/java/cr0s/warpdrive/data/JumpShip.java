package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;

public class JumpShip {
	
	public World world;
	public BlockPos core;
	public int dx;
	public int dz;
	public int maxX;
	public int maxZ;
	public int maxY;
	public int minX;
	public int minZ;
	public int minY;
	public JumpBlock[] jumpBlocks;
	public int actualMass;
	public TileEntityShipCore shipCore;
	public List<MovingEntity> entitiesOnShip;
	
	public JumpShip() {
	}
	
	public static JumpShip createFromFile(final String fileName, final WarpDriveText reason) {
		final NBTTagCompound schematic = Commons.readNBTFromFile(WarpDriveConfig.G_SCHEMATICS_LOCATION + "/" + fileName + ".schematic");
		if (schematic == null) {
			reason.append(Commons.styleWarning, "warpdrive.ship.guide.schematic_not_found",
			              fileName);
			return null;
		}
		
		final JumpShip jumpShip = new JumpShip();
		
		// Compute geometry
		// int shipMass = schematic.getInteger("shipMass");
		// String shipName = schematic.getString("shipName");
		// int shipVolume = schematic.getInteger("shipVolume");
		if (schematic.hasKey("ship")) {
			jumpShip.readFromNBT(schematic.getCompoundTag("ship"));
			
		} else {
			// Set deployment variables
			final short width = schematic.getShort("Width");
			final short height = schematic.getShort("Height");
			final short length = schematic.getShort("Length");
			jumpShip.minX = 0;
			jumpShip.maxX = width - 1;
			jumpShip.minY = 0;
			jumpShip.maxY = height - 1;
			jumpShip.minZ = 0;
			jumpShip.maxZ = length - 1;
			jumpShip.core = null;
			jumpShip.jumpBlocks = new JumpBlock[width * height * length];
			
			// Read blocks and TileEntities from NBT to internal storage array
			final byte localBlocks[] = schematic.getByteArray("Blocks");
			final byte localAddBlocks[] = schematic.hasKey("AddBlocks") ? schematic.getByteArray("AddBlocks") : null;
			final byte localMetadata[] = schematic.getByteArray("Data");
			
			// Load Tile Entities
			final NBTTagCompound[] tileEntities = new NBTTagCompound[jumpShip.jumpBlocks.length];
			final NBTTagList tagListTileEntities = schematic.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
			
			for (int i = 0; i < tagListTileEntities.tagCount(); i++) {
				final NBTTagCompound tagTileEntity = tagListTileEntities.getCompoundTagAt(i);
				final int teX = tagTileEntity.getInteger("x");
				final int teY = tagTileEntity.getInteger("y");
				final int teZ = tagTileEntity.getInteger("z");
				
				tileEntities[teX + (teY * length + teZ) * width] = tagTileEntity;
			}
			
			// Create list of blocks to deploy
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					for (int z = 0; z < length; z++) {
						final int index = x + (y * length + z) * width;
						JumpBlock jumpBlock = new JumpBlock();
						
						jumpBlock.x = x;
						jumpBlock.y = y;
						jumpBlock.z = z;
						
						// rebuild block id from signed byte + nibble tables
						int blockId = localBlocks[index];
						if (blockId < 0) {
							blockId += 256;
						}
						if (localAddBlocks != null) {
							int MSB = localAddBlocks[index / 2];
							if (MSB < 0) {
								MSB += 256;
							}
							if (index % 2 == 0) {
								blockId += (MSB & 0x0F) << 8;
							} else {
								blockId += (MSB & 0xF0) << 4;
							}
						}
						
						jumpBlock.block = Block.getBlockById(blockId);
						jumpBlock.blockMeta = (localMetadata[index]) & 0x0F;
						jumpBlock.blockNBT = tileEntities[index];
						
						if (jumpBlock.block != null) {
							if (WarpDriveConfig.LOGGING_BUILDING) {
								if (tileEntities[index] == null) {
									WarpDrive.logger.info("Adding block to deploy: "
										                      + jumpBlock.block.getTranslationKey() + ":" + jumpBlock.blockMeta
										                      + " (no tile entity)");
								} else {
									WarpDrive.logger.info("Adding block to deploy: "
										                      + jumpBlock.block.getTranslationKey() + ":" + jumpBlock.blockMeta
										                      + " with tile entity " + tileEntities[index].getString("id"));
								}
							}
						} else {
							jumpBlock = null;
						}
						jumpShip.jumpBlocks[index] = jumpBlock;
					}
				}
			}
		}
		return jumpShip;
	}
	
	public void messageToAllPlayersOnShip(final WarpDriveText textComponent) {
		final ITextComponent messageFormatted = new TextComponentString((shipCore != null && !shipCore.name.isEmpty()) ? shipCore.name + " " : "ShipCore ").setStyle(Commons.styleHeader)
						.appendSibling(textComponent);
		if (entitiesOnShip == null) {
			// entities not saved yet, get them now
			final WarpDriveText reason = new WarpDriveText();
			saveEntities(reason);
		}
		
		WarpDrive.logger.info(this + " messageToAllPlayersOnShip: " + textComponent.getUnformattedText());
		for (final MovingEntity movingEntity : entitiesOnShip) {
			final Entity entity = movingEntity.getEntity();
			if (entity instanceof EntityPlayer) {
				Commons.addChatMessage(entity, messageFormatted);
			}
		}
	}
	
	public boolean saveEntities(final WarpDriveText reason) {
		boolean isSuccess = true;
		entitiesOnShip = new ArrayList<>();
		
		if (world == null) {
			WarpDrive.logger.error("Invalid call to saveEntities, please report it to mod author: world is null");
			reason.append(Commons.styleWarning, "warpdrive.error.internal_check_console");
			return false;
		}
		
		final AxisAlignedBB axisalignedbb = new AxisAlignedBB(minX, minY, minZ, maxX + 0.99D, maxY + 0.99D, maxZ + 0.99D);
		
		final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
		
		for (final Entity entity : list) {
			if (entity == null) {
				continue;
			}
			
			if (Dictionary.isAnchor(entity)) {
				reason.append(Commons.styleWarning, "warpdrive.ship.guide.anchor_entity_detected",
				              Dictionary.getId(entity),
				              Math.round(entity.posX), Math.round(entity.posY), Math.round(entity.posZ) );
				isSuccess = false;
				// we need to continue so players are added so they can see the message...
				continue;
			}
			if (Dictionary.isLeftBehind(entity)) {
				if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
					WarpDrive.logger.info(String.format("Leaving entity %s behind: %s",
					                                    Dictionary.getId(entity),
					                                    entity ));
				}
				continue;
			}
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
					WarpDrive.logger.info(String.format("Adding entity %s: %s",
					                                    Dictionary.getId(entity),
					                                    entity ));
				}
			}
			final MovingEntity movingEntity = new MovingEntity(entity);
			entitiesOnShip.add(movingEntity);
		}
		
		return isSuccess;
	}
	
	public void setCaptain(final String playerName) {
		entitiesOnShip = new ArrayList<>();
		final EntityPlayerMP entityPlayerMP = Commons.getOnlinePlayerByName(playerName);
		if (entityPlayerMP == null) {
			WarpDrive.logger.error(String.format("%s setCaptain: captain is missing", this));
			return;
		}
		final MovingEntity movingEntity = new MovingEntity(entityPlayerMP);
		entitiesOnShip.add(movingEntity);
	}
	
	public boolean isUnlimited() {
		if (entitiesOnShip == null) {
			return false;
		}
		for (final MovingEntity movingEntity : entitiesOnShip) {
			if (movingEntity.isUnlimited()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return String.format("%s/%d \'%s\' %s",
		                     getClass().getSimpleName(), hashCode(),
		                     shipCore == null ? "~NULL~" : (shipCore.uuid + ":" + shipCore.name),
			                 Commons.format(world, core));
	}
	
	public boolean checkBorders(final WarpDriveText reason) {
		final MutableBlockPos mutableBlockPos = new MutableBlockPos();
		// Abort jump if blocks with TE are connecting to the ship (avoid crash when splitting multi-blocks)
		for (int x = minX - 1; x <= maxX + 1; x++) {
			final boolean xBorder = (x == minX - 1) || (x == maxX + 1);
			for (int z = minZ - 1; z <= maxZ + 1; z++) {
				final boolean zBorder = (z == minZ - 1) || (z == maxZ + 1);
				
				// skip the corners
				if (xBorder && zBorder) {
					continue;
				}
				
				for (int y = minY - 1; y <= maxY + 1; y++) {
					final boolean yBorder = (y == minY - 1) || (y == maxY + 1);
					if ((y < 0) || (y > 255)) {
						continue;
					}
					if (!(xBorder || yBorder || zBorder)) {
						continue;
					}
					
					// skip the corners
					if (yBorder && (xBorder || zBorder)) {
						continue;
					}
					
					mutableBlockPos.setPos(x, y, z);
					final IBlockState blockState = world.getBlockState(mutableBlockPos);
					
					final Block block = blockState.getBlock();
					
					// Skipping any air block & ignored blocks
					if ( world.isAirBlock(mutableBlockPos)
					  || Dictionary.BLOCKS_LEFTBEHIND.contains(block) ) {
						continue;
					}
					
					// Skipping non-movable blocks
					if (Dictionary.BLOCKS_ANCHOR.contains(block)) {
						continue;
					}
					
					// Skipping blocks without tile entities
					if (!block.hasTileEntity(blockState)) {
						continue;
					}
					
					
					// Check inner block
					mutableBlockPos.setPos(
							x == minX - 1 ? minX : x == maxX + 1 ? maxX : x,
							y == minY - 1 ? minY : y == maxY + 1 ? maxY : y,
							z == minZ - 1 ? minZ : z == maxZ + 1 ? maxZ : z );
					final IBlockState blockStateInner = world.getBlockState(mutableBlockPos);
					final Block blockInner = blockStateInner.getBlock();
					
					// Skipping any air block & ignored blocks
					if ( world.isAirBlock(mutableBlockPos)
					  || Dictionary.BLOCKS_LEFTBEHIND.contains(blockInner) ) {
						continue;
					}
					
					// Skipping blocks without tile entities
					if (!blockInner.hasTileEntity(blockStateInner)) {
						continue;
					}
					
					reason.append(Commons.styleWarning, "warpdrive.ship.guide.ship_snagged1",
					              blockState.getBlock().getLocalizedName(),
					              x, y, z);
					reason.append(Commons.styleCommand, "warpdrive.ship.guide.ship_snagged2");
					world.createExplosion(null, x, y, z, Math.min(4F * 30, 4F * (jumpBlocks.length / 50.0F)), false);
					return false;
				}
			}
		}
		
		return true;
	}

	/**
	 * Saving ship to memory
	 */
	public boolean save(final WarpDriveText reason) {
		BlockPos blockPos = new BlockPos(0, -1, 0);
		try {
			final int estimatedVolume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
			final JumpBlock[][] placeTimeJumpBlocks = { new JumpBlock[estimatedVolume], new JumpBlock[estimatedVolume], new JumpBlock[estimatedVolume], new JumpBlock[estimatedVolume], new JumpBlock[estimatedVolume] };
			final int[] placeTimeIndexes = { 0, 0, 0, 0, 0 };
			
			int actualVolume = 0;
			int newMass = 0;
			final int xc1 = minX >> 4;
			final int xc2 = maxX >> 4;
			final int zc1 = minZ >> 4;
			final int zc2 = maxZ >> 4;
			
			for (int xc = xc1; xc <= xc2; xc++) {
				final int x1 = Math.max(minX, xc << 4);
				final int x2 = Math.min(maxX, (xc << 4) + 15);
				
				for (int zc = zc1; zc <= zc2; zc++) {
					final int z1 = Math.max(minZ, zc << 4);
					final int z2 = Math.min(maxZ, (zc << 4) + 15);
					
					for (int y = minY; y <= maxY; y++) {
						for (int x = x1; x <= x2; x++) {
							for (int z = z1; z <= z2; z++) {
								blockPos = new BlockPos(x, y, z);
								final IBlockState blockState = world.getBlockState(blockPos);
								
								// Skipping vanilla air & ignored blocks
								if (blockState.getBlock() == Blocks.AIR || Dictionary.BLOCKS_LEFTBEHIND.contains(blockState.getBlock())) {
									continue;
								}
								actualVolume++;
								
								if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
									WarpDrive.logger.info(String.format("Checking for save from (%d %d %d) of %s (%d)",
									                                    x, y, z, blockState, blockState.getBlock().getMetaFromState(blockState)));
								}
								
								if (!Dictionary.BLOCKS_NOMASS.contains(blockState.getBlock())) {
									newMass++;
								}
								
								// Stop on non-movable blocks
								if (Dictionary.BLOCKS_ANCHOR.contains(blockState.getBlock())) {
									reason.append(Commons.styleWarning, "warpdrive.ship.guide.anchor_block_detected",
									              blockState.getBlock().getLocalizedName(),
									              x, y, z);
									return false;
								}
								
								final TileEntity tileEntity = world.getTileEntity(blockPos);
								final JumpBlock jumpBlock = new JumpBlock(world, blockPos, blockState, tileEntity);
								
								if (tileEntity != null && jumpBlock.externals != null) {
									for (final Entry<String, NBTBase> external : jumpBlock.externals.entrySet()) {
										final IBlockTransformer blockTransformer = WarpDriveConfig.blockTransformers.get(external.getKey());
										if (blockTransformer != null) {
											if (!blockTransformer.isJumpReady(jumpBlock.block, jumpBlock.blockMeta, tileEntity, reason)) {
												reason.append(Commons.styleWarning, "warpdrive.ship.guide.block_not_ready_for_jump",
												              jumpBlock.block.getLocalizedName(),
												              jumpBlock.x, jumpBlock.y, jumpBlock.z);
												return false;
											}
										}
									}
								}
								
								// default priority is 2 for block, 3 for tile entities
								Integer placeTime = Dictionary.BLOCKS_PLACE.get(blockState.getBlock());
								if (placeTime == null) {
									if (tileEntity == null) {
										placeTime = 2;
									} else {
										placeTime = 3;
									}
								}
								
								placeTimeJumpBlocks[placeTime][placeTimeIndexes[placeTime]] = jumpBlock;
								placeTimeIndexes[placeTime]++;
							}
						}
					}
				}
			}
			
			jumpBlocks = new JumpBlock[actualVolume];
			int indexShip = 0;
			for (int placeTime = 0; placeTime < 5; placeTime++) {
				for (int placeTimeIndex = 0; placeTimeIndex < placeTimeIndexes[placeTime]; placeTimeIndex++) {
					jumpBlocks[indexShip] = placeTimeJumpBlocks[placeTime][placeTimeIndex];
					indexShip++;
				}
			}
			actualMass = newMass;
		} catch (final Exception exception) {
			exception.printStackTrace();
			final WarpDriveText textComponent = new WarpDriveText(Commons.styleWarning, "warpdrive.ship.guide.corrupted_block",
			                                                      Commons.format(world, blockPos));
			WarpDrive.logger.error(textComponent.getUnformattedText());
			reason.appendSibling(textComponent);
			return false;
		}
		
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(String.format("%s Ship saved as %d blocks",
			                                    this, jumpBlocks.length));
		}
		return true;
	}
	
	public void readFromNBT(final NBTTagCompound tagCompound) {
		core = new BlockPos(tagCompound.getInteger("coreX"), tagCompound.getInteger("coreY"), tagCompound.getInteger("coreZ"));
		dx = tagCompound.getInteger("dx");
		dz = tagCompound.getInteger("dz");
		maxX = tagCompound.getInteger("maxX");
		maxZ = tagCompound.getInteger("maxZ");
		maxY = tagCompound.getInteger("maxY");
		minX = tagCompound.getInteger("minX");
		minZ = tagCompound.getInteger("minZ");
		minY = tagCompound.getInteger("minY");
		actualMass = tagCompound.getInteger("actualMass");
		final NBTTagList tagList = tagCompound.getTagList("jumpBlocks", Constants.NBT.TAG_COMPOUND);
		jumpBlocks = new JumpBlock[tagList.tagCount()];
		for (int index = 0; index < tagList.tagCount(); index++) {
			jumpBlocks[index] = new JumpBlock();
			jumpBlocks[index].readFromNBT(tagList.getCompoundTagAt(index));
		}
	}
	
	public void writeToNBT(final NBTTagCompound tagCompound) {
		tagCompound.setInteger("coreX", core.getX());
		tagCompound.setInteger("coreY", core.getY());
		tagCompound.setInteger("coreZ", core.getZ());
		tagCompound.setInteger("dx", dx);
		tagCompound.setInteger("dz", dz);
		tagCompound.setInteger("maxX", maxX);
		tagCompound.setInteger("maxZ", maxZ);
		tagCompound.setInteger("maxY", maxY);
		tagCompound.setInteger("minX", minX);
		tagCompound.setInteger("minZ", minZ);
		tagCompound.setInteger("minY", minY);
		tagCompound.setInteger("actualMass", actualMass);
		final NBTTagList tagListJumpBlocks = new NBTTagList();
		for (final JumpBlock jumpBlock : jumpBlocks) {
			final NBTTagCompound tagCompoundBlock = new NBTTagCompound();
			jumpBlock.writeToNBT(world, tagCompoundBlock);
			tagListJumpBlocks.appendTag(tagCompoundBlock);
		}
		tagCompound.setTag("jumpBlocks", tagListJumpBlocks);
	}
}