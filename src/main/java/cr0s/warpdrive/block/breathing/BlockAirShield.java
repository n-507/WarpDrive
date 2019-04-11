package cr0s.warpdrive.block.breathing;

import cr0s.warpdrive.block.BlockAbstractOmnipanel;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

import net.minecraft.block.BlockColored;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockAirShield extends BlockAbstractOmnipanel implements IBlockColor {
	
	public BlockAirShield(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.CLOTH);
		
		setTranslationKey("warpdrive.breathing.air_shield");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public int colorMultiplier(@Nonnull final IBlockState blockState, @Nullable final IBlockAccess blockAccess, @Nullable final BlockPos blockPos, final int tintIndex) {
		switch (blockState.getValue(BlockColored.COLOR)) {
		case WHITE:
			return 0xFFFFFF;
		case ORANGE:
			return 0xFF5A02;
		case MAGENTA:
			return 0xF269FF;
		case LIGHT_BLUE:
			return 0x80AAFF;
		case YELLOW:
			return 0xFFEE3C;
		case LIME:
			return 0x90E801;
		case PINK:
			return 0xFB0680;
		case SILVER: // gray
			return 0x2C2C2C;
		case GRAY: // light gray
			return 0x686868;
		case CYAN:
		default: // SciFi cyan
			return 0x0FD7FF;
		case PURPLE:
			return 0x5D1072;
		case BLUE:
			return 0x4351CC;
		case BROWN:
			return 0x99572E;
		case GREEN:
			return 0x75993C;
		case RED:
			return 0xCC4d41;
		case BLACK:
			return 0x080808;
		}
		// return MapColor.getBlockColor(blockState.getValue(BlockColored.COLOR)).colorValue;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean causesSuffocation(final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullBlock(final IBlockState blockState) {
		return false;
	}
	
	@Override
	public void addCollisionBoxToList(final IBlockState blockState, final @Nonnull World world, final @Nonnull BlockPos blockPos,
	                                  final @Nonnull AxisAlignedBB entityBox, final @Nonnull List<AxisAlignedBB> collidingBoxes,
	                                  final @Nullable Entity entity, final boolean isActualState) {
	}
	
	@SuppressWarnings("deprecation")
	@Nullable
	@Override
	public AxisAlignedBB getCollisionBoundingBox(final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return NULL_AABB;
	}
	
	@Override
	public boolean canCollideCheck(final IBlockState blockState, final boolean hitIfLiquid) {
		return !hitIfLiquid;
	}
	
	@Override
	public boolean isCollidable() {
		return false;
	}
}