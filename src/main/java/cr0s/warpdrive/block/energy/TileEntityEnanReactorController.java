package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.api.computer.IEnanReactorController;
import cr0s.warpdrive.block.TileEntityAbstractEnergyCoreOrController;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.Collections;

import net.minecraft.tileentity.TileEntity;

import net.minecraftforge.fml.common.Optional;

public class TileEntityEnanReactorController extends TileEntityAbstractEnergyCoreOrController implements IEnanReactorController {
	
	// persistent properties
	// (none)
	
	// computed properties
	// (none)
	
	private WeakReference<TileEntityEnanReactorCore> tileEntityEnanReactorCoreWeakReference = null;
	
	public TileEntityEnanReactorController() {
		super();
		
		peripheralName = "warpdriveEnanReactorController";
		addMethods(new String[] {
				"instability",
				"instabilityTarget",
				"outputMode",
				"stabilizerEnergy",
				"state"
		});
		CC_scripts = Collections.singletonList("startup");
	}
 
	private TileEntityEnanReactorCore findCoreBlock() {
		TileEntity tileEntity;
		
		tileEntity = world.getTileEntity(pos.add(1, 0, 0));
		if (tileEntity instanceof TileEntityEnanReactorCore) {
			return (TileEntityEnanReactorCore) tileEntity;
		}
		
		tileEntity = world.getTileEntity(pos.add(-1, 0, 0));
		if (tileEntity instanceof TileEntityEnanReactorCore) {
			return (TileEntityEnanReactorCore) tileEntity;
		}
		
		tileEntity = world.getTileEntity(pos.add(0, 0, 1));
		if (tileEntity instanceof TileEntityEnanReactorCore) {
			return (TileEntityEnanReactorCore) tileEntity;
		}
		
		tileEntity = world.getTileEntity(pos.add(0, 0, -1));
		if (tileEntity instanceof TileEntityEnanReactorCore) {
			return (TileEntityEnanReactorCore) tileEntity;
		}
		
		return null;
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getEnergyRequired() {
		return new Object[0];
	}
	
	@Override
	public Object[] getLocalPosition() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return null;
		}
		return tileEntityEnanReactorCore.getLocalPosition();
	}
	
	@Override
	public Object[] isAssemblyValid() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Object[] { false, "No core detected" };
		}
		return tileEntityEnanReactorCore.isAssemblyValid();
	}
	
	@Override
	public String[] name(final Object[] arguments) {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return super.name(null); // return current local values
		}
		return tileEntityEnanReactorCore.name(arguments);
	}
	
	@Override
	public Double[] getInstabilities() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Double[] { -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D,
			                      -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D };
		}
		return tileEntityEnanReactorCore.getInstabilities();
	}
	
	@Override
	public Double[] instabilityTarget(final Object[] arguments) {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Double[] { -1.0D };
		}
		return tileEntityEnanReactorCore.instabilityTarget(arguments);
	}
	
	@Override
	public Object[] outputMode(final Object[] arguments) {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Object[] { "???", -1, "Core not found" };
		}
		return tileEntityEnanReactorCore.outputMode(arguments);
	}
	
	@Override
	public Object[] stabilizerEnergy(final Object[] arguments) {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Object[] { -1, "Core not found" };
		}
		return tileEntityEnanReactorCore.stabilizerEnergy(arguments);
	}
	
	@Override
	public Object[] state() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Object[] { -1, "Core not found" };
		}
		return tileEntityEnanReactorCore.state();
	}
	
	@Override
	public Object[] energy() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return null;
		}
		return tileEntityEnanReactorCore.energy();
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] getInstabilities(final Context context, final Arguments arguments) {
		return getInstabilities();
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] instabilityTarget(final Context context, final Arguments arguments) {
		return instabilityTarget(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] outputMode(final Context context, final Arguments arguments) {
		return outputMode(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] stabilizerEnergy(final Context context, final Arguments arguments) {
		return stabilizerEnergy(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] state(final Context context, final Arguments arguments) {
		return state();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	public Object[] callMethod(@Nonnull final IComputerAccess computer, @Nonnull final ILuaContext context, final int method, @Nonnull final Object[] arguments) {
		final String methodName = CC_getMethodNameAndLogCall(method, arguments);
		
		switch (methodName) {
		case "getInstabilities":
			return getInstabilities();
			
		case "instabilityTarget":
			return instabilityTarget(arguments);
			
		case "outputMode":
			return outputMode(arguments);
			
		case "stabilizerEnergy":
			return stabilizerEnergy(arguments);
			
		case "state":
			return state();
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
}
