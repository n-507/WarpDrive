package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.entity.EntityOfflineAvatar;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;

public class OfflineAvatarManager {
	
	private static final HashMap<UUID, GlobalPosition> registry = new HashMap<>(512);
	
	public static void update(@Nonnull final EntityOfflineAvatar entityOfflineAvatar) {
		// validate context
		if (!Commons.isSafeThread()) {
			WarpDrive.logger.error(String.format("Non-threadsafe call to OfflineAvatarManager:update outside main thread, for %s",
			                                     entityOfflineAvatar ));
			return;
		}
		if (entityOfflineAvatar.getPlayerUUID() == null) {
			WarpDrive.logger.error(String.format("Ignoring update for invalid EntityOfflineAvatar with no UUID %s",
			                                     entityOfflineAvatar ));
			return;
		}
		
		// add new entry
		// or update existing entry
		GlobalPosition globalPosition = registry.get(entityOfflineAvatar.getPlayerUUID());
		if ( globalPosition == null
		  || globalPosition.dimensionId != entityOfflineAvatar.world.provider.getDimension()
		  || globalPosition.x != (int) Math.floor(entityOfflineAvatar.posX)
		  || globalPosition.y != (int) Math.floor(entityOfflineAvatar.posY)
		  || globalPosition.z != (int) Math.floor(entityOfflineAvatar.posZ) ) {
			globalPosition = new GlobalPosition(entityOfflineAvatar);
			registry.put(entityOfflineAvatar.getPlayerUUID(), globalPosition);
		}
	}
	
	public static void remove(@Nonnull final EntityOfflineAvatar entityOfflineAvatar) {
		// validate context
		if (!Commons.isSafeThread()) {
			WarpDrive.logger.error(String.format("Non-threadsafe call to OfflineAvatarManager:remove outside main thread, for %s",
			                                     entityOfflineAvatar ));
			return;
		}
		if (entityOfflineAvatar.getPlayerUUID() == null) {
			WarpDrive.logger.error(String.format("Ignoring removal for invalid EntityOfflineAvatar with no UUID %s",
			                                     entityOfflineAvatar ));
			return;
		}
		
		// remove existing entry, if coordinates are matching
		final GlobalPosition globalPosition = registry.get(entityOfflineAvatar.getPlayerUUID());
		if ( globalPosition != null
		  && globalPosition.dimensionId == entityOfflineAvatar.world.provider.getDimension()
		  && globalPosition.x == (int) Math.floor(entityOfflineAvatar.posX)
		  && globalPosition.y == (int) Math.floor(entityOfflineAvatar.posY)
		  && globalPosition.z == (int) Math.floor(entityOfflineAvatar.posZ) ) {
			registry.remove(entityOfflineAvatar.getPlayerUUID());
		}
	}
	
	@Nullable
	public static GlobalPosition get(@Nonnull final UUID uuidPlayer) {
		// validate context
		if (!Commons.isSafeThread()) {
			WarpDrive.logger.error(String.format("Non-threadsafe call to OfflineAvatarManager:get outside main thread, for %s",
			                                     uuidPlayer ));
			return null;
		}
		
		// get existing entry
		return registry.get(uuidPlayer);
	}
	
	public static void readFromNBT(@Nullable final NBTTagCompound tagCompound) {
		if ( tagCompound == null
		  || !tagCompound.hasKey("offlineAvatars") ) {
			registry.clear();
			return;
		}
		
		// read all entries in a pre-build local collections using known stats to avoid re-allocations
		final NBTTagList tagList = tagCompound.getTagList("offlineAvatars", Constants.NBT.TAG_COMPOUND);
		final HashMap<UUID, GlobalPosition> registryLocal = new HashMap<>(tagList.tagCount());
		for (int index = 0; index < tagList.tagCount(); index++) {
			final NBTTagCompound tagCompoundItem = tagList.getCompoundTagAt(index);
			final UUID uuid = tagCompoundItem.getUniqueId("");
			final GlobalPosition globalPosition = new GlobalPosition(tagCompoundItem);
			registryLocal.put(uuid, globalPosition);
		}
		
		// transfer to main one
		registry.clear();
		registry.putAll(registryLocal);
		for (final Entry<UUID, GlobalPosition> entry : registryLocal.entrySet()) {
			registry.put(entry.getKey(), entry.getValue());
		}
	}
	
	public static void writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		final NBTTagList tagList = new NBTTagList();
		for (final Entry<UUID, GlobalPosition> entry : registry.entrySet()) {
			final NBTTagCompound tagCompoundItem = new NBTTagCompound();
			tagCompoundItem.setUniqueId("", entry.getKey());
			entry.getValue().writeToNBT(tagCompoundItem);
			tagList.appendTag(tagCompoundItem);
		}
		tagCompound.setTag("offlineAvatars", tagList);
	}
	
	public static void onPlayerLoggedOut(@Nonnull final EntityPlayer entityPlayer) {
		// skip dead players
		if (entityPlayer.isDead) {
			return;
		}
		
		// skip players away from a ship
		final World world = entityPlayer.world;
		final BlockPos blockPos = entityPlayer.getPosition();
		if (WarpDriveConfig.OFFLINE_AVATAR_CREATE_ONLY_ABOARD_SHIPS) {
			final GlobalRegion globalRegionNearestShip = GlobalRegionManager.getNearest(EnumGlobalRegionType.SHIP, world, blockPos);
			if ( globalRegionNearestShip == null
			  || !globalRegionNearestShip.contains(blockPos) ) {
				return;
			}
		}
		
		// spawn an offline avatar entity at location
		WarpDrive.logger.debug(String.format("Spawning offline avatar for %s",
		                                     entityPlayer ));
		final EntityOfflineAvatar entityOfflineAvatar = new EntityOfflineAvatar(world);
		entityOfflineAvatar.setPosition(blockPos.getX() + 0.5D, blockPos.getY() + 0.1D, blockPos.getZ() + 0.5D);
		entityOfflineAvatar.setCustomNameTag(entityPlayer.getDisplayNameString());
		entityOfflineAvatar.setPlayer(entityPlayer.getUniqueID(), entityPlayer.getName());
		// copy equipment with a marker to remember those aren't 'legit' items
		for (final EntityEquipmentSlot entityEquipmentSlot : EntityEquipmentSlot.values()) {
			final ItemStack itemStack = entityPlayer.getItemStackFromSlot(entityEquipmentSlot).copy();
			if (!itemStack.isEmpty()) {
				if (!itemStack.hasTagCompound()) {
					itemStack.setTagCompound(new NBTTagCompound());
				}
				assert itemStack.getTagCompound() != null;
				itemStack.getTagCompound().setBoolean("isFakeItem", true);
				entityOfflineAvatar.setItemStackToSlot(entityEquipmentSlot, itemStack);
				entityOfflineAvatar.setDropChance(entityEquipmentSlot, 0.0F);
			}
		}
		world.spawnEntity(entityOfflineAvatar);
	}
	
	public static void onPlayerLoggedIn(@Nonnull final EntityPlayer entityPlayer) {
		assert !entityPlayer.isAddedToWorld();
		
		// skip if we have no record
		final GlobalPosition globalPosition = registry.get(entityPlayer.getUniqueID());
		if (globalPosition == null) {
			return;
		}
		
		// skip if player is already close by
		if (globalPosition.dimensionId == entityPlayer.dimension) {
			final double distance = entityPlayer.getDistance(globalPosition.x + 0.5D, globalPosition.y, globalPosition.z + 0.5D);
			if (distance < WarpDriveConfig.OFFLINE_AVATAR_MAX_RANGE_FOR_REMOVAL) {
				return;
			}
		}
		
		// teleport player
		// note: this is done before server loads the player's world
		entityPlayer.dimension = globalPosition.dimensionId;
		entityPlayer.setPosition(globalPosition.x, globalPosition.y, globalPosition.z);
	}
}
