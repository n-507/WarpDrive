package cr0s.warpdrive.render;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.forcefield.BlockForceFieldProjector;
import cr0s.warpdrive.data.EnumForceFieldShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.*;

// Wrapper around OBJLoader to re-texture faces depending on IExtendedBlockState
public enum CustomModelLoaderProjector implements ICustomModelLoader {
	
	INSTANCE;
	
	private static boolean spriteInitialisationDone = false; 
	private static TextureAtlasSprite spriteShape_none;
	private static final HashMap<EnumForceFieldShape, TextureAtlasSprite> spriteShapes = new HashMap<>(EnumForceFieldShape.length);
	private static void initSprites() {
		if (!spriteInitialisationDone) {
			final TextureMap textureMapBlocks = Minecraft.getMinecraft().getTextureMapBlocks();
			spriteShapes.put(EnumForceFieldShape.NONE      , textureMapBlocks.getAtlasSprite("warpdrive:blocks/forcefield/projector-shape_none"));
			spriteShapes.put(EnumForceFieldShape.CUBE      , textureMapBlocks.getAtlasSprite("warpdrive:blocks/forcefield/projector-shape_cube"));
			spriteShapes.put(EnumForceFieldShape.CYLINDER_H, textureMapBlocks.getAtlasSprite("warpdrive:blocks/forcefield/projector-shape_cylinder_h"));
			spriteShapes.put(EnumForceFieldShape.CYLINDER_V, textureMapBlocks.getAtlasSprite("warpdrive:blocks/forcefield/projector-shape_cylinder_v"));
			spriteShapes.put(EnumForceFieldShape.PLANE     , textureMapBlocks.getAtlasSprite("warpdrive:blocks/forcefield/projector-shape_plane"));
			spriteShapes.put(EnumForceFieldShape.SPHERE    , textureMapBlocks.getAtlasSprite("warpdrive:blocks/forcefield/projector-shape_sphere"));
			spriteShapes.put(EnumForceFieldShape.TUBE      , textureMapBlocks.getAtlasSprite("warpdrive:blocks/forcefield/projector-shape_tube"));
			spriteShapes.put(EnumForceFieldShape.TUNNEL    , textureMapBlocks.getAtlasSprite("warpdrive:blocks/forcefield/projector-shape_tunnel"));
			spriteShape_none = spriteShapes.get(EnumForceFieldShape.NONE);
		}
	}
	
	@Override
	public void onResourceManagerReload(@Nonnull final IResourceManager resourceManager) {
		OBJLoader.INSTANCE.onResourceManagerReload(resourceManager);
		spriteInitialisationDone = false;
	}
	
	@Override
	public boolean accepts(final ResourceLocation modelLocation) {
		return WarpDrive.MODID.equals(modelLocation.getNamespace()) && modelLocation.getPath().endsWith(".wobj");
	}
	
	@Override
	public IModel loadModel(@Nonnull final ResourceLocation modelLocation) throws Exception {
		return new MyModel(OBJLoader.INSTANCE.loadModel(modelLocation));
	}
	
	private class MyModel implements IModel {
		private final IModel model;
		
		MyModel(final IModel model) {
			this.model = model;
		}
		
		@Nonnull
		@Override
		public Collection<ResourceLocation> getDependencies() {
			return model.getDependencies();
		}
		
		@Nonnull
		@Override
		public Collection<ResourceLocation> getTextures() {
			return model.getTextures();
		}
		
		@Nonnull
		@Override
		public IBakedModel bake(@Nonnull final IModelState state, @Nonnull final VertexFormat format,
		                        @Nonnull final java.util.function.Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
			return new MyBakedModel(model.bake(state, format, bakedTextureGetter));
		}
		
		@Nonnull
		@Override
		public IModelState getDefaultState() {
			return model.getDefaultState();
		}
	}
	
	class MyBakedModel implements IBakedModel {
		
		private final IBakedModel bakedModel;
		
		private long timeLastError = -1L;
		
		MyBakedModel(final IBakedModel bakedModel) {
			this.bakedModel = bakedModel;
			initSprites();
		}
		
		@Nonnull
		@Override
		public List<BakedQuad> getQuads(@Nullable final IBlockState blockState, @Nullable final EnumFacing enumFacing, final long rand) {
			final List<BakedQuad> bakedQuadsIn = bakedModel.getQuads(blockState, enumFacing, rand);
			final IExtendedBlockState exState = (IExtendedBlockState) blockState;
			EnumForceFieldShape enumForceFieldShape = exState != null ? exState.getValue(BlockForceFieldProjector.SHAPE) : EnumForceFieldShape.NONE;
			if (enumForceFieldShape == null) {
				final long time = System.currentTimeMillis();
				if (time - timeLastError > 5000L) {
					timeLastError = time;
					new RuntimeException("Invalid shape").printStackTrace();
					WarpDrive.logger.error(String.format("Invalid shape for %s facing %s",
					                                     blockState, enumFacing));
				}
				enumForceFieldShape = EnumForceFieldShape.NONE;
			}
			final TextureAtlasSprite spriteShape = spriteShapes.get(enumForceFieldShape);
			final List<BakedQuad> bakedQuadsOut = Lists.newArrayList();
			for(final BakedQuad bakedQuadIn : bakedQuadsIn) {
				if (bakedQuadIn.getSprite().equals(spriteShape_none)) {
					final BakedQuad bakedQuadOut = new BakedQuadRetextured(bakedQuadIn, spriteShape);
					bakedQuadsOut.add(bakedQuadOut);
				} else {
					bakedQuadsOut.add(bakedQuadIn);
				}
			}
			return ImmutableList.copyOf(bakedQuadsOut);
		}
		
		@Override
		public boolean isAmbientOcclusion() {
			return bakedModel.isAmbientOcclusion();
		}
		
		@Override
		public boolean isGui3d() {
			return bakedModel.isGui3d();
		}
		
		@Override
		public boolean isBuiltInRenderer() {
			return bakedModel.isBuiltInRenderer();
		}
		
		@Nonnull
		@Override
		public TextureAtlasSprite getParticleTexture() {
			return bakedModel.getParticleTexture();
		}
		
		@SuppressWarnings("deprecation")
		@Nonnull
		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return bakedModel.getItemCameraTransforms();
		}
		
		@Nonnull
		@Override
		public ItemOverrideList getOverrides() {
			return bakedModel.getOverrides();
		}
		
		@Nonnull
		@Override
		public Pair<? extends IBakedModel, Matrix4f> handlePerspective(@Nonnull final TransformType cameraTransformType) {
			return ((IBakedModel) bakedModel).handlePerspective(cameraTransformType);
		}
	}
}
