package cr0s.warpdrive.block;

import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

public class TileEntityLaserMedium extends TileEntityAbstractEnergy {
	
	private static final int BLOCKSTATE_REFRESH_PERIOD_TICKS = 20;
	
	// persistent properties
	// (none)
	
	// computed properties
	private int ticks = BLOCKSTATE_REFRESH_PERIOD_TICKS;
	
	public TileEntityLaserMedium() {
		super();
		
		peripheralName = "warpdriveLaserMedium";
		doRequireUpgradeToInterface();
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		ticks--;
		if (ticks < 0) {
			ticks = BLOCKSTATE_REFRESH_PERIOD_TICKS;
			
			final int level = Math.max(0, Math.min(7, Math.round((energy_getEnergyStored() * 8) / energy_getMaxStorage())));
			updateBlockState(null, BlockLaserMedium.LEVEL, level);
		}
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(final NBTTagCompound tagCompound) {
		return super.writeToNBT(tagCompound);
	}
	
	// IEnergySink methods
	@Override
	public int energy_getMaxStorage() {
		return WarpDriveConfig.LASER_MEDIUM_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()];
	}
	
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		return true;
	}
}
