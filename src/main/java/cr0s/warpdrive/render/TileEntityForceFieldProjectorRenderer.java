package cr0s.warpdrive.render;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.forcefield.TileEntityForceFieldProjector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.model.TRSRTransformation;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class TileEntityForceFieldProjectorRenderer extends TileEntitySpecialRenderer<TileEntityForceFieldProjector> {
	
	private IBakedModel bakedModel;
	
	private IBakedModel getBakedModel() {
		// Since we cannot bake in preInit() we do lazy baking of the model as soon as we need it for rendering
		if (bakedModel == null) {
			final ResourceLocation resourceLocation = new ResourceLocation(WarpDrive.MODID, "block/forcefield/projector_ring.obj");
			final IModel model = RenderCommons.getModel(resourceLocation);
			bakedModel = model.bake(TRSRTransformation.identity(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
		}
		return bakedModel;
	}
	
	private static List<BakedQuad> quads;
	
	@Override
	public void render(final TileEntityForceFieldProjector tileEntityForceFieldProjector, final double x, final double y, final double z,
	                   final float partialTicks, final int destroyStage, final float alpha) {
		if (!tileEntityForceFieldProjector.getWorld().isBlockLoaded(tileEntityForceFieldProjector.getPos(), false)) {
			return;
		}
		if (quads == null) {
			quads = getBakedModel().getQuads(null, null, 0L);
		}
		final Tessellator tessellator = Tessellator.getInstance();
		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();
		
		GlStateManager.translate(x + 0.5D, y + 0.5D, z + 0.5D);
		switch (tileEntityForceFieldProjector.enumFacing) {
			case DOWN : GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F); break;
			case UP   : break;
			case NORTH: GlStateManager.rotate(+90.0F, 1.0F, 0.0F, 0.0F); GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F); break;
			case SOUTH: GlStateManager.rotate(+90.0F, 1.0F, 0.0F, 0.0F); break;
			case WEST : GlStateManager.rotate(+90.0F, 1.0F, 0.0F, 0.0F); GlStateManager.rotate(+90.0F, 0.0F, 0.0F, 1.0F); break;
			case EAST : GlStateManager.rotate(+90.0F, 1.0F, 0.0F, 0.0F); GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F); break;
			default: break;
		}
		
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.enableBlend();
		// GlStateManager.disableCull();
		RenderHelper.disableStandardItemLighting();
		GlStateManager.enableLighting();
		// @TODO setLightmapDisabled
		
		final float wheelRotation = tileEntityForceFieldProjector.rotation_deg + partialTicks * tileEntityForceFieldProjector.rotationSpeed_degPerTick;
		GlStateManager.rotate(wheelRotation, 0.0F, 1.0F, 0.0F);
		
		Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		final BufferBuilder worldRenderer = tessellator.getBuffer();
		worldRenderer.setTranslation(-0.5, -0.5, -0.5);
		worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
		
		RenderCommons.renderModelTESR(quads, worldRenderer, tileEntityForceFieldProjector.getWorld().getCombinedLight(tileEntityForceFieldProjector.getPos(), 15));
		
		tessellator.draw();
		worldRenderer.setTranslation(0.0D, 0.0D, 0.0D);
		
		RenderHelper.enableStandardItemLighting();
		GlStateManager.disableBlend();
		// GlStateManager.enableCull();
		GlStateManager.popMatrix();
		GlStateManager.popAttrib();
	}
}
