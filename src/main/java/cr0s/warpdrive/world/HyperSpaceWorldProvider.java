package cr0s.warpdrive.world;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.client.ClientProxy;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.StarMapRegistry;
import cr0s.warpdrive.render.RenderBlank;
import cr0s.warpdrive.render.RenderSpaceSky;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

public class HyperSpaceWorldProvider extends WorldProvider {
	
	private CelestialObject celestialObjectDimension = null;
	
	public HyperSpaceWorldProvider() {
		biomeProvider  = new BiomeProviderSingle(WarpDrive.spaceBiome);
		hasNoSky = true;
	}
	
	@Override
	public void setDimension(final int dimensionId) {
		super.setDimension(dimensionId);
		celestialObjectDimension = CelestialObjectManager.get(WarpDrive.proxy instanceof ClientProxy, dimensionId, 0, 0);
	}
	
	@Nonnull
	@Override
	public DimensionType getDimensionType() {
		return WarpDrive.dimensionTypeHyperSpace;
	}
	
	// @Nonnull
	@Override
	public String getSaveFolder() {
		return celestialObjectDimension == null ? "WarpDriveHyperSpace" + getDimension() : celestialObjectDimension.id;
	}
	
	@Override
	public boolean canRespawnHere() {
		return true;
	}
	
	@Override
	public boolean isSurfaceWorld() {
		return true;
	}
	
	@Override
	public int getAverageGroundLevel() {
		return 1;
	}
	
	@Override
	public double getHorizon() {
		return -256;
	}
	
	@Override
	public void updateWeather() {
		super.resetRainAndThunder();
	}
	
	@Nonnull
	@Override
	public Biome getBiomeForCoords(@Nonnull BlockPos blockPos) {
		return WarpDrive.spaceBiome;
	}
	
	@Override
	public void setAllowedSpawnTypes(boolean allowHostile, boolean allowPeaceful) {
		super.setAllowedSpawnTypes(true, true);
	}
	
	@Override
	public float calculateCelestialAngle(long time, float partialTick) {
		return 0.5F;
	}
	
	@Override
	protected void generateLightBrightnessTable() {
		float f = 0.0F;
		
		for (int i = 0; i <= 15; ++i) {
			float f1 = 1.0F - i / 15.0F;
			lightBrightnessTable[i] = (1.0F - f1) / (f1 * 3.0F + 1.0F) * (1.0F - f) + f;
		}
	}
	
	// shared for getFogColor(), getStarBrightness()
	// @SideOnly(Side.CLIENT)
	private static CelestialObject celestialObject = null;
	
	@Override
	public boolean canCoordinateBeSpawn(int x, int z) {
		final BlockPos blockPos = worldObj.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z));
		return blockPos.getY() != 0;
	}
	
	@Nonnull
	@Override
	public Vec3d getSkyColor(@Nonnull Entity cameraEntity, float partialTicks) {
		if (getCloudRenderer() == null) {
			setCloudRenderer(RenderBlank.getInstance());
		}
		if (getSkyRenderer() == null) {
			setSkyRenderer(RenderSpaceSky.getInstance());
		}
		
		celestialObject = cameraEntity.worldObj == null ? null : CelestialObjectManager.get(
				cameraEntity.worldObj,
				MathHelper.floor_double(cameraEntity.posX), MathHelper.floor_double(cameraEntity.posZ));
		if (celestialObject == null) {
			return new Vec3d(1.0D, 0.0D, 0.0D);
		} else {
			return new Vec3d(celestialObject.backgroundColor.red, celestialObject.backgroundColor.green, celestialObject.backgroundColor.blue);
		}
	}
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public Vec3d getFogColor(float celestialAngle, float par2) {
		final float factor = Commons.clamp(0.0F, 1.0F, MathHelper.cos(celestialAngle * (float) Math.PI * 2.0F) * 2.0F + 0.5F);
		
		float red   = celestialObject == null ? 0.0F : celestialObject.colorFog.red;
		float green = celestialObject == null ? 0.0F : celestialObject.colorFog.green;
		float blue  = celestialObject == null ? 0.0F : celestialObject.colorFog.blue;
		float factorRed   = celestialObject == null ? 0.0F : celestialObject.factorFog.red;
		float factorGreen = celestialObject == null ? 0.0F : celestialObject.factorFog.green;
		float factorBlue  = celestialObject == null ? 0.0F : celestialObject.factorFog.blue;
		red   *= factor * factorRed   + (1.0F - factorRed  );
		green *= factor * factorGreen + (1.0F - factorGreen);
		blue  *= factor * factorBlue  + (1.0F - factorBlue );
		return new Vec3d(red, green, blue);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public float getStarBrightness(float partialTicks) {
		if (celestialObject == null) {
			return 0.0F;
		}
		final float starBrightnessVanilla = super.getStarBrightness(partialTicks);
		return celestialObject.baseStarBrightness + celestialObject.vanillaStarBrightness * starBrightnessVanilla;
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public boolean isSkyColored() {
		return true;
	}
		
	@Override
	public int getRespawnDimension(EntityPlayerMP player) {
		if (player == null || player.worldObj == null) {
			WarpDrive.logger.error("Invalid player passed to getRespawnDimension: " + player);
			return 0;
		}
		return StarMapRegistry.getHyperspaceDimensionId(player.worldObj, (int) player.posX, (int) player.posZ);
	}
	
	@Nonnull
	@Override
	public IChunkGenerator createChunkGenerator() {
		return new HyperSpaceChunkProvider(worldObj, 46);
	}
	
	@Override
	public boolean canBlockFreeze(@Nonnull BlockPos blockPos, boolean byWater) {
		return false;
	}
	
	@Nonnull
	@Override
	public BlockPos getRandomizedSpawnPoint() {
		BlockPos blockPos = new BlockPos(worldObj.getSpawnPoint());
		// boolean isAdventure = worldObj.getWorldInfo().getGameType() == EnumGameType.ADVENTURE;
		int spawnFuzz = 100;
		int spawnFuzzHalf = spawnFuzz / 2;
		{
			blockPos = new BlockPos(
				blockPos.getX() + worldObj.rand.nextInt(spawnFuzz) - spawnFuzzHalf,
				200,
				blockPos.getZ() + worldObj.rand.nextInt(spawnFuzz) - spawnFuzzHalf);
		}
		
		if (worldObj.isAirBlock(blockPos)) {
			worldObj.setBlockState(blockPos, Blocks.STONE.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 1, 1,  0), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 1, 2,  0), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add(-1, 1,  0), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add(-1, 2,  0), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 0, 1,  1), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 0, 2,  1), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 0, 1, -1), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 0, 2, -1), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 0, 3,  0), Blocks.GLASS.getDefaultState(), 2);
			worldObj.setBlockState(blockPos.add( 0, 0,  0), WarpDrive.blockAir.getStateFromMeta(15), 2);
			worldObj.setBlockState(blockPos.add( 0, 1,  0), WarpDrive.blockAir.getStateFromMeta(15), 2);
		}
		
		return blockPos;
	}
	
	@Override
	public boolean isDaytime() {
		return false;
	}
	
	@Override
	public boolean canDoLightning(Chunk chunk) {
		return false;
	}
	
	@Override
	public boolean canDoRainSnowIce(Chunk chunk) {
		return false;
	}
}