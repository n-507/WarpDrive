package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObjectManager;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.Collections;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileEntityShipController extends TileEntityAbstractShipController {
	
	// persistent properties
	// (none)
	
	// computed properties
	private final int updateInterval_ticks = 20 * WarpDriveConfig.SHIP_CONTROLLER_UPDATE_INTERVAL_SECONDS;
	private int updateTicks = updateInterval_ticks;
	private int bootTicks = 20;
	
	private WeakReference<TileEntityShipCore> tileEntityShipCoreWeakReference = null;
	
	public TileEntityShipController() {
		super();
		
		peripheralName = "warpdriveShipController";
		// addMethods(new String[] {});
		CC_scripts = Collections.singletonList("startup");
	}
    
    @Override
    public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		// accelerate update ticks during boot
		if (bootTicks > 0) {
			bootTicks--;
			if (tileEntityShipCoreWeakReference == null) {
				updateTicks = 1;
			}
		}
		updateTicks--;
		if (updateTicks <= 0) {
			updateTicks = updateInterval_ticks;
			
			final TileEntityShipCore tileEntityShipCore = findCoreBlock();
			if (tileEntityShipCore != null) {
				if ( tileEntityShipCoreWeakReference == null
				  || tileEntityShipCore != tileEntityShipCoreWeakReference.get() ) {
					tileEntityShipCoreWeakReference = new WeakReference<>(tileEntityShipCore);
				}
				
				final boolean isSynchronized = tileEntityShipCore.refreshLink(this);
				if (isSynchronized) {
					onCoreUpdated(tileEntityShipCore);
					if ( !tileEntityShipCore.isCommandConfirmed
					  && isCommandConfirmed ) {
						tileEntityShipCore.command(new Object[] { enumShipCommand.getName(), true });
					}
				}
			}
			
			updateBlockState(null, BlockShipController.COMMAND, enumShipCommand);
		}
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		return tagCompound;
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		return tagCompound;
	}
	
	private TileEntityShipCore findCoreBlock() {
		TileEntity tileEntity;
		
		tileEntity = world.getTileEntity(pos.add(1, 0, 0));
		if (tileEntity instanceof TileEntityShipCore) {
			return (TileEntityShipCore) tileEntity;
		}
		
		tileEntity = world.getTileEntity(pos.add(-1, 0, 0));
		if (tileEntity instanceof TileEntityShipCore) {
			return (TileEntityShipCore) tileEntity;
		}
		
		tileEntity = world.getTileEntity(pos.add(0, 0, 1));
		if (tileEntity instanceof TileEntityShipCore) {
			return (TileEntityShipCore) tileEntity;
		}
		
		tileEntity = world.getTileEntity(pos.add(0, 0, -1));
		if (tileEntity instanceof TileEntityShipCore) {
			return (TileEntityShipCore) tileEntity;
		}
		
		return null;
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getLocalPosition() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return tileEntityShipCore.getLocalPosition();
	}
	
	@Override
	public Object[] isAssemblyValid() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return new Object[] { false, "No core detected" };
		}
		return tileEntityShipCore.isAssemblyValid();
	}
	
	@Override
	public Object[] getOrientation() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return tileEntityShipCore.getOrientation();
	}
	
	@Override
	public Object[] isInSpace() {
		return new Boolean[] { CelestialObjectManager.isInSpace(world, pos.getX(), pos.getZ()) };
	}
	
	@Override
	public Object[] isInHyperspace() {
		return new Boolean[] { CelestialObjectManager.isInHyperspace(world, pos.getX(), pos.getZ()) };
	}
	
	@Override
	public String[] name(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return super.name(null); // return current local values
		}
		return tileEntityShipCore.name(arguments);
	}
	
	@Override
	public Object[] dim_positive(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return super.dim_positive(null); // return current local values
		}
		return new Object[] { tileEntityShipCore.dim_positive(arguments) };
	}
	
	@Override
	public Object[] dim_negative(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return super.dim_negative(null); // return current local values
		}
		return new Object[] { tileEntityShipCore.dim_negative(arguments) };
	}
	
	@Override
	public Object[] energyDisplayUnits(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return tileEntityShipCore.energyDisplayUnits(arguments);
	}
	
	@Override
	public Object[] getEnergyStatus() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return tileEntityShipCore.getEnergyStatus();
	}
	
	@Override
	public Object[] getShipSize() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return tileEntityShipCore.getShipSize();
	}
	
	@Override
	public Object[] movement(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return super.movement(arguments); // return current local values
		}
		return tileEntityShipCore.movement(arguments);
	}
	
	@Override
	public Object[] getMaxJumpDistance() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return new Object[] { false, "No ship core detected" };
		}
		return tileEntityShipCore.getMaxJumpDistance();
	}
	
	@Override
	public Object[] rotationSteps(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return super.rotationSteps(arguments); // return current local values
		}
		return tileEntityShipCore.rotationSteps(arguments);
	}
	
	@Override
	public Object[] targetName(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return super.targetName(arguments); // return current local values
		}
		return tileEntityShipCore.targetName(arguments);
	}
	
	@Override
	public Object[] getEnergyRequired() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return new Object[] { false, "No ship core detected" };
		}
		return tileEntityShipCore.getEnergyRequired();
	}
}
