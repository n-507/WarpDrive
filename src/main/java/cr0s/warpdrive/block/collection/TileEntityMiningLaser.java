package cr0s.warpdrive.block.collection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.EnumMiningLaserMode;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Optional;

public class TileEntityMiningLaser extends TileEntityAbstractMiner {
	
	private final boolean canSilktouch = (WarpDriveConfig.MINING_LASER_SILKTOUCH_DEUTERIUM_L <= 0 || FluidRegistry.isFluidRegistered("deuterium"));
	
	private boolean isActive() {
		return currentState != STATE_IDLE;
	}
	
	// persistent properties
	private int layerOffset = 1;
	private boolean mineAllBlocks = true;
	
	private static final int STATE_IDLE = 0;
	private static final int STATE_WARMUP = 1;
	private static final int STATE_SCANNING = 2;
	private static final int STATE_MINING = 3;
	private int currentState = 0;
	
	private int currentLayer;
	
	// computed properties
	private float explosionResistanceMax = 10000;
	private int delayTicks = 0;
	private boolean enoughPower = false;
	private int radiusCapacity = WarpDriveConfig.MINING_LASER_RADIUS_NO_LASER_MEDIUM;
	private final ArrayList<BlockPos> valuablesInLayer = new ArrayList<>();
	private int valuableIndex = 0;
	
	public TileEntityMiningLaser() {
		super();
		
		laserOutputSide = EnumFacing.DOWN;
		peripheralName = "warpdriveMiningLaser";
		addMethods(new String[] {
				"start",
				"stop",
				"state",
				"offset",
				"onlyOres",
				"silktouch"
		});
		CC_scripts = Arrays.asList("mine", "stop");
		doRequireUpgradeToInterface();
		
		laserMedium_maxCount = WarpDriveConfig.MINING_LASER_MAX_MEDIUMS_COUNT;
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		explosionResistanceMax = Blocks.OBSIDIAN.getExplosionResistance(world, pos, null, null);
	}
	
	@SuppressWarnings("UnnecessaryReturnStatement")
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		final IBlockState blockState = world.getBlockState(pos);
		if (currentState == STATE_IDLE) {
			delayTicks = WarpDriveConfig.MINING_LASER_WARMUP_DELAY_TICKS;
			updateBlockState(blockState, BlockMiningLaser.MODE, EnumMiningLaserMode.INACTIVE);
			
			// force start if no computer control is available
			if (!isInterfaceEnabled()) {
				enableSilktouch = false;
				layerOffset = 1;
				mineAllBlocks = true;
				start();
			}
			return;
		}
		
		final boolean isOnPlanet = CelestialObjectManager.hasAtmosphere(world, pos.getX(), pos.getZ());
		radiusCapacity = WarpDriveConfig.MINING_LASER_RADIUS_NO_LASER_MEDIUM
		               + (int) Math.floor(cache_laserMedium_factor * WarpDriveConfig.MINING_LASER_RADIUS_PER_LASER_MEDIUM);
		
		delayTicks--;
		if (currentState == STATE_WARMUP) {
			updateBlockState(blockState, BlockMiningLaser.MODE, EnumMiningLaserMode.SCANNING_LOW_POWER);
			if (delayTicks < 0) {
				delayTicks = WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS;
				currentState = STATE_SCANNING;
				updateBlockState(blockState, BlockMiningLaser.MODE, EnumMiningLaserMode.SCANNING_LOW_POWER);
				return;
			}
			
		} else if (currentState == STATE_SCANNING) {
			if (delayTicks == WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS - 1) {
				// check power level
				enoughPower = laserMedium_consumeExactly(isOnPlanet ? WarpDriveConfig.MINING_LASER_PLANET_ENERGY_PER_LAYER : WarpDriveConfig.MINING_LASER_SPACE_ENERGY_PER_LAYER, true);
				if (!enoughPower) {
					updateBlockState(blockState, BlockMiningLaser.MODE, EnumMiningLaserMode.SCANNING_LOW_POWER);
					delayTicks = WarpDriveConfig.MINING_LASER_WARMUP_DELAY_TICKS;
					return;
				} else {
					updateBlockState(blockState, BlockMiningLaser.MODE, EnumMiningLaserMode.SCANNING_POWERED);
				}
				
				// show current layer
				final int age = Math.max(40, 5 * WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS);
				final double xMax = pos.getX() + radiusCapacity + 1.0D;
				final double xMin = pos.getX() - radiusCapacity + 0.0D;
				final double zMax = pos.getZ() + radiusCapacity + 1.0D;
				final double zMin = pos.getZ() - radiusCapacity + 0.0D;
				final double y = currentLayer + 1.0D;
				PacketHandler.sendBeamPacket(world, new Vector3(xMin, y, zMin), new Vector3(xMax, y, zMin), 0.3F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(world, new Vector3(xMax, y, zMin), new Vector3(xMax, y, zMax), 0.3F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(world, new Vector3(xMax, y, zMax), new Vector3(xMin, y, zMax), 0.3F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(world, new Vector3(xMin, y, zMax), new Vector3(xMin, y, zMin), 0.3F, 0.0F, 1.0F, age, 0, 50);
				
			} else if (delayTicks < 0) {
				delayTicks = WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS;
				if (currentLayer <= 0) {
					stop();
					return;
				}
				
				// consume power
				enoughPower = laserMedium_consumeExactly(isOnPlanet ? WarpDriveConfig.MINING_LASER_PLANET_ENERGY_PER_LAYER : WarpDriveConfig.MINING_LASER_SPACE_ENERGY_PER_LAYER, false);
				if (!enoughPower) {
					updateBlockState(blockState, BlockMiningLaser.MODE, EnumMiningLaserMode.SCANNING_LOW_POWER);
					delayTicks = WarpDriveConfig.MINING_LASER_WARMUP_DELAY_TICKS;
					return;
				} else {
					updateBlockState(blockState, BlockMiningLaser.MODE, EnumMiningLaserMode.SCANNING_POWERED);
				}
				
				// scan
				scanLayer();
				if (!valuablesInLayer.isEmpty()) {
					final int r = (int) Math.ceil(radiusCapacity / 2.0D);
					final int offset = (pos.getY() - currentLayer) % (2 * r);
					final int age = Math.max(20, Math.round(2.5F * WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS));
					final double y = currentLayer + 1.0D;
					PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(pos.getX() - r + offset, y, pos.getZ() + r).translate(0.3D),
							0.0F, 0.0F, 1.0F, age, 0, 50);
					PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(pos.getX() + r, y, pos.getZ() + r - offset).translate(0.3D),
							0.0F, 0.0F, 1.0F, age, 0, 50);
					PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(pos.getX() + r - offset, y, pos.getZ() - r).translate(0.3D),
							0.0F, 0.0F, 1.0F, age, 0, 50);
					PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(pos.getX() - r, y, pos.getZ() - r + offset).translate(0.3D),
							0.0F, 0.0F, 1.0F, age, 0, 50);
					world.playSound(null, pos, SoundEvents.LASER_HIGH, SoundCategory.BLOCKS, 4F, 1F);
					delayTicks = WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS;
					if (currentState == STATE_SCANNING) {// remain stopped if an hard block was encountered
						currentState = STATE_MINING;
					}
					updateBlockState(blockState, BlockMiningLaser.MODE, EnumMiningLaserMode.MINING_POWERED);
					return;
					
				} else {
					world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 4F, 1F);
					currentLayer--;
				}
			}
		} else if (currentState == STATE_MINING) {
			if (delayTicks < 0) {
				delayTicks = WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS;
				
				if (valuableIndex < 0 || valuableIndex >= valuablesInLayer.size()) {
					delayTicks = WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS;
					currentState = STATE_SCANNING;
					updateBlockState(blockState, BlockMiningLaser.MODE, EnumMiningLaserMode.SCANNING_POWERED);
					
					// rescan same layer
					scanLayer();
					if (valuablesInLayer.size() <= 0) {
						currentLayer--;
					}
					return;
				}
				
				// consume power
				int requiredPower = isOnPlanet ? WarpDriveConfig.MINING_LASER_PLANET_ENERGY_PER_BLOCK : WarpDriveConfig.MINING_LASER_SPACE_ENERGY_PER_BLOCK;
				if (!mineAllBlocks) {
					requiredPower *= WarpDriveConfig.MINING_LASER_ORESONLY_ENERGY_FACTOR;
				}
				if (enableSilktouch) {
					requiredPower *= WarpDriveConfig.MINING_LASER_SILKTOUCH_ENERGY_FACTOR;
				}
				enoughPower = laserMedium_consumeExactly(requiredPower, false);
				if (!enoughPower) {
					updateBlockState(blockState, BlockMiningLaser.MODE, EnumMiningLaserMode.MINING_LOW_POWER);
					return;
				} else {
					updateBlockState(blockState, BlockMiningLaser.MODE, EnumMiningLaserMode.MINING_POWERED);
				}
				
				final BlockPos valuable = valuablesInLayer.get(valuableIndex);
				valuableIndex++;
				
				// Mine valuable ore
				final IBlockState blockStateValuable = world.getBlockState(valuable);
				
				// Skip if block is too hard or its empty block (check again in case it changed)
				if (!canDig(blockStateValuable, valuable)) {
					delayTicks = Math.round(WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS * 0.2F);
					return;
				}
				final int age = Math.max(10, Math.round((4 + world.rand.nextFloat()) * WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS));
				PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(valuable).translate(0.5D),
						1.0F, 1.0F, 0.0F, age, 0, 50);
				world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 4F, 1F);
				harvestBlock(valuable);
			}
		}
	}
	
	@Override
	protected void stop() {
		super.stop();
		currentState = STATE_IDLE;
		if (Commons.isSafeThread()) {
			updateBlockState(null, BlockMiningLaser.MODE, EnumMiningLaserMode.INACTIVE);
		}
	}
	
	private boolean canDig(final IBlockState blockState, final BlockPos blockPos) {
		// ignore air
		if (world.isAirBlock(blockPos)) {
			return false;
		}
		// check blacklists
		if (Dictionary.BLOCKS_SKIPMINING.contains(blockState.getBlock())) {
			return false;
		}
		if (Dictionary.BLOCKS_STOPMINING.contains(blockState.getBlock())) {
			stop();
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.info(String.format("%s Mining stopped by %s %s",
				                                    this, blockState, Commons.format(world, blockPos)));
			}
			return false;
		}
		// check whitelist
		if ( Dictionary.BLOCKS_MINING.contains(blockState.getBlock())
		  || Dictionary.BLOCKS_ORES.contains(blockState.getBlock())) {
			return true;
		}
		// check area protection
		if (isBlockBreakCanceled(null, world, blockPos)) {
			stop();
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.info(String.format("%s Mining stopped by cancelled event %s",
				                                    this, Commons.format(world, blockPos)));
			}
			return false;
		}
		// check default (explosion resistance is used to test for force fields and reinforced blocks, basically preventing mining a base or ship) 
		if (blockState.getBlock().getExplosionResistance(world, blockPos, null, null) <= explosionResistanceMax) {
			return true;
		}
		if (WarpDriveConfig.LOGGING_COLLECTION) {
			WarpDrive.logger.info(String.format("%s Rejecting %s %s",
			                                    this, blockState, Commons.format(world, blockPos)));
		}
		return false;
	}
	
	private void scanLayer() {
		// WarpDrive.logger.info("Scanning layer");
		final MutableBlockPos mutableBlockPos = new MutableBlockPos(pos);
		IBlockState blockState;
		for (int y = pos.getY() - 1; y > currentLayer; y --) {
			mutableBlockPos.setPos(pos.getX(), y, pos.getZ());
			blockState = world.getBlockState(mutableBlockPos);
			if (Dictionary.BLOCKS_STOPMINING.contains(blockState.getBlock())) {
				stop();
				if (WarpDriveConfig.LOGGING_COLLECTION) {
					WarpDrive.logger.info(String.format("%s Mining stopped by %s %s",
					                                    this, blockState, Commons.format(world, pos)));
				}
				return;
			}
		}
		
		BlockPos blockPos;
		valuablesInLayer.clear();
		valuableIndex = 0;
		int radius, x, z;
		int xMax, zMax;
		int xMin, zMin;
		
		// Search for valuable blocks
		x = pos.getX();
		z = pos.getZ();
		blockPos = new BlockPos(x, currentLayer, z);
		blockState = world.getBlockState(blockPos);
		if (canDig(blockState, blockPos)) {
			if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(blockState.getBlock())) {// Quarry collects all blocks or only collect valuables blocks
				valuablesInLayer.add(blockPos);
			}
		}
		for (radius = 1; radius <= radiusCapacity; radius++) {
			xMax = pos.getX() + radius;
			xMin = pos.getX() - radius;
			zMax = pos.getZ() + radius;
			zMin = pos.getZ() - radius;
			x = pos.getX();
			z = zMin;
			for (; x <= xMax; x++) {
				blockPos = new BlockPos(x, currentLayer, z);
				blockState = world.getBlockState(blockPos);
				if (canDig(blockState, blockPos)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(blockState.getBlock())) {// Quarry collects all blocks or only collect valuables blocks
						valuablesInLayer.add(blockPos);
					}
				}
			}
			x = xMax;
			z++;
			for (; z <= zMax; z++) {
				blockPos = new BlockPos(x, currentLayer, z);
				blockState = world.getBlockState(blockPos);
				if (canDig(blockState, blockPos)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(blockState.getBlock())) {// Quarry collects all blocks or only collect valuables blocks
						valuablesInLayer.add(blockPos);
					}
				}
			}
			x--;
			z = zMax;
			for (; x >= xMin; x--) {
				blockPos = new BlockPos(x, currentLayer, z);
				blockState = world.getBlockState(blockPos);
				if (canDig(blockState, blockPos)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(blockState.getBlock())) {// Quarry collects all blocks or only collect valuables blocks
						valuablesInLayer.add(blockPos);
					}
				}
			}
			x = xMin;
			z--;
			for (; z > zMin; z--) {
				blockPos = new BlockPos(x, currentLayer, z);
				blockState = world.getBlockState(blockPos);
				if (canDig(blockState, blockPos)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(blockState.getBlock())) {// Quarry collects all blocks or only collect valuables blocks
						valuablesInLayer.add(blockPos);
					}
				}
			}
			x = xMin;
			z = zMin;
			for (; x < pos.getX(); x++) {
				blockPos = new BlockPos(x, currentLayer, z);
				blockState = world.getBlockState(blockPos);
				if (canDig(blockState, blockPos)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(blockState.getBlock())) {// Quarry collects all blocks or only collect valuables blocks
						valuablesInLayer.add(blockPos);
					}
				}
			}
		}
		
		if (WarpDriveConfig.LOGGING_COLLECTION) {
			WarpDrive.logger.info(String.format("%s Found %s valueables",
			                                    this, valuablesInLayer.size()));
		}
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		layerOffset = tagCompound.getInteger("layerOffset");
		mineAllBlocks = tagCompound.getBoolean("mineAllBlocks");
		currentState = tagCompound.getInteger("currentState");
		currentLayer = tagCompound.getInteger("currentLayer");
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setInteger("layerOffset", layerOffset);
		tagCompound.setBoolean("mineAllBlocks", mineAllBlocks);
		tagCompound.setInteger("currentState", currentState);
		tagCompound.setInteger("currentLayer", currentLayer);
		return tagCompound;
	}
	
	// OpenComputers callback methods
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] start(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return start();
	}
	
	@SuppressWarnings("SameReturnValue")
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] stop(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		stop();
		return null;
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] state(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return state();
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] offset(final Context context, final Arguments arguments) {
		return offset(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] onlyOres(final Context context, final Arguments arguments) {
		return onlyOres(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] silktouch(final Context context, final Arguments arguments) {
		return silktouch(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// Common OC/CC methods
	private Object[] start() {
		if (isActive()) {
			return new Object[] { false, "Already started" };
		}
		
		enableSilktouch &= canSilktouch;
		delayTicks = WarpDriveConfig.MINING_LASER_WARMUP_DELAY_TICKS;
		currentState = STATE_WARMUP;
		currentLayer = pos.getY() - layerOffset - 1;
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(String.format("%s Starting from Y %d with silktouch %s",
			                                    this, currentLayer, enableSilktouch));
		}
		return new Boolean[] { true };
	}
	
	private Object[] state() {
		final int energy = laserMedium_getEnergyStored();
		final String status = getStatusHeaderInPureText();
		final Integer retValuablesInLayer, retValuablesMined;
		if (isActive()) {
			retValuablesInLayer = valuablesInLayer.size();
			retValuablesMined = valuableIndex;
		} else {
			retValuablesInLayer = 0;
			retValuablesMined = 0;
		}
		return new Object[] { status, isActive(), energy, currentLayer, retValuablesMined, retValuablesInLayer };
	}
	
	private Object[] onlyOres(final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			try {
				mineAllBlocks = ! Commons.toBool(arguments[0]);
				markDirty();
			} catch (final Exception exception) {
				return new Object[] { !mineAllBlocks };
			}
		}
		return new Object[] { !mineAllBlocks };
	}
	
	private Object[] offset(final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			try {
				layerOffset = Math.min(256, Math.abs(Commons.toInt(arguments[0])));
				markDirty();
			} catch (final Exception exception) {
				return new Integer[] { layerOffset };
			}
		}
		return new Integer[] { layerOffset };
	}
	
	private Object[] silktouch(final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			try {
				enableSilktouch = Commons.toBool(arguments[0]);
				markDirty();
			} catch (final Exception exception) {
				return new Object[] { enableSilktouch };
			}
		}
		return new Object[] { enableSilktouch };
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	public Object[] callMethod(@Nonnull final IComputerAccess computer, @Nonnull final ILuaContext context, final int method, @Nonnull final Object[] arguments) {
		final String methodName = CC_getMethodNameAndLogCall(method, arguments);
		
		switch (methodName) {
		case "start":
			return start();
			
		case "stop":
			stop();
			return null;
			
		case "state":
			return state();
			
		case "offset":
			return offset(arguments);
			
		case "onlyOres":
			return onlyOres(arguments);
			
		case "silktouch":
			return silktouch(arguments);
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	@Override
	public WarpDriveText getStatusHeader() {
		final int energy = laserMedium_getEnergyStored();
		WarpDriveText textState = new WarpDriveText(Commons.styleCorrect, "warpdrive.error.internal_check_console");
		if (currentState == STATE_IDLE) {
			textState = new WarpDriveText(Commons.styleCorrect, "warpdrive.mining_laser.status_line.idle");
		} else if (currentState == STATE_WARMUP) {
			textState = new WarpDriveText(Commons.styleCorrect, "warpdrive.mining_laser.status_line.warming_up");
		} else if (currentState == STATE_SCANNING) {
			if (mineAllBlocks) {
				textState = new WarpDriveText(Commons.styleCorrect, "warpdrive.mining_laser.status_line.scanning_all");
			} else {
				textState = new WarpDriveText(Commons.styleCorrect, "warpdrive.mining_laser.status_line.scanning_ores");
			}
		} else if (currentState == STATE_MINING) {
			if (!enableSilktouch) {
				if (mineAllBlocks) {
					textState = new WarpDriveText(Commons.styleCorrect, "warpdrive.mining_laser.status_line.mining_all");
				} else {
					textState = new WarpDriveText(Commons.styleCorrect, "warpdrive.mining_laser.status_line.mining_ores");
				}
			} else {
				if (mineAllBlocks) {
					textState = new WarpDriveText(Commons.styleCorrect, "warpdrive.mining_laser.status_line.mining_all_with_silktouch");
				} else {
					textState = new WarpDriveText(Commons.styleCorrect, "warpdrive.mining_laser.status_line.mining_ores_with_silktouch");
				}
			}
		}
		if (energy <= 0) {
			textState.appendSibling(new WarpDriveText(Commons.styleWarning, "warpdrive.mining_laser.status_line._insufficient_energy"));
		} else if (((currentState == STATE_SCANNING) || (currentState == STATE_MINING)) && !enoughPower) {
			textState.appendSibling(new WarpDriveText(Commons.styleWarning, "warpdrive.mining_laser.status_line._insufficient_energy"));
		}
		return textState;
	}
}
