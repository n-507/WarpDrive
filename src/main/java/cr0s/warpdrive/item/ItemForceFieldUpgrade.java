package cr0s.warpdrive.item;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.forcefield.BlockForceFieldProjector;
import cr0s.warpdrive.block.forcefield.BlockForceFieldRelay;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumForceFieldUpgrade;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemForceFieldUpgrade extends ItemAbstractBase {
	
	private static ItemStack[] itemStackCache;
	
	public ItemForceFieldUpgrade(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier);
		
		setHasSubtypes(true);
		setTranslationKey("warpdrive.force_field.upgrade");
		
		itemStackCache = new ItemStack[EnumForceFieldUpgrade.length];
	}
	
	public static ItemStack getItemStack(final EnumForceFieldUpgrade forceFieldUpgrade) {
		if (forceFieldUpgrade != null) {
			final int damage = forceFieldUpgrade.ordinal();
			if (itemStackCache[damage] == null) {
				itemStackCache[damage] = new ItemStack(WarpDrive.itemForceFieldUpgrade, 1, damage);
			}
			return itemStackCache[damage];
		}
		return null;
	}
	
	public static ItemStack getItemStackNoCache(final EnumForceFieldUpgrade forceFieldUpgrade, final int amount) {
		return new ItemStack(WarpDrive.itemForceFieldUpgrade, amount, forceFieldUpgrade.ordinal());
	}

	@Nonnull
	@Override
	public String getTranslationKey(final ItemStack itemStack) {
		final int damage = itemStack.getItemDamage();
		if (damage >= 0 && damage < EnumForceFieldUpgrade.length) {
			return getTranslationKey() + "." + EnumForceFieldUpgrade.get(damage).getName();
		}
		return getTranslationKey();
	}
	
	@Override
	public void getSubItems(@Nonnull final CreativeTabs creativeTab, @Nonnull final NonNullList<ItemStack> list) {
		if (!isInCreativeTab(creativeTab)) {
			return;
		}
		for(final EnumForceFieldUpgrade enumForceFieldUpgrade : EnumForceFieldUpgrade.values()) {
			if (enumForceFieldUpgrade != EnumForceFieldUpgrade.NONE) {
				list.add(new ItemStack(this, 1, enumForceFieldUpgrade.ordinal()));
			}
		}
	}
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public ModelResourceLocation getModelResourceLocation(final ItemStack itemStack) {
		final int damage = itemStack.getItemDamage();
		ResourceLocation resourceLocation = getRegistryName();
		assert resourceLocation != null;
		if (damage >= 0 && damage < EnumComponentType.length) {
			resourceLocation = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath() + "-" + EnumForceFieldUpgrade.get(damage).getName());
		}
		return new ModelResourceLocation(resourceLocation, "inventory");
	}
	
	@Override
	public boolean doesSneakBypassUse(final ItemStack itemStack, final IBlockAccess blockAccess, final BlockPos blockPos, final EntityPlayer player) {
		final Block block = blockAccess.getBlockState(blockPos).getBlock();
		return block instanceof BlockForceFieldRelay || block instanceof BlockForceFieldProjector || super.doesSneakBypassUse(itemStack, blockAccess, blockPos, player);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(@Nonnull final ItemStack itemStack, @Nullable final World world,
	                           @Nonnull final List<String> list, @Nullable final ITooltipFlag advancedItemTooltips) {
		super.addInformation(itemStack, world, list, advancedItemTooltips);
		
		Commons.addTooltip(list, "\n");
		
		final EnumForceFieldUpgrade forceFieldUpgrade = EnumForceFieldUpgrade.get(itemStack.getItemDamage());
		if (forceFieldUpgrade.maxCountOnProjector > 0) {
			Commons.addTooltip(list, new TextComponentTranslation("item.warpdrive.force_field.upgrade.tooltip.usage.projector").getFormattedText());
		}
		if (forceFieldUpgrade.maxCountOnRelay > 0) {
			Commons.addTooltip(list, new TextComponentTranslation("item.warpdrive.force_field.upgrade.tooltip.usage.relay").getFormattedText());
		}
		Commons.addTooltip(list, new TextComponentTranslation("item.warpdrive.force_field.upgrade.tooltip.usage.dismount").getFormattedText());
	}
}
