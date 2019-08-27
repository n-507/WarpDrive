package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.TileEntityAbstractEnergyCoreOrController;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.CloakedArea;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;
import java.util.Arrays;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

public class TileEntityCloakingCore extends TileEntityAbstractEnergyCoreOrController {
	
	// global properties
	private static final int CLOAKING_CORE_SOUND_UPDATE_TICKS = 40;
	private static final int DISTANCE_INNER_COILS_BLOCKS = 2;
	private static final int LASER_REFRESH_TICKS = 100;
	private static final int LASER_DURATION_TICKS = 110;
	
	// inner coils color map
	private static final float[] innerCoilColor_r = { 1.00f, 1.00f, 1.00f, 1.00f, 0.75f, 0.25f, 0.00f, 0.00f, 0.00f, 0.00f, 0.50f, 1.00f };
	private static final float[] innerCoilColor_g = { 0.00f, 0.25f, 0.75f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 0.50f, 0.25f, 0.00f, 0.00f };
	private static final float[] innerCoilColor_b = { 0.25f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.50f, 1.00f, 1.00f, 1.00f, 1.00f, 0.75f };
	
	private static final UpgradeSlot upgradeSlotTransparency = new UpgradeSlot("cloaking.transparency",
	                                                                           ItemComponent.getItemStackNoCache(EnumComponentType.DIAMOND_CRYSTAL, 6),
	                                                                           1);
	
	// computed properties
	// spatial cloaking field parameters
	private final boolean[] isValidInnerCoils = { false, false, false, false, false, false };
	private final int[] distanceOuterCoils_blocks = { 0, 0, 0, 0, 0, 0 };   // 0 means invalid
	private int minX = 0;
	private int minY = 0;
	private int minZ = 0;
	private int maxX = 0;
	private int maxY = 0;
	private int maxZ = 0;
	
	private boolean isFullyTransparent = false;
	private boolean isCloaking = false;
	private int volume = 0;
	private int energyRequired = 0;
	private int updateTicks = 0;
	private int laserDrawingTicks = 0;
	
	private boolean soundPlayed = false;
	private int soundTicks = 0;
	
	public TileEntityCloakingCore() {
		super();
		
		peripheralName = "warpdriveCloakingCore";
		// addMethods(new String[] {});
		CC_scripts = Arrays.asList("enable", "disable");
		
		registerUpgradeSlot(upgradeSlotTransparency);
	}
	
	@Override
	protected void onConstructed() {
		super.onConstructed();
		
		energy_setParameters(WarpDriveConfig.CLOAKING_MAX_ENERGY_STORED,
		                     16384, 0,
		                     "EV", 2, "HV", 0);
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		// Reset sound timer
		soundTicks--;
		if (soundTicks < 0) {
			soundTicks = CLOAKING_CORE_SOUND_UPDATE_TICKS;
			soundPlayed = false;
		}
		
		boolean isRefreshNeeded = false;
		
		updateTicks--;
		if (updateTicks <= 0) {
			isFullyTransparent = hasUpgrade(upgradeSlotTransparency);
			updateTicks = ((!isFullyTransparent) ? 20 : 10) * WarpDriveConfig.CLOAKING_FIELD_REFRESH_INTERVAL_SECONDS; // resetting timer
			
			isCloaking = WarpDrive.cloaks.isAreaExists(world, pos);
			final boolean hasEnoughPower = energy_consume(energyRequired, (!isAssemblyValid && isEnabled));
			final boolean shouldBeCloaking = isAssemblyValid && isEnabled && hasEnoughPower;
			
			if (!isCloaking) {
				if (shouldBeCloaking) {// start cloaking
					updateCoils(true, true);
					isRefreshNeeded = true;
					
					// register cloak
					final CloakedArea area = WarpDrive.cloaks.updateCloakedArea(world, pos, isFullyTransparent,
					                                                            minX, minY, minZ, maxX, maxY, maxZ);
					if (!soundPlayed) {
						soundPlayed = true;
						world.playSound(null, pos, SoundEvents.CLOAK, SoundCategory.BLOCKS, 4F, 1F);
					}
					
					// start cloaking
					area.sendCloakPacketToPlayersEx(false);
				}
				
			} else {// is cloaking
				if (shouldBeCloaking) {// maintain cloaking
					// Refresh the field (workaround to re-synchronize players since client may 'eat up' the packets)
					final CloakedArea area = WarpDrive.cloaks.getCloakedArea(world, pos);
					if (area == null) {
						WarpDrive.logger.error(String.format("Cloaked area lost %s",
						                                     Commons.format(world, pos) ));
					} else {
						area.sendCloakPacketToPlayersEx(false); // re-cloak field
					}
					
				} else {// stop cloaking
					if (WarpDriveConfig.LOGGING_CLOAKING) {
						WarpDrive.logger.info(String.format("%s Disabled, cloak field going down...",
						                                    this ));
					}
					
					if (isEnabled) {// collapsing
						if (!isAssemblyValid) {
							if (WarpDriveConfig.LOGGING_CLOAKING) {
								WarpDrive.logger.info(String.format("%s Coil(s) lost, cloak field is collapsing...",
								                                    this ));
							}
							energy_consume(energy_getEnergyStored());
							
						} else if (!hasEnoughPower) {
							if (WarpDriveConfig.LOGGING_CLOAKING) {
								WarpDrive.logger.info(String.format("%s Low power, cloak field is collapsing...",
								                                    this ));
							}
						}
					}
					
					updateCoils(true, false);
					disableCloakingField();
				}
			} 
		}
		
		laserDrawingTicks--;
		if (isRefreshNeeded || laserDrawingTicks <= 0) {
			laserDrawingTicks = LASER_REFRESH_TICKS;
			
			if (isEnabled && isAssemblyValid) {
				drawLasers();
			}
		}
	}
	
	@Override
	public void onBlockBroken(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		setIsEnabled(false);
		updateCoils(false, false);
		disableCloakingField();
		
		super.onBlockBroken(world, blockPos, blockState);
	}
	
	@Override
	protected boolean doScanAssembly(final boolean isDirty, final WarpDriveText textReason) {
		boolean isValid = super.doScanAssembly(isDirty, textReason);
		
		final int maxOuterCoilDistance = WarpDriveConfig.CLOAKING_MAX_FIELD_RADIUS - WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		boolean isRefreshNeeded = false;
		int countIntegrity = 1; // 1 for the core + 1 per coil
		final StringBuilder messageInnerCoils = new StringBuilder();
		final StringBuilder messageOuterCoils = new StringBuilder();
		
		// Directions to check (all six directions: left, right, up, down, front, back)
		for (final EnumFacing direction : EnumFacing.values()) {
			
			// check validity of inner coil
			BlockPos blockPos = new BlockPos(pos.offset(direction, DISTANCE_INNER_COILS_BLOCKS));
			final boolean isInnerValid = world.getBlockState(blockPos).getBlock() instanceof BlockCloakingCoil;
			if (isInnerValid) {
				BlockCloakingCoil.setBlockState(world, blockPos, true, isCloaking, false, direction);
			}
			
			// whenever a change is detected, force a laser redraw 
			if (isInnerValid != isValidInnerCoils[direction.ordinal()]) {
				isRefreshNeeded = true;
				isValidInnerCoils[direction.ordinal()] = isInnerValid;
			}
			
			// update validity results
			if (isValidInnerCoils[direction.ordinal()]) {
				countIntegrity++;
			} else {
				if (messageInnerCoils.length() != 0) {
					messageInnerCoils.append(", ");
				}
				messageInnerCoils.append(direction.name());
			}
			
			// find closest outer coil
			int newCoilDistance = 0;
			for (int distance = DISTANCE_INNER_COILS_BLOCKS + 1; distance < maxOuterCoilDistance; distance++) {
				blockPos = blockPos.offset(direction);
				
				if (world.getBlockState(blockPos).getBlock() instanceof BlockCloakingCoil) {
					BlockCloakingCoil.setBlockState(world, blockPos, true, isCloaking, true, direction);
					newCoilDistance = distance;
					break;
				}
			}
			
			// whenever a change is detected, disable previous outer coil if it was valid and force a laser redraw
			final int oldCoilDistance = distanceOuterCoils_blocks[direction.ordinal()];
			if (newCoilDistance != oldCoilDistance) {
				if (oldCoilDistance > 0) {
					final BlockPos blockPosOld = pos.offset(direction, oldCoilDistance);
					if (world.getBlockState(blockPosOld).getBlock() instanceof BlockCloakingCoil) {
						BlockCloakingCoil.setBlockState(world, blockPos, false, false, false, EnumFacing.DOWN);
					}
				}
				isRefreshNeeded = true;
				distanceOuterCoils_blocks[direction.ordinal()] = Math.max(0, newCoilDistance);
			}
			
			// update validity results
			if (newCoilDistance > 0) {
				countIntegrity++;
			} else {
				if (messageOuterCoils.length() != 0) {
					messageOuterCoils.append(", ");
				}
				messageOuterCoils.append(direction.name());
			}
		}
		
		// build status message
		final float integrity = countIntegrity / 13.0F;
		if (messageInnerCoils.length() > 0 && messageOuterCoils.length() > 0) {
			textReason.append(Commons.styleWarning, "warpdrive.cloaking_core.missing_channeling_and_projecting_coils",
			                  Math.round(100.0F * integrity), messageInnerCoils, messageOuterCoils);
		} else if (messageInnerCoils.length() > 0) {
			textReason.append(Commons.styleWarning, "warpdrive.cloaking_core.missing_channeling_coils",
			                  Math.round(100.0F * integrity), messageInnerCoils);
		} else if (messageOuterCoils.length() > 0) {
			textReason.append(Commons.styleWarning, "warpdrive.cloaking_core.missing_projecting_coils",
			                  Math.round(100.0F * integrity), messageOuterCoils);
		} else {
			textReason.append(Commons.styleCorrect, "warpdrive.cloaking_core.valid");
		}
		
		// Update cloaking field parameters defined by coils
		isValid = isValid && countIntegrity >= 13;
		minX =               pos.getX() - distanceOuterCoils_blocks[4] - WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		maxX =               pos.getX() + distanceOuterCoils_blocks[5] + WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		minY = Math.max(  0, pos.getY() - distanceOuterCoils_blocks[0] - WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS);
		maxY = Math.min(255, pos.getY() + distanceOuterCoils_blocks[1] + WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS);
		minZ =               pos.getZ() - distanceOuterCoils_blocks[2] - WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		maxZ =               pos.getZ() + distanceOuterCoils_blocks[3] + WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		
		// refresh cloaking area only when needed
		if (isCloaking && isValid && isRefreshNeeded) {
			WarpDrive.cloaks.updateCloakedArea(world, pos, isFullyTransparent,
			                                   minX, minY, minZ, maxX, maxY, maxZ);
		}
		
		// scan volume @TODO reuse ShipCore scanner for cloaking volume computation
		if (isEnabled && isValid) {
			updateVolumeAndEnergyRequired();
		}
		
		return isValid;
	}
	
	private void updateVolumeAndEnergyRequired() {
		int x, y, z;
		final int energyRequired_new;
		int volume_new = 0;
		final MutableBlockPos mutableBlockPos = new MutableBlockPos(pos);
		if (!isFullyTransparent) {// partial transparency = gas and air blocks don't count
			for (y = minY; y <= maxY; y++) {
				for (x = minX; x <= maxX; x++) {
					for (z = minZ; z <= maxZ; z++) {
						mutableBlockPos.setPos(x, y, z);
						if (!world.isAirBlock(mutableBlockPos)) {
							volume_new++;
						}
					}
				}
			}
			energyRequired_new = volume_new * WarpDriveConfig.CLOAKING_TIER1_ENERGY_PER_BLOCK;
		} else {// full transparency = everything counts
			for (y = minY; y <= maxY; y++) {
				for (x = minX; x <= maxX; x++) {
					for (z = minZ; z <= maxZ; z++) {
						mutableBlockPos.setPos(x, y, z);
						if (world.getBlockState(mutableBlockPos).getBlock() != Blocks.AIR) {
							volume_new++;
						}
					}
				}
			}
			energyRequired_new = volume_new * WarpDriveConfig.CLOAKING_TIER2_ENERGY_PER_BLOCK;
		}
		
		volume = volume_new;
		energyRequired = energyRequired_new;
		
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s Requiring %d EU for %d blocks",
			                                    this, energyRequired, volume));
		}
	}
	
	@Override
	protected void doUpdateParameters(final boolean isDirty) {
		
	}
	
	private void updateCoils(final boolean isConnected, final boolean isActive) {
		updateBlockState(null, BlockProperties.ACTIVE, isActive);
		
		for (final EnumFacing direction : EnumFacing.VALUES) {
			if (isValidInnerCoils[direction.ordinal()]) {
				updateCoil(isConnected, isActive, direction, DISTANCE_INNER_COILS_BLOCKS);
			}
			if (distanceOuterCoils_blocks[direction.ordinal()] > 0) {
				updateCoil(isConnected, isActive, direction, distanceOuterCoils_blocks[direction.ordinal()]);
			}
		}
	}
	
	private void updateCoil(final boolean isConnected, final boolean isActive, final EnumFacing direction, final int distance) {
		final BlockPos blockPos = pos.offset(direction, distance);
		final IBlockState blockState = world.getBlockState(blockPos);
		if (blockState.getBlock() instanceof BlockCloakingCoil) {
			if (isConnected) {
				if (distance == DISTANCE_INNER_COILS_BLOCKS) {
					BlockCloakingCoil.setBlockState(world, blockPos, true, isActive, false, EnumFacing.DOWN);
				} else {
					BlockCloakingCoil.setBlockState(world, blockPos, true, isActive, true, direction);
				}
			} else {
				BlockCloakingCoil.setBlockState(world, blockPos, false, false, true, EnumFacing.DOWN);
			}
		}
	}
	
	private void drawLasers() {
		float r;
		float g;
		float b;
		if (!isCloaking) {// out of energy
			r = 0.75f;
			g = 0.50f;
			b = 0.50f;
		} else if (!isFullyTransparent) {
			r = 0.00f;
			g = 1.00f;
			b = 0.25f;
		} else {
			r = 0.00f;
			g = 0.25f;
			b = 1.00f;
		}
		
		// Directions to check (all six directions: left, right, up, down, front, back)
		for (final EnumFacing direction : EnumFacing.values()) {
			if ( isValidInnerCoils[direction.ordinal()]
			  && distanceOuterCoils_blocks[direction.ordinal()] > 0) {
				PacketHandler.sendBeamPacketToPlayersInArea(world,
				        new Vector3(
				                   pos.getX() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * direction.getXOffset(),
				                   pos.getY() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * direction.getYOffset(),
				                   pos.getZ() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * direction.getZOffset()),
				        new Vector3(
				                   pos.getX() + 0.5D + distanceOuterCoils_blocks[direction.ordinal()] * direction.getXOffset(),
				                   pos.getY() + 0.5D + distanceOuterCoils_blocks[direction.ordinal()] * direction.getYOffset(),
				                   pos.getZ() + 0.5D + distanceOuterCoils_blocks[direction.ordinal()] * direction.getZOffset()),
				        r, g, b,
				        LASER_DURATION_TICKS,
				    		new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
			}
		}
		
		// draw connecting coils
		for (int i = 0; i < 5; i++) {
			final EnumFacing start = EnumFacing.VALUES[i];
			for (int j = i + 1; j < 6; j++) {
				final EnumFacing stop = EnumFacing.VALUES[j];
				// skip mirrored coils (removing the inner lines)
				if (start.getOpposite() == stop) {
					continue;
				}
				
				// draw a random colored beam
				final int mapIndex = world.rand.nextInt(innerCoilColor_b.length);
				r = innerCoilColor_r[mapIndex];
				g = innerCoilColor_g[mapIndex];
				b = innerCoilColor_b[mapIndex];
				
				PacketHandler.sendBeamPacketToPlayersInArea(world,
					new Vector3(
							pos.getX() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * start.getXOffset() + 0.2D * stop .getXOffset(),
							pos.getY() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * start.getYOffset() + 0.2D * stop .getYOffset(),
							pos.getZ() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * start.getZOffset() + 0.2D * stop .getZOffset()),
					new Vector3(
							pos.getX() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * stop .getXOffset() + 0.2D * start.getXOffset(),
							pos.getY() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * stop .getYOffset() + 0.2D * start.getYOffset(),
							pos.getZ() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * stop .getZOffset() + 0.2D * start.getZOffset()),
					r, g, b,
					LASER_DURATION_TICKS,
					new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
			}
		}
	}
	
	public void disableCloakingField() {
		if (WarpDrive.cloaks.isAreaExists(world, pos)) {
			WarpDrive.cloaks.removeCloakedArea(world.provider.getDimension(), pos);
			
			if (!soundPlayed) {
				soundPlayed = true;
				world.playSound(null, pos, SoundEvents.DECLOAK, SoundCategory.BLOCKS, 4F, 1F);
			}
		}
	}
	
	@Override
	public WarpDriveText getStatusHeader() {
		if (world == null) {
			return super.getStatusHeader();
		}
		
		final WarpDriveText textStatus;
		if (!isAssemblyValid) {
			textStatus = textValidityIssues;
		} else if (!isEnabled) {
			textStatus = new WarpDriveText(Commons.styleNormal, "warpdrive.cloaking_core.disabled",
			                               isFullyTransparent ? 2 : 1,
			                               volume);
		} else if (!isCloaking) {
			textStatus = new WarpDriveText(Commons.styleWarning, "warpdrive.cloaking_core.low_power",
			                               isFullyTransparent ? 2 : 1,
			                               volume);
		} else {
			textStatus = new WarpDriveText(Commons.styleCorrect, "warpdrive.cloaking_core.cloaking",
			                               isFullyTransparent ? 2 : 1,
			                               volume);
		}
		return super.getStatusHeader().append(textStatus);
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		final double updateRate = ((!isFullyTransparent) ? 20 : 10) * WarpDriveConfig.CLOAKING_FIELD_REFRESH_INTERVAL_SECONDS;
		final double energyRate = energyRequired / updateRate;
		return new Object[] {
				true,
				EnergyWrapper.convert((long) Math.ceil(energyRate), units) };
	}
	
	// OpenComputers callback methods
	// (none)
	
	// ComputerCraft IPeripheral methods
	// (none)
	
	// TileEntityAbstractEnergy methods
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		return true;
	}
}
