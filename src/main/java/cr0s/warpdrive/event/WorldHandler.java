package cr0s.warpdrive.event;

import cr0s.warpdrive.BreathingManager;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.network.PacketHandler;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;

/**
*
* @author LemADEC
*/
public class WorldHandler {
	
	//TODO: register as event receiver
	public void onChunkLoaded(final ChunkWatchEvent event) {
		final Chunk chunk = event.getChunkInstance();
		assert chunk != null;
		
		// Check chunk for locating in cloaked areas
		WarpDrive.logger.info(String.format("onChunkLoaded %d %d", chunk.x, chunk.z));
		WarpDrive.cloaks.onChunkLoaded(event.getPlayer(), chunk.x, chunk.z);
		
		/*
		List<Chunk> list = new ArrayList<Chunk>();
		list.add(c);
		
		// Send obscured chunk
		WarpDrive.logger.info(String.format("[Cloak] Sending to player %s obscured chunk at (%d %d)",
		                                    p, chunk.x, chunk.z));
		((EntityPlayerMP)p).connection.sendPacketToPlayer(new Packet56MapChunks(list));
		*/
	}
	
	// Server side
	@SubscribeEvent
	public void onEntityJoinWorld(final EntityJoinWorldEvent event){
		if (event.getWorld().isRemote) {
			return;
		}
		// WarpDrive.logger.info(String.format("onEntityJoinWorld %s", event.entity));
		if (event.getEntity() instanceof EntityLivingBase) {
			final EntityLivingBase entityLivingBase = (EntityLivingBase) event.getEntity();
			final int x = MathHelper.floor(entityLivingBase.posX);
			final int y = MathHelper.floor(entityLivingBase.posY);
			final int z = MathHelper.floor(entityLivingBase.posZ);
			final CelestialObject celestialObject = CelestialObjectManager.get(event.getWorld(), x, z);
			
			if (entityLivingBase instanceof EntityPlayerMP) {
				WarpDrive.cloaks.onPlayerJoinWorld((EntityPlayerMP) entityLivingBase, event.getWorld());
				PacketHandler.sendClientSync((EntityPlayerMP) entityLivingBase, celestialObject);
				
			} else {
				if (celestialObject == null) {
					// unregistered dimension => exit
					return;
				}
				if (entityLivingBase.ticksExisted > 5) {
					// just changing dimension
					return;
				}
				if (!celestialObject.hasAtmosphere()) {
					final boolean canJoin = BreathingManager.onLivingJoinEvent(entityLivingBase, x, y, z);
					if (!canJoin) {
						event.setCanceled(true);
					}
				}
				if (!celestialObject.isInsideBorder(entityLivingBase.posX, entityLivingBase.posZ)) {
					event.setCanceled(true);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerChangedDimension(final PlayerChangedDimensionEvent event) {
		WarpDrive.logger.info(String.format("onPlayerChangedDimension %s %d -> %d",
		                                    event.player.getName(), event.fromDim, event.toDim ));
		WarpDrive.cloaks.onPlayerJoinWorld((EntityPlayerMP) event.player, ((EntityPlayerMP) event.player).world);
	}
	
	// Client side
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onClientConnectedToServer(final ClientConnectedToServerEvent event) {
		// WarpDrive.logger.info(String.format("onClientConnectedToServer connectionType %s isLocal %s", event.connectionType, event.isLocal));
		WarpDrive.cloaks.onClientChangingDimension();
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onWorldUnload(final WorldEvent.Unload event) {
		// WarpDrive.logger.info(String.format("onWorldUnload world %s", Commons.format(event.getWorld()));
		WarpDrive.cloaks.onClientChangingDimension();
	}
	
	@SubscribeEvent
	public void onServerTick(final ServerTickEvent event) {
		if (event.side != Side.SERVER || event.phase != Phase.END) {
			return;
		}
		
		AbstractSequencer.updateTick();
		LivingHandler.updateTick();
	}
	
	@SubscribeEvent
	public void onBlockUpdated(final BlockEvent blockEvent) {
		if (WarpDriveConfig.LOGGING_BREAK_PLACE && WarpDrive.isDev) {
			WarpDrive.logger.info(String.format("onBlockUpdate args %s actual %s",
			                                    blockEvent.getState(), blockEvent.getWorld().getBlockState(blockEvent.getPos())));
		}
		if ( WarpDriveConfig.isGregtechLoaded
		  && blockEvent.getWorld().getWorldInfo().getWorldName().equals("DummyServer") ) {
			return;
		}
		WarpDrive.starMap.onBlockUpdated(blockEvent.getWorld(), blockEvent.getPos(), blockEvent.getState());
		ChunkHandler.onBlockUpdated(blockEvent.getWorld(), blockEvent.getPos().getX(), blockEvent.getPos().getY(), blockEvent.getPos().getZ());
	}
}
