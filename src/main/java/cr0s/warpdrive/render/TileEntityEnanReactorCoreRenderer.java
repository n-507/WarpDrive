package cr0s.warpdrive.render;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.energy.TileEntityEnanReactorCore;
import cr0s.warpdrive.client.SpriteManager;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.model.TRSRTransformation;

public class TileEntityEnanReactorCoreRenderer extends TileEntitySpecialRenderer<TileEntityEnanReactorCore> {
	
	private IBakedModel bakedModelCrystal;
	private IBakedModel bakedModelMatter;
	private static List<BakedQuad> quadsCrystal;
	private static List<BakedQuad> quadsMatter;
	
	public TileEntityEnanReactorCoreRenderer() {
		super();
		SpriteManager.add(new ResourceLocation("warpdrive:blocks/energy/reactor_core-crystal"));
		SpriteManager.add(new ResourceLocation("warpdrive:blocks/energy/reactor_core-grip"));
		SpriteManager.add(new ResourceLocation("warpdrive:blocks/energy/reactor_matter"));
	}
	
	private List<BakedQuad> getCrystalQuads() {
		// Since we cannot bake in preInit() we do lazy baking of the model as soon as we need it for rendering
		if (bakedModelCrystal == null) {
			final ResourceLocation resourceLocation = new ResourceLocation(WarpDrive.MODID, "block/energy/reactor_crystal.obj");
			final IModel model = RenderCommons.getModel(resourceLocation);
			bakedModelCrystal = model.bake(TRSRTransformation.identity(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
		}
		quadsCrystal = bakedModelCrystal.getQuads(null, null, 0L);
		return quadsCrystal;
	}
	
	private List<BakedQuad> getMatterQuads() {
		// Since we cannot bake in preInit() we do lazy baking of the model as soon as we need it for rendering
		if (bakedModelMatter == null) {
			final ResourceLocation resourceLocation = new ResourceLocation(WarpDrive.MODID, "block/energy/reactor_matter.obj");
			final IModel model = RenderCommons.getModel(resourceLocation);
			bakedModelMatter = model.bake(TRSRTransformation.identity(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
		}
		quadsMatter = bakedModelMatter.getQuads(null, null, 0L);
		return quadsMatter;
	}
	
	@Override
	public void render(final TileEntityEnanReactorCore tileEntityEnanReactorCore, final double x, final double y, final double z,
	                   final float partialTicks, final int destroyStage, final float alpha) {
		if (!tileEntityEnanReactorCore.getWorld().isBlockLoaded(tileEntityEnanReactorCore.getPos(), false)) {
			return;
		}
		if (quadsCrystal == null) {
			quadsCrystal = getCrystalQuads();
		}
		if (quadsMatter == null) {
			quadsMatter = getMatterQuads();
		}
		final Tessellator tessellator = Tessellator.getInstance();
		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();
		
		final double yCore = y + tileEntityEnanReactorCore.client_yCore + partialTicks * tileEntityEnanReactorCore.client_yCoreSpeed_mPerTick;
		GlStateManager.translate(x + 0.5D, yCore, z + 0.5D);
		
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.enableBlend();
		// GlStateManager.disableCull();
		RenderHelper.disableStandardItemLighting();
		
		// render the crystal
		GlStateManager.enableLighting();
		Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		final BufferBuilder worldRenderer = tessellator.getBuffer();
		
		final float rotationCrystal = tileEntityEnanReactorCore.client_rotationCore_deg + partialTicks * tileEntityEnanReactorCore.client_rotationSpeedCore_degPerTick;
		GlStateManager.rotate(rotationCrystal, 0.0F, 1.0F, 0.0F);
		
		worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
		RenderCommons.renderModelTESR(quadsCrystal, worldRenderer, tileEntityEnanReactorCore.getWorld().getCombinedLight(tileEntityEnanReactorCore.getPos(), 15));
		tessellator.draw();
		
		// render the matter cloud
		if (tileEntityEnanReactorCore.client_radiusMatter_m > 0.0F) {
			GlStateManager.disableLighting();
			
			// main model
			final float radiusMatter = tileEntityEnanReactorCore.client_radiusMatter_m + partialTicks * tileEntityEnanReactorCore.client_radiusSpeedMatter_mPerTick;
			final float heightMatter = Math.max(0.75F, radiusMatter / 2.0F);
			GlStateManager.scale(radiusMatter, heightMatter, radiusMatter);
			
			final float rotationMatter = tileEntityEnanReactorCore.client_rotationMatter_deg + partialTicks * tileEntityEnanReactorCore.client_rotationSpeedMatter_degPerTick
			                             - rotationCrystal;
			GlStateManager.rotate(rotationMatter, 0.0F, 1.0F, 0.0F);
			
			worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
			RenderCommons.renderModelTESR(quadsMatter, worldRenderer, tileEntityEnanReactorCore.getWorld().getCombinedLight(tileEntityEnanReactorCore.getPos(), 15));
			tessellator.draw();
			
			// surface model, slightly bigger
			GlStateManager.scale(1.1F, 1.05F, 1.1F);
			final float rotationSurface = 0.5F * tileEntityEnanReactorCore.client_rotationSpeedMatter_degPerTick;
			GlStateManager.rotate(rotationSurface, 0.0F, 1.0F, 0.0F);
			
			worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
			RenderCommons.renderModelTESR(quadsMatter, worldRenderer, tileEntityEnanReactorCore.getWorld().getCombinedLight(tileEntityEnanReactorCore.getPos(), 15));
			tessellator.draw();
			
			// core model, slightly smaller
			GlStateManager.scale(1.1F, 1.05F, 1.1F);
			final float rotationCore = -0.75F * tileEntityEnanReactorCore.client_rotationSpeedMatter_degPerTick;
			GlStateManager.rotate(rotationCore, 0.0F, 1.0F, 0.0F);
			
			worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
			RenderCommons.renderModelTESR(quadsMatter, worldRenderer, tileEntityEnanReactorCore.getWorld().getCombinedLight(tileEntityEnanReactorCore.getPos(), 15));
			tessellator.draw();
		}
		
		worldRenderer.setTranslation(0.0D, 0.0D, 0.0D);
		
		RenderHelper.enableStandardItemLighting();
		GlStateManager.disableBlend();
		// GlStateManager.enableCull();
		GlStateManager.popMatrix();
		GlStateManager.popAttrib();
	}
}
