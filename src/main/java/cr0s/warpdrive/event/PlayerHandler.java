package cr0s.warpdrive.event;

import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.OfflineAvatarManager;

import javax.annotation.Nonnull;

import net.minecraftforge.event.entity.player.PlayerEvent;
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
}
