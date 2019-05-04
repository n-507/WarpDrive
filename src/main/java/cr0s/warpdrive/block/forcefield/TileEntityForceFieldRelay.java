package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.IForceFieldUpgrade;
import cr0s.warpdrive.api.IForceFieldUpgradeEffector;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.data.EnumForceFieldUpgrade;
import cr0s.warpdrive.data.ForceFieldSetup;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nonnull;

public class TileEntityForceFieldRelay extends TileEntityAbstractForceField implements IForceFieldUpgrade {

	// persistent properties
	private EnumForceFieldUpgrade upgrade = EnumForceFieldUpgrade.NONE;
	
	public TileEntityForceFieldRelay() {
		super();
		
		peripheralName = "warpdriveForceFieldRelay";
		doRequireUpgradeToInterface();
	}
	
	// onFirstUpdateTick
	// update
	
	protected EnumForceFieldUpgrade getUpgrade() {
		if (upgrade == null) {
			return EnumForceFieldUpgrade.NONE;
		}
		return upgrade;
	}
	
	protected void setUpgrade(final EnumForceFieldUpgrade upgrade) {
		this.upgrade = upgrade;
		markDirty();
	}
	
	@Override
	protected WarpDriveText getUpgradeStatus() {
		final EnumForceFieldUpgrade enumForceFieldUpgrade = getUpgrade();
		final ITextComponent strDisplayName = new TextComponentTranslation("warpdrive.force_field.upgrade.status_line." + enumForceFieldUpgrade.getName());
		if (enumForceFieldUpgrade == EnumForceFieldUpgrade.NONE) {
			return new WarpDriveText(Commons.styleWarning,"warpdrive.upgrade.status_line.none",
			                         strDisplayName);
		} else {
			return new WarpDriveText(Commons.styleCorrect,"warpdrive.upgrade.status_line.valid",
			                         strDisplayName);
		}
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		setUpgrade(EnumForceFieldUpgrade.get(tagCompound.getByte("upgrade")));
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		tagCompound.setByte("upgrade", (byte) getUpgrade().ordinal());
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
	public Object[] getEnergyRequired() {
		return new Object[] { false, "No energy consumption" };
	}
	
	@Override
	public IForceFieldUpgradeEffector getUpgradeEffector() {
		return isEnabled ? getUpgrade() : null;
	}
	
	@Override
	public float getUpgradeValue() {
		return isEnabled ? getUpgrade().getUpgradeValue() * (1.0F + (enumTier.getIndex() - 1) * ForceFieldSetup.FORCEFIELD_UPGRADE_BOOST_FACTOR_PER_RELAY_TIER) : 0.0F;
	}
}
