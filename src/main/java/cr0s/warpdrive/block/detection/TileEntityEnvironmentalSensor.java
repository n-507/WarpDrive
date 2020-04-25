package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.block.TileEntityAbstractMachine;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;

import java.util.Collections;

import net.minecraft.block.state.IBlockState;

import net.minecraftforge.fml.common.Optional;

public class TileEntityEnvironmentalSensor extends TileEntityAbstractMachine {
	
	// persistent properties
	// (none)
	
	// computed properties
	private int tickUpdate;
	
	public TileEntityEnvironmentalSensor() {
		super();
		
		peripheralName = "warpdriveEnvironmentalSensor";
		addMethods(new String[] {
			"getWeather",
			"getWorldTime"
		});
		CC_scripts = Collections.singletonList("clock");
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		tickUpdate--;
		if (tickUpdate >= 0) {
			return;
		}
		tickUpdate = WarpDriveConfig.G_PARAMETERS_UPDATE_INTERVAL_TICKS;
		
		final IBlockState blockState = world.getBlockState(pos);
		updateBlockState(blockState, BlockProperties.ACTIVE, isEnabled);
	}
	
	// Common OC/CC methods
	public Object[] getWeather() {
		if (!isEnabled) {
			return new Object[] { false, "Sensor is disabled." };
		}
		
		// note: we return estimated time in seconds as it's more natural. Also the smooth transition adds a bit of delay anyway.
		if (world.isThundering()) {
			return new Object[] { true, "THUNDER", world.getWorldInfo().getThunderTime() / 20 };
		} else if (world.isRaining()) {
			return new Object[] { true, "RAIN", world.getWorldInfo().getRainTime() / 20 };
		}
		return new Object[] { true, "CLEAR", world.getWorldInfo().getCleanWeatherTime() / 20 };
	}
	
	public Object[] getWorldTime() {
		if (!isEnabled) {
			return new Object[] { false, "Sensor is disabled." };
		}
		
		// returns the current day, hour of the day, minutes of the day, and number of seconds simulated in the world (or play time for single player).
		// note: we return simulated seconds as it's more natural and discourages continuous pooling/abuse in LUA.
		final int day = (int) ((6000L + world.getWorldTime()) / 24000L);
		final int dayTime = 2400 * (int) ((6000L + world.getWorldTime()) % 24000L) / 24000;
		return new Object[] { true, day, dayTime / 100, (dayTime % 100) * 60 / 100, world.getTotalWorldTime() / 20 };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getWeather(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getWeather();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getWorldTime(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getWorldTime();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "getWeather":
			return getWeather();
		case "getWorldTime":
			return getWorldTime();
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
}