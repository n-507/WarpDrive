package cr0s.warpdrive.block;

import cr0s.warpdrive.CommonProxy;
import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IBlockUpdateDetector;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.data.CameraRegistryItem;
import cr0s.warpdrive.data.EnumTier;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class TileEntityAbstractBase extends TileEntity implements IBlockUpdateDetector, ITickable {
	
	// persistent properties
	// (none)
	
	// computed properties
	private boolean isConstructed = false;
	private boolean isFirstTick = true;
	private boolean isDirty = false;
	
	protected EnumTier enumTier;
	
	public TileEntityAbstractBase() {
		super();
	}
	
	@Override
	public void onLoad() {
		super.onLoad();
		assert hasWorld() && pos != BlockPos.ORIGIN;
		if (!isConstructed) {
			onConstructed();
		}
	}
	
	protected void onConstructed() {
		// warning: we can't use Block.CreateNewTileEntity() as world loading calls the TileEntity constructor directly
		// warning: we can't use setPos(), setWorld() or validate() as getBlockType() will cause a stack overflow
		// warning: we can't use onLoad() to trigger this method as onLoad() isn't always called, see https://github.com/MinecraftForge/MinecraftForge/issues/5061
		
		if (world == null) {// we're client side, tier is already set
			return;
		}
		
		// immediately retrieve tier, we need it to connect energy conduits and save the world before the first tick
		final Block block = getBlockType();
		if (block instanceof IBlockBase) {
			enumTier = ((IBlockBase) block).getTier(ItemStack.EMPTY);
		} else {
			WarpDrive.logger.error(String.format("Invalid block for %s %s: %s",
			                                     this, Commons.format(world, pos), block));
			enumTier = EnumTier.BASIC;
		}
		
		isConstructed = true;
	}
	
	protected void onFirstUpdateTick() {
		// No operation
		assert isConstructed;
		assert enumTier != null;
	}
	
	@Override
	public void update() {
		if (isFirstTick) {
			isFirstTick = false;
			onFirstUpdateTick();
		}
		
		if (isDirty) {
			markDirty();
		}
	}
	
	protected boolean isFirstTick() {
		return isFirstTick;
	}
	
	@Override
	public void onBlockUpdateDetected() {
		assert Commons.isSafeThread();
		if (!isConstructed) {
			onConstructed();
		}
	}
	
	protected <T extends Comparable<T>, V extends T> void updateBlockState(final IBlockState blockState_in, final IProperty<T> property, final V value) {
		IBlockState blockState = blockState_in;
		if (blockState == null) {
			blockState = world.getBlockState(pos);
		}
		if (property != null) {
			if (!blockState.getProperties().containsKey(property)) {
				WarpDrive.logger.error(String.format("Unable to update block state due to missing property in %s: %s calling updateBlockState(%s, %s, %s)",
				                                     blockState.getBlock(), this, blockState_in, property, value));
				return;
			}
			if (blockState.getValue(property) == value) {
				return;
			}
			blockState = blockState.withProperty(property, value);
		}
		if (getBlockMetadata() != blockState.getBlock().getMetaFromState(blockState)) {
			world.setBlockState(pos, blockState, 2);
		}
	}
	
	protected void updateBlockState(final IBlockState blockState_in, @Nonnull final IBlockState blockState_new) {
		IBlockState blockState_old = blockState_in;
		if (blockState_old == null) {
			blockState_old = world.getBlockState(pos);
		}
		
		final Block block_old = blockState_old.getBlock();
		final Block block_new = blockState_new.getBlock();
		if (block_new != block_old) {
			WarpDrive.logger.error(String.format("Unable to update block state from %s to %s: %s calling updateBlockState(%s, %s)",
			                                     block_old, block_new, this, blockState_in, blockState_new));
			return;
		}
		
		final int metadata_old = block_old.getMetaFromState(blockState_old);
		final int metadata_new = block_new.getMetaFromState(blockState_new);
		if (metadata_old != metadata_new) {
			world.setBlockState(pos, blockState_new, 2);
		}
	}
	
	@Override
	public void markDirty() {
		if ( hasWorld()
		  && Commons.isSafeThread() ) {
			super.markDirty();
			isDirty = false;
			final IBlockState blockState = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, blockState, blockState, 3);
			WarpDrive.starMap.onBlockUpdated(world, pos, blockState);
		} else {
			isDirty = true;
		}
	}
	
	@Override
	public boolean shouldRefresh(final World world, final BlockPos pos, @Nonnull final IBlockState blockStateOld, @Nonnull final IBlockState blockStateNew) {
		return blockStateOld.getBlock() != blockStateNew.getBlock();
	}
	
	
	// area protection
	protected boolean isBlockBreakCanceled(final UUID uuidPlayer, final World world, final BlockPos blockPosEvent) {
		return CommonProxy.isBlockBreakCanceled(uuidPlayer, pos, world, blockPosEvent);
	}
	
	protected boolean isBlockPlaceCanceled(final UUID uuidPlayer, final World world, final BlockPos blockPosEvent, final IBlockState blockState) {
		return CommonProxy.isBlockPlaceCanceled(uuidPlayer, pos, world, blockPosEvent, blockState);
	}
	
	// saved properties
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		if (tagCompound.hasKey("upgrades")) {
			final NBTTagCompound nbtTagCompoundUpgrades = tagCompound.getCompoundTag("upgrades");
			final Set<String> keys = nbtTagCompoundUpgrades.getKeySet();
			for (final String key : keys) {
				Object object = getUpgradeFromString(key);
				final int value = nbtTagCompoundUpgrades.getByte(key);
				if (object == null) {
					WarpDrive.logger.error(String.format("Found an unknown upgrade named %s in %s",
					                                     key, this));
					object = key;
				}
				installedUpgrades.put(object, value);
			}
		}
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		if (!installedUpgrades.isEmpty()) {
			final NBTTagCompound nbtTagCompoundUpgrades = new NBTTagCompound();
			for (final Entry<Object, Integer> entry : installedUpgrades.entrySet()) {
				final String key = getUpgradeAsString(entry.getKey());
				nbtTagCompoundUpgrades.setByte(key, (byte)(int)entry.getValue());
			}
			tagCompound.setTag("upgrades", nbtTagCompoundUpgrades);
		}
		return tagCompound;
	}
	
	public NBTTagCompound writeItemDropNBT(final NBTTagCompound tagCompound) {
		writeToNBT(tagCompound);
		tagCompound.removeTag("x");
		tagCompound.removeTag("y");
		tagCompound.removeTag("z");
		return tagCompound;
	}
	
	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, getBlockMetadata(), getUpdateTag());
	}
	
	// status
	protected WarpDriveText getUpgradeStatus() {
		final String strUpgrades = getUpgradesAsString();
		if (strUpgrades.isEmpty()) {
			return new WarpDriveText(Commons.styleCorrect, "warpdrive.upgrade.status_line.none",
			                         strUpgrades);
		} else {
			return new WarpDriveText(Commons.styleCorrect, "warpdrive.upgrade.status_line.valid",
			                         strUpgrades);
		}
	}
	
	protected WarpDriveText getStatusPrefix() {
		if (world != null) {
			final Item item = Item.getItemFromBlock(getBlockType());
			if (item != Items.AIR) {
				final ItemStack itemStack = new ItemStack(item, 1, getBlockMetadata());
				return Commons.getChatPrefix(itemStack);
			}
		}
		return new WarpDriveText();
	}
	
	protected WarpDriveText getBeamFrequencyStatus(final int beamFrequency) {
		if (beamFrequency == -1) {
			return new WarpDriveText(Commons.styleWarning, "warpdrive.beam_frequency.status_line.undefined");
		} else if (beamFrequency < 0) {
			return new WarpDriveText(Commons.styleWarning, "warpdrive.beam_frequency.status_line.invalid", beamFrequency);
		} else {
			return new WarpDriveText(Commons.styleCorrect, "warpdrive.beam_frequency.status_line.valid", beamFrequency);
		}
	}
	
	protected WarpDriveText getVideoChannelStatus(final int videoChannel) {
		if (videoChannel == -1) {
			return new WarpDriveText(Commons.styleWarning, "warpdrive.video_channel.status_line.undefined");
		} else if (videoChannel < 0) {
			return new WarpDriveText(Commons.styleWarning, "warpdrive.video_channel.status_line.invalid",
			                         videoChannel);
		} else {
			final CameraRegistryItem camera = WarpDrive.cameras.getCameraByVideoChannel(world, videoChannel);
			if (camera == null) {
				return new WarpDriveText(Commons.styleWarning, "warpdrive.video_channel.status_line.not_loaded",
				                         videoChannel);
			} else if (camera.isTileEntity(this)) {
				return new WarpDriveText(Commons.styleCorrect, "warpdrive.video_channel.status_line.valid_self",
				                         videoChannel);
			} else {
				return new WarpDriveText(Commons.styleCorrect, "warpdrive.video_channel.status_line.valid_other",
				                         videoChannel,
				                         Commons.format(world, camera.blockPos) );
			}
		}
	}
	
	public WarpDriveText getStatusHeader() {
		return new WarpDriveText();
	}
	
	public WarpDriveText getStatus() {
		final WarpDriveText message = getStatusPrefix();
		message.appendSibling( getStatusHeader() );
		
		if (this instanceof IBeamFrequency) {
			// only show in item form or from server side
			if ( world == null
			  || !world.isRemote ) {
				message.append( getBeamFrequencyStatus(((IBeamFrequency) this).getBeamFrequency()) );
			}
		}
		
		if (this instanceof IVideoChannel) {
			// only show in item form or from client side
			if ( world == null
			  || world.isRemote ) {
				message.append( getVideoChannelStatus(((IVideoChannel) this).getVideoChannel()) );
			}
		}
		
		if (isUpgradeable()) {
			message.append( getUpgradeStatus() );
		}
		
		return message;
	}
	
	public final WarpDriveText getStatus(final ItemStack itemStack, final IBlockState blockState) {
		// (this is a temporary object to compute status)
		// get tier from ItemStack
		final Block block = blockState.getBlock();
		if (block instanceof IBlockBase) {
			enumTier = ((IBlockBase) block).getTier(itemStack);
			onConstructed();
		}
		
		// get persistent properties
		final NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound != null) {
			readFromNBT(tagCompound);
		}
		
		// compute status
		return getStatus();
	}
	
	public String getStatusHeaderInPureText() {
		return Commons.removeFormatting( getStatusHeader().getUnformattedText() );
	}
	
	// upgrade system
	private final HashMap<Object, Integer> installedUpgrades = new HashMap<>(10);
	private final HashMap<Object, Integer> maxUpgrades = new HashMap<>(10);
	public boolean isUpgradeable() {
		return !maxUpgrades.isEmpty();
	}
	public boolean hasUpgrade(final Object upgrade) {
		return getUpgradeCount(upgrade) > 0;
	}
	
	private String getUpgradeAsString(final Object object) {
		if (object instanceof Item) {
			return Item.REGISTRY.getNameForObject((Item) object).toString();
		} else if (object instanceof Block) {
			return Block.REGISTRY.getNameForObject((Block) object).toString();
		} else if (object instanceof ItemStack) {
			return Item.REGISTRY.getNameForObject(((ItemStack) object).getItem()) + ":" + ((ItemStack) object).getItemDamage();
		} else {
			return object.toString();
		}
	}
	
	private Object getUpgradeFromString(final String name) {
		for (final Object object : maxUpgrades.keySet()) {
			if (getUpgradeAsString(object).equals(name)) {
				return object;
			}
		}
		return null;
	}
	
	public Object getFirstUpgradeOfType(final Class clazz, final Object defaultValue) {
		for (final Object object : installedUpgrades.keySet()) {
			if (clazz != null && clazz.isInstance(object)) {
				return object;
			}
		}
		return defaultValue;
	}
	
	public Map<Object, Integer> getUpgradesOfType(final Class clazz) {
		if (clazz == null) {
			return installedUpgrades;
		}
		final Map<Object, Integer> mapResult = new HashMap<>(installedUpgrades.size());
		for (final Entry<Object, Integer> entry : installedUpgrades.entrySet()) {
			if (clazz.isInstance(entry.getKey())) {
				mapResult.put(entry.getKey(), entry.getValue());
			}
		}
		return mapResult;
	}
	
	public int getValidUpgradeCount(final Object upgrade) {
		return Math.min(getUpgradeMaxCount(upgrade), getUpgradeCount(upgrade));
	}
	
	public int getUpgradeCount(final Object upgrade) {
		final Integer value = installedUpgrades.get(upgrade);
		return value == null ? 0 : value;
	}
	
	public int getUpgradeMaxCount(final Object upgrade) {
		final Integer value = maxUpgrades.get(upgrade);
		return value == null ? 0 : value;
	}
	
	protected String getUpgradesAsString() {
		final StringBuilder message = new StringBuilder();
		for (final Entry<Object, Integer> entry : installedUpgrades.entrySet()) {
			if (message.length() > 0) {
				message.append(", ");
			}
			final Object key = entry.getKey();
			String keyName = key.toString();
			if (key instanceof Item) {
				keyName = ((Item) key).getTranslationKey();
			} else if (key instanceof Block) {
				keyName = ((Block) key).getTranslationKey();
			}
			if (entry.getValue() == 1) {
				message.append(keyName);
			} else {
				message.append(entry.getValue()).append(" x ").append(keyName);
			}
		}
		return message.toString();
	}
	
	protected void setUpgradeMaxCount(final Object upgrade, final int value) {
		maxUpgrades.put(upgrade, value);
	}
	
	public boolean canUpgrade(final Object upgrade) {
		return getUpgradeMaxCount(upgrade) >= getUpgradeCount(upgrade) + 1;
	}
	
	public boolean mountUpgrade(final Object upgrade) {
		if (canUpgrade(upgrade)) {
			final int countNew = getUpgradeCount(upgrade) + 1;
			installedUpgrades.put(upgrade, countNew);
			onUpgradeChanged(upgrade, countNew, true);
			markDirty();
			return true;
		}
		return false;
	}
	
	public boolean dismountUpgrade(final Object upgrade) {
		final int count = getUpgradeCount(upgrade);
		if (count > 1) {
			installedUpgrades.put(upgrade, count - 1);
			onUpgradeChanged(upgrade, count - 1, false);
			markDirty();
			return true;
			
		} else if (count > 0) {
			installedUpgrades.remove(upgrade);
			onUpgradeChanged(upgrade, 0, false);
			markDirty();
			return true;
		}
		return false;
	}
	
	protected void onUpgradeChanged(final Object upgrade, final int countNew, final boolean isAdded) {
	
	}
	
	public void onEMP(final float efficiency) {
	
	}
	
	@Override
	public String toString() {
		return String.format("%s %s",
		                     getClass().getSimpleName(),
		                     Commons.format(world, pos));
	}
}
