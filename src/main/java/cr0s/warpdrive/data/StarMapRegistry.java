package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IStarMapRegistryTileEntity;
import cr0s.warpdrive.block.atomic.BlockAcceleratorCore;
import cr0s.warpdrive.block.detection.BlockVirtualAssistant;
import cr0s.warpdrive.block.detection.TileEntityVirtualAssistant;
import cr0s.warpdrive.block.movement.BlockShipCore;
import cr0s.warpdrive.block.movement.BlockTransporterCore;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

/**
 * Registry of all known ships, jumpgates, etc. in the world
 * 
 * @author LemADEC
 */
public class StarMapRegistry {
	
	public static String GALAXY_UNDEFINED = "???";
	
	private final HashMap<Integer, CopyOnWriteArraySet<StarMapRegistryItem>> registry;
	private int countAdd = 0;
	private int countRemove = 0;
	private int countRead = 0;
	
	public StarMapRegistry() {
		registry = new HashMap<>();
	}
	
	public void updateInRegistry(final IStarMapRegistryTileEntity tileEntity) {
		// validate context
		assert tileEntity instanceof TileEntity;
		if (!Commons.isSafeThread()) {
			WarpDrive.logger.error(String.format("Non-threadsafe call to StarMapRegistry:updateInRegistry outside main thread, for %s",
			                                     tileEntity));
			return;
		}
		if (tileEntity.getSignatureUUID() == null) {
			WarpDrive.logger.error(String.format("Ignoring invalid StarMapRegistryItem with no UUID %s",
			                                     tileEntity));
			return;
		}
		
		// update statistics
		countRead++;
		if (WarpDriveConfig.LOGGING_STARMAP) {
			if (countRead % 1000 == 0) {
				WarpDrive.logger.info(String.format("Starmap registry stats: read %d add %d remove %d => %.2f%% read",
				                                    countRead, countAdd, countRemove, ((float) countRead) / (countRemove + countRead + countAdd)));
			}
		}
		
		// get dimension
		CopyOnWriteArraySet<StarMapRegistryItem> setRegistryItems = registry.get(((TileEntity) tileEntity).getWorld().provider.getDimension());
		if (setRegistryItems == null) {
			setRegistryItems = new CopyOnWriteArraySet<>();
		}
		
		// get entry
		final ArrayList<StarMapRegistryItem> listToRemove = new ArrayList<>(3);
		final UUID uuidTileEntity = tileEntity.getSignatureUUID();
		for (final StarMapRegistryItem registryItem : setRegistryItems) {
			if (registryItem.uuid == null) {
				WarpDrive.logger.error(String.format("Removing invalid StarMapRegistryItem %s",
				                                     registryItem));
				listToRemove.add(registryItem);
				continue;
			}
			
			if ( registryItem.type.equals(tileEntity.getStarMapType())
			  && registryItem.uuid.equals(uuidTileEntity) ) {// already registered
				registryItem.update(tileEntity);    // in-place update only works as long as hashcode remains unchanged
				setRegistryItems.removeAll(listToRemove);
				return;
			} else if (registryItem.sameCoordinates(tileEntity)) {
				listToRemove.add(registryItem);
			}
		}
		setRegistryItems.removeAll(listToRemove);
		
		// not found => add
		countAdd++;
		setRegistryItems.add(new StarMapRegistryItem(tileEntity));
		registry.put(((TileEntity) tileEntity).getWorld().provider.getDimension(), setRegistryItems);
		if (WarpDriveConfig.LOGGING_STARMAP) {
			printRegistry("added");
		}
	}
	
	public void removeFromRegistry(final IStarMapRegistryTileEntity tileEntity) {
		assert tileEntity instanceof TileEntity;
		
		countRead++;
		final Set<StarMapRegistryItem> setRegistryItems = registry.get(((TileEntity) tileEntity).getWorld().provider.getDimension());
		if (setRegistryItems == null) {
			// noting to remove
			return;
		}
		
		for (final StarMapRegistryItem registryItem : setRegistryItems) {
			if (registryItem.sameCoordinates(tileEntity)) {
				// found it, remove and exit
				countRemove++;
				setRegistryItems.remove(registryItem);
				return;
			}
		}
		// not found => ignore it
	}
	
	public StarMapRegistryItem getByName(final EnumStarMapEntryType enumStarMapEntryType, final String name) {
		for (final Integer dimensionId : registry.keySet()) {
			final CopyOnWriteArraySet<StarMapRegistryItem> setStarMapRegistryItems = registry.get(dimensionId);
			if (setStarMapRegistryItems == null) {
				continue;
			}
			
			for (final StarMapRegistryItem starMapRegistryItem : setStarMapRegistryItems) {
				if ( enumStarMapEntryType == null
				  || starMapRegistryItem.type == enumStarMapEntryType ) {
					if (starMapRegistryItem.name.equals(name)) {
						return starMapRegistryItem;
					}
				}
			}
		}
		return null;
	}
	
	public StarMapRegistryItem getByUUID(final EnumStarMapEntryType enumStarMapEntryType, final UUID uuid) {
		for (final Integer dimensionId : registry.keySet()) {
			final CopyOnWriteArraySet<StarMapRegistryItem> setStarMapRegistryItems = registry.get(dimensionId);
			if (setStarMapRegistryItems == null) {
				continue;
			}
			
			for (final StarMapRegistryItem starMapRegistryItem : setStarMapRegistryItems) {
				if ( enumStarMapEntryType == null
				  || starMapRegistryItem.type == enumStarMapEntryType ) {
					if (starMapRegistryItem.uuid.equals(uuid)) {
						return starMapRegistryItem;
					}
				}
			}
		}
		return null;
	}
	
	public String find(final String nameShip) {
		final int MAX_LENGTH = 2000;
		final StringBuilder resultMatch = new StringBuilder();
		final StringBuilder resultCaseInsensitive = new StringBuilder();
		final StringBuilder resultContains = new StringBuilder();
		for (final Integer dimensionId : registry.keySet()) {
			final CopyOnWriteArraySet<StarMapRegistryItem> setStarMapRegistryItems = registry.get(dimensionId);
			if (setStarMapRegistryItems == null) {
				continue;
			}
			
			for (final StarMapRegistryItem starMapRegistryItem : setStarMapRegistryItems) {
				if (starMapRegistryItem.type == EnumStarMapEntryType.SHIP) {
					if (starMapRegistryItem.name.equals(nameShip)) {
						if (resultMatch.length() < MAX_LENGTH) {
							if (resultMatch.length() > 0) {
								resultMatch.append("\n");
							}
							resultContains.append(String.format("Ship '%s' found in %s",
							                                    starMapRegistryItem.name,
							                                    starMapRegistryItem.getFormattedLocation()));
						} else {
							resultMatch.append(".");
						}
					} else if (starMapRegistryItem.name.equalsIgnoreCase(nameShip)) {
						if (resultCaseInsensitive.length() < MAX_LENGTH) {
							if (resultCaseInsensitive.length() > 0) {
								resultCaseInsensitive.append("\n");
							}
							resultContains.append(String.format("Ship '%s' found in %s",
							                                    starMapRegistryItem.name,
							                                    starMapRegistryItem.getFormattedLocation()));
						} else {
							resultCaseInsensitive.append(".");
						}
					} else if (starMapRegistryItem.name.contains(nameShip)) {
						if (resultContains.length() < MAX_LENGTH) {
							if (resultContains.length() > 0) {
								resultContains.append("\n");
							}
							resultContains.append(String.format("Ship '%s' found in %s",
							                                    starMapRegistryItem.name,
							                                    starMapRegistryItem.getFormattedLocation()));
						} else {
							resultContains.append(".");
						}
					}
				}
			}
		}
		
		if (resultMatch.length() > 0) {
			return resultMatch.toString();
		}
		if (resultCaseInsensitive.length() > 0) {
			return resultCaseInsensitive.toString();
		}
		if (resultContains.length() > 0) {
			return resultContains.toString();
		}
		return String.format("No ship found with name '%s'", nameShip);
	}
	
	public StarMapRegistryItem findNearest(final EnumStarMapEntryType enumStarMapEntryType, @Nonnull final World world, @Nonnull final BlockPos blockPos) {
		final CopyOnWriteArraySet<StarMapRegistryItem> setStarMapRegistryItems = registry.get(world.provider.getDimension());
		if (setStarMapRegistryItems == null) {
			return null;
		}
		
		double distanceSquared_min = Double.MAX_VALUE;
		StarMapRegistryItem result = null;
		for (final StarMapRegistryItem starMapRegistryItem : setStarMapRegistryItems) {
			if ( enumStarMapEntryType != null
			  && starMapRegistryItem.type != enumStarMapEntryType ) {
				continue;
			}
			
			final double dX = starMapRegistryItem.x - blockPos.getX();
			final double dY = starMapRegistryItem.y - blockPos.getY();
			final double dZ = starMapRegistryItem.z - blockPos.getZ();
			final double distanceSquared = dX * dX + dY * dY + dZ * dZ;
			
			if ( distanceSquared < distanceSquared_min) {
				distanceSquared_min = distanceSquared;
				result = starMapRegistryItem;
			}
		}
		
		return result;
	}
	
	public boolean onBlockUpdating(@Nullable final Entity entity, @Nonnull final World world, @Nonnull final BlockPos blockPos, final IBlockState blockState) {
		if (!Commons.isSafeThread()) {
			WarpDrive.logger.error(String.format("Non-threadsafe call to StarMapRegistry:onBlockUpdating outside main thread, for %s %s",
			                                     blockState, Commons.format(world, blockPos) ));
			return false;
		}
		final CopyOnWriteArraySet<StarMapRegistryItem> setStarMapRegistryItems = registry.get(world.provider.getDimension());
		if (setStarMapRegistryItems == null) {
			return true;
		}
		boolean isAllowed = true;
		for (final StarMapRegistryItem registryItem : setStarMapRegistryItems) {
			if (registryItem.contains(blockPos)) {
				final TileEntity tileEntity = world.getTileEntity(new BlockPos(registryItem.x, registryItem.y, registryItem.z));
				if (tileEntity instanceof IStarMapRegistryTileEntity) {
					isAllowed = isAllowed && ((IStarMapRegistryTileEntity) tileEntity).onBlockUpdatingInArea(entity, blockPos, blockState);
				}
			}
		}
		return isAllowed;
	}
	
	public boolean onChatReceived(@Nonnull final EntityPlayer entityPlayer, @Nonnull final String message) {
		if (!Commons.isSafeThread()) {
			WarpDrive.logger.error(String.format("Non-threadsafe call to StarMapRegistry:onChatReceived outside main thread, for %s %s",
			                                     entityPlayer, message ));
			return false;
		}
		final CopyOnWriteArraySet<StarMapRegistryItem> setStarMapRegistryItems = registry.get(entityPlayer.world.provider.getDimension());
		if (setStarMapRegistryItems == null) {
			return true;
		}
		final BlockPos blockPos = entityPlayer.getPosition();
		boolean isCancelled = false;
		for (final StarMapRegistryItem registryItem : setStarMapRegistryItems) {
			if ( registryItem.type == EnumStarMapEntryType.VIRTUAL_ASSISTANT
			  && registryItem.contains(blockPos) ) {
				final TileEntity tileEntity = entityPlayer.world.getTileEntity(new BlockPos(registryItem.x, registryItem.y, registryItem.z));
				if (tileEntity instanceof TileEntityVirtualAssistant) {
					isCancelled = isCancelled || ((TileEntityVirtualAssistant) tileEntity).onChatReceived(entityPlayer, message);
				}
			}
		}
		return isCancelled;
	}
	
	public static double getGravity(final Entity entity) {
		final CelestialObject celestialObject = CelestialObjectManager.get(entity.world, (int) entity.posX, (int) entity.posZ);
		return celestialObject == null ? 1.0D : celestialObject.getGravity();
	}
	
	public static int getSpaceDimensionId(final World world, final int x, final int z) {
		CelestialObject celestialObject = CelestialObjectManager.get(world, x, z);
		if (celestialObject == null) {
			return world.provider.getDimension();
		}
		// already in space?
		if (celestialObject.isSpace()) {
			return celestialObject.dimensionId;
		}
		// coming from hyperspace?
		if (celestialObject.isHyperspace()) {
			celestialObject = CelestialObjectManager.getClosestChild(world, x, z);
			return celestialObject == null ? 0 : celestialObject.dimensionId;
		}
		// coming from a planet?
		while (celestialObject != null && !celestialObject.isSpace()) {
			celestialObject = celestialObject.parent;
		}
		return celestialObject == null ? 0 : celestialObject.dimensionId;
	}
	
	public static int getHyperspaceDimensionId(final World world, final int x, final int z) {
		CelestialObject celestialObject = CelestialObjectManager.get(world, x, z);
		if (celestialObject == null) {
			return world.provider.getDimension();
		}
		// already in hyperspace?
		if (celestialObject.isHyperspace()) {
			return celestialObject.dimensionId;
		}
		// coming from space?
		if (celestialObject.isSpace()) {
			return celestialObject.parent.dimensionId;
		}
		// coming from a planet?
		while (celestialObject != null && !celestialObject.isSpace()) {
			celestialObject = celestialObject.parent;
		}
		return celestialObject == null || celestialObject.parent == null ? 0 : celestialObject.parent.dimensionId;
	}
	
	public static int getDimensionId(final String stringDimension, final Entity entity) {
		switch (stringDimension.toLowerCase()) {
		case "world":
		case "overworld":
		case "0":
			return 0;
		case "nether":
		case "thenether":
		case "-1":
			return -1;
		case "s":
		case "space":
			return getSpaceDimensionId(entity.world, (int) entity.posX, (int) entity.posZ);
		case "h":
		case "hyper":
		case "hyperspace":
			return getHyperspaceDimensionId(entity.world, (int) entity.posX, (int) entity.posZ);
		default:
			try {
				return Integer.parseInt(stringDimension);
			} catch (final Exception exception) {
				// exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.info(String.format("Invalid dimension %s, expecting integer or overworld/nether/end/theend/space/hyper/hyperspace",
				                                    stringDimension));
			}
		}
		return 0;
	}
	
	public ArrayList<RadarEcho> getRadarEchos(final TileEntity tileEntity, final int radius) {
		final ArrayList<RadarEcho> arrayListRadarEchos = new ArrayList<>(registry.size());
		cleanup();
		
		final CelestialObject celestialObject = CelestialObjectManager.get(tileEntity.getWorld(), tileEntity.getPos().getX(), tileEntity.getPos().getZ());
		final Vector3 vectorRadar = getUniversalCoordinates(
			celestialObject,
			tileEntity.getPos().getX(), tileEntity.getPos().getY(), tileEntity.getPos().getZ());
		// printRegistry();
		final int radius2 = radius * radius;
		for (final Map.Entry<Integer, CopyOnWriteArraySet<StarMapRegistryItem>> entryDimension : registry.entrySet()) {
			for (final StarMapRegistryItem starMapRegistryItem : entryDimension.getValue()) {
				if (!starMapRegistryItem.type.hasRadarEcho()) {
					continue;
				}
				final Vector3 vectorItem = starMapRegistryItem.getUniversalCoordinates(tileEntity.getWorld().isRemote);
				if (vectorItem == null) {
					continue;
				}
				final double dX = vectorItem.x - vectorRadar.x;
				final double dY = vectorItem.y - vectorRadar.y;
				final double dZ = vectorItem.z - vectorRadar.z;
				final double distance2 = dX * dX + dY * dY + dZ * dZ;
				if (distance2 > radius2) {
					continue;
				}
				if ( starMapRegistryItem.isolationRate != 0.0D
				  && tileEntity.getWorld().rand.nextDouble() < starMapRegistryItem.isolationRate) {
					continue;
				}
				
				arrayListRadarEchos.add( new RadarEcho(starMapRegistryItem.type.getName(),
				                                       vectorItem,
				                                       starMapRegistryItem.mass,
				                                       starMapRegistryItem.name) );
			}
		}
		
		return arrayListRadarEchos;
	}
	
	public static String getGalaxyName(final CelestialObject celestialObject, final double x, final double y, final double z) {
		if (celestialObject == null) {
			// not a registered area
			return GALAXY_UNDEFINED;
		}
		CelestialObject celestialObjectNode = celestialObject;
		boolean hasHyperspace = celestialObjectNode.isHyperspace();
		while (celestialObjectNode.parent != null) {
			celestialObjectNode = celestialObjectNode.parent;
			hasHyperspace |= celestialObjectNode.isHyperspace();
		}
		return hasHyperspace ? celestialObjectNode.getDisplayName() : GALAXY_UNDEFINED;
	}
	
	public static Vector3 getUniversalCoordinates(final CelestialObject celestialObject, final double x, final double y, final double z) {
		if (celestialObject == null) {
			// not a registered area
			return null;
		}
		final Vector3 vec3Result = new Vector3(x, y + 512.0D, z);
		CelestialObject celestialObjectNode = celestialObject;
		boolean hasHyperspace = celestialObjectNode.isHyperspace();
		while (celestialObjectNode.parent != null) {
			final VectorI vEntry = celestialObjectNode.getEntryOffset();
			vec3Result.x -= vEntry.x;
			vec3Result.y -= 256.0D;
			vec3Result.z -= vEntry.z;
			celestialObjectNode = celestialObjectNode.parent;
			hasHyperspace |= celestialObjectNode.isHyperspace();
		}
		return hasHyperspace ? vec3Result : null;
	}
	
	public void printRegistry(final String trigger) {
		WarpDrive.logger.info(String.format("Starmap registry (%s entries after %s):",
		                                    registry.size(), trigger));
		
		for (final Map.Entry<Integer, CopyOnWriteArraySet<StarMapRegistryItem>> entryDimension : registry.entrySet()) {
			final StringBuilder message = new StringBuilder();
			for (final StarMapRegistryItem registryItem : entryDimension.getValue()) {
				message.append(String.format("\n- %s '%s' @ DIM%d (%d %d %d) with %.3f isolation rate",
				                             registryItem.type, registryItem.name,
				                             registryItem.dimensionId, registryItem.x, registryItem.y, registryItem.z,
				                             registryItem.isolationRate));
			}
			WarpDrive.logger.info(String.format("- %d entries in dimension %d: %s",
			                                    entryDimension.getValue().size(), entryDimension.getKey(), message.toString()));
		}
	}
	
	public TileEntityShipCore getIntersectingShipCore(@Nonnull final TileEntityShipCore shipCore1) {
		cleanup();
		
		if (!shipCore1.isAssemblyValid()) {
			WarpDrive.logger.error(String.format("isShipCoreIntersectingWithOthers() with invalid ship %s, assuming intersection",
			                                     shipCore1));
			return null;
		}
		final AxisAlignedBB aabb1 = shipCore1.getStarMapArea();		
		
		final CopyOnWriteArraySet<StarMapRegistryItem> setRegistryItems = registry.get(shipCore1.getWorld().provider.getDimension());
		if (setRegistryItems == null) {
			return null;
		}
		for (final StarMapRegistryItem registryItem : setRegistryItems) {
			assert registryItem.dimensionId == shipCore1.getWorld().provider.getDimension();
			
			// only check ships
			if (registryItem.type != EnumStarMapEntryType.SHIP) {
				continue;
			}
			
			// Skip self
			if ( registryItem.x == shipCore1.getPos().getX()
			  && registryItem.y == shipCore1.getPos().getY()
			  && registryItem.z == shipCore1.getPos().getZ() ) {
				continue;
			}
			
			// Compare areas for intersection
			final AxisAlignedBB aabb2 = registryItem.getArea();
			if (!aabb1.intersects(aabb2)) {
				continue;
			}
			
			// Skip missing ship cores
			final TileEntity tileEntity = shipCore1.getWorld().getTileEntity(new BlockPos(registryItem.x, registryItem.y, registryItem.z));
			if (!(tileEntity instanceof TileEntityShipCore)) {
				continue;
			}
			final TileEntityShipCore shipCore2 = (TileEntityShipCore) tileEntity;
			
			// Skip invalid ships
			if (!shipCore2.isAssemblyValid()) {
				continue;
			}
			
			// Skip offline ship cores
			if (shipCore2.isOffline()) {
				continue;
			}
			
			// Skip lower tiers ships
			if (shipCore2.getTierIndex() < shipCore1.getTierIndex()) {
				continue;
			}
			
			// ship is intersecting, online and valid
			return shipCore2;
		}
		
		return null;
	}
	
	// do not call during tileEntity construction (readFromNBT and validate)
	private static boolean isExceptionReported = false;
	private void cleanup() {
		if (!Commons.throttleMe("Starmap registry cleanup", 180000)) {
			return;
		}
		LocalProfiler.start("Starmap registry cleanup");
		
		boolean isValid;
		for (final Map.Entry<Integer, CopyOnWriteArraySet<StarMapRegistryItem>> entryDimension : registry.entrySet()) {
			final WorldServer world = DimensionManager.getWorld(entryDimension.getKey());
			// skip unloaded worlds
			if (world == null) {
				continue;
			}
			for (final StarMapRegistryItem registryItem : entryDimension.getValue()) {
				isValid = false;
				if (registryItem != null) {
					
					boolean isLoaded;
					if (world.getChunkProvider() instanceof ChunkProviderServer) {
						final ChunkProviderServer chunkProviderServer = world.getChunkProvider();
						try {
							final Chunk chunk = chunkProviderServer.loadedChunks.get(ChunkPos.asLong(registryItem.x >> 4, registryItem.z >> 4));
							isLoaded = chunk != null && chunk.isLoaded();
						} catch (final NoSuchFieldError exception) {
							if (!isExceptionReported) {
								exception.printStackTrace(WarpDrive.printStreamError);
								WarpDrive.logger.info(String.format("Unable to check non-loaded chunks for star map entry %s",
								                                    registryItem));
								isExceptionReported = true;
							}
							isLoaded = chunkProviderServer.chunkExists(registryItem.x >> 4, registryItem.z >> 4);
						}
					} else {
						isLoaded = world.getChunkProvider().chunkExists(registryItem.x >> 4, registryItem.z >> 4);
					}
					// skip unloaded chunks
					if (!isLoaded) {
						if (WarpDrive.isDev) {
							WarpDrive.logger.info(String.format("Skipping non-loaded star map entry %s",
							                                    registryItem));
						}
						continue;
					}
					
					// get block and tile entity
					final Block block = world.getBlockState(new BlockPos(registryItem.x, registryItem.y, registryItem.z)).getBlock();
					
					final TileEntity tileEntity = world.getTileEntity(new BlockPos(registryItem.x, registryItem.y, registryItem.z));
					isValid = true;
					switch (registryItem.type) {
					case UNDEFINED:
						break;
					case SHIP:
						isValid = block instanceof BlockShipCore && tileEntity != null && !tileEntity.isInvalid();
						break;
					case JUMP_GATE:
						// isValid = block == WarpDrive.blockJumpGateCore && tileEntity != null && !tileEntity.isInvalid();
						break;
					case PLANET:
						break;
					case STAR:
						break;
					case STRUCTURE:
						break;
					case WARP_ECHO:
						break;
					case ACCELERATOR:
						isValid = block instanceof BlockAcceleratorCore && tileEntity != null && !tileEntity.isInvalid();
						break;
					case TRANSPORTER:
						isValid = block instanceof BlockTransporterCore && tileEntity != null && !tileEntity.isInvalid();
						break;
					case VIRTUAL_ASSISTANT:
						isValid = block instanceof BlockVirtualAssistant && tileEntity != null && !tileEntity.isInvalid();
						break;
					default:
						break;
					}
				}
				
				if (!isValid) {
					// if (WarpDriveConfig.LOGGING_STARMAP) {
						if (registryItem == null) {
							WarpDrive.logger.info("Cleaning up starmap object ~null~");
						} else {
							WarpDrive.logger.info(String.format("Cleaning up starmap object %s at dimension %d (%d %d %d)",
							                                    registryItem.type,
							                                    registryItem.dimensionId, registryItem.x, registryItem.y, registryItem.z));
						}
					// }
					countRemove++;
					entryDimension.getValue().remove(registryItem);
				}
			}
		}
		
		LocalProfiler.stop();
	}
	
	public void readFromNBT(final NBTTagCompound tagCompound) {
		if (tagCompound == null || !tagCompound.hasKey("starMapRegistryItems")) {
			registry.clear();
			return;
		}
		
		// read all entries in a flat structure
		final NBTTagList tagList = tagCompound.getTagList("starMapRegistryItems", Constants.NBT.TAG_COMPOUND);
		final StarMapRegistryItem[] registryFlat = new StarMapRegistryItem[tagList.tagCount()];
		final HashMap<Integer, Integer> sizeDimensions = new HashMap<>();
		for (int index = 0; index < tagList.tagCount(); index++) {
			final StarMapRegistryItem starMapRegistryItem = new StarMapRegistryItem(tagList.getCompoundTagAt(index));
			registryFlat[index] = starMapRegistryItem;
			
			// update stats
			Integer count = sizeDimensions.computeIfAbsent(starMapRegistryItem.dimensionId, k -> 0);
			count++;
			sizeDimensions.put(starMapRegistryItem.dimensionId, count);
		}
		
		// pre-build the local collections using known stats to avoid re-allocations
		final HashMap<Integer, ArrayList<StarMapRegistryItem>> registryLocal = new HashMap<>();
		for (final Entry<Integer, Integer> entryDimension : sizeDimensions.entrySet()) {
			registryLocal.put(entryDimension.getKey(), new ArrayList<>(entryDimension.getValue()));
		}
		
		// fill the local collections
		for (final StarMapRegistryItem starMapRegistryItem : registryFlat) {
			registryLocal.get(starMapRegistryItem.dimensionId).add(starMapRegistryItem);
		}
		
		// transfer to main one
		registry.clear();
		for (final Entry<Integer, ArrayList<StarMapRegistryItem>> entry : registryLocal.entrySet()) {
			registry.put(entry.getKey(), new CopyOnWriteArraySet<>(entry.getValue()));
		}
	}
	
	public void writeToNBT(final NBTTagCompound tagCompound) {
		final NBTTagList tagList = new NBTTagList();
		for (final CopyOnWriteArraySet<StarMapRegistryItem> starMapRegistryItems : registry.values()) {
			for (final StarMapRegistryItem starMapRegistryItem : starMapRegistryItems) {
				final NBTTagCompound tagCompoundItem = new NBTTagCompound();
				starMapRegistryItem.writeToNBT(tagCompoundItem);
				tagList.appendTag(tagCompoundItem);
			}
		}
		tagCompound.setTag("starMapRegistryItems", tagList);
	}
}
