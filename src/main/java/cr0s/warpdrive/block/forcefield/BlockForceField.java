package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IDamageReceiver;
import cr0s.warpdrive.block.ItemBlockAbstractBase;
import cr0s.warpdrive.block.hull.BlockHullGlass;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumPermissionNode;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.ForceFieldSetup;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.event.ModelBakeEventHandler;
import cr0s.warpdrive.render.BakedModelCamouflage;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Random;

import net.minecraft.block.BlockGlass;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockForceField extends BlockAbstractForceField implements IDamageReceiver {
	
	private static final float BOUNDING_TOLERANCE = 0.05F;
	private static final AxisAlignedBB AABB_FORCEFIELD = new AxisAlignedBB(
			BOUNDING_TOLERANCE, BOUNDING_TOLERANCE, BOUNDING_TOLERANCE,
		    1 - BOUNDING_TOLERANCE, 1 - BOUNDING_TOLERANCE, 1 - BOUNDING_TOLERANCE);
	
	public static final PropertyInteger FREQUENCY = PropertyInteger.create("frequency", 0, 15);
	
	public BlockForceField(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.GLASS);
		
		setSoundType(SoundType.CLOTH);
		setTranslationKey("warpdrive.force_field.block." + enumTier.getName());
		setBlockUnbreakable();
		
		setDefaultState(getDefaultState()
				                .withProperty(FREQUENCY, 0)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new ExtendedBlockState(this,
		                              new IProperty[] { FREQUENCY },
		                              new IUnlistedProperty[] { BlockProperties.CAMOUFLAGE });
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public MapColor getMapColor(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
		final IExtendedBlockState blockStateExtended = (IExtendedBlockState) getExtendedState(blockState, blockAccess, blockPos);
		final IBlockState blockStateCamouflage = blockStateExtended.getValue(BlockProperties.CAMOUFLAGE);
		if (blockStateCamouflage != Blocks.AIR) {
			try {
				return blockStateCamouflage.getMapColor(blockAccess, blockPos);
			} catch (final Exception exception) {
				if (!Dictionary.BLOCKS_NOCAMOUFLAGE.contains(blockStateCamouflage.getBlock())) {
					exception.printStackTrace();
					WarpDrive.logger.error(String.format("Exception trying to get MapColor for %s",
					                                     blockStateCamouflage));
					Dictionary.BLOCKS_NOCAMOUFLAGE.add(blockStateCamouflage.getBlock());
				}
			}
		}
		return MapColor.getBlockColor(EnumDyeColor.byMetadata(blockState.getValue(FREQUENCY)));
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return this.getDefaultState().withProperty(FREQUENCY, metadata);
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return blockState.getValue(FREQUENCY);
	}
	
	@Nonnull
	@Override
	public IBlockState getExtendedState(@Nonnull final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
		if (!(blockState instanceof IExtendedBlockState)) {
			return blockState;
		}
		final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityForceField)) {
			return blockState;
		}
		final TileEntityForceField tileEntityForceField = (TileEntityForceField) tileEntity;
		IBlockState blockStateCamouflage = tileEntityForceField.cache_blockStateCamouflage;
		if (!Commons.isValidCamouflage(blockStateCamouflage)) {
			blockStateCamouflage = Blocks.AIR.getDefaultState();
		}
		return ((IExtendedBlockState) blockState)
		       .withProperty(BlockProperties.CAMOUFLAGE, blockStateCamouflage);
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockAbstractBase(this, true, true);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(final CreativeTabs creativeTab, final NonNullList<ItemStack> list) {
		// hide in NEI
		for (int i = 0; i < 16; i++) {
			Commons.hideItemStack(new ItemStack(this, 1, i));
		}
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityForceField();
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void modelInitialisation() {
		super.modelInitialisation();
		
		// register camouflage
		for (final Integer integer : FREQUENCY.getAllowedValues()) {
			final ResourceLocation registryName = getRegistryName();
			assert registryName != null;
			final String variant = String.format("%s=%d", FREQUENCY.getName(), integer);
			ModelBakeEventHandler.registerBakedModel(new ModelResourceLocation(registryName, variant), BakedModelCamouflage.class);
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean causesSuffocation(final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullBlock(final IBlockState blockState) {
		return false;
	}
	
	@Nonnull
	@Override
	public ItemStack getPickBlock(@Nonnull final IBlockState blockState, final RayTraceResult target, @Nonnull final World world, @Nonnull final BlockPos blockPos, final EntityPlayer entityPlayer) {
		return new ItemStack(Blocks.AIR);
	}
	
	@Override
	public int quantityDropped(final Random random) {
		return 0;
	}
	
	@Nonnull
	@Override
	public EnumBlockRenderType getRenderType(final IBlockState blockState) {
		return EnumBlockRenderType.MODEL;
	}
	
	@SuppressWarnings("deprecation")
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing) {
		final BlockPos blockPosSide = blockPos.offset(facing);
		if (blockAccess.isAirBlock(blockPosSide)) {
			return true;
		}
		final EnumFacing opposite = facing.getOpposite();
		final IBlockState blockStateSide = blockAccess.getBlockState(blockPosSide);
		if ( blockStateSide.getBlock() instanceof BlockGlass 
		  || blockStateSide.getBlock() instanceof BlockHullGlass
		  || blockStateSide.getBlock() instanceof BlockForceField ) {
			return blockState.getBlock().getMetaFromState(blockState)
				!= blockStateSide.getBlock().getMetaFromState(blockStateSide);
		}
		return !blockAccess.isSideSolid(blockPosSide, opposite, false);
	}
	
	protected TileEntityForceFieldProjector getProjector(final World world, @Nonnull final BlockPos blockPos) {
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityForceField) {
			return ((TileEntityForceField) tileEntity).getProjector();
		}
		return null;
	}
	
	private ForceFieldSetup getForceFieldSetup(final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityForceField) {
			return ((TileEntityForceField) tileEntity).getForceFieldSetup();
		}
		return null;
	}
	
	@Override
	public void onBlockClicked(final World world, final BlockPos blockPos, final EntityPlayer entityPlayer) {
		if (world.isRemote) {
			return;
		}
		final ForceFieldSetup forceFieldSetup = getForceFieldSetup(world, blockPos);
		if (forceFieldSetup != null) {
			forceFieldSetup.onEntityEffect(world, blockPos, entityPlayer);
		}
	}
	
	@SuppressWarnings("deprecation")
	@Nullable
	@Override
	public AxisAlignedBB getCollisionBoundingBox(final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		final ForceFieldSetup forceFieldSetup = getForceFieldSetup(blockAccess, blockPos);
		if ( forceFieldSetup != null
		  && blockAccess instanceof World ) {// @TODO lag when placing force field due to permission checks?
			final List<EntityPlayer> entities = ((World) blockAccess).getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(
				blockPos.getX(), blockPos.getY(), blockPos.getZ(),
				blockPos.getX() + 1.0D, blockPos.getY() + 1.0D, blockPos.getZ() + 1.0D));
			
			for (final EntityPlayer entityPlayer : entities) {
				if (entityPlayer != null && entityPlayer.isSneaking()) {
					if ( entityPlayer.capabilities.isCreativeMode 
					  || forceFieldSetup.isAccessGranted(entityPlayer, EnumPermissionNode.SNEAK_THROUGH)) {
							return null;
					}
				}
			}
		}
		
		return AABB_FORCEFIELD;
	}
	
	@Override
	public void onEntityCollision(final World world, final BlockPos blockPos, final IBlockState blockState, final Entity entity) {
		super.onEntityCollision(world, blockPos, blockState, entity);
		
		if (world.isRemote) {
			return;
		}
		
		final ForceFieldSetup forceFieldSetup = getForceFieldSetup(world, blockPos);
		if (forceFieldSetup != null) {
			forceFieldSetup.onEntityEffect(world, blockPos, entity);
			final double distance2 = new Vector3(blockPos).translate(0.5F).distanceTo_square(entity);
			if (entity instanceof EntityLiving && distance2 < 0.26D) {
				boolean hasPermission = false;
				
				final List<EntityPlayer> entities = world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(
					blockPos.getX(), blockPos.getY(), blockPos.getZ(),
					blockPos.getX() + 1.0D, blockPos.getY() + 0.9D, blockPos.getZ() + 1.0D));
				for (final EntityPlayer entityPlayer : entities) {
					if (entityPlayer != null && entityPlayer.isSneaking()) {
						if ( entityPlayer.capabilities.isCreativeMode
						  || forceFieldSetup.isAccessGranted(entityPlayer, EnumPermissionNode.SNEAK_THROUGH) ) {
							hasPermission = true;
							break;
						}
					}
				}
				
				// always slowdown
				((EntityLiving) entity).addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 20, 1));
				
				if (!hasPermission) {
					((EntityLiving) entity).addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 80, 3));
					if (distance2 < 0.24D) {
						entity.attackEntityFrom(WarpDrive.damageShock, 5);
					}
				}
			}
		}
	}
	
	@Override
	public int getLightValue(@Nonnull final IBlockState blockState, final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityForceField) {
			return ((TileEntityForceField)tileEntity).cache_lightCamouflage;
		}
		
		return 0;
	}
	
	private void downgrade(final World world, final BlockPos blockPos) {
		if (enumTier.getIndex() > 1) {
			final TileEntityForceFieldProjector tileEntityForceFieldProjector = getProjector(world, blockPos);
			final IBlockState blockState = world.getBlockState(blockPos);
			final int frequency = blockState.getBlock() == this ? blockState.getValue(FREQUENCY) : 0;
			world.setBlockState(blockPos, WarpDrive.blockForceFields[enumTier.getIndex() - 1].getDefaultState().withProperty(FREQUENCY, (frequency + 1) % 16), 2);
			if (tileEntityForceFieldProjector != null) {
				final TileEntity tileEntity = world.getTileEntity(blockPos);
				if (tileEntity instanceof TileEntityForceField) {
					((TileEntityForceField) tileEntity).setProjector(new VectorI(tileEntityForceFieldProjector));
				}
			}
			
		} else {
			world.setBlockToAir(blockPos);
		}
	}
	
	// explosion handling, preferably without ASM
	private int previous_exploderId = -1;
	private long previous_tickWorld = -1L;
	private int previous_idDimension = Integer.MAX_VALUE;
	private Vec3d previous_vExplosion = new Vec3d(0.0D, -1.0D, 0.0D);
	
	@SuppressWarnings("deprecation")
	@Override
	public float getExplosionResistance(@Nullable final Entity exploder) {
		final int exploderId = exploder == null ? -1 : exploder.getEntityId();
		if ( exploderId != previous_exploderId
		  && Commons.isServerThread() ) {
			new RuntimeException(String.format("Invalid call to deprecated getExplosionResistance(%s)", exploder)).printStackTrace();
			WarpDrive.logger.error(String.format("Invalid call to getExplosionResistance from %s",
			                                     exploder));
			return Float.MAX_VALUE;
		}
		return super.getExplosionResistance(exploder);
	}
	
	@Override
	public float getExplosionResistance(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nullable final Entity exploder, @Nonnull final Explosion explosion) {
		previous_exploderId = exploder == null ? -1 : exploder.getEntityId();
		final long tickWorld = world.getTotalWorldTime();
		final Vec3d vExplosion = explosion.getPosition();
		final boolean isFirstHit = Math.abs(tickWorld - previous_tickWorld) > 100L
		                        || previous_idDimension != world.provider.getDimension()
		                        || Math.abs(previous_vExplosion.x - vExplosion.x) > 5.0D
		                        || Math.abs(previous_vExplosion.y - vExplosion.y) > 5.0D
		                        || Math.abs(previous_vExplosion.z - vExplosion.z) > 5.0D;
		if (isFirstHit) {
			previous_tickWorld = tickWorld;
			previous_idDimension = world.provider.getDimension();
			previous_vExplosion = new Vec3d(vExplosion.x, vExplosion.y, vExplosion.z);
			if (!Commons.isSafeThread())  {
				WarpDrive.logger.error(String.format("Bad multithreading detected %s from exploder %s explosion %s",
				                                     Commons.format(world, blockPos), exploder, explosion ));
				new ConcurrentModificationException().printStackTrace();
			}
			WarpDrive.logger.error(String.format("Explosion check %s from exploder %s explosion %s",
			                                     Commons.format(world, blockPos), exploder, explosion ));
		}
		
		// find explosion strength, defaults to no effect
		double strength = 0.0D;
		if ( exploder == null
		  && vExplosion.x == Math.rint(vExplosion.x)
		  && vExplosion.y == Math.rint(vExplosion.y)
		  && vExplosion.z == Math.rint(vExplosion.z) ) {
			final BlockPos blockPosExplosion = new BlockPos((int) vExplosion.x, (int) vExplosion.y, (int) vExplosion.z);
			// IC2 Reactor blowing up => block is already air
			final IBlockState blockState = world.getBlockState(blockPosExplosion);
			final TileEntity tileEntity = world.getTileEntity(blockPosExplosion);
			if (isFirstHit && !WarpDriveConfig.LOGGING_FORCE_FIELD) {
				WarpDrive.logger.info(String.format("Force field %s: explosion from %s %s with tileEntity %s",
				                                    Commons.format(world, blockPos),
				                                    blockState.getBlock(), blockState.getBlock().getRegistryName(), tileEntity ));
			}
			// explosion with no entity and block removed, hence we can't compute the energy impact => boosting explosion resistance
			return 2.0F * super.getExplosionResistance(world, blockPos, exploder, explosion);
		}
		
		if (exploder != null) {
			final String nameExploder = exploder.getClass().toString();
			switch (nameExploder) {
			// Vanilla explosive
			case "class net.minecraft.entity.item.EntityEnderCrystal": strength = 6.0D; break;
			case "class net.minecraft.entity.item.EntityMinecartTNT": strength = 4.0D; break;
			case "class net.minecraft.entity.item.EntityTNTPrimed": strength = 5.0D; break;
			case "class net.minecraft.entity.monster.EntityCreeper": strength = 3.0D; break;  // *2 for powered ones
			
			// Applied Energistics Tiny TNT
			case "class appeng.entity.EntityTinyTNTPrimed": strength = 0.2D; break;
			
			// Blood Arsenal Blood TNT
			case "class com.arc.bloodarsenal.common.entity.EntityBloodTNT": strength = 1.0D; break;
			
			// IC2 explosives
			case "class ic2.core.block.EntityItnt": strength = 5.5D; break; 
			case "class ic2.core.block.EntityNuke": strength = 0.02D; break;
			case "class ic2.core.block.EntityDynamite": strength = 1.0D; break;
			case "class ic2.core.block.EntityStickyDynamite": strength = 1.0D; break;
			
			// ICBM Classic & DefenseTech S-mine (initial explosion)
			case "class defense.common.entity.EntityExplosion": strength = 1.0D; break;
			case "class icbm.classic.content.entity.EntityExplosion": strength = 1.0D; break;
			
			// ICBM Classic & DefenseTech Condensed, Incendiary, Repulsive, Attractive, Fragmentation, Sonic, Breaching, Thermobaric, Nuclear,
			// Exothermic, Endothermic, Anti-gravitational, Hypersonic, (Antimatter?)
			case "class defense.common.entity.EntityExplosive": strength = 15.0D; break;
			case "class icbm.classic.content.entity.EntityExplosive": strength = 15.0D; break;
			
			// ICBM Classic & DefenseTechFragmentation, S-mine fragments
			case "class defense.common.entity.EntityFragments": strength = 0.02D; break;
			case "class icbm.classic.content.entity.EntityFragments": strength = 0.02D; break;
			
			// ICBM Classic & DefenseTech Conventional, Attractive, Repulsive, Sonic, Breaching, Thermobaric, Nuclear, 
			// Exothermic, Endothermic, Anti-Gravitational, Hypersonic missile, (Antimatter?), (Red matter?), (Homing?), (Anti-Ballistic?)
			case "class defense.common.entity.EntityMissile": strength = 15.0D; break;
			case "class icbm.classic.content.entity.EntityMissile": strength = 15.0D; break;
			
			// ICBM Classic & DefenseTech Conventional/Incendiary/Repulsive grenade
			case "class defense.common.entity.EntityGrenade": strength = 3.0D; break;
			case "class icbm.classic.content.entity.EntityGrenade": strength = 3.0D; break;
			
			// Tinker's Construct SDX explosives
			case "class tconstruct.mechworks.entity.item.ExplosivePrimed": strength = 5.0D; break;
			
			default:
				if (isFirstHit) {
					WarpDrive.logger.error(String.format("Unknown exploder instance %s %s %s",
					                                     EntityList.getKey(exploder), nameExploder, exploder ));
				}
				break;
			}
		}
		
		if (strength == 0.0D) {// (no exploder or not a valid one, let's check the explosion itself)
			final String nameExplosion = explosion.getClass().toString();
			switch (nameExplosion) {
			// Vanilla explosive
			case "class icbm.classic.content.explosive.blast.threaded.BlastNuclear": strength = 15.0D; break;
			
			default:
				if (isFirstHit) {
					WarpDrive.logger.error(String.format("Unknown explosion instance %s %s %s",
					                                     vExplosion, nameExplosion, explosion ));
				}
				break;
			}
		}
		
		// apply damages to force field by consuming energy
		final Vector3 vDirection = new Vector3(blockPos.getX() + 0.5D - vExplosion.x,
		                                       blockPos.getY() + 0.5D - vExplosion.y,
		                                       blockPos.getZ() + 0.5D - vExplosion.z );
		final double magnitude = Math.max(1.0D, vDirection.getMagnitude());
		if (magnitude != 0) {// normalize
			vDirection.scale(1 / magnitude);
		}
		final double damageLevel = strength / (magnitude * magnitude) * 1.0D;
		double damageLeft = 0;
		final ForceFieldSetup forceFieldSetup = Commons.isSafeThread() ? getForceFieldSetup(world, blockPos) : null;
		if (forceFieldSetup != null) {
			damageLeft = forceFieldSetup.applyDamage(world, DamageSource.causeExplosionDamage(explosion), damageLevel);
		}
		
		assert damageLeft >= 0;
		if (isFirstHit && !WarpDriveConfig.LOGGING_FORCE_FIELD) {
			WarpDrive.logger.info(String.format("Force field %s %s involved in explosion %s strength %.3f magnitude %.3f damageLevel %.3f damageLeft %.3f",
			                                    enumTier, Commons.format(world, blockPos),
			                                    ((exploder != null) ? String.format("from %s", exploder) : String.format("from %s at %s", explosion, vExplosion)),
			                                    strength, magnitude, damageLevel, damageLeft));
		}
		return super.getExplosionResistance(world, blockPos, exploder, explosion);
	}
	
	@Override
	public boolean canDropFromExplosion(final Explosion explosion) {
		return false;
	}
	
	@Override
	public boolean canEntityDestroy(final IBlockState state, final IBlockAccess world, final BlockPos pos, final Entity entity) {
		return false;
	}
	
	@Override
	public void onBlockExploded(final World world, @Nonnull final BlockPos blockPos, @Nonnull final Explosion explosion) {
		// weapon mods should be intercepted earlier, so we'll spam the console for now...
		WarpDrive.logger.warn(String.format("Force field %s %s has exploded in explosion %s at %s, please report to mod author",
		                                    enumTier, Commons.format(world, blockPos),
		                                    explosion, explosion.getPosition() ));
		downgrade(world, blockPos);
		super.onBlockExploded(world, blockPos, explosion);
	}
	
	@Override
	public void onEMP(@Nonnull final World world, @Nonnull final BlockPos blockPos, final float efficiency) {
		if (efficiency * (1.0F - 0.20F * (enumTier.getIndex() - 1)) > world.rand.nextFloat()) {
			downgrade(world, blockPos);
		}
		// already handled => no ancestor call
	}
	
	@Override
	public void onExplosionDestroy(final World world, final BlockPos blockPos, final Explosion explosion) {
		// (block is already set to air by caller, see IC2 iTNT for example)
		downgrade(world, blockPos);
		super.onExplosionDestroy(world, blockPos, explosion);
	}
	
	@Override
	public float getBlockHardness(final IBlockState blockState, final World world, final BlockPos blockPos,
	                              final DamageSource damageSource, final int damageParameter, final Vector3 damageDirection, final int damageLevel) {
		return WarpDriveConfig.HULL_HARDNESS[enumTier.getIndex()];
	}
	
	@Override
	public int applyDamage(final IBlockState blockState, final World world, final BlockPos blockPos, final DamageSource damageSource,
	                       final int damageParameter, final Vector3 damageDirection, final int damageLevel) {
		final ForceFieldSetup forceFieldSetup = getForceFieldSetup(world, blockPos);
		if (forceFieldSetup != null) {
			return (int) Math.round(forceFieldSetup.applyDamage(world, damageSource, damageLevel));
		}
		
		return damageLevel;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public float getBlockHardness(final IBlockState blockState, final World worldIn, final BlockPos pos) {
		final String name = Thread.currentThread().getName();
		// hide unbreakable status from ICBM explosion handler (as of ICBM-classic-1.12.2-3.3.0b63, Nuclear skip unbreakable blocks)
		if (name.startsWith("ICBM")) {
			return WarpDriveConfig.HULL_HARDNESS[enumTier.getIndex()];
		}
		return super.getBlockHardness(blockState, worldIn, pos);
	}
}
