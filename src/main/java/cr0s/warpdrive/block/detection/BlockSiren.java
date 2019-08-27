package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.BlockAbstractRotatingContainer;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockSiren extends BlockAbstractRotatingContainer {
	
	private final boolean isIndustrial;
	
	public BlockSiren(final String registryName, final EnumTier enumTier, final boolean isIndustrial) {
		super(registryName, enumTier, Material.IRON);
		
		this.isIndustrial = isIndustrial;
		setTranslationKey("warpdrive.detection.siren_" + (isIndustrial ? "industrial" : "military") + "." + enumTier.getName());
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntitySiren();
	}
	
	public boolean getIsIndustrial() {
		return isIndustrial;
	}
	
	@Override
	public int damageDropped(final IBlockState blockState) {
		return 0;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(@Nonnull final ItemStack itemStack, @Nullable final World world,
	                           @Nonnull final List<String> list, @Nullable final ITooltipFlag advancedItemTooltips) {
		super.addInformation(itemStack, world, list, advancedItemTooltips);
		
		final int range = MathHelper.floor(WarpDriveConfig.SIREN_RANGE_BLOCKS_BY_TIER[enumTier.getIndex()]);
		final String unlocalizedName_withoutTier = getTranslationKey().replace("." + enumTier.getName(), "");
		Commons.addTooltip(list, new TextComponentTranslation(unlocalizedName_withoutTier + ".tooltip.usage",
		                                                      new WarpDriveText(Commons.getStyleValue(), range) ).getFormattedText());
	}
}
