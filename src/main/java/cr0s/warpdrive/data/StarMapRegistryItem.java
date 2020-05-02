package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IStarMapRegistryTileEntity;
import cr0s.warpdrive.api.computer.ICoreSignature;

import javax.annotation.Nonnull;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class StarMapRegistryItem extends GlobalPosition {
	
	// persistent properties
	public final EnumStarMapEntryType type;
	public final UUID uuid;
	public int maxX, maxY, maxZ;
	public int minX, minY, minZ;
	public int mass;
	public double isolationRate;
	public String name;
	
	// computed properties
	private AxisAlignedBB cache_aabbArea;
	
	public StarMapRegistryItem(
	                          final EnumStarMapEntryType type, final UUID uuid,
	                          final int dimensionId, final int x, final int y, final int z,
	                          final AxisAlignedBB aabbArea,
	                          final int mass, final double isolationRate,
	                          final String name) {
		super(dimensionId, x, y, z);
		this.type = type;
		this.uuid = uuid;
		if (aabbArea == null) {
			this.minX = x;
			this.minY = y;
			this.minZ = z;
			this.maxX = x;
			this.maxY = y;
			this.maxZ = z;
		} else {
			this.minX = (int) aabbArea.minX;
			this.minY = (int) aabbArea.minY;
			this.minZ = (int) aabbArea.minZ;
			this.maxX = (int) aabbArea.maxX - 1;
			this.maxY = (int) aabbArea.maxY - 1;
			this.maxZ = (int) aabbArea.maxZ - 1;
		}
		this.mass = mass;
		this.isolationRate = isolationRate;
		this.name = name;
		
		this.cache_aabbArea = null;
	}
	
	public StarMapRegistryItem(final IStarMapRegistryTileEntity tileEntity) {
		this(
			tileEntity.getStarMapType(), tileEntity.getSignatureUUID(),
			((TileEntity) tileEntity).getWorld().provider.getDimension(),
			((TileEntity) tileEntity).getPos().getX(), ((TileEntity) tileEntity).getPos().getY(), ((TileEntity) tileEntity).getPos().getZ(),
			tileEntity.getStarMapArea(),
			tileEntity.getMass(), tileEntity.getIsolationRate(),
			tileEntity.getSignatureName() );
	}
	
	public boolean sameCoordinates(final IStarMapRegistryTileEntity tileEntity) {
		assert tileEntity instanceof TileEntity;
		return dimensionId == ((TileEntity) tileEntity).getWorld().provider.getDimension()
			&& x == ((TileEntity) tileEntity).getPos().getX()
			&& y == ((TileEntity) tileEntity).getPos().getY()
			&& z == ((TileEntity) tileEntity).getPos().getZ();
	}
	
	public void update(final IStarMapRegistryTileEntity tileEntity) {
		if (WarpDrive.isDev) {
			assert tileEntity instanceof TileEntity;
			assert type == tileEntity.getStarMapType();
			assert uuid.equals(tileEntity.getSignatureUUID());
		}
		final AxisAlignedBB aabbAreaUpdated = tileEntity.getStarMapArea();
		if (aabbAreaUpdated != null) {
			minX = (int) aabbAreaUpdated.minX;
			minY = (int) aabbAreaUpdated.minY;
			minZ = (int) aabbAreaUpdated.minZ;
			maxX = (int) aabbAreaUpdated.maxX - 1;
			maxY = (int) aabbAreaUpdated.maxY - 1;
			maxZ = (int) aabbAreaUpdated.maxZ - 1;
			cache_aabbArea = null;
		}
		mass = tileEntity.getMass();
		isolationRate = tileEntity.getIsolationRate();
		name = tileEntity.getSignatureName();
	}
	
	public boolean contains(@Nonnull final BlockPos blockPos) {
		return    minX <= blockPos.getX() && blockPos.getX() <= maxX
		       && minY <= blockPos.getY() && blockPos.getY() <= maxY
		       && minZ <= blockPos.getZ() && blockPos.getZ() <= maxZ;
	}
	
	public AxisAlignedBB getArea() {
		if (cache_aabbArea == null) {
			cache_aabbArea = new AxisAlignedBB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
		}
		return cache_aabbArea;
	}
	
	public StarMapRegistryItem(final NBTTagCompound tagCompound) {
		super(tagCompound);
		type = EnumStarMapEntryType.getByName(tagCompound.getString("type"));
		name = tagCompound.getString(ICoreSignature.NAME_TAG);
		UUID uuidLocal = new UUID(tagCompound.getLong(ICoreSignature.UUID_MOST_TAG), tagCompound.getLong(ICoreSignature.UUID_LEAST_TAG));
		if (uuidLocal.getMostSignificantBits() == 0L && uuidLocal.getLeastSignificantBits() == 0L) {
			uuidLocal = UUID.randomUUID();
		}
		uuid = uuidLocal;
		maxX = tagCompound.getInteger("maxX");
		maxY = tagCompound.getInteger("maxY");
		maxZ = tagCompound.getInteger("maxZ");
		minX = tagCompound.getInteger("minX");
		minY = tagCompound.getInteger("minY");
		minZ = tagCompound.getInteger("minZ");
		mass = tagCompound.getInteger("mass");
		isolationRate = tagCompound.getDouble("isolationRate");
		
		cache_aabbArea = null;
	}
	
	@Override
	public void writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setString("type", type.getName());
		if (name != null && !name.isEmpty()) {
			tagCompound.setString(ICoreSignature.NAME_TAG, name);
		}
		if (uuid != null) {
			tagCompound.setLong(ICoreSignature.UUID_MOST_TAG, uuid.getMostSignificantBits());
			tagCompound.setLong(ICoreSignature.UUID_LEAST_TAG, uuid.getLeastSignificantBits());
		}
		tagCompound.setInteger("maxX", maxX);
		tagCompound.setInteger("maxY", maxY);
		tagCompound.setInteger("maxZ", maxZ);
		tagCompound.setInteger("minX", minX);
		tagCompound.setInteger("minY", minY);
		tagCompound.setInteger("minZ", minZ);
		tagCompound.setInteger("mass", mass);
		tagCompound.setDouble("isolationRate", isolationRate);
	}
	
	public String getFormattedLocation() {
		final CelestialObject celestialObject = CelestialObjectManager.get(false, dimensionId, x, z);
		if (celestialObject == null) {
			return String.format("DIM%d @ (%d %d %d)",
			                     dimensionId,
			                     x, y, z);
		} else {
			return String.format("%s [DIM%d] @ (%d %d %d)",
			                     celestialObject.getDisplayName(),
			                     dimensionId,
			                     x, y, z);
		}
	}
	
	@Override
	public int hashCode() {
		return dimensionId << 24 + (x >> 10) << 12 + y << 10 + (z >> 10);
	}
	
	@Override
	public String toString() {
		return String.format("%s '%s' %s @ DIM%d (%d %d %d) (%d %d %d) -> (%d %d %d)",
		                     getClass().getSimpleName(),
			                 type, uuid,
			                 dimensionId,
			                 x, y, z,
			                 minX, minY, minZ,
			                 maxX, maxY, maxZ);
	}
}