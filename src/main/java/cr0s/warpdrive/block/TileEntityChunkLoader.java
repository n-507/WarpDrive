package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumComponentType;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.ChunkPos;

import net.minecraftforge.fml.common.Optional;

public class TileEntityChunkLoader extends TileEntityAbstractChunkLoading {
	
	// persistent properties
	private int radiusXneg = 0;
	private int radiusXpos = 0;
	private int radiusZneg = 0;
	private int radiusZpos = 0;
	
	// fuel status is needed before first tick
	private boolean isPowered = false;
	
	// computed properties
	// (none)
	
	public TileEntityChunkLoader() {
		super();
		
		peripheralName = "warpdriveChunkLoader";
		addMethods(new String[] {
				"bounds",
				"radius",
		});
		doRequireUpgradeToInterface();
		
		setUpgradeMaxCount(EnumComponentType.SUPERCONDUCTOR, 5);
		setUpgradeMaxCount(EnumComponentType.EMERALD_CRYSTAL, WarpDriveConfig.CHUNK_LOADER_MAX_RADIUS);
	}
	
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		return true;
	}
	
	@Override
	public boolean dismountUpgrade(final Object upgrade) {
		final boolean isSuccess = super.dismountUpgrade(upgrade);
		if (isSuccess) {
			final int maxRange = getMaxRange();
			setBounds(maxRange, maxRange, maxRange, maxRange);
		}
		return isSuccess;
	}
	
	@Override
	public boolean mountUpgrade(final Object upgrade) {
		final boolean isSuccess = super.mountUpgrade(upgrade);
		if (isSuccess) {
			final int maxRange = getMaxRange();
			setBounds(maxRange, maxRange, maxRange, maxRange);
		}
		return isSuccess;
	}
	
	private int getMaxRange() {
		return getValidUpgradeCount(EnumComponentType.EMERALD_CRYSTAL);
	}
	
	private double getEnergyFactor() {
		final int upgradeCount = getValidUpgradeCount(EnumComponentType.SUPERCONDUCTOR);
		return 1.0D - 0.1D * upgradeCount;
	}
	
	public int calculateEnergyRequired() {
		return (int) Math.ceil(getEnergyFactor() * chunkloading_getArea() * WarpDriveConfig.CHUNK_LOADER_ENERGY_PER_CHUNK);
	}
	
	@Override
	public boolean shouldChunkLoad() {
		return isEnabled && isPowered;
	}
	
	@Override
	protected void onConstructed() {
		super.onConstructed();
		
		energy_setParameters(WarpDriveConfig.CHUNK_LOADER_MAX_ENERGY_STORED,
		                     1024, 0,
		                     "MV", 2, "MV", 0);
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		
		if (world.isRemote) {
			return;
		}
		
		refreshChunkRange();
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		isPowered = energy_consume(calculateEnergyRequired(), !isEnabled);
		
		updateBlockState(null, BlockProperties.ACTIVE, isEnabled && isPowered);
	}
	
	private void setBounds(final int negX, final int posX, final int negZ, final int posZ) {
		// compute new values
		final int maxRange = getMaxRange();
		final int radiusXneg_new = - Commons.clamp(0, 1000, Math.abs(negX));
		final int radiusXpos_new =   Commons.clamp(0, 1000, Math.abs(posX));
		final int radiusZneg_new = - Commons.clamp(0, 1000, Math.abs(negZ));
		final int radiusZpos_new =   Commons.clamp(0, 1000, Math.abs(posZ));
		
		// validate size constrains
		final int maxArea = (1 + 2 * maxRange) * (1 + 2 * maxRange);
		final int newArea = (-radiusXneg_new + 1 + radiusXpos_new)
		                  * (-radiusZneg_new + 1 + radiusZpos_new);
		if (newArea <= maxArea) {
			radiusXneg = radiusXneg_new;
			radiusXpos = radiusXpos_new;
			radiusZneg = radiusZneg_new;
			radiusZpos = radiusZpos_new;
			refreshChunkRange();
		}
	}
	
	private void refreshChunkRange() {
		if (world == null) {
			return;
		}
		final ChunkPos chunkSelf = world.getChunk(pos).getPos();
		
		chunkMin = new ChunkPos(chunkSelf.x + radiusXneg, chunkSelf.z + radiusZneg);
		chunkMax = new ChunkPos(chunkSelf.x + radiusXpos, chunkSelf.z + radiusZpos);
		refreshChunkLoading();
	}
	
	// Forge overrides
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		setBounds(tagCompound.getInteger("radiusXneg"), tagCompound.getInteger("radiusXpos"),
		          tagCompound.getInteger("radiusZneg"), tagCompound.getInteger("radiusZpos"));
		isPowered = tagCompound.getBoolean("isPowered");
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setInteger("radiusXneg", radiusXneg);
		tagCompound.setInteger("radiusZneg", radiusZneg);
		tagCompound.setInteger("radiusXpos", radiusXpos);
		tagCompound.setInteger("radiusZpos", radiusZpos);
		tagCompound.setBoolean("isPowered", isPowered);
		return tagCompound;
	}
	
	// Common OC/CC methods
	public Object[] bounds(final Object[] arguments) {
		if (arguments.length == 4) {
			setBounds(Commons.toInt(arguments[0]), Commons.toInt(arguments[1]), Commons.toInt(arguments[2]), Commons.toInt(arguments[3]));
		}
		return new Object[] { radiusXneg, radiusXpos, radiusZneg, radiusZpos };
	}
	
	public Object[] radius(final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			final int radius = Commons.toInt(arguments[0]);
			setBounds(radius, radius, radius, radius);
		}
		return new Object[] { radiusXneg, radiusXpos, radiusZneg, radiusZpos };
	}
	
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		return new Object[] {
				true,
				EnergyWrapper.convert(calculateEnergyRequired(), units) };
	}
	
	// OpenComputers callback methods
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] bounds(final Context context, final Arguments arguments) {
		return bounds(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] radius(final Context context, final Arguments arguments) {
		return radius(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "bounds":
			return bounds(arguments);
			
		case "radius":
			return radius(arguments);
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
}
