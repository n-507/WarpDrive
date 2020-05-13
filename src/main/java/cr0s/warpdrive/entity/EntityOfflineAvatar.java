package cr0s.warpdrive.entity;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.OfflineAvatarManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.UUID;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;

import com.google.common.base.Optional;

public class EntityOfflineAvatar extends EntityLiving {
	
	// persistent properties
	private static final DataParameter<Optional<UUID>> DATA_PLAYER_UUID = EntityDataManager.createKey(EntityOfflineAvatar.class, DataSerializers.OPTIONAL_UNIQUE_ID);
	private static final DataParameter<String>         DATA_PLAYER_NAME = EntityDataManager.createKey(EntityOfflineAvatar.class, DataSerializers.STRING);
	
	// computed properties
	private boolean isDirtyGlobalPosition = true;
	private int tickUpdateGlobalPosition = 0;
	
	public EntityOfflineAvatar(@Nonnull final World world) {
		super(world);
		
		setCanPickUpLoot(false);
		setNoAI(true);
		setCustomNameTag("Offline avatar");
		setAlwaysRenderNameTag(WarpDriveConfig.OFFLINE_AVATAR_ALWAYS_RENDER_NAME_TAG);
		setSize(width * WarpDriveConfig.OFFLINE_AVATAR_MODEL_SCALE, height  * WarpDriveConfig.OFFLINE_AVATAR_MODEL_SCALE);
	}
	
	@Override
	protected void entityInit() {
		super.entityInit();
		
		dataManager.register(DATA_PLAYER_UUID, Optional.absent());
		dataManager.register(DATA_PLAYER_NAME, "");
	}
	
	public void setPlayer(@Nonnull final UUID uuidPlayer, @Nonnull final String namePlayer) {
		dataManager.set(DATA_PLAYER_UUID, Optional.of(uuidPlayer));
		dataManager.set(DATA_PLAYER_NAME, namePlayer);
	}
	
	@Nullable
	public UUID getPlayerUUID() {
		return dataManager.get(DATA_PLAYER_UUID).orNull();
	}
	
	public String getPlayerName() {
		return dataManager.get(DATA_PLAYER_NAME);
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		if (world.isRemote) {
			return;
		}
		
		// update registry
		if (isDirtyGlobalPosition) {
			tickUpdateGlobalPosition = 0;
		}
		tickUpdateGlobalPosition--;
		if (tickUpdateGlobalPosition <= 0) {
			tickUpdateGlobalPosition = WarpDriveConfig.G_REGISTRY_UPDATE_INTERVAL_TICKS;
			isDirtyGlobalPosition = false;
			
			final UUID uuidPlayer = getPlayerUUID();
			if (uuidPlayer == null) {
				// cleanup invalid entities
				if (ticksExisted > 5) {
					WarpDrive.logger.error(String.format("Removing invalid EntityOfflineAvatar with no UUID %s",
					                                     this ));
					setDead();
				}
				
			} else {
				// cleanup online players
				final EntityPlayer entityPlayer = Commons.getOnlinePlayerByUUID(uuidPlayer);
				if ( entityPlayer != null
				  && world.provider.getDimension() == entityPlayer.world.provider.getDimension()
				  && isInRange(entityPlayer) ) {// (actually online in close proximity)
					OfflineAvatarManager.remove(this);
					setDead();
					
				} else {// (actually offline or far away)
					OfflineAvatarManager.update(this);
				}
			}
		}
	}
	
	private boolean isInRange(@Nonnull final EntityPlayer entityPlayer) {
		final float distance = entityPlayer.getDistance(this);
		return distance >= WarpDriveConfig.OFFLINE_AVATAR_MIN_RANGE_FOR_REMOVAL
		    && distance <= WarpDriveConfig.OFFLINE_AVATAR_MAX_RANGE_FOR_REMOVAL; 
	}
	
	@Override
	public void setDead() {
		super.setDead();
		
		if (WarpDriveConfig.OFFLINE_AVATAR_FORGET_ON_DEATH) {
			OfflineAvatarManager.remove(this);
		}
	}
	
	@Override
	public float getRenderSizeModifier() {
		return WarpDriveConfig.OFFLINE_AVATAR_MODEL_SCALE;
	}
	
	@Override
	public void readEntityFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readEntityFromNBT(tagCompound);
		
		final UUID uuidPlayer = tagCompound.getUniqueId("player");
		final String namePlayer = tagCompound.getString("playerName");
		if ( uuidPlayer == null
		  || namePlayer.isEmpty() ) {
			WarpDrive.logger.error(String.format("Removing on reading invalid offline avatar in %s",
			                                     tagCompound ));
			setDead();
			return;
		}
		setPlayer(uuidPlayer, namePlayer);
	}
	
	@Override
	public void writeEntityToNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.writeEntityToNBT(tagCompound);
		
		final UUID uuidPlayer = getPlayerUUID();
		if (uuidPlayer == null) {
			WarpDrive.logger.error(String.format("Removing on writing invalid offline avatar in %s",
			                                     tagCompound ));
			setDead();
			return;
		}
		tagCompound.setUniqueId("player", uuidPlayer);
		tagCompound.setString("playerName", getPlayerName());
	}
	
	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
	}
	
	@Override
	protected boolean isMovementBlocked() {
		return true;
	}
	
	@Override
	public boolean getAlwaysRenderNameTagForRender() {
		return super.getAlwaysRenderNameTagForRender();
	}
	
	@Override
	public boolean getAlwaysRenderNameTag() {
		return super.getAlwaysRenderNameTag();
	}
}
