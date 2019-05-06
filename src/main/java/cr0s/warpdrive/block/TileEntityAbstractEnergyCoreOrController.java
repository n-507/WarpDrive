package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.computer.ICoreSignature;
import cr0s.warpdrive.api.computer.IEnergyConsumer;
import cr0s.warpdrive.api.computer.IMultiBlockCoreOrController;
import cr0s.warpdrive.api.computer.IMultiBlockCore;
import javax.annotation.Nonnull;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

public abstract class TileEntityAbstractEnergyCoreOrController extends TileEntityAbstractEnergyConsumer implements IMultiBlockCoreOrController, IEnergyConsumer {
	
	// persistent properties
	public UUID uuid = null;
	
	// computed properties
	// (none)
	
	public TileEntityAbstractEnergyCoreOrController() {
		super();
		
		// (abstract) peripheralName = "xxx";
		// addMethods(new String[] {
		// 		});
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
		if (uuid.getMostSignificantBits() == 0 && uuid.getLeastSignificantBits() == 0) {
			uuid = UUID.randomUUID();
		}
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		if (uuid != null) {
			tagCompound.setLong(ICoreSignature.UUID_MOST_TAG, uuid.getMostSignificantBits());
			tagCompound.setLong(ICoreSignature.UUID_LEAST_TAG, uuid.getLeastSignificantBits());
		}
		
		return tagCompound;
	}
	
	// writeItemDropNBT
	
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
		return String.format("%s \'%s\' %s %s",
		                     getClass().getSimpleName(),
		                     name,
		                     Commons.format(world, pos),
		                     computer_isConnected() ? "Connected" : "Disconnected" );
	}
}
