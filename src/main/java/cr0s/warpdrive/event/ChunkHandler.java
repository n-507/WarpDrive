package cr0s.warpdrive.event;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.ExceptionChunkNotLoaded;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.ChunkData;
import cr0s.warpdrive.data.StateAir;

import javax.annotation.Nonnull;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ChunkEvent.Unload;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;

public class ChunkHandler {
	
	private static final long CHUNK_HANDLER_UNLOADED_CHUNK_MAX_AGE_MS = 30000L;
	
	// persistent properties
	private static final Map<Integer, Map<Long, ChunkData>> registryClient = new HashMap<>(32);
	private static final Map<Integer, Map<Long, ChunkData>> registryServer = new HashMap<>(32);
	
	// computed properties
	public static long delayLogging = 0;
	
	/* event catchers */
	@SubscribeEvent
	public void onLoadWorld(final WorldEvent.Load event) {
		if (event.getWorld().isRemote || event.getWorld().provider.getDimension() == 0) {
			if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
				WarpDrive.logger.info(String.format("%s world %s load.",
				                                    event.getWorld().isRemote ? "Client" : "Server",
				                                    Commons.format(event.getWorld())));
			}
		}
		
		if ( !event.getWorld().isRemote
		  && event.getWorld().provider.getDimension() == 0 ) {
			// load star map
			final String filename = String.format("%s/%s.dat", event.getWorld().getSaveHandler().getWorldDirectory().getPath(), WarpDrive.MODID);
			final NBTTagCompound tagCompound = Commons.readNBTFromFile(filename);
			WarpDrive.starMap.readFromNBT(tagCompound);
		}
	}
	
	// new chunks aren't loaded
	public static void onGenerated(final World world, final int chunkX, final int chunkZ) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk [%d, %d] generating",
			                                    world.isRemote ? "Client" : "Server",
			                                    Commons.format(world),
			                                    chunkX, chunkZ));
		}
		
		final ChunkData chunkData = getChunkData(world.isRemote, world.provider.getDimension(), chunkX, chunkZ, true);
		assert chunkData != null;
		// (world can load a non-generated chunk, or the chunk be regenerated, so we reset only as needed)
		if (!chunkData.isLoaded()) { 
			chunkData.load(new NBTTagCompound());
		}
	}
	
	// (server side only)
	@SubscribeEvent
	public void onLoadChunkData(final ChunkDataEvent.Load event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s loading data (1)", 
			                                    event.getWorld().isRemote ? "Client" : "Server",
			                                    Commons.format(event.getWorld()),
			                                    event.getChunk().getPos()));
		}
		
		final ChunkData chunkData = getChunkData(event.getWorld().isRemote, event.getWorld().provider.getDimension(), event.getChunk().x, event.getChunk().z, true);
		assert chunkData != null;
		chunkData.load(event.getData());
	}
	
	// (called after data loading, or before a late generation, or on client side) 
	@SubscribeEvent
	public void onLoadChunk(final ChunkEvent.Load event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s loaded (2)",
			                                    event.getWorld().isRemote ? "Client" : "Server",
			                                    Commons.format(event.getWorld()),
			                                    event.getChunk().getPos()));
		}
		
		final ChunkData chunkData = getChunkData(event.getWorld().isRemote, event.getWorld().provider.getDimension(), event.getChunk().x, event.getChunk().z, true);
		assert chunkData != null;
		if (!chunkData.isLoaded()) {
			chunkData.load(new NBTTagCompound());
		}
	}
	/*
	// (server side only)
	@SubscribeEvent
	public void onWatchChunk(ChunkWatchEvent.Watch event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("World %s chunk %s watch by %s",
			                                    Commons.format(event.getPlayer().world),
			                                    event.getChunk(),
			                                    event.getPlayer()));
		}
	}
	/**/
	// (server side only)
	// not called when chunk wasn't changed since last save?
	@SubscribeEvent
	public void onSaveChunkData(final ChunkDataEvent.Save event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s save data",
			                                    event.getWorld().isRemote ? "Client" : "Server",
			                                    Commons.format(event.getWorld()),
			                                    event.getChunk().getPos()));
		}
		final ChunkData chunkData = getChunkData(event.getWorld().isRemote, event.getWorld().provider.getDimension(), event.getChunk().x, event.getChunk().z, false);
		if (chunkData != null) {
			chunkData.save(event.getData());
		} else if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.error(String.format("%s world %s chunk %s is saving data without loading it first!",
			                                     event.getWorld().isRemote ? "Client" : "Server",
			                                     Commons.format(event.getWorld()),
			                                     event.getChunk().getPos()));
		}
	}
	
	// (server side only)
	@SubscribeEvent
	public void onSaveWorld(final WorldEvent.Save event) {
		if (event.getWorld().provider.getDimension() != 0) {
			return;
		}
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s saved.",
			                                    event.getWorld().isRemote ? "Client" : "Server",
			                                    Commons.format(event.getWorld())));
		}
		
		if (event.getWorld().isRemote) {
			return;
		}
		
		// save star map
		final String filename = String.format("%s/%s.dat", event.getWorld().getSaveHandler().getWorldDirectory().getPath(), WarpDrive.MODID);
		final NBTTagCompound tagCompound = new NBTTagCompound();
		WarpDrive.starMap.writeToNBT(tagCompound);
		Commons.writeNBTToFile(filename, tagCompound);
	}
	
	@SubscribeEvent
	public void onUnloadWorld(final WorldEvent.Unload event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s unload",
			                                    event.getWorld().isRemote ? "Client" : "Server",
			                                    Commons.format(event.getWorld())));
		}
		
		// get dimension data
		LocalProfiler.updateCallStat("onUnloadWorld");
		final Map<Integer, Map<Long, ChunkData>> registry = event.getWorld().isRemote ? registryClient : registryServer;
		final Map<Long, ChunkData> mapRegistryItems = registry.get(event.getWorld().provider.getDimension());
		if (mapRegistryItems != null) {
			// unload chunks during shutdown
			for (final ChunkData chunkData : mapRegistryItems.values()) {
				if (chunkData.isLoaded()) {
					chunkData.unload();
				}
			}
		}
		
		// @TODO unload star map
	}
	
	
	// (not called when closing SSP game)
	@SubscribeEvent
	public void onUnloadChunk(final Unload event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s unload",
			                                    event.getWorld().isRemote ? "Client" : "Server",
			                                    Commons.format(event.getWorld()),
			                                    event.getChunk().getPos()));
		}
		
		final ChunkData chunkData = getChunkData(event.getWorld().isRemote, event.getWorld().provider.getDimension(), event.getChunk().x, event.getChunk().z, false);
		if (chunkData != null) {
			chunkData.unload();
		} else if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.error(String.format("%s world %s chunk %s is unloading without loading it first!", 
			                                     event.getWorld().isRemote ? "Client" : "Server",
			                                     Commons.format(event.getWorld()),
			                                     event.getChunk().getPos()));
		}
	}
	/*
	// (not called when closing SSP game)
	// warning: will return invalid world when switching dimensions
	@SubscribeEvent
	public void onUnwatchChunk(ChunkWatchEvent.UnWatch event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s unwatch by %s",
			                                    event.player.world.isRemote ? "Client" : "Server",
			                                    Commons.format(event.player.world),
			                                    event.chunk,
			                                    event.player));
		}
	}
	/**/
	@SubscribeEvent
	public void onWorldTick(final WorldTickEvent event) {
		if (event.side != Side.SERVER || event.phase != Phase.END) {
			return;
		}
		updateTick(event.world);
	}
	
	@SuppressWarnings("ConstantConditions")
	public static void onBlockUpdated(@Nonnull final World world, @Nonnull final BlockPos blockPos) {
		if (!world.isRemote) {
			final ChunkData chunkData = getChunkData(world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
			if (chunkData != null) {
				chunkData.onBlockUpdated(blockPos.getX(), blockPos.getY(), blockPos.getZ());
			} else {
				if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
					WarpDrive.logger.error(String.format("%s block updating %s, while chunk isn't loaded!",
					                                     world.isRemote ? "Client" : "Server",
					                                     Commons.format(world, blockPos)));
					Commons.dumpAllThreads();
				}
			}
		}
	}
	
	/* internal access */
	/**
	 * Return null and spam logs if chunk isn't already generated or loaded 
	 */
	public static ChunkData getChunkData(final World world, final int x, final int y, final int z) {
		final ChunkData chunkData = getChunkData(world.isRemote, world.provider.getDimension(), x, y, z);
		if (chunkData == null) {
			WarpDrive.logger.error(String.format("Trying to get data from an non-loaded chunk in %s %s",
			                                     world.isRemote ? "Client" : "Server",
			                                     Commons.format(world, x, y, z)));
			LocalProfiler.printCallStats();
			Commons.dumpAllThreads();
			assert false;
		}
		return chunkData;
	}
	
	/**
	 * Return null if chunk isn't already generated or loaded
	 */
	private static ChunkData getChunkData(final boolean isRemote, final int dimensionId, final int x, final int y, final int z) {
		assert y >= -1 && y <= 256;   // includes 1 block tolerance for mirroring
		return getChunkData(isRemote, dimensionId, x >> 4, z >> 4, false);
	}
	
	private static ChunkData getChunkData(final boolean isRemote, final int dimensionId, final int xChunk, final int zChunk, final boolean doCreate) {
		// get dimension data
		LocalProfiler.updateCallStat("getChunkData");
		final Map<Integer, Map<Long, ChunkData>> registry = isRemote ? registryClient : registryServer;
		Map<Long, ChunkData> mapRegistryItems = registry.get(dimensionId);
		// (lambda expressions are forcing synchronisation, so we don't use them here)
		if (mapRegistryItems == null) {
			if (!doCreate) {
				return null;
			}
			// TLongObjectMap<ChunkData> m = TCollections.synchronizedMap(new TLongObjectHashMap<ChunkData>(2048) );
			// @TODO: http://trove4j.sourceforge.net/javadocs/gnu/trove/TCollections.html#synchronizedMap(gnu.trove.map.TLongObjectMap)
			mapRegistryItems = new LinkedHashMap<>(2048); // Collections.synchronizedMap(new LinkedHashMap<>(2048));
			registry.put(dimensionId, mapRegistryItems);
		}
		// get chunk data
		final long index = ChunkPos.asLong(xChunk, zChunk);
		ChunkData chunkData = mapRegistryItems.get(index);
		// (lambda expressions are forcing synchronisation, so we don't use them here)
		if (chunkData == null) {
			if (!doCreate) {
				if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
					WarpDrive.logger.info(String.format("getChunkData(%s, %d, %d, %d, false) returning null",
					                                     isRemote, dimensionId, xChunk, zChunk));
				}
				return null;
			}
			chunkData = new ChunkData(xChunk, zChunk);
			if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
				WarpDrive.logger.info(String.format("%s world DIM%d chunk %s is being added to the registry",
				                                    isRemote ? "Client" : "Server",
				                                    dimensionId,
				                                    chunkData.getChunkCoords()));
			}
			if (Commons.isSafeThread()) {
				mapRegistryItems.put(index, chunkData);
			} else {
				WarpDrive.logger.error(String.format("%s world DIM%d chunk %s is being added to the registry outside main thread!",
				                                    isRemote ? "Client" : "Server",
				                                    dimensionId,
				                                    chunkData.getChunkCoords()));
				Commons.dumpAllThreads();
				mapRegistryItems.put(index, chunkData);
			}
		}
		return chunkData;
	}
	
	private static boolean isLoaded(final Map<Long, ChunkData> mapRegistryItems, final int xChunk, final int zChunk) {
		// get chunk data
		final long index = ChunkPos.asLong(xChunk, zChunk);
		final ChunkData chunkData = mapRegistryItems.get(index);
		return chunkData != null && chunkData.isLoaded();
	}
	
	/* commons */
	public static boolean isLoaded(final World world, final int x, final int y, final int z) {
		final ChunkData chunkData = getChunkData(world.isRemote, world.provider.getDimension(), x, y, z);
		return chunkData != null && chunkData.isLoaded();
	}
	
	/* air handling */
	public static StateAir getStateAir(final World world, final int x, final int y, final int z) {
		final ChunkData chunkData = getChunkData(world, x, y, z);
		if (chunkData == null) {
			// chunk isn't loaded, skip it
			return null;
		}
		try {
			return chunkData.getStateAir(world, x, y, z);
		} catch (final ExceptionChunkNotLoaded exceptionChunkNotLoaded) {
			WarpDrive.logger.warn(String.format("Aborting air evaluation: chunk isn't loaded %s",
			                                    Commons.format(world, x, y, z)));
			return null;
		}
	}
	
	private static void updateTick(final World world) {
		// get dimension data
		LocalProfiler.updateCallStat("updateTick");
		final Map<Integer, Map<Long, ChunkData>> registry = world.isRemote ? registryClient : registryServer;
		final Map<Long, ChunkData> mapRegistryItems = registry.get(world.provider.getDimension());
		if (mapRegistryItems == null) {
			return;
		}
		int countLoaded = 0;
		int countRemoved = 0;
		final long timeForRemoval = System.currentTimeMillis() - CHUNK_HANDLER_UNLOADED_CHUNK_MAX_AGE_MS;
		final long timeForThrottle = System.currentTimeMillis() + 200;
		final long sizeBefore = mapRegistryItems.size();
		long indexCurrent = 0L;
		
		try {
			
			for (final Iterator<Entry<Long, ChunkData>> entryIterator = mapRegistryItems.entrySet().iterator(); entryIterator.hasNext(); ) {
				final Map.Entry<Long, ChunkData> entryChunkData = entryIterator.next();
				indexCurrent = entryChunkData.getKey();
				final ChunkData chunkData = entryChunkData.getValue();
				// update loaded chunks, remove old unloaded chunks
				if (chunkData.isLoaded()) {
					countLoaded++;
					if (System.currentTimeMillis() < timeForThrottle) {
						updateTickLoopStep(world, mapRegistryItems, entryChunkData.getValue());
					}
				} else if (chunkData.timeUnloaded < timeForRemoval) {
					if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
						WarpDrive.logger.info(String.format("%s world %s chunk %s is being removed from updateTick (size is %d)",
						                                    world.isRemote ? "Client" : "Server",
						                                    Commons.format(world),
						                                    chunkData.getChunkCoords(),
						                                    mapRegistryItems.size()));
					}
					entryIterator.remove();
					countRemoved++;
				}
			}
			
		} catch (final ConcurrentModificationException exception) {
			WarpDrive.logger.error(String.format("%s world %s had some chunks changed outside main thread? (size %d -> %d, loaded %d, removed %d, index 0x%X x %d z %d)",
			                                     world.isRemote ? "Client" : "Server",
			                                     Commons.format(world),
			                                     sizeBefore, mapRegistryItems.size(), countLoaded, countRemoved,
			                                     indexCurrent, indexCurrent & 0xFFFFFFFFL, (indexCurrent >> 32) & 0xFFFFFFFFL));
			exception.printStackTrace();
			LocalProfiler.printCallStats();
		}
		
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			if (world.provider.getDimension() == 0) {
				delayLogging = (delayLogging + 1) % 4096;
			}
			if (delayLogging == 1) {
				WarpDrive.logger.info(String.format("Dimension %d has %d / %d chunks loaded",
				                                    world.provider.getDimension(),
				                                    countLoaded,
				                                    mapRegistryItems.size()));
			}
		}
	}
	
	// apparently, the GC triggers sooner when using sub-function here?
	private static void updateTickLoopStep(final World world, final Map<Long, ChunkData> mapRegistryItems, final ChunkData chunkData) {
		final ChunkPos chunkCoordIntPair = chunkData.getChunkCoords();
		// skip empty chunks (faster and more frequent)
		// ship chunk with unloaded neighbours
		if ( chunkData.isNotEmpty()
		  && isLoaded(mapRegistryItems, chunkCoordIntPair.x + 1, chunkCoordIntPair.z)
		  && isLoaded(mapRegistryItems, chunkCoordIntPair.x - 1, chunkCoordIntPair.z)
		  && isLoaded(mapRegistryItems, chunkCoordIntPair.x, chunkCoordIntPair.z + 1)
		  && isLoaded(mapRegistryItems, chunkCoordIntPair.x, chunkCoordIntPair.z - 1) ) {
			chunkData.updateTick(world);
		}
	}
}