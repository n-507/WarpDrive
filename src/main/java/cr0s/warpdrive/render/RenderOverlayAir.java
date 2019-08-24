package cr0s.warpdrive.render;

import cr0s.warpdrive.BreathingManager;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.CelestialObject;

import javax.annotation.Nonnull;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderOverlayAir {
	
	private static final int WARNING_ON_JOIN_TICKS = 20 * 20;
	
	private static final Minecraft minecraft = Minecraft.getMinecraft();
	
	private static float ratioPreviousAir = 1.0F;
	private static long timePreviousAir = 0;
	
	private void renderAir(final int width, final int height) {
		// get player
		final EntityPlayer entityPlayer = minecraft.player;
		if (entityPlayer == null) {
			return;
		}
		final int x = MathHelper.floor(entityPlayer.posX);
		final int y = MathHelper.floor(entityPlayer.posY);
		final int z = MathHelper.floor(entityPlayer.posZ);
		
		// get celestial object
		final CelestialObject celestialObject = CelestialObjectManager.get(entityPlayer.world, x, z);
		if (celestialObject == null || celestialObject.hasAtmosphere()) {// skip (no display) if environment is breathable
			return;
		}
		
		// get air stats
		final boolean hasVoidNearby = isVoid(entityPlayer.world, x, y, z)
		                           || isVoid(entityPlayer.world, x - 2, y, z)
		                           || isVoid(entityPlayer.world, x + 2, y, z)
		                           || isVoid(entityPlayer.world, x, y, z - 2)
		                           || isVoid(entityPlayer.world, x, y, z + 2);
		final boolean hasValidSetup = BreathingManager.hasValidSetup(entityPlayer);
		final float ratioAirReserve = BreathingManager.getAirReserveRatio(entityPlayer);
		
		// start rendering
		GlStateManager.enableBlend();
		
		// show splash message
		int alpha = 255;
		if (hasVoidNearby || entityPlayer.ticksExisted < WARNING_ON_JOIN_TICKS) {
			if (!hasValidSetup) {
				alpha = RenderCommons.drawSplashAlarm(width, height, "warpdrive.breathing.alarm", "warpdrive.breathing.invalid_setup");
			} else if (ratioAirReserve <= 0.0F) {
				alpha = RenderCommons.drawSplashAlarm(width, height, "warpdrive.breathing.alarm", "warpdrive.breathing.no_air");
			} else if (ratioAirReserve < 0.15F) {
				alpha = RenderCommons.drawSplashAlarm(width, height, "warpdrive.breathing.alarm", "warpdrive.breathing.low_reserve");
			}
		}
		
		// restore texture
		minecraft.getTextureManager().bindTexture(Gui.ICONS);
		
		// position right above food bar
		final int left = width / 2 + 91;
		final int top = height - GuiIngameForge.right_height;
		
		// draw animated air bubble
		final long timeWorld =  entityPlayer.world.getTotalWorldTime();
		if (ratioAirReserve != ratioPreviousAir) {
			timePreviousAir = timeWorld;
			ratioPreviousAir = ratioAirReserve;
		}
		final long timeDelta = timeWorld - timePreviousAir;
		if (timeDelta >= 0 && timeDelta <= 8) {
			RenderCommons.drawTexturedModalRect(left - 9, top, 25, 18, 9, 9, 100);
		} else if (timeDelta < 0 || timeDelta > 16) {
			RenderCommons.drawTexturedModalRect(left - 9, top, 16, 18, 9, 9, 100);
		}
		
		// draw air level bar
		final int full = MathHelper.ceil(ratioAirReserve * 71.0D);
		RenderCommons.drawTexturedModalRect(left - 81, top + 2, 20, 84, 71, 5, 100);
		final float fRed  ;
		final float fGreen;
		final float fBlue ;
		final float fAlpha;
		if (alpha == 255) {
			fRed   = 1.0F;
			fGreen = 1.0F;
			fBlue  = 1.0F;
			fAlpha = 1.0F;
		} else {
			final float factor = 1.0F - alpha / 255.0F;
			fRed   = 1.0F;
			fGreen = 0.2F + 0.8F * factor;
			fBlue  = 0.2F + 0.8F * factor;
			fAlpha = 1.0F;
		}
		RenderCommons.drawTexturedModalRect(left - 10 - full, top + 2, 91 - full, 89, full, 5, 100, fRed, fGreen, fBlue, fAlpha);
		
		// close rendering
		GuiIngameForge.right_height += 10;
		
		GlStateManager.disableBlend();
	}
	
	private boolean isVoid(final IBlockAccess blockAccess, final int x, final int y, final int z) {
		final BlockPos blockPos = new BlockPos(x, y, z);
		final IBlockState blockState = blockAccess.getBlockState(blockPos);
		return blockState.getBlock().isAir(blockState, blockAccess, blockPos) && !BreathingManager.isAirBlock(blockState.getBlock());
	}
	
	@SubscribeEvent
	public void onRender(@Nonnull final RenderGameOverlayEvent.Pre event) {
		if (event.getType() == ElementType.AIR) {
			renderAir(event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight());
		}
	}
}