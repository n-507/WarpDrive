package cr0s.warpdrive.block;

import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import net.minecraft.block.BlockPane;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class BlockAbstractOmnipanel extends BlockAbstractBase {
	
	public static final float CENTER_MIN = 7.0F / 16.0F;
	public static final float CENTER_MAX = 9.0F / 16.0F;
	
	protected static AxisAlignedBB AABB_XN_YN = new AxisAlignedBB(0.0F, 0.0F, CENTER_MIN, CENTER_MAX, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_XP_YN = new AxisAlignedBB(CENTER_MIN, 0.0F, CENTER_MIN, 1.0F, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_XN_YP = new AxisAlignedBB(0.0F, CENTER_MIN, CENTER_MIN, CENTER_MAX, 1.0F, CENTER_MAX);
	protected static AxisAlignedBB AABB_XP_YP = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, CENTER_MIN, 1.0F, 1.0F, CENTER_MAX);
	 
	protected static AxisAlignedBB AABB_ZN_YN = new AxisAlignedBB(CENTER_MIN, 0.0F, 0.0F, CENTER_MAX, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_ZP_YN = new AxisAlignedBB(CENTER_MIN, 0.0F, CENTER_MIN, CENTER_MAX, CENTER_MAX, 1.0F);
	protected static AxisAlignedBB AABB_ZN_YP = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, 0.0F, CENTER_MAX, 1.0F, CENTER_MAX);
	protected static AxisAlignedBB AABB_ZP_YP = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, CENTER_MIN, CENTER_MAX, 1.0F, 1.0F);
	 
	protected static AxisAlignedBB AABB_XN_ZN = new AxisAlignedBB(0.0F, CENTER_MIN, 0.0F, CENTER_MAX, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_XP_ZN = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, 0.0F, 1.0F, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_XN_ZP = new AxisAlignedBB(0.0F, CENTER_MIN, CENTER_MIN, CENTER_MAX, CENTER_MAX, 1.0F);
	protected static AxisAlignedBB AABB_XP_ZP = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, CENTER_MIN, 1.0F, CENTER_MAX, 1.0F);
	
	protected static AxisAlignedBB AABB_YN = new AxisAlignedBB(CENTER_MIN, 0.0F, CENTER_MIN, CENTER_MAX, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_YP = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, CENTER_MIN, CENTER_MAX, 1.0F, CENTER_MAX);
	protected static AxisAlignedBB AABB_ZN = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, 0.0F, CENTER_MAX, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_ZP = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, CENTER_MIN, CENTER_MAX, CENTER_MAX, 1.0F);
	protected static AxisAlignedBB AABB_XN = new AxisAlignedBB(0.0F, CENTER_MIN, CENTER_MIN, CENTER_MAX, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_XP = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, CENTER_MIN, 1.0F, CENTER_MAX, CENTER_MAX);
	
	
	public BlockAbstractOmnipanel(final String registryName, final EnumTier enumTier, final Material material) {
		super(registryName, enumTier, material);
	}
	
	@Override
	public int damageDropped(final IBlockState blockState) {
		return getMetaFromState(blockState);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isBlockNormalCube(final IBlockState blockState) {
		// not supposed to be called, upstream should use isNormalCube(IBlockState, IBlockAccess, BlockPos) instead
		// practically, Forge still use it in WorldEntitySpawner.isValidEmptySpawnBlock() calling BlockStateContainer$StateImplementation.isBlockNormalCube()
		return false;
	}
	
	@Override
	public boolean isNormalCube(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullCube(final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, final EnumFacing facing) {
		return blockAccess.getBlockState(blockPos).getBlock() != this && super.shouldSideBeRendered(blockState, blockAccess, blockPos, facing);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void addCollisionBoxToList(final IBlockState blockState, final @Nonnull World world, final @Nonnull BlockPos blockPos,
	                                  final @Nonnull AxisAlignedBB entityBox, final @Nonnull List<AxisAlignedBB> collidingBoxes,
	                                  final @Nullable Entity entity, final boolean isActualState) {
		final MutableBlockPos mutableBlockPos = new MutableBlockPos(blockPos);
		
		// get direct connections
		final int maskConnectY_neg = getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() - 1, blockPos.getZ()    ), EnumFacing.DOWN);
		final int maskConnectY_pos = getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() + 1, blockPos.getZ()    ), EnumFacing.UP);
		final int maskConnectZ_neg = getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY()    , blockPos.getZ() - 1), EnumFacing.NORTH);
		final int maskConnectZ_pos = getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY()    , blockPos.getZ() + 1), EnumFacing.SOUTH);
		final int maskConnectX_neg = getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY()    , blockPos.getZ()    ), EnumFacing.WEST);
		final int maskConnectX_pos = getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY()    , blockPos.getZ()    ), EnumFacing.EAST);
		
		final boolean canConnectY_neg = maskConnectY_neg > 0;
		final boolean canConnectY_pos = maskConnectY_pos > 0;
		final boolean canConnectZ_neg = maskConnectZ_neg > 0;
		final boolean canConnectZ_pos = maskConnectZ_pos > 0;
		final boolean canConnectX_neg = maskConnectX_neg > 0;
		final boolean canConnectX_pos = maskConnectX_pos > 0;
		final boolean canConnectNone = !canConnectY_neg && !canConnectY_pos && !canConnectZ_neg && !canConnectZ_pos && !canConnectX_neg && !canConnectX_pos;
		
		// get diagonal connections
		final boolean canConnectXn_Y_neg = (maskConnectX_neg > 1 && maskConnectY_neg > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY() - 1, blockPos.getZ()    ), EnumFacing.DOWN ) > 0;
		final boolean canConnectXn_Y_pos = (maskConnectX_neg > 1 && maskConnectY_pos > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY() + 1, blockPos.getZ()    ), EnumFacing.UP   ) > 0;
		final boolean canConnectXn_Z_neg = (maskConnectX_neg > 1 && maskConnectZ_neg > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY()    , blockPos.getZ() - 1), EnumFacing.NORTH) > 0;
		final boolean canConnectXn_Z_pos = (maskConnectX_neg > 1 && maskConnectZ_pos > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY()    , blockPos.getZ() + 1), EnumFacing.SOUTH) > 0;
		final boolean canConnectZn_Y_neg = (maskConnectZ_neg > 1 && maskConnectY_neg > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() - 1, blockPos.getZ() - 1), EnumFacing.DOWN ) > 0;
		final boolean canConnectZn_Y_pos = (maskConnectZ_neg > 1 && maskConnectY_pos > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() + 1, blockPos.getZ() - 1), EnumFacing.UP   ) > 0;
		
		final boolean canConnectXp_Y_neg = (maskConnectX_pos > 1 && maskConnectY_neg > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY() - 1, blockPos.getZ()    ), EnumFacing.DOWN ) > 0;
		final boolean canConnectXp_Y_pos = (maskConnectX_pos > 1 && maskConnectY_pos > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ()    ), EnumFacing.UP   ) > 0;
		final boolean canConnectXp_Z_neg = (maskConnectX_pos > 1 && maskConnectZ_neg > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY()    , blockPos.getZ() - 1), EnumFacing.NORTH) > 0;
		final boolean canConnectXp_Z_pos = (maskConnectX_pos > 1 && maskConnectZ_pos > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY()    , blockPos.getZ() + 1), EnumFacing.SOUTH) > 0;
		final boolean canConnectZp_Y_neg = (maskConnectZ_pos > 1 && maskConnectY_neg > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() - 1, blockPos.getZ() + 1), EnumFacing.DOWN ) > 0;
		final boolean canConnectZp_Y_pos = (maskConnectZ_pos > 1 && maskConnectY_pos > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() + 1, blockPos.getZ() + 1), EnumFacing.UP   ) > 0;
		
		// get panels
		final boolean hasXnYn = canConnectNone || (canConnectX_neg && canConnectY_neg && canConnectXn_Y_neg);
		final boolean hasXpYn = canConnectNone || (canConnectX_pos && canConnectY_neg && canConnectXp_Y_neg);
		final boolean hasXnYp = canConnectNone || (canConnectX_neg && canConnectY_pos && canConnectXn_Y_pos);
		final boolean hasXpYp = canConnectNone || (canConnectX_pos && canConnectY_pos && canConnectXp_Y_pos);
		
		final boolean hasXnZn = canConnectNone || (canConnectX_neg && canConnectZ_neg && canConnectXn_Z_neg);
		final boolean hasXpZn = canConnectNone || (canConnectX_pos && canConnectZ_neg && canConnectXp_Z_neg);
		final boolean hasXnZp = canConnectNone || (canConnectX_neg && canConnectZ_pos && canConnectXn_Z_pos);
		final boolean hasXpZp = canConnectNone || (canConnectX_pos && canConnectZ_pos && canConnectXp_Z_pos);
		
		final boolean hasZnYn = canConnectNone || (canConnectZ_neg && canConnectY_neg && canConnectZn_Y_neg);
		final boolean hasZpYn = canConnectNone || (canConnectZ_pos && canConnectY_neg && canConnectZp_Y_neg);
		final boolean hasZnYp = canConnectNone || (canConnectZ_neg && canConnectY_pos && canConnectZn_Y_pos);
		final boolean hasZpYp = canConnectNone || (canConnectZ_pos && canConnectY_pos && canConnectZp_Y_pos);
		
		{// z plane
			if (hasXnYn) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XN_YN);
			}
			
			if (hasXpYn) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XP_YN);
			}
			
			if (hasXnYp) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XN_YP);
			}
			
			if (hasXpYp) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XP_YP);
			}
		}
		
		{// x plane
			if (hasZnYn) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_ZN_YN);
			}
			
			if (hasZpYn) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_ZP_YN);
			}
			
			if (hasZnYp) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_ZN_YP);
			}
			
			if (hasZpYp) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_ZP_YP);
			}
		}
		
		{// z plane
			if (hasXnZn) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XN_ZN);
			}
			
			if (hasXpZn) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XP_ZN);
			}
			
			if (hasXnZp) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XN_ZP);
			}
			
			if (hasXpZp) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XP_ZP);
			}
		}
		
		// central nodes
		if (canConnectY_neg && !hasXnYn && !hasXpYn && !hasZnYn && !hasZpYn) {
			addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_YN);
		}
		if (canConnectY_pos && !hasXnYp && !hasXpYp && !hasZnYp && !hasZpYp) {
			addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_YP);
		}
		if (canConnectZ_neg && !hasXnZn && !hasXpZn && !hasZnYn && !hasZnYp) {
			addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_ZN);
		}
		if (canConnectZ_pos && !hasXnZp && !hasXpZp && !hasZpYn && !hasZpYp) {
			addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_ZP);
		}
		if (canConnectX_neg && !hasXnYn && !hasXnYp && !hasXnZn && !hasXnZp) {
			addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XN);
		}
		if (canConnectX_pos && !hasXpYn && !hasXpYp && !hasXpZn && !hasXpZp) {
			addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XP);
		}
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public AxisAlignedBB getBoundingBox(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
		final MutableBlockPos mutableBlockPos = new MutableBlockPos(blockPos);
		
		// get direct connections
		final int maskConnectY_neg = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() - 1, blockPos.getZ()    ), EnumFacing.DOWN);
		final int maskConnectY_pos = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() + 1, blockPos.getZ()    ), EnumFacing.UP);
		final int maskConnectZ_neg = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY()    , blockPos.getZ() - 1), EnumFacing.NORTH);
		final int maskConnectZ_pos = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY()    , blockPos.getZ() + 1), EnumFacing.SOUTH);
		final int maskConnectX_neg = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY()    , blockPos.getZ()    ), EnumFacing.WEST);
		final int maskConnectX_pos = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY()    , blockPos.getZ()    ), EnumFacing.EAST);
		
		final boolean canConnectY_neg = maskConnectY_neg > 0;
		final boolean canConnectY_pos = maskConnectY_pos > 0;
		final boolean canConnectZ_neg = maskConnectZ_neg > 0;
		final boolean canConnectZ_pos = maskConnectZ_pos > 0;
		final boolean canConnectX_neg = maskConnectX_neg > 0;
		final boolean canConnectX_pos = maskConnectX_pos > 0;
		final boolean canConnectNone = !canConnectY_neg && !canConnectY_pos && !canConnectZ_neg && !canConnectZ_pos && !canConnectX_neg && !canConnectX_pos;
		
		// x axis
		final float xMin = canConnectNone || canConnectX_neg ? 0.0F : CENTER_MIN;
		final float xMax = canConnectNone || canConnectX_pos ? 1.0F : CENTER_MAX;
		
		// y axis
		final float yMin = canConnectNone || canConnectY_neg ? 0.0F : CENTER_MIN;
		final float yMax = canConnectNone || canConnectY_pos ? 1.0F : CENTER_MAX;
		
		// z axis
		final float zMin = canConnectNone || canConnectZ_neg ? 0.0F : CENTER_MIN;
		final float zMax = canConnectNone || canConnectZ_pos ? 1.0F : CENTER_MAX;
		
		return new AxisAlignedBB(xMin, yMin, zMin, xMax, yMax, zMax);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected boolean canSilkHarvest()
	{
		return true;
	}
	
	public int getConnectionMask(final IBlockAccess blockAccess, final BlockPos blockPos, final EnumFacing facing) {
		final IBlockState blockState = blockAccess.getBlockState(blockPos);
		return ( blockState.isFullCube()
		      || blockState.getBlock() == this
		      || blockState.getMaterial() == Material.GLASS
		      || blockState.getBlock() instanceof BlockPane ? 1 : 0 )
		     + (blockState.isSideSolid(blockAccess, blockPos, facing.getOpposite()) ? 2 : 0);
	}
}