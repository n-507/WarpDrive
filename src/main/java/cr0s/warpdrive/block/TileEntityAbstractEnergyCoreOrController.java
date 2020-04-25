package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IStarMapRegistryTileEntity;
import cr0s.warpdrive.api.computer.ICoreSignature;
import cr0s.warpdrive.api.computer.IEnergyConsumer;
import cr0s.warpdrive.api.computer.IMultiBlockCoreOrController;
import cr0s.warpdrive.api.computer.IMultiBlockCore;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class TileEntityAbstractEnergyCoreOrController extends TileEntityAbstractEnergyConsumer implements IMultiBlockCoreOrController, IEnergyConsumer {
	
	// persistent properties
	public UUID uuid = null;
	
	// computed properties
	private boolean isDirtyParameters = true;
	private int tickUpdateParameters = 0;
	private boolean isDirtyStarMapEntry = true;
	private int tickUpdateStarMapEntry = 0;
	
	public TileEntityAbstractEnergyCoreOrController() {
		super();
		
		// (abstract) peripheralName = "xxx";
		// addMethods(new String[] {
		// 		});
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		// update operational parameters when dirty or periodically to recover whatever may have desynchronized them
		if (isDirtyParameters) {
			tickUpdateParameters = 0;
		}
		tickUpdateParameters--;
		if (tickUpdateParameters <= 0) {
			tickUpdateParameters = WarpDriveConfig.G_PARAMETERS_UPDATE_INTERVAL_TICKS;
			final boolean isDirty = isDirtyParameters;
			isDirtyParameters = false;
			
			doUpdateParameters(isDirty);
		}
		
		// update starmap registry upon request or periodically to recover whatever may have desynchronized it
		if (this instanceof IStarMapRegistryTileEntity) {
			if (isDirtyStarMapEntry) {
				tickUpdateStarMapEntry = 0;
			}
			tickUpdateStarMapEntry--;
			if (tickUpdateStarMapEntry <= 0) {
				tickUpdateStarMapEntry = WarpDriveConfig.STARMAP_REGISTRY_UPDATE_INTERVAL_TICKS;
				final boolean isDirty = isDirtyStarMapEntry;
				isDirtyStarMapEntry = false;
				
				doRegisterStarMapEntry(isDirty);
			}
		}
	}
	
	public boolean isDirtyParameters() {
		return isDirtyParameters;
	}
	
	protected void markDirtyParameters() {
		isDirtyParameters = true;
	}
	
	protected abstract void doUpdateParameters(final boolean isDirty);
	
	public boolean isDirtyStarMapEntry() {
		return isDirtyStarMapEntry;
	}
	
	protected void markDirtyStarMapEntry() {
		assert this instanceof IStarMapRegistryTileEntity;
		isDirtyStarMapEntry = true;
	}
	
	protected void doRegisterStarMapEntry(final boolean isDirty) {
		if (uuid == null || (uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() == 0L)) {
			uuid = UUID.randomUUID();
		}
		
		WarpDrive.starMap.updateInRegistry((IStarMapRegistryTileEntity) this);
	}
	
	@Override
	public void onBlockBroken(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		if ( !world.isRemote
		  && this instanceof IStarMapRegistryTileEntity ) {
			WarpDrive.starMap.removeFromRegistry((IStarMapRegistryTileEntity) this);
		}
		
		super.onBlockBroken(world, blockPos, blockState);
	}
	
	@Override
	public void onCoreUpdated(@Nonnull final IMultiBlockCore multiblockCore) {
		assert multiblockCore instanceof TileEntityAbstractEnergyCoreOrController;
		name = ((TileEntityAbstractEnergyCoreOrController) multiblockCore).name;
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		uuid = new UUID(tagCompound.getLong(ICoreSignature.UUID_MOST_TAG), tagCompound.getLong(ICoreSignature.UUID_LEAST_TAG));
		if (uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() == 0L) {
			uuid = UUID.randomUUID();
		}
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		if ( uuid != null
		  && uuid.getMostSignificantBits() != 0L
		  && uuid.getLeastSignificantBits() != 0L ) {
			tagCompound.setLong(ICoreSignature.UUID_MOST_TAG, uuid.getMostSignificantBits());
			tagCompound.setLong(ICoreSignature.UUID_LEAST_TAG, uuid.getLeastSignificantBits());
		}
		
		return tagCompound;
	}
	
	// writeItemDropNBT
	
	@Nullable
	@Override
	public UUID getSignatureUUID() {
		return uuid;
	}
	
	@Override
	public String getSignatureName() {
		return name;
	}
	
	@Override
	public boolean setSignature(final UUID uuidSignature, final String nameSignature) {
		if (this instanceof IMultiBlockCore) {
			return false;
		}
		
		uuid = uuidSignature;
		name = nameSignature;
		return true;
	}
	
	// Common OC/CC methods
	// (none)
	
	// OpenComputers callback methods
	// (none)
	
	// ComputerCraft IPeripheral methods
	// (none)
	
	@Override
	public String toString() {
		return String.format("%s '%s' %s %s",
		                     getClass().getSimpleName(),
		                     name,
		                     Commons.format(world, pos),
		                     computer_isConnected() ? "Connected" : "Disconnected" );
	}
}
