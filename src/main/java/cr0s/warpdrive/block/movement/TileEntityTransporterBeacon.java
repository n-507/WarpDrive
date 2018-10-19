package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.api.computer.ITransporterBeacon;
import cr0s.warpdrive.block.TileEntityAbstractEnergyConsumer;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.StarMapRegistryItem;
import cr0s.warpdrive.data.EnumStarMapEntryType;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.util.UUID;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import net.minecraftforge.fml.common.Optional;

public class TileEntityTransporterBeacon extends TileEntityAbstractEnergyConsumer implements ITransporterBeacon {
	
	// persistent properties
	private String nameTransporterCore;
	private UUID uuidTransporterCore;
	private int tickDeploying = 0;
	
	// computer properties
	private boolean isActive = false;
	
	public TileEntityTransporterBeacon() {
		super();
		
		IC2_sinkTier = 2;
		isEnergyLostWhenBroken = false;
		
		peripheralName = "warpdriveTransporterBeacon";
		addMethods(new String[] {
				"isActive"
		});
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		// deploy
		final boolean isDeployed = tickDeploying > WarpDriveConfig.TRANSPORTER_BEACON_DEPLOYING_DELAY_TICKS;
		if (!isDeployed) {
			tickDeploying++;
		}
		
		if (!isEnabled) {
			isActive = false;
		} else {
			// get current status
			final boolean isConnected = uuidTransporterCore != null
			                         && ( uuidTransporterCore.getLeastSignificantBits() != 0
			                           || uuidTransporterCore.getMostSignificantBits() != 0 );
			final boolean isPowered = energy_consume(WarpDriveConfig.TRANSPORTER_BEACON_ENERGY_PER_TICK, true);
			// final boolean isLowPower = energy_getEnergyStored() < WarpDriveConfig.TRANSPORTER_BEACON_ENERGY_PER_TICK * TICK_LOW_POWER;
			
			// reach transporter
			boolean isActiveNew = false;
			if (isPowered) {
				if (isConnected) {// only consume is transporter is reachable
					isActiveNew = pingTransporter();
					if (isActiveNew) {
						energy_consume(WarpDriveConfig.TRANSPORTER_BEACON_ENERGY_PER_TICK, false);
					}
					
				} else {// always consume
					energy_consume(WarpDriveConfig.TRANSPORTER_BEACON_ENERGY_PER_TICK, false);
				}
			}
			isActive = isActiveNew;
		}
		
		// report updated status
		final IBlockState blockState_actual = world.getBlockState(pos);
		updateBlockState(blockState_actual,
		                 blockState_actual.withProperty(BlockProperties.ACTIVE, isActive)
		                                  .withProperty(BlockTransporterBeacon.DEPLOYED, isDeployed));
	}
	
	private boolean pingTransporter() {
		final StarMapRegistryItem starMapRegistryItem = WarpDrive.starMap.getByUUID(EnumStarMapEntryType.TRANSPORTER, uuidTransporterCore);
		if (starMapRegistryItem == null) {
			return false;
		}
		
		final WorldServer worldTransporter = Commons.getOrCreateWorldServer(starMapRegistryItem.dimensionId);
		if (worldTransporter == null) {
			WarpDrive.logger.error(String.format("%s Unable to load dimension %d for transporter with UUID %s",
			                                     this, starMapRegistryItem.dimensionId, uuidTransporterCore));
			return false;
		}
		
		final TileEntity tileEntity = worldTransporter.getTileEntity(new BlockPos(starMapRegistryItem.x, starMapRegistryItem.y, starMapRegistryItem.z));
		if (!(tileEntity instanceof TileEntityTransporterCore)) {
			WarpDrive.logger.warn(String.format("%s Transporter has gone missing for %s, found %s",
			                                    this, starMapRegistryItem, tileEntity));
			return false;
		}
		
		final TileEntityTransporterCore tileEntityTransporterCore = (TileEntityTransporterCore) tileEntity;
		return tileEntityTransporterCore.updateBeacon(this, uuidTransporterCore);
	}
	
	@Override
	public void energizeDone() {
		isEnabled = false;
	}
	
	// Common OC/CC methods
	@Override
	public void setIsEnabled(final boolean isEnabled) {
		super.setIsEnabled(isEnabled);
		// enabling up => redeploy
		if (isEnabled) {
			tickDeploying = 0;
		}
	}
	
	@Override
	public Boolean[] isActive(final Object[] arguments) {
		return new Boolean[] { isActive };
	}
	
	@Override
	public boolean isActive() {
		return isActive;
	}
	
	@Override
	public Object[] getEnergyRequired() {
		return new Object[] { WarpDriveConfig.TRANSPORTER_BEACON_ENERGY_PER_TICK };
	}
	
	// OpenComputers callback methods
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] isActive(final Context context, final Arguments arguments) {
		return isActive(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	public Object[] callMethod(@Nonnull final IComputerAccess computer, @Nonnull final ILuaContext context, final int method, @Nonnull final Object[] arguments) {
		final String methodName = CC_getMethodNameAndLogCall(method, arguments);
		
		switch (methodName) {
		case "isActive":
			return isActive(arguments);
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	// TileEntityAbstractBase overrides
	private WarpDriveText getSignatureStatus() {
		if (uuidTransporterCore == null) {
			return new WarpDriveText(Commons.styleWarning, "warpdrive.transporter_signature.status_line.invalid");
		}
		return new WarpDriveText(Commons.styleCorrect, "warpdrive.transporter_signature.status_line.valid",
		                         nameTransporterCore, uuidTransporterCore);
	}
	
	@Override
	public WarpDriveText getStatus() {
		final WarpDriveText textSignatureStatus = getSignatureStatus();
		if (textSignatureStatus.getUnformattedText().isEmpty()) {
			return super.getStatus();
		} else {
			return super.getStatus()
			            .append(textSignatureStatus);
		}
	}
	
	// TileEntityAbstractEnergy overrides
	@Override
	public int energy_getMaxStorage() {
		return WarpDriveConfig.TRANSPORTER_BEACON_MAX_ENERGY_STORED;
	}
	
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		// only from bottom
		return (from == EnumFacing.DOWN);
	}
	
	@Override
	public boolean energy_canOutput(final EnumFacing to) {
		return false;
	}
	
	// Forge overrides
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		if (uuidTransporterCore != null) {
			tagCompound.setString("name", nameTransporterCore);
			tagCompound.setLong("uuidMost", uuidTransporterCore.getMostSignificantBits());
			tagCompound.setLong("uuidLeast", uuidTransporterCore.getLeastSignificantBits());
		}
		
		tagCompound.setInteger("tickDeploying", tickDeploying);
		return tagCompound;
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		nameTransporterCore = tagCompound.getString("name");
		uuidTransporterCore = new UUID(tagCompound.getLong("uuidMost"), tagCompound.getLong("uuidLeast"));
		if (uuidTransporterCore.getMostSignificantBits() == 0 && uuidTransporterCore.getLeastSignificantBits() == 0) {
			uuidTransporterCore = null;
			nameTransporterCore = "";
		}
		
		tickDeploying = tagCompound.getInteger("tickDeploying");
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		tagCompound.removeTag("tickDeploying");
		return tagCompound;
	}
	
	@Nonnull
	@Override
	public NBTTagCompound getUpdateTag() {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		writeToNBT(tagCompound);
		
		return tagCompound;
	}
	
	@Override
	public void onDataPacket(final NetworkManager networkManager, final SPacketUpdateTileEntity packet) {
		final NBTTagCompound tagCompound = packet.getNbtCompound();
		readFromNBT(tagCompound);
	}
	
	@Override
	public String toString() {
		return String.format("%s %s %8d EU linked to %s %s",
		                     getClass().getSimpleName(),
		                     Commons.format(world, pos),
		                     energy_getEnergyStored(),
		                     nameTransporterCore, uuidTransporterCore);
	}
}