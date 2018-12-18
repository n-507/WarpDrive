package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.CommonProxy;
import cr0s.warpdrive.Commons;
import cr0s.warpdrive.block.BlockAbstractRotatingContainer;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.item.ItemComponent;

import javax.annotation.Nonnull;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class BlockShipCore extends BlockAbstractRotatingContainer {
	
	public BlockShipCore(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.movement.ship_core." + enumTier.getName());
		ignoreFacingOnPlacement = true;
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityShipCore();
	}
	
	@Nonnull
	@Override
	public IBlockState getStateForPlacement(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing,
	                                        final float hitX, final float hitY, final float hitZ, final int metadata,
	                                        @Nonnull final EntityLivingBase entityLivingBase, final EnumHand enumHand) {
		final IBlockState blockState = super.getStateForPlacement(world, blockPos, facing, hitX, hitY, hitZ, metadata, entityLivingBase, enumHand);
		final EnumFacing enumFacing = Commons.getHorizontalDirectionFromEntity(entityLivingBase).getOpposite();
		return blockState.withProperty(BlockProperties.FACING, enumFacing);
	}
	
	@Override
	public void getDrops(@Nonnull final NonNullList<ItemStack> itemStacks, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos,
	                     @Nonnull final IBlockState blockState, final int fortune) {
		final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityShipCore) {
			if (((TileEntityShipCore) tileEntity).jumpCount == 0) {
				super.getDrops(itemStacks, blockAccess, blockPos, blockState, fortune);
				return;
			}
		}
		if (blockAccess instanceof WorldServer) {
			final WorldServer worldServer = (WorldServer) blockAccess;
			final EntityPlayer entityPlayer = CommonProxy.getFakePlayer(null, worldServer, blockPos);
			// trigger explosion
			final EntityTNTPrimed entityTNTPrimed = new EntityTNTPrimed(worldServer,
				blockPos.getX() + 0.5F, blockPos.getY() + 0.5F, blockPos.getZ() + 0.5F, entityPlayer);
			entityTNTPrimed.setFuse(10 + worldServer.rand.nextInt(10));
			worldServer.spawnEntity(entityTNTPrimed);
			
			// get a chance to get the drops
			itemStacks.add(ItemComponent.getItemStackNoCache(EnumComponentType.CAPACITIVE_CRYSTAL, 1));
			if (fortune > 0 && worldServer.rand.nextBoolean()) {
				itemStacks.add(ItemComponent.getItemStackNoCache(EnumComponentType.CAPACITIVE_CRYSTAL, 1));
			}
			if (fortune > 1 && worldServer.rand.nextBoolean()) {
				itemStacks.add(ItemComponent.getItemStackNoCache(EnumComponentType.CAPACITIVE_CRYSTAL, 1));
			}
			if (fortune > 1 & worldServer.rand.nextBoolean()) {
				itemStacks.add(ItemComponent.getItemStackNoCache(EnumComponentType.POWER_INTERFACE, 1));
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public float getPlayerRelativeBlockHardness(final IBlockState blockState, @Nonnull final EntityPlayer entityPlayer, @Nonnull final World world, @Nonnull final BlockPos blockPos) {
		boolean willBreak = true;
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityShipCore) {
			if (((TileEntityShipCore)tileEntity).jumpCount == 0) {
				willBreak = false;
			}
		}
		return (willBreak ? 0.02F : 1.0F) * super.getPlayerRelativeBlockHardness(blockState, entityPlayer, world, blockPos);
	}
	
	@Override
	public boolean onBlockActivated(final World world, final BlockPos blockPos, final IBlockState blockState,
	                                final EntityPlayer entityPlayer, final EnumHand enumHand,
	                                final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		if (enumHand != EnumHand.MAIN_HAND) {
			return true;
		}
		
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(enumHand);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityShipCore)) {
			return false;
		}
		final TileEntityShipCore tileEntityShipCore = (TileEntityShipCore) tileEntity;
		
		if (itemStackHeld.isEmpty()) {
			if ( world.isRemote
			  && entityPlayer.isSneaking() ) {
				tileEntityShipCore.showBoundingBox = !tileEntityShipCore.showBoundingBox;
				if (tileEntityShipCore.showBoundingBox) {
					world.playSound(null, blockPos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 4.0F, 2.0F);
				} else {
					world.playSound(null, blockPos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 4.0F, 1.4F);
				}
				Commons.addChatMessage(entityPlayer, tileEntityShipCore.getBoundingBoxStatus());
				return true;
			} else if ( !world.isRemote
			         && !entityPlayer.isSneaking() ) {
				Commons.addChatMessage(entityPlayer, tileEntityShipCore.getStatus());
				return true;
			}
		}
		
		return false;
	}
}