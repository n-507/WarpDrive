package cr0s.warpdrive.render;

import com.google.common.collect.ImmutableList;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IMyBakedModel;
import cr0s.warpdrive.block.energy.BlockCapacitor;
import cr0s.warpdrive.data.EnumDisabledInputOutput;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import net.minecraftforge.common.property.IExtendedBlockState;

public class BakedModelCapacitor implements IBakedModel, IMyBakedModel {
	
	private ResourceLocation resourceLocation;
	private IBakedModel bakedModelOriginal;
	private IExtendedBlockState extendedBlockStateDefault;
	
	private long timeLastError = -1L;
	
	public BakedModelCapacitor() {
	}
	
	@Override
	public void setResourceLocation(final ResourceLocation resourceLocation) {
		this.resourceLocation = resourceLocation;
	}
	
	@Override
	public void setOriginalBakedModel(final IBakedModel bakedModel) {
		this.bakedModelOriginal = bakedModel;
	}
	
	public IBakedModel getOriginalBakedModel() {
		return bakedModelOriginal;
	}
	
	@Nonnull
	@Override
	public List<BakedQuad> getQuads(@Nullable final IBlockState blockState, @Nullable final EnumFacing facing, final long rand) {
		assert resourceLocation != null;
		assert bakedModelOriginal != null;
		
		final IExtendedBlockState extendedBlockState;
		if (blockState == null) {
			// dead code until we have different blocks for each tiers to support item rendering and 1.13+
			if (extendedBlockStateDefault == null) {
				extendedBlockStateDefault = ((IExtendedBlockState) WarpDrive.blockCapacitor[0].getDefaultState())
				        .withProperty(BlockCapacitor.DOWN , EnumDisabledInputOutput.INPUT)
				        .withProperty(BlockCapacitor.UP   , EnumDisabledInputOutput.INPUT)
				        .withProperty(BlockCapacitor.NORTH, EnumDisabledInputOutput.OUTPUT)
				        .withProperty(BlockCapacitor.SOUTH, EnumDisabledInputOutput.OUTPUT)
				        .withProperty(BlockCapacitor.WEST , EnumDisabledInputOutput.OUTPUT)
				        .withProperty(BlockCapacitor.EAST , EnumDisabledInputOutput.OUTPUT);
			}
			extendedBlockState = extendedBlockStateDefault;
		} else if (blockState instanceof IExtendedBlockState) {
			extendedBlockState = (IExtendedBlockState) blockState;
		} else {
			extendedBlockState = null;
		}
		if (extendedBlockState != null) {
			final EnumDisabledInputOutput enumDisabledInputOutput = getEnumDisabledInputOutput(extendedBlockState, facing);
			if (enumDisabledInputOutput == null) {
				final long time = System.currentTimeMillis();
				if (time - timeLastError > 5000L) {
					timeLastError = time;
					new RuntimeException("Invalid extended property").printStackTrace();
					WarpDrive.logger.error(String.format("%s Invalid extended property for %s facing %s\n%s",
					                                     this, extendedBlockState, facing, formatDetails()));
				}
				return getDefaultQuads(facing, rand);
			}
			final IBlockState blockStateToRender = extendedBlockState.getClean().withProperty(BlockCapacitor.CONFIG, enumDisabledInputOutput);
			
			// remap to the json model representing the proper state
			final BlockModelShapes blockModelShapes = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes();
			final IBakedModel bakedModelWrapped = blockModelShapes.getModelForState(blockStateToRender);
			final IBakedModel bakedModelToRender = ((BakedModelCapacitor) bakedModelWrapped).getOriginalBakedModel();
			return bakedModelToRender.getQuads(blockStateToRender, facing, rand);
		}
		return getDefaultQuads(facing, rand);
	}
	
	public EnumDisabledInputOutput getEnumDisabledInputOutput(final IExtendedBlockState extendedBlockState, @Nullable final EnumFacing facing) {
		if (facing == null) {
			return EnumDisabledInputOutput.DISABLED;
		}
		switch (facing) {
		case DOWN : return extendedBlockState.getValue(BlockCapacitor.DOWN);
		case UP   : return extendedBlockState.getValue(BlockCapacitor.UP);
		case NORTH: return extendedBlockState.getValue(BlockCapacitor.NORTH);
		case SOUTH: return extendedBlockState.getValue(BlockCapacitor.SOUTH);
		case WEST : return extendedBlockState.getValue(BlockCapacitor.WEST);
		case EAST : return extendedBlockState.getValue(BlockCapacitor.EAST);
		default: return EnumDisabledInputOutput.DISABLED;
		}
	}
	
	public List<BakedQuad> getDefaultQuads(final EnumFacing side, final long rand) {
		final IBlockState blockState = Blocks.FIRE.getDefaultState();
		return Minecraft.getMinecraft().getBlockRendererDispatcher()
		       .getModelForState(blockState).getQuads(blockState, side, rand);
	}
	
	@Override
	public boolean isAmbientOcclusion() {
		return bakedModelOriginal.isAmbientOcclusion();
	}
	
	@Override
	public boolean isGui3d() {
		return bakedModelOriginal.isGui3d();
	}
	
	@Override
	public boolean isBuiltInRenderer() {
		return bakedModelOriginal.isBuiltInRenderer();
	}
	
	@Nonnull
	@Override
	public TextureAtlasSprite getParticleTexture() {
		// Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("warpdrive:someTexture")
		return bakedModelOriginal.getParticleTexture();
	}
	
	@Nonnull
	@Override
	public ItemCameraTransforms getItemCameraTransforms() {
		return bakedModelOriginal.getItemCameraTransforms();
	}
	
	@Nonnull
	@Override
	public ItemOverrideList getOverrides() {
		return itemOverrideList;
	}
	
	private final ItemOverrideList itemOverrideList = new ItemOverrideList(ImmutableList.of()) {
		@Nonnull
		@Override
		public IBakedModel handleItemState(@Nonnull final IBakedModel model, @Nonnull final ItemStack itemStack,
		                                   final World world, final EntityLivingBase entity) {
			final Block block = ((ItemBlock) itemStack.getItem()).getBlock();
			final IBlockState blockState = block.getStateFromMeta(itemStack.getMetadata());
			final IBakedModel bakedModel = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(blockState);
			if (bakedModel instanceof BakedModelCapacitor) {
				return ((BakedModelCapacitor) bakedModel).bakedModelOriginal;
			}
			return bakedModel;
		}
	};
	
	private String formatDetails() {
		return String.format("resourceLocation %s\nbakedModelOriginal %s\nextendedBlockStateDefault %s]",
		                     resourceLocation,
		                     bakedModelOriginal,
		                     extendedBlockStateDefault);
	}
}