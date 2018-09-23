package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.computer.IEnergyConsumer;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;

import net.minecraftforge.fml.common.Optional;

public abstract class TileEntityAbstractEnergyConsumer extends TileEntityAbstractEnergy implements IEnergyConsumer {
	
	// persistent properties
	// (none)
	
	// computed properties
	// (none)
	
	public TileEntityAbstractEnergyConsumer() {
		super();
		
		// (abstract) peripheralName = "xxx";
		addMethods(new String[] {
				"getEnergyRequired",
				});
	}
	
	// Common OC/CC methods
	@Override
	public abstract Object[] getEnergyRequired();
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] getEnergyRequired(final Context context, final Arguments arguments) {
		return getEnergyRequired();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	public Object[] callMethod(@Nonnull final IComputerAccess computer, @Nonnull final ILuaContext context, final int method, @Nonnull final Object[] arguments) {
		final String methodName = CC_getMethodNameAndLogCall(method, arguments);
		
		switch (methodName) {
		case "getEnergyRequired":
			return getEnergyRequired();
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	@Override
	public String toString() {
		return String.format("%s \'%s\' %s",
		                     getClass().getSimpleName(),
		                     name,
		                     Commons.format(world, pos));
	}
}
