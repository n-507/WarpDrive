package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.Filler;
import cr0s.warpdrive.config.structures.Orb.OrbShell;
import cr0s.warpdrive.data.VectorI;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MetaOrbInstance extends OrbInstance {
	
	private static final int CORE_MAX_TRIES = 10;
	protected final MetaShell metaShell;
	
	public MetaOrbInstance(final MetaOrb asteroid, final Random random) {
		super(asteroid, random);
		metaShell = new MetaShell(asteroid, random);
		// FIXME setRadius(Math.round(totalThickness + metaShell.radius));
	}
	
	@Override
	public boolean generate(@Nonnull final World world, @Nonnull final Random random, @Nonnull final BlockPos blockPos) {
		if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
			WarpDrive.logger.info(String.format("Generating MetaOrb %s of %d cores with radius of %d",
			                                    structure.name, metaShell.count, totalThickness));
		}
		LocalProfiler.start(String.format("[AsteroidInstance] Generating MetaOrb %s of %d cores with radius of %d",
		                                  structure.name, metaShell.count, totalThickness));
		
		final int y2 = Math.min(WarpDriveConfig.SPACE_GENERATOR_Y_MAX_BORDER - totalThickness - (int) metaShell.radius,
		                        Math.max(blockPos.getY(), WarpDriveConfig.SPACE_GENERATOR_Y_MIN_BORDER + totalThickness + (int) metaShell.radius));
		if (((MetaOrb) structure).metaShell == null) {
			return super.generate(world, random, new BlockPos(blockPos.getX(), y2, blockPos.getZ()));
		}
		
		// generate an abstract form for the core
		for (final VectorI location: metaShell.locations) {
			// place core block
			if (metaShell.block != null) {
				world.setBlockState(new BlockPos(blockPos.getX() + location.x, y2 + location.y, blockPos.getZ() + location.z), metaShell.block.getStateFromMeta(metaShell.metadata), 2);
			}
			
			// calculate distance to borders of generation area
			final int maxRadX = totalThickness - Math.abs(location.x);
			final int maxRadY = totalThickness - Math.abs(location.y);
			final int maxRadZ = totalThickness - Math.abs(location.z);
			// keep the biggest one to have a bumpy effect
			int maxLocalRadius = Math.max(maxRadX, Math.max(maxRadY, maxRadZ));
			// enforce a minimum thickness to prevent lone core blocks
			// (see case where core radius is close to totalThickness)
			maxLocalRadius = Math.max(minThickness, maxLocalRadius);
			
			// Generate shell
			addShell(world, new VectorI(blockPos.getX(), y2, blockPos.getZ()).add(location), maxLocalRadius);
		}
		
		final int minY_clamped = Math.max(0, y2 - totalThickness);
		final int maxY_clamped = Math.min(255, y2 + totalThickness);
		for (int xIndex = blockPos.getX() - totalThickness; xIndex <= blockPos.getX() + totalThickness; xIndex++) {
			for (int zIndex = blockPos.getZ() - totalThickness; zIndex <= blockPos.getZ() + totalThickness; zIndex++) {
				for (int yIndex = minY_clamped; yIndex <= maxY_clamped; yIndex++) {
					final BlockPos blockPosNotify = new BlockPos(xIndex, yIndex, zIndex);
					final IBlockState blockState = world.getBlockState(blockPosNotify);
					if (blockState.getBlock() != Blocks.AIR) {
						world.notifyBlockUpdate(blockPosNotify, blockState, blockState, 3);
					}
				}
			}
		}
		
		LocalProfiler.stop();
		return false;
	}
	
	private void addShell(final World world, final VectorI location, final int radius) {
		final double sqRadius = radius * radius;
		final BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(location.x, location.y, location.z);
		// iterate all blocks within cube with side 2 * radius
		for (int x = location.x - radius; x <= location.x + radius; x++) {
			final int dX2 = (x - location.x) * (x - location.x);
			for (int y = location.y - radius; y <= location.y + radius; y++) {
				final int dX2Y2 = dX2 + (y - location.y) * (y - location.y);
				for (int z = location.z - radius; z <= location.z + radius; z++) {
					// current radius
					final int sqRange = dX2Y2 + (location.z - z) * (location.z - z);
					
					mutableBlockPos.setPos(x, y, z);
					// if inside radius
					if (sqRange <= sqRadius && isReplaceableOreGen(world, mutableBlockPos)) {
						final OrbShell shell = getShellForSqRadius(sqRange);
						final Filler filler = shell.getRandomUnit(world.rand);
						filler.setBlock(world, mutableBlockPos);
					}
				}
			}
		}
	}
	
	private static boolean isReplaceableOreGen(final World world, final BlockPos blockPos) {
		final IBlockState blockState = world.getBlockState(blockPos);
		return blockState.getBlock().isReplaceableOreGen(blockState, world, blockPos, BlockMatcher.forBlock(Blocks.AIR));
	}
	
	public class MetaShell {
		protected final int count;
		protected final double radius;
		protected ArrayList<VectorI> locations;
		protected final Block block;
		protected final int metadata;
		
		public MetaShell(final MetaOrb asteroid, final Random random) {
			if (asteroid.metaShell == null) {
				count = 1;
				radius = 0;
				block = null;
				metadata = 0;
				return;
			}
			count = Commons.randomRange(random, asteroid.metaShell.minCount, asteroid.metaShell.maxCount);
			radius = Math.max(asteroid.metaShell.minRadius, asteroid.metaShell.relativeRadius * totalThickness);
			block = asteroid.metaShell.block;
			metadata = asteroid.metaShell.metadata;
			
			// evaluate core positions
			locations = new ArrayList<>();
			final double diameter = Math.max(1D, 2 * radius);
			final double xMin = -radius;
			final double yMin = -radius;
			final double zMin = -radius;
			
			for (int index = 0; index < count; index++) {
				boolean found = false;
				
				for (int step = 0; step < CORE_MAX_TRIES && !found; step++) {
					final VectorI location = new VectorI(
							(int)Math.round(xMin + diameter * random.nextDouble()),
							(int)Math.round(yMin + diameter * random.nextDouble()),
							(int)Math.round(zMin + diameter * random.nextDouble()));
					if (!locations.contains(location)) {
						locations.add(location);
						found = true;
					}
				}
			}
		}
	}
}
