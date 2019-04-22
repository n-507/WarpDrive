package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.ReactorFace;
import cr0s.warpdrive.data.EnumReactorOutputMode;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.Arrays;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

public class TileEntityEnanReactorCore extends TileEntityEnanReactorController {
	
	private static final int ENAN_REACTOR_SETUP_TICKS = 1200;
	
	// generation & instability is 'per tick'
	private static final double INSTABILITY_MIN = 0.004D;
	private static final double INSTABILITY_MAX = 0.060D;
	
	// laser stabilization is per shot
	// target is to consume 10% max output power every second, hence 2.5% per side
	// laser efficiency is 33% at 16% power (target spot), 50% at 24% power, 84% at 50% power, etc.
	// 10% * 20 * PR_MAX_GENERATION / (4 * 0.16) => ~200kRF => ~ max laser energy
	private static final double PR_MAX_LASER_ENERGY = 200000.0D;
	private static final double PR_MAX_LASER_EFFECT = INSTABILITY_MAX * 20 / 0.33D;
	
	private boolean hold = true; // hold updates and power output until reactor is controlled (i.e. don't explode on chunk-loading while computer is booting)
	
	// persistent properties
	private EnumReactorOutputMode enumReactorOutputMode = EnumReactorOutputMode.OFF;
	private int outputThreshold = 0;
	private double instabilityTarget = 50.0D;
	private int stabilizerEnergy = 10000;
	
	private int containedEnergy = 0;
	private final double[] instabilityValues = new double[ReactorFace.maxInstabilities]; // no instability  = 0, explosion = 100
	
	// computed properties
	private boolean isFirstException = true;
	private int energyStored_max;
	private int generation_offset;
	private int generation_range;
	
	private int updateTicks = 0;
	private int setupTicks = 0;
	
	private float lasersReceived = 0;
	private int lastGenerationRate = 0;
	private int releasedThisTick = 0; // amount of energy released during current tick update
	private long releasedThisCycle = 0; // amount of energy released during current cycle
	private long energyReleasedLastCycle = 0;
	
	@SuppressWarnings("unchecked")
	private final WeakReference<TileEntityEnanReactorLaser>[] weakTileEntityLasers = (WeakReference<TileEntityEnanReactorLaser>[]) Array.newInstance(WeakReference.class, ReactorFace.maxInstabilities);
	
	public TileEntityEnanReactorCore() {
		super();
		
		peripheralName = "warpdriveEnanReactorCore";
		
		// disable reactor by default
		isEnabled = false;
	}
	
	@Override
	public void onConstructed() {
		super.onConstructed();
		
		energyStored_max  = WarpDriveConfig.ENAN_REACTOR_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()];
		generation_offset = WarpDriveConfig.ENAN_REACTOR_GENERATION_MIN_RF_BY_TIER[enumTier.getIndex()];
		generation_range  = WarpDriveConfig.ENAN_REACTOR_GENERATION_MAX_RF_BY_TIER[enumTier.getIndex()] - generation_offset;
		
		energy_setParameters(EnergyWrapper.convertRFtoInternal_floor(energyStored_max),
		                     262144, 262144,
		                     "HV", 0, "LuV", 2);
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("updateTicks %d setupTicks %d releasedThisTick %6d lasersReceived %.5f releasedThisCycle %6d containedEnergy %8d",
			                                    updateTicks, setupTicks, releasedThisTick, lasersReceived, releasedThisCycle, containedEnergy));
		}
		releasedThisTick = 0;
		
		setupTicks--;
		if (setupTicks <= 0) {
			setupTicks = ENAN_REACTOR_SETUP_TICKS;
			scanSetup();
		}
		
		lasersReceived = Math.max(0.0F, lasersReceived - 0.05F);
		updateTicks--;
		if (updateTicks > 0) {
			return;
		}
		updateTicks = WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS;
		energyReleasedLastCycle = releasedThisCycle;
		releasedThisCycle = 0;
		
		refreshBlockState();
		
		if (!hold) {// still loading/booting => hold simulation
			// unstable at all time
			if (shouldExplode()) {
				explode();
			}
			increaseInstability();
			
			generateEnergy();
			
			runControlLoop();
		}
		
		sendEvent("reactorPulse", lastGenerationRate);
	}
	
	private void increaseInstability() {
		for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
			// increase instability
			final int indexStability = reactorFace.indexStability;
			if (containedEnergy > 2000) {
				final double amountToIncrease = WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS
						* Math.max(INSTABILITY_MIN, INSTABILITY_MAX * Math.pow((world.rand.nextDouble() * containedEnergy) / energyStored_max, 0.1));
				if (WarpDriveConfig.LOGGING_ENERGY) {
					WarpDrive.logger.info(String.format("increaseInstability %.5f",
					                                    amountToIncrease));
				}
				instabilityValues[indexStability] += amountToIncrease;
			} else {
				// when charge is extremely low, reactor is naturally stabilizing, to avoid infinite decay
				final double amountToDecrease = WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS * Math.max(INSTABILITY_MIN, instabilityValues[indexStability] * 0.02D);
				instabilityValues[indexStability] = Math.max(0.0D, instabilityValues[indexStability] - amountToDecrease);
			}
		}
	}
	
	void decreaseInstability(@Nonnull final ReactorFace reactorFace, final int energy) {
		if (reactorFace.indexStability < 0) {
			return;
		}
		
		final int amount = EnergyWrapper.convertInternalToRF_floor(energy);
		if (amount <= 1) {
			return;
		}
		
		lasersReceived = Math.min(10.0F, lasersReceived + 1.0F / WarpDriveConfig.ENAN_REACTOR_MAX_LASERS_PER_SECOND[enumTier.getIndex()]);
		double nospamFactor = 1.0D;
		if (lasersReceived > 1.0F) {
			nospamFactor = 0.5;
			world.newExplosion(null,
			                   pos.getX() + reactorFace.x - reactorFace.facingLaserProperty.getXOffset(),
			                   pos.getY() + reactorFace.y - reactorFace.facingLaserProperty.getYOffset(),
			                   pos.getZ() + reactorFace.z - reactorFace.facingLaserProperty.getZOffset(),
			                   1, false, false);
		}
		final double normalisedAmount = Math.min(1.0D, Math.max(0.0D, amount / PR_MAX_LASER_ENERGY)); // 0.0 to 1.0
		final double baseLaserEffect = 0.5D + 0.5D * Math.cos( Math.PI * Math.log10(0.1D + 0.9D * normalisedAmount) ); // 0.0 to 1.0
		final double randomVariation = 0.8D + 0.4D * world.rand.nextDouble(); // ~1.0
		final double amountToRemove = PR_MAX_LASER_EFFECT * baseLaserEffect * randomVariation * nospamFactor;
		
		final int indexStability = reactorFace.indexStability;
		
		if (WarpDriveConfig.LOGGING_ENERGY) {
			if (indexStability == 3) {
				WarpDrive.logger.info(String.format("Instability on %s decreased by %.1f/%.1f after consuming %d/%.1f laserReceived is %.1f hence nospamFactor is %.3f",
				                                    reactorFace, amountToRemove, PR_MAX_LASER_EFFECT,
				                                    amount, PR_MAX_LASER_ENERGY, lasersReceived, nospamFactor));
			}
		}
		
		instabilityValues[indexStability] = Math.max(0, instabilityValues[indexStability] - amountToRemove);
	}
	
	private void generateEnergy() {
		double stabilityOffset = 0.5;
		for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
			stabilityOffset *= Math.max(0.01D, instabilityValues[reactorFace.indexStability] / 100.0D);
		}
		
		if (isEnabled) {// producing, instability increases output, you want to take the risk
			final int amountToGenerate = (int) Math.ceil(WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS * (0.5D + stabilityOffset)
					* ( generation_offset
					  + generation_range * Math.pow(containedEnergy / (double) energyStored_max, 0.6D)));
			containedEnergy = Math.min(containedEnergy + amountToGenerate, energyStored_max);
			lastGenerationRate = amountToGenerate / WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS;
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info(String.format("Generated %d", amountToGenerate));
			}
		} else {// decaying over 20s without producing power, you better have power for those lasers
			final int amountToDecay = (int) (WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS * (1.0D - stabilityOffset) * (generation_offset + containedEnergy * 0.01D));
			containedEnergy = Math.max(0, containedEnergy - amountToDecay);
			lastGenerationRate = 0;
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info(String.format("Decayed %d", amountToDecay));
			}
		}
	}
	
	private void runControlLoop() {
		for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
			if (instabilityValues[reactorFace.indexStability] > instabilityTarget) {
				final TileEntityEnanReactorLaser tileEntityEnanReactorLaser = getLaser(reactorFace);
				if (tileEntityEnanReactorLaser != null) {
					if (tileEntityEnanReactorLaser.stabilize(stabilizerEnergy) == -stabilizerEnergy) {
						// chunk isn't updating properly => protect the reactor
						instabilityValues[reactorFace.indexStability] = instabilityTarget;
						hold = true;
					}
				}
			}
		}
	}
	
	@Nullable
	private TileEntityEnanReactorLaser getLaser(@Nonnull final ReactorFace reactorFace) {
		final WeakReference<TileEntityEnanReactorLaser> weakTileEntityLaser = weakTileEntityLasers[reactorFace.indexStability];
		TileEntityEnanReactorLaser tileEntityEnanReactorLaser;
		if (weakTileEntityLaser != null) {
			tileEntityEnanReactorLaser = weakTileEntityLaser.get();
			if ( tileEntityEnanReactorLaser != null
			  && !tileEntityEnanReactorLaser.isInvalid() ) {
				return tileEntityEnanReactorLaser;
			}
		}
		final TileEntity tileEntity = world.getTileEntity(
				pos.add(reactorFace.x, reactorFace.y, reactorFace.z));
		if (tileEntity instanceof TileEntityEnanReactorLaser) {
			tileEntityEnanReactorLaser =(TileEntityEnanReactorLaser) tileEntity;
			weakTileEntityLasers[reactorFace.indexStability] = new WeakReference<>(tileEntityEnanReactorLaser);
			return tileEntityEnanReactorLaser;
		}
		return null;
	}
	
	Vector3 getCenter() {
		final Vector3 vCenter = new Vector3(this).translate(0.5D);
		switch (enumTier) {
		case BASIC:
		default:
			break;
		case ADVANCED:
			vCenter.y += 3;
			break;
		case SUPERIOR:
			vCenter.y += 4;
			break;
		}
		return vCenter;
	}
	
	private boolean shouldExplode() {
		boolean exploding = false;
		for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
			exploding = exploding || (instabilityValues[reactorFace.indexStability] >= 100);
		}
		exploding &= (world.rand.nextInt(4) == 2);
		
		if (exploding) {
			final StringBuilder statusLasers = new StringBuilder();
			final MutableBlockPos mutableBlockPos = new MutableBlockPos();
			for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
				long energyStored = -1L;
				int countLaserMediums = 0;
				mutableBlockPos.setPos(
						pos.getX() + reactorFace.x,
						pos.getY() + reactorFace.y,
						pos.getZ() + reactorFace.z );
				final TileEntity tileEntity = world.getTileEntity(mutableBlockPos);
				if (tileEntity instanceof TileEntityEnanReactorLaser) {
					try {
						energyStored = ((TileEntityEnanReactorLaser) tileEntity).laserMedium_getEnergyStored(true);
						countLaserMediums = ((TileEntityEnanReactorLaser) tileEntity).laserMedium_getCount();
					} catch (final Exception exception) {
						if (isFirstException) {
							exception.printStackTrace();
							isFirstException = false;
						}
						WarpDrive.logger.error(String.format("%s tileEntity is %s",
						                                     this, tileEntity));
					}
					statusLasers.append(String.format("\n- face %s has reached instability %.2f while laser has %d energy available with %d laser medium(s)",
					                                  reactorFace.name,
					                                  instabilityValues[reactorFace.indexStability],
					                                  energyStored,
					                                  countLaserMediums));
				} else {
					statusLasers.append(String.format("\n- face %s has reached instability %.2f while laser is missing in action",
					                                 reactorFace.name,
					                                 instabilityValues[reactorFace.indexStability] ));
				}
			}
			
			WarpDrive.logger.info(String.format("%s Explosion triggered\nEnergy stored is %d, Laser received is %.2f, reactor is %s%s",
			                                    this,
			                                    containedEnergy,
			                                    lasersReceived,
			                                    isEnabled ? "ENABLED" : "DISABLED",
			                                    statusLasers.toString()));
			isEnabled = false;
		}
		return exploding;
	}
	
	private void explode() {
		// remove blocks randomly up to x blocks around (breaking whatever protection is there)
		final double normalizedEnergy = containedEnergy / (double) energyStored_max;
		final double factorEnergy = Math.pow(normalizedEnergy, 0.125);
		final int radius = (int) Math.round( WarpDriveConfig.ENAN_REACTOR_EXPLOSION_MAX_RADIUS_BY_TIER[enumTier.getIndex()]
		                                   * factorEnergy );
		final double chanceOfRemoval = WarpDriveConfig.ENAN_REACTOR_EXPLOSION_MAX_REMOVAL_CHANCE_BY_TIER[enumTier.getIndex()]
		                             * factorEnergy;
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(this + " Explosion radius is " + radius + ", Chance of removal is " + chanceOfRemoval);
		}
		if (radius > 1) {
			final MutableBlockPos mutableBlockPos = new MutableBlockPos(pos);
			final float explosionResistanceThreshold = Blocks.OBSIDIAN.getExplosionResistance(world, mutableBlockPos, null, null);
			for (int x = pos.getX() - radius; x <= pos.getX() + radius; x++) {
				for (int y = pos.getY() - radius; y <= pos.getY() + radius; y++) {
					for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; z++) {
						if (z != pos.getZ() || y != pos.getY() || x != pos.getX()) {
							if (world.rand.nextDouble() < chanceOfRemoval) {
								mutableBlockPos.setPos(x, y, z);
								final float explosionResistanceActual = world.getBlockState(mutableBlockPos).getBlock().getExplosionResistance(world, mutableBlockPos, null, null);
								if (explosionResistanceActual >= explosionResistanceThreshold) {
									world.setBlockToAir(mutableBlockPos);
								}
							}
						}
					}
				}
			}
		}
		
		// remove reactor
		world.setBlockToAir(pos);
		
		// set a few augmented TnT around reactor core
		final int countExplosions = WarpDriveConfig.ENAN_REACTOR_EXPLOSION_COUNT_BY_TIER[enumTier.getIndex()];
		final float strengthMin = WarpDriveConfig.ENAN_REACTOR_EXPLOSION_STRENGTH_MIN_BY_TIER[enumTier.getIndex()];
		final int strengthRange = (int) Math.ceil(WarpDriveConfig.ENAN_REACTOR_EXPLOSION_STRENGTH_MAX_BY_TIER[enumTier.getIndex()] - strengthMin);
		for (int i = 0; i < countExplosions; i++) {
			world.newExplosion(null,
			                   pos.getX() + world.rand.nextInt(3) - 1.5D,
			                   pos.getY() + world.rand.nextInt(3) - 0.5D,
			                   pos.getZ() + world.rand.nextInt(3) - 1.5D,
				               strengthMin + world.rand.nextInt(strengthRange), true, true);
		}
	}
	
	private void refreshBlockState() {
		double maxInstability = 0.0D;
		for (final Double instability : instabilityValues) {
			if (instability > maxInstability) {
				maxInstability = instability;
			}
		}
		final int instabilityNibble = (int) Math.max(0, Math.min(3, Math.round(maxInstability / 25.0D)));
		final int energyNibble = (int) Math.max(0, Math.min(3, Math.round(4.0D * containedEnergy / energyStored_max)));
		
		final IBlockState blockStateNew = getBlockType().getDefaultState()
		                                                .withProperty(BlockEnanReactorCore.ENERGY, energyNibble)
		                                                .withProperty(BlockEnanReactorCore.INSTABILITY, instabilityNibble);
		updateBlockState(null, blockStateNew);
	}
	
	@Override
	public void onBlockUpdateDetected() {
		super.onBlockUpdateDetected();
		
		setupTicks = 0;
	}
	
	private void scanSetup() {
		final MutableBlockPos mutableBlockPos = new MutableBlockPos(pos);
		
		// first check if we have the required 'air' blocks
		boolean isValid = true;
		for (final ReactorFace reactorFace : ReactorFace.get(enumTier)) {
			assert reactorFace.enumTier == enumTier;
			if (reactorFace.indexStability < 0) {
				mutableBlockPos.setPos(pos.getX() + reactorFace.x,
				                       pos.getY() + reactorFace.y,
				                       pos.getZ() + reactorFace.z);
				final IBlockState blockState = world.getBlockState(mutableBlockPos);
				final boolean isAir = blockState.getBlock().isAir(blockState, world, mutableBlockPos);
				if (!isAir) {
					isValid = false;
					final Vector3 vPosition = new Vector3(mutableBlockPos).translate(0.5D);
					PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5, vPosition,
					                                      new Vector3(0.0D, 0.0D, 0.0D),
					                                      1.0F, 1.0F, 1.0F,
					                                      1.0F, 1.0F, 1.0F,
					                                      32);
				}
			}
		}
		
		// then update the stabilization lasers accordingly
		for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
			mutableBlockPos.setPos(pos.getX() + reactorFace.x,
			                       pos.getY() + reactorFace.y,
			                       pos.getZ() + reactorFace.z);
			final TileEntity tileEntity = world.getTileEntity(mutableBlockPos);
			if (tileEntity instanceof TileEntityEnanReactorLaser) {
				if (isValid) {
					((TileEntityEnanReactorLaser) tileEntity).setReactorFace(reactorFace, this);
				} else {
					((TileEntityEnanReactorLaser) tileEntity).setReactorFace(ReactorFace.UNKNOWN, null);
				}
			} else {
				final Vector3 vPosition = new Vector3(mutableBlockPos).translate(0.5D);
				PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5, vPosition,
				                                      new Vector3(0.0D, 0.0D, 0.0D),
				                                      1.0F, 1.0F, 1.0F,
				                                      1.0F, 1.0F, 1.0F,
				                                      32);
			}
		}
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getEnergyStatus() {
		final String units = energy_getDisplayUnits();
		return new Object[] {
				EnergyWrapper.convert(containedEnergy, units),
				EnergyWrapper.convert(energyStored_max, units),
				energy_getDisplayUnits(),
				0,
				EnergyWrapper.convert(energyReleasedLastCycle, units) / WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS };
	}
	
	@Override
	public Object[] isAssemblyValid() {
		return null; // @TODO isAssemblyValid()
	}
	
	@Override
	public Double[] getInstabilities() {
		// computer is alive => start updating reactor
		hold = false;
		
		final ReactorFace[] lasers = ReactorFace.getLasers(enumTier);
		final Double[] result = new Double[lasers.length];
		for (final ReactorFace reactorFace : lasers) {
			final double value = instabilityValues[reactorFace.indexStability];
			result[reactorFace.indexStability] = value;
		}
		return result;
	}
	
	@Override
	public Double[] instabilityTarget(final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			final double instabilityTargetRequested;
			try {
				instabilityTargetRequested = Commons.toDouble(arguments[0]);
			} catch (final Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(String.format("%s LUA error on instabilityTarget(): Double expected for 1st argument %s",
					                                     this, arguments[0]));
				}
				return new Double[] { instabilityTarget };
			}
			
			instabilityTarget = Commons.clamp(0.0D, 100.0D, instabilityTargetRequested);
		}
		return new Double[] { instabilityTarget };
	}
	
	@Override
	public Object[] outputMode(final Object[] arguments) {
		if ( arguments.length == 2
		  && arguments[0] != null ) {
			final EnumReactorOutputMode enumReactorOutputModeRequested;
			try {
				enumReactorOutputModeRequested = EnumReactorOutputMode.byName(arguments[0].toString());
				if (enumReactorOutputModeRequested == null) {
					throw new NullPointerException();
				}
			} catch (final Exception exception) {
				final String message = String.format("%s LUA error on outputMode(): enum(%s) expected for 1st argument %s",
				                                     this, Arrays.toString(EnumReactorOutputMode.values()), arguments[0]);
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(message);
				}
				return new Object[] { enumReactorOutputMode.getName(), outputThreshold };
			}
			
			final int outputThresholdRequested;
			try {
				outputThresholdRequested = Commons.toInt(arguments[1]);
			} catch (final Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(String.format("%s LUA error on outputMode(): integer expected for 2nd argument %s",
					                                     this, arguments[0]));
				}
				return new Object[] { enumReactorOutputMode.getName(), outputThreshold };
			}
			
			enumReactorOutputMode = enumReactorOutputModeRequested;
			outputThreshold = outputThresholdRequested;
		}
		return new Object[] { enumReactorOutputMode.getName(), outputThreshold };
	}
	
	@Override
	public Object[] stabilizerEnergy(final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			final int stabilizerEnergyRequested;
			try {
				stabilizerEnergyRequested = Commons.toInt(arguments[0]);
			} catch (final Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(String.format("%s LUA error on stabilizerEnergy(): Integer expected for 1st argument %s",
					                                     this, arguments[0]));
				}
				return new Object[] { stabilizerEnergy };
			}
			
			stabilizerEnergy = Commons.clamp(0, Integer.MAX_VALUE, stabilizerEnergyRequested);
		}
		return new Object[] { stabilizerEnergy };
	}
	
	@Override
	public Object[] state() {
		final String status = getStatusHeaderInPureText();
		return new Object[] { status, isEnabled, containedEnergy, enumReactorOutputMode.getName(), outputThreshold };
	}
	
	// POWER INTERFACES
	@Override
	public int energy_getPotentialOutput() {
		if (hold) {// still loading/booting => hold output
			return 0;
		}
		
		// restrict max output rate to twice the generation
		final int capacity = Math.max(0, 2 * lastGenerationRate - releasedThisTick);
		
		int result = 0;
		if (enumReactorOutputMode == EnumReactorOutputMode.UNLIMITED) {
			result = Math.min(Math.max(0, containedEnergy), capacity);
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info(String.format("PotentialOutput Manual %d RF (%d internal) capacity %d",
				                                    result, EnergyWrapper.convertRFtoInternal_floor(result), capacity));
			}
		} else if (enumReactorOutputMode == EnumReactorOutputMode.ABOVE) {
			result = Math.min(Math.max(0, lastGenerationRate - outputThreshold), capacity);
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info(String.format("PotentialOutput Above %d RF (%d internal) capacity %d",
				                                    result, EnergyWrapper.convertRFtoInternal_floor(result), capacity));
			}
		} else if (enumReactorOutputMode == EnumReactorOutputMode.AT_RATE) {
			final int remainingRate = Math.max(0, outputThreshold - releasedThisTick);
			result = Math.min(Math.max(0, containedEnergy), Math.min(remainingRate, capacity));
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info(String.format("PotentialOutput Rated %d RF (%d internal) remainingRate %d RF/t capacity %d",
				                                    result, EnergyWrapper.convertRFtoInternal_floor(result), remainingRate, capacity));
			}
		}
		return (int) EnergyWrapper.convertRFtoInternal_floor(result);
	}
	
	@Override
	public boolean energy_canOutput(final EnumFacing from) {
		return from == null
		    || from.equals(EnumFacing.UP)
		    || from.equals(EnumFacing.DOWN);
	}
	
	@Override
	protected void energy_outputDone(final long energyOutput_internal) {
		final long energyOutput_RF = EnergyWrapper.convertInternalToRF_ceil(energyOutput_internal);
		containedEnergy -= energyOutput_RF;
		if (containedEnergy < 0) {
			containedEnergy = 0;
		}
		releasedThisTick += energyOutput_RF;
		releasedThisCycle += energyOutput_RF;
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("OutputDone %d (%d RF)",
			                                    energyOutput_internal, energyOutput_RF));
		}
	}
	
	@Override
	public long energy_getEnergyStored() {
		return Commons.clamp(0L, energy_getMaxStorage(), EnergyWrapper.convertRFtoInternal_floor(containedEnergy));
	}
	
	// Forge overrides
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setString("outputMode", enumReactorOutputMode.getName());
		tagCompound.setInteger("outputThreshold", outputThreshold);
		tagCompound.setDouble("instabilityTarget", instabilityTarget);
		tagCompound.setInteger("stabilizerEnergy", stabilizerEnergy);
		
		tagCompound.setInteger("energy", containedEnergy);
		final NBTTagCompound tagCompoundInstability = new NBTTagCompound();
		for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
			tagCompoundInstability.setDouble(reactorFace.name, instabilityValues[reactorFace.indexStability]);
		}
		tagCompound.setTag("instability", tagCompoundInstability);
		return tagCompound;
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		// skip empty NBT on placement to use defaults values
		if (!tagCompound.hasKey("outputMode")) {
			return;
		}
		
		enumReactorOutputMode = EnumReactorOutputMode.byName(tagCompound.getString("outputMode"));
		if (enumReactorOutputMode == null) {
			enumReactorOutputMode = EnumReactorOutputMode.OFF;
		}
		outputThreshold = tagCompound.getInteger("outputThreshold");
		instabilityTarget = tagCompound.getDouble("instabilityTarget");
		stabilizerEnergy = tagCompound.getInteger("stabilizerEnergy");
		
		containedEnergy = tagCompound.getInteger("energy");
		final NBTTagCompound tagCompoundInstability = tagCompound.getCompoundTag("instability");
		// tier isn't defined yet, so we check all candidates
		for (final ReactorFace reactorFace : ReactorFace.getLasers()) {
			if (reactorFace.indexStability < 0) {
				continue;
			}
			if (tagCompoundInstability.hasKey(reactorFace.name)) {
				instabilityValues[reactorFace.indexStability] = tagCompoundInstability.getDouble(reactorFace.name);
			}
		}
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		tagCompound.removeTag("outputMode");
		tagCompound.removeTag("outputThreshold");
		tagCompound.removeTag("instabilityTarget");
		tagCompound.removeTag("stabilizerEnergy");
		
		tagCompound.removeTag("energy");
		tagCompound.removeTag("instability");
		return tagCompound;
	}
}