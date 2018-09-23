package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.block.BlockAbstractRotatingContainer;
import cr0s.warpdrive.data.CameraRegistryItem;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.render.ClientCameraHandler;

import javax.annotation.Nonnull;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class BlockMonitor extends BlockAbstractRotatingContainer {
	
	public BlockMonitor(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.detection.monitor");
	}

	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityMonitor();
	}

	@Override
	public boolean onBlockActivated(final World world, final BlockPos blockPos, final IBlockState blockState,
	                                final EntityPlayer entityPlayer, final EnumHand enumHand,
	                                final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		// Monitor is only reacting client side
		if (!world.isRemote) {
			return false;
		}
		
		if (enumHand != EnumHand.MAIN_HAND) {
			return true;
		}
		
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(enumHand);
		
		if (itemStackHeld.isEmpty()) {
			final TileEntity tileEntity = world.getTileEntity(blockPos);
			
			if (tileEntity instanceof TileEntityMonitor) {
				// validate video channel
				final int videoChannel = ((TileEntityMonitor) tileEntity).getVideoChannel();
				if ( videoChannel < IVideoChannel.VIDEO_CHANNEL_MIN
				  || videoChannel > IVideoChannel.VIDEO_CHANNEL_MAX ) {
					Commons.addChatMessage(entityPlayer, ((TileEntityMonitor) tileEntity).getStatus());
					return true;
				}
				
				// validate camera
				final CameraRegistryItem camera = WarpDrive.cameras.getCameraByVideoChannel(world, videoChannel);
				if ( camera == null
				  || entityPlayer.isSneaking() ) {
					Commons.addChatMessage(entityPlayer, ((TileEntityMonitor) tileEntity).getStatus());
					return true;
				}
				
				Commons.addChatMessage(entityPlayer, new TextComponentTranslation("warpdrive.monitor.viewing_camera",
						videoChannel,
						camera.blockPos.getX(),
						camera.blockPos.getY(),
						camera.blockPos.getZ() ));
				ClientCameraHandler.setupViewpoint(
						camera.type, entityPlayer, entityPlayer.rotationYaw, entityPlayer.rotationPitch,
						blockPos, blockState,
						camera.blockPos, world.getBlockState(camera.blockPos));
			}
		}
		
		return false;
	}
}