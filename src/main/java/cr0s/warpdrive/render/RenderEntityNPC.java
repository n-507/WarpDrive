package cr0s.warpdrive.render;

import cr0s.warpdrive.entity.EntityNPC;

import javax.annotation.Nonnull;

import java.io.File;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class RenderEntityNPC extends RenderBiped<EntityNPC> {
	
	public RenderEntityNPC(@Nonnull final RenderManager renderManager, @Nonnull final ModelBiped modelBiped, final float shadowSize) {
		super(renderManager, modelBiped, shadowSize);
	}
	
	@Override
	protected void preRenderCallback(final EntityNPC entityNPC, final float partialTickTime) {
		super.preRenderCallback(entityNPC, partialTickTime);
		
		final float sizeScale = entityNPC.getSizeScale();
		GlStateManager.scale(sizeScale, sizeScale, sizeScale);
	}
	
	@Override
	protected ResourceLocation getEntityTexture(@Nonnull final EntityNPC entityNPC) {
		
		final String textureString = entityNPC.getTextureString();
		if (!textureString.isEmpty()) {
			String path = "WarpDriveConfig.config.getConfigFile().getAbsolutePath()";
			path = path.substring(0, path.length() - 4 - 10);
			final File fileTexture = new File(path + "/assets/npctextures/" + textureString + ".png");
			
			if (fileTexture.exists()) {
				return new ResourceLocation("npctextures", textureString + ".png");
			}
		}
		
		return super.getEntityTexture(entityNPC);
	}
	
	@Override
	public void doRender(@Nonnull final EntityNPC entityNPC, final double x, final double y, final double z, final float entityYaw, final float partialTicks) {
		super.doRender(entityNPC, x, y, z, entityYaw, partialTicks);
	}
}
