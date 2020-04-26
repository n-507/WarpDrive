package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatHandler {
	
	@SubscribeEvent
	public void onServerChat(final ServerChatEvent event) {
		final boolean isCancelled = WarpDrive.starMap.onChatReceived(event.getPlayer(), event.getMessage());
		if ( isCancelled
		  && WarpDriveConfig.VIRTUAL_ASSISTANT_HIDE_COMMANDS_IN_CHAT ) {
			event.setCanceled(true);
		}
	}
}
