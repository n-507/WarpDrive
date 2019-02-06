package cr0s.warpdrive.world;

import cr0s.warpdrive.data.Vector3;

import javax.annotation.Nonnull;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.util.ITeleporter;

public class SpaceTeleporter implements ITeleporter {
	
	final Vector3 v3Destination;
	final WorldServer worldServerDestination;
	
	public SpaceTeleporter(@Nonnull final WorldServer worldServerDestination, @Nonnull final Vector3 v3Destination) {
		this.v3Destination = v3Destination;
		this.worldServerDestination = worldServerDestination;
	}
	
	@Override
	public void placeEntity(final World world, @Nonnull final Entity entity, final float yaw) {
		if (entity instanceof EntityPlayerMP) {
			((EntityPlayerMP)entity).connection.setPlayerLocation(v3Destination.x, v3Destination.y, v3Destination.z, yaw, entity.rotationPitch);
		} else {
			entity.setLocationAndAngles(v3Destination.x, v3Destination.y, v3Destination.z, entity.rotationYaw, entity.rotationPitch);
		}
	}
}
