package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.block.ItemBlockAbstractBase;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumFrameType;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockEnanReactorFramePlain extends BlockAbstractBase {
	
	public BlockEnanReactorFramePlain(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.ROCK);
		
		setHardness(WarpDriveConfig.HULL_HARDNESS[enumTier.getIndex()] / 3.0F);
		setResistance(WarpDriveConfig.HULL_BLAST_RESISTANCE[enumTier.getIndex()] / 3.0F * 5.0F / 3.0F);
		setTranslationKey("warpdrive.energy.enan_reactor_frame." + enumTier.getName());
		
		setDefaultState(getDefaultState()
				                .withProperty(BlockProperties.FRAME, EnumFrameType.PLAIN)
		               );
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return this.getDefaultState().withProperty(BlockProperties.FRAME, EnumFrameType.byMetadata(metadata));
	}
	
	@Override
	public int getMetaFromState(final IBlockState blockState) {
		return blockState.getValue(BlockProperties.FRAME).getMetadata();
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockProperties.FRAME);
	}
	
	@Override
	public int damageDropped(final IBlockState blockState) {
		return blockState.getValue(BlockProperties.FRAME).getMetadata();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(final CreativeTabs creativeTab, final NonNullList<ItemStack> list) {
		for (final EnumFrameType enumFrameType : EnumFrameType.values()) {
			list.add(new ItemStack(this, 1, enumFrameType.getMetadata()));
		}
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockAbstractBase(this, true, true);
	}
}