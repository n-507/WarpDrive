package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.block.forcefield.BlockForceField;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumGlobalRegionType;
import cr0s.warpdrive.data.GlobalRegion;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.data.OfflineAvatarManager;

import javax.annotation.Nonnull;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;

public class PlayerHandler {
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerLoadFromFile(@Nonnull final PlayerEvent.LoadFromFile event) {
		OfflineAvatarManager.onPlayerLoggedIn(event.getEntityPlayer());
	}
	
	@SubscribeEvent
	public void onPlayerLoggedOut(@Nonnull final PlayerLoggedOutEvent event) {
		if (WarpDriveConfig.OFFLINE_AVATAR_ENABLE) {
			OfflineAvatarManager.onPlayerLoggedOut(event.player);
		}
	}
	
	@SubscribeEvent
	public void onBreakSpeed(@Nonnull final BreakSpeed event) {
		final EntityPlayer entityPlayer = event.getEntityPlayer();
		final BlockPos blockPos = entityPlayer.getPosition();
		
		// check for lock
		doCancelEventDuringJump(event, event.getPos());
		if (event.isCanceled()) {
			return;
		}
		
		// check for maintenance boost
		final IBlockState blockState = event.getState();
		if ( !(blockState.getBlock() instanceof IBlockBase)
		  || blockState.getBlock() instanceof BlockForceField
		  || blockState.getBlockHardness(entityPlayer.world, blockPos) < WarpDriveConfig.HULL_HARDNESS[1] ) {
			return;
		}
		final GlobalRegion globalRegion = GlobalRegionManager.getNearest(EnumGlobalRegionType.SHIP, entityPlayer.world, blockPos);
		if ( globalRegion == null
		  || !globalRegion.contains(blockPos) ) {
			return;
		}
		
		// skip enabled or invalid ship cores
		final TileEntity tileEntity = entityPlayer.world.getTileEntity(globalRegion.getBlockPos());
		if (!(tileEntity instanceof TileEntityShipCore)) {
			WarpDrive.logger.error(String.format("Unable to adjust harvest speed due to invalid tile entity for global region, expecting TileEntityShipCore, got %s",
			                                     this ));
			return;
		}
		final TileEntityShipCore tileEntityShipCoreClosest = (TileEntityShipCore) tileEntity;
		if ( !tileEntityShipCoreClosest.isAssemblyValid()
		  || tileEntityShipCoreClosest.getIsEnabled() ) {
			return;
		}
		
		// skip overlapping tier ship cores with same or higher tiers
		final TileEntityShipCore tileEntityShipCoreIntersect = GlobalRegionManager.getIntersectingShipCore(tileEntityShipCoreClosest);
		if (tileEntityShipCoreIntersect == null) {
			final int indexTier = ((IBlockBase) blockState.getBlock()).getTier(null).getIndex();
			event.setNewSpeed(100.0F * indexTier);
		}
	}
	
	@SubscribeEvent
	public void onEntityItemPickup(@Nonnull final EntityItemPickupEvent event) {
		doCancelEventDuringJump(event, event.getItem().getPosition());
	}
	
	@SubscribeEvent
	public void onRightClickBlock(@Nonnull final RightClickBlock event) {
		doCancelEventDuringJump(event, event.getPos());
	}
	
	private void doCancelEventDuringJump(@Nonnull final PlayerEvent event, @Nonnull final BlockPos blockPos) {
		assert event.isCancelable();
		if (event.isCanceled()) {
			return;
		}
		
		if (AbstractSequencer.isLocked(blockPos)) {
			event.setCanceled(true);
		}
	}
}
