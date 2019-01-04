package cr0s.warpdrive.block.breathing;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityAbstractEnergyConsumer;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.StateAir;
import cr0s.warpdrive.event.ChunkHandler;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class TileEntityAirGeneratorTiered extends TileEntityAbstractEnergyConsumer {
	
	// persistent properties
	// (none)
	
	// computed properties
	private int maxEnergyStored = 0;
	private int cooldownTicks = 0;
	
	public TileEntityAirGeneratorTiered() {
		super();
		
		peripheralName = "warpdriveAirGenerator";
		// addMethods(new String[] {
		// });
		doRequireUpgradeToInterface();
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		final Block block = getBlockType();
		if (block instanceof BlockAirGeneratorTiered) {
			enumTier = ((BlockAirGeneratorTiered) block).getTier(null);
			maxEnergyStored = WarpDriveConfig.BREATHING_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()];
		} else {
			WarpDrive.logger.error(String.format("Missing block for %s %s",
			                                     this, Commons.format(world, pos)));
		}
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		if (isInvalid()) {
			return;
		}
		
		// Air generator works only in space & hyperspace
		final IBlockState blockState = world.getBlockState(pos);
		if (CelestialObjectManager.hasAtmosphere(world, pos.getX(), pos.getZ())) {
			if (blockState.getValue(BlockProperties.ACTIVE)) {
				world.setBlockState(pos, blockState.withProperty(BlockProperties.ACTIVE, false)); // set disabled texture
			}
			return;
		}
		
		cooldownTicks++;
		if (cooldownTicks > WarpDriveConfig.BREATHING_AIR_GENERATION_TICKS) {
			final boolean isActive = releaseAir(blockState.getValue(BlockProperties.FACING));
			if (isActive) {
				if (!blockState.getValue(BlockProperties.ACTIVE)) {
					world.setBlockState(pos, blockState.withProperty(BlockProperties.ACTIVE, true)); // set enabled texture
				}
			} else {
				if (blockState.getValue(BlockProperties.ACTIVE)) {
					world.setBlockState(pos, blockState.withProperty(BlockProperties.ACTIVE, false)); // set disabled texture
				}
			}
			releaseAir(blockState.getValue(BlockProperties.FACING));
			
			cooldownTicks = 0;
		}
	}
	
	private boolean releaseAir(final EnumFacing direction) {
		final BlockPos posDirection = pos.offset(direction);
		
		// reject cables or signs in front of the fan (it's inconsistent and not really supported)
		if (!world.isAirBlock(posDirection)) {
			return false;
		}
		
		// get the state object
		// assume it works when chunk isn't loaded
		final StateAir stateAir = ChunkHandler.getStateAir(world, posDirection.getX(), posDirection.getY(), posDirection.getZ());
		if (stateAir == null) {
			return true;
		}
		stateAir.updateBlockCache(world);
		
		// only accept air block (i.e. rejecting the dictionary blacklist)
		if (!stateAir.isAir()) {
			return false;
		}
		
		if (isEnabled) {
			final int energy_cost = !stateAir.isAirSource() ? WarpDriveConfig.BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[enumTier.getIndex()]
			                                                : WarpDriveConfig.BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[enumTier.getIndex()];
			if (energy_consume(energy_cost, true)) {// enough energy
				final short range = (short) (WarpDriveConfig.BREATHING_AIR_GENERATION_RANGE_BLOCKS_BY_TIER[enumTier.getIndex()] - 1);
				stateAir.setAirSource(world, direction, range);
				energy_consume(energy_cost, false);
				return true;
			}
		}
		
		// disabled or low energy => remove air block
		if (stateAir.concentration > 4) {
			stateAir.setConcentration(world, (byte) (stateAir.concentration / 2));
		} else if (stateAir.concentration > 0) {
			stateAir.removeAirSource(world);
		}
		return false;
	}
	
	@Override
	public int energy_getMaxStorage() {
		return maxEnergyStored;
	}
	
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		return true;
	}
	
	@Override
	public Object[] getEnergyRequired() {
		return new Object[] { WarpDriveConfig.BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[enumTier.getIndex()] / (double) WarpDriveConfig.BREATHING_AIR_GENERATION_TICKS,
		                      WarpDriveConfig.BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[enumTier.getIndex()] / (double) WarpDriveConfig.BREATHING_AIR_GENERATION_TICKS };
	}
}
