package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.computer.IMultiBlockCoreOrController;
import cr0s.warpdrive.api.computer.IMultiBlockCore;
import javax.annotation.Nonnull;

public abstract class TileEntityAbstractEnergyCoreOrController extends TileEntityAbstractEnergyConsumer implements IMultiBlockCoreOrController {
	
	// persistent properties
	// (none)
	
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
