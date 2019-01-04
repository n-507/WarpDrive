package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.computer.IMachine;
import cr0s.warpdrive.config.WarpDriveConfig;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

import net.minecraftforge.fml.common.Optional;

public abstract class TileEntityAbstractMachine extends TileEntityAbstractInterfaced implements IMachine {
	
	// persistent properties
	public String name = "default";
	protected boolean isEnabled = true;
	
	public TileEntityAbstractMachine() {
		super();
		
		// (abstract) peripheralName = "xxx";
		addMethods(new String[] {
				"name",
				"enable",
				"isAssemblyValid"
				});
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		name = tagCompound.getString("name");
		isEnabled = tagCompound.hasKey("isEnabled") && tagCompound.getBoolean("isEnabled");
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setString("name", name);
		tagCompound.setBoolean("isEnabled", isEnabled);
		return tagCompound;
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		tagCompound.removeTag("isEnabled");
		return tagCompound;
	}
	
	public String getAllPlayersInArea() {
		final AxisAlignedBB axisalignedbb = new AxisAlignedBB(pos).grow(10.0D);
		final List list = world.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
		final StringBuilder stringBuilderResult = new StringBuilder();
		
		boolean isFirst = true;
		for (final Object object : list) {
			if (!(object instanceof EntityPlayer)) {
				continue;
			}
			if (isFirst) {
				isFirst = false;
			} else {
				stringBuilderResult.append(", ");
			}
			stringBuilderResult.append(((EntityPlayer) object).getName());
		}
		return stringBuilderResult.toString();
	}
	
	public boolean getIsEnabled() {
		return isEnabled;
	}
	
	public void setIsEnabled(final boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	
	// Common OC/CC methods
	@Override
	public String[] name(final Object[] arguments) {
		if ( arguments != null
		  && arguments.length == 1
		  && arguments[0] != null ) {
			final String namePrevious = name;
			name = Commons.sanitizeFileName((String) arguments[0]);
			if (!name.equals(namePrevious)) {
				WarpDrive.logger.info(String.format("Machine renamed from '%s' to '%s' with player(s) %s",
				                                    namePrevious == null ? "-null-" : namePrevious,
				                                    name,
				                                    getAllPlayersInArea()));
			}
		}
		return new String[] { name };
	}
	
	@Override
	public Object[] enable(final Object[] arguments) {
		if ( arguments != null
		  && arguments.length == 1
		  && arguments[0] != null ) {
			final boolean enableRequest;
			try {
				enableRequest = Commons.toBool(arguments[0]);
			} catch (final Exception exception) {
				final String message = String.format("%s LUA error on enable(): Boolean expected for 1st argument %s",
				                                     this, arguments[0]);
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(message);
				}
				return new Object[] { isEnabled, message };
			}
			if (isEnabled && !enableRequest) {
				setIsEnabled(false);
				sendEvent("disabled");
			} else if (!isEnabled && enableRequest) {
				setIsEnabled(true);
				sendEvent("enabled");
			}
		}
		return new Object[] { isEnabled };
	}
	
	@Override
	public Object[] isAssemblyValid() {
		return new Object[] { true, "ok" };
	}
	
	// OpenComputers callback methods
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] name(final Context context, final Arguments arguments) {
		return name(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] enable(final Context context, final Arguments arguments) {
		return enable(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] isAssemblyValid(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return isAssemblyValid();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	public Object[] callMethod(@Nonnull final IComputerAccess computer, @Nonnull final ILuaContext context, final int method, @Nonnull final Object[] arguments) {
		final String methodName = CC_getMethodNameAndLogCall(method, arguments);
		
		switch (methodName) {
		case "name":
			return name(arguments);
			
		case "enable":
			return enable(arguments);
			
		case "isAssemblyValid":
			return isAssemblyValid();
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
