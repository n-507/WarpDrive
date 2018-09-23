package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.structures.Orb.OrbShell;
import cr0s.warpdrive.world.EntitySphereGen;
import cr0s.warpdrive.world.EntityStarCore;
import cr0s.warpdrive.world.WorldGenSmallShip;

import javax.annotation.Nonnull;
import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class OrbInstance extends AbstractStructureInstance {
	
	protected OrbShell[] orbShells;
	protected int[] orbShellThicknesses;
	protected int totalThickness;
	protected int minThickness;
	protected String schematicName;
	
	// internal look-up table to accelerate computations
	private OrbShell[] sqRadiusToOrbShell;
	
	public OrbInstance(final Orb orb, final Random random) {
		super(orb, random);
		orbShells = new OrbShell[orb.orbShells.length];
		orbShellThicknesses = new int[orb.orbShells.length];
		totalThickness = 0;
		minThickness = 0;
		int orbShellIndexOut = 0;
		for (int orbShellIndexIn = 0; orbShellIndexIn < orb.orbShells.length; orbShellIndexIn++) {
			final OrbShell orbShell = orb.orbShells[orbShellIndexIn].instantiate(random);
			if (orbShell != null) {
				orbShells[orbShellIndexOut] = orbShell;
				final int thickness = Commons.randomRange(random, orbShell.minThickness, orbShell.maxThickness);
				orbShellThicknesses[orbShellIndexOut] = thickness;
				totalThickness += thickness;
				minThickness += orbShell.minThickness;
				orbShellIndexOut++;
			}
		}
		
		sqRadiusToOrbShell = new OrbShell[totalThickness * totalThickness];
		for (int sqRadius = 0; sqRadius < sqRadiusToOrbShell.length; sqRadius++) {
			int cumulatedRange = 0;
			for (int shellIndex = 0; shellIndex < orbShells.length; shellIndex++) {
				cumulatedRange += orbShellThicknesses[shellIndex];
				if (sqRadius <= cumulatedRange * cumulatedRange) {
					sqRadiusToOrbShell[sqRadius] = orbShells[shellIndex];
					break;
				}
			}
		}
		
		schematicName = orb.schematicName;
	}
	
	public OrbInstance(final NBTTagCompound tagCompound) {
		super(tagCompound);
		// TODO not implemented
	}
	
	@Override
	public void WriteToNBT(final NBTTagCompound tagCompound) {
		super.WriteToNBT(tagCompound);
		// TODO not implemented
	}
	
	public int getTotalThickness() {
		return totalThickness;
	}
	
	@Override
	public boolean generate(@Nonnull final World world, @Nonnull final Random random, @Nonnull final BlockPos blockPos) {
		final boolean hasShip = schematicName != null && !schematicName.isEmpty();
		final int y2 = Math.min(WarpDriveConfig.SPACE_GENERATOR_Y_MAX_BORDER - totalThickness,
			  Math.max(blockPos.getY(), WarpDriveConfig.SPACE_GENERATOR_Y_MIN_BORDER + totalThickness));
		if (hasShip) {
			new WorldGenSmallShip(random.nextFloat() < 0.2F, false).generate(world, random, new BlockPos(blockPos.getX(), y2, blockPos.getZ()));
		}
		final EntitySphereGen entitySphereGen = new EntitySphereGen(world, blockPos.getX(), y2, blockPos.getZ(), this, !hasShip);
		world.spawnEntity(entitySphereGen);
		if (((Orb) structure).hasStarCore) {
			return world.spawnEntity(new EntityStarCore(world, blockPos.getX(), y2, blockPos.getZ(), totalThickness));
		}
		return true;
	}
	
	public OrbShell getShellForSqRadius(final double sqRadius) {
		final int intSqRadius = (int) Math.round(sqRadius);
		if (intSqRadius < sqRadiusToOrbShell.length) {
			return sqRadiusToOrbShell[intSqRadius];
		} else {
			return sqRadiusToOrbShell[sqRadiusToOrbShell.length - 1];
		}
	}
}
