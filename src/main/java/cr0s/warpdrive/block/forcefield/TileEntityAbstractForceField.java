package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.block.TileEntityAbstractEnergyConsumer;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.ForceFieldRegistry;
import cr0s.warpdrive.data.Vector3;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;

public abstract class TileEntityAbstractForceField extends TileEntityAbstractEnergyConsumer implements IBeamFrequency {
	
	// persistent properties
	protected int beamFrequency = -1;
	protected boolean isConnected = false;
	
	// computed properties
	protected Vector3 vRGB;
	
	public TileEntityAbstractForceField() {
		super();
		
		addMethods(new String[] {
			"beamFrequency"
		});
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		if (beamFrequency >= 0 && beamFrequency <= IBeamFrequency.BEAM_FREQUENCY_MAX) {
			ForceFieldRegistry.updateInRegistry(this);
		}
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		// Frequency is not set
		final boolean new_isConnected = beamFrequency > 0 && beamFrequency <= IBeamFrequency.BEAM_FREQUENCY_MAX;
		if (isConnected != new_isConnected) {
			isConnected = new_isConnected;
			markDirty();
		}
	}
	
	@Override
	public void onBlockBroken(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		ForceFieldRegistry.removeFromRegistry(this);
		super.onBlockBroken(world, blockPos, blockState);
	}
	
	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		// reload chunks as needed
		// ForceFieldRegistry.removeFromRegistry(this);
	}
	
	@Override
	public int getBeamFrequency() {
		return beamFrequency;
	}
	
	@Override
	public void setBeamFrequency(final int parBeamFrequency) {
		if (beamFrequency != parBeamFrequency && (parBeamFrequency <= BEAM_FREQUENCY_MAX) && (parBeamFrequency > BEAM_FREQUENCY_MIN)) {
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(String.format("%s Beam frequency set from %d to %d",
				                                    this, beamFrequency, parBeamFrequency));
			}
			if (hasWorld()) {
				ForceFieldRegistry.removeFromRegistry(this);
			}
			beamFrequency = parBeamFrequency;
			vRGB = IBeamFrequency.getBeamColor(beamFrequency);
		}
		markDirty();
		if (hasWorld()) {
			ForceFieldRegistry.updateInRegistry(this);
		}
	}
	
	@Override
		public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		setBeamFrequency(tagCompound.getInteger(BEAM_FREQUENCY_TAG));
		isConnected = tagCompound.getBoolean("isConnected");
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setInteger(BEAM_FREQUENCY_TAG, beamFrequency);
		tagCompound.setBoolean("isConnected", isConnected);
		return tagCompound;
	}
	
	@Nonnull
	@Override
	public NBTTagCompound getUpdateTag() {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		writeToNBT(tagCompound);
		tagCompound.setBoolean("isConnected", isConnected);
		return tagCompound;
	}
	
	@Override
	public void onDataPacket(final NetworkManager networkManager, final SPacketUpdateTileEntity packet) {
		final NBTTagCompound tagCompound = packet.getNbtCompound();
		readFromNBT(tagCompound);
	}
	
	// Common OC/CC methods
	public Object[] beamFrequency(final Object[] arguments) {
		if ( arguments != null
		  && arguments.length == 1
		  && arguments[0] != null ) {
			final int beamFrequencyRequested;
			try {
				beamFrequencyRequested = Commons.toInt(arguments[0]);
			} catch (final Exception exception) {
				final String message = String.format("%s LUA error on beamFrequency(): Boolean expected for 1st argument %s",
				                                     this, arguments[0]);
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(message);
				}
				return new Object[] { beamFrequency, message };
			}
			setBeamFrequency(beamFrequencyRequested);
		}
		return new Object[] { beamFrequency };
	}
	
	// OpenComputers callback methods
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] beamFrequency(final Context context, final Arguments arguments) {
		return beamFrequency(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "beamFrequency":
			return beamFrequency(arguments);
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
	
	@Override
	public String toString() {
		return String.format("%s Beam \'%d\' %s",
		                     getClass().getSimpleName(),
		                     beamFrequency,
		                     Commons.format(world, pos));
	}
}
