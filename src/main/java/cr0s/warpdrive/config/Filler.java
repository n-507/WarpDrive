package cr0s.warpdrive.config;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IXmlRepresentableUnit;
import cr0s.warpdrive.data.JumpBlock;

import javax.annotation.Nonnull;

import net.minecraft.util.math.BlockPos;
import org.w3c.dom.Element;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Represents a single filler block.
 **/
public class Filler implements IXmlRepresentableUnit {
	
	public static final Filler DEFAULT;
	static {
		DEFAULT = new Filler();
		DEFAULT.name           = "-default-";
		DEFAULT.block          = Blocks.AIR;
		DEFAULT.metadata       = 0;
		DEFAULT.tagCompound    = null;
	}
	
	private String name;
	public Block block;
	public int metadata;
	public NBTTagCompound tagCompound = null;
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
	
	public Filler() {
	}
	
	@Override
	public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
		
		// Check there is a block name
		if (!element.hasAttribute("block")) {
			throw new InvalidXmlException(String.format("Filler %s is missing a block attribute!",
			                                            element));
		}
		
		final String nameBlock = element.getAttribute("block");
		block = Block.getBlockFromName(nameBlock);
		if (block == null) {
			WarpDrive.logger.warn(String.format("Skipping missing block %s",
			                                    nameBlock));
			return false;
		}
		
		// Get metadata attribute, defaults to 0
		metadata = 0;
		final String stringMetadata = element.getAttribute("metadata");
		if (!stringMetadata.isEmpty()) {
			try {
				metadata = Integer.parseInt(stringMetadata);
			} catch (final NumberFormatException exception) {
				throw new InvalidXmlException(String.format("Invalid metadata for block %s: %s",
				                                            nameBlock, stringMetadata));
			}
		}
		
		// Get nbt attribute, default to null/none
		tagCompound = null;
		final String stringNBT = element.getAttribute("nbt");
		if (!stringNBT.isEmpty()) {
			try {
				tagCompound = JsonToNBT.getTagFromJson(stringNBT);
			} catch (final NBTException exception) {
				WarpDrive.logger.error(exception.getMessage());
				throw new InvalidXmlException(String.format("Invalid nbt for block %s: %s",
				                                            nameBlock, stringNBT));
			}
		}
		
		name = nameBlock + "@" + metadata + "{" + tagCompound + "}";
		
		return true;
	}
	
	public void setBlock(final World world, final BlockPos blockPos) {
		JumpBlock.setBlockNoLight(world, blockPos, block.getStateFromMeta(metadata), 2);
		
		if (tagCompound != null) {
			final TileEntity tileEntity = world.getTileEntity(blockPos);
			if (tileEntity == null) {
				WarpDrive.logger.error(String.format("No TileEntity found for Filler %s %s",
				                                     getName(),
				                                     Commons.format(world, blockPos)));
				return;
			}
			
			final NBTTagCompound nbtTagCompoundTileEntity = new NBTTagCompound();
			tileEntity.writeToNBT(nbtTagCompoundTileEntity);
			
			for (final Object key : tagCompound.getKeySet()) {
				if (key instanceof String) {
					nbtTagCompoundTileEntity.setTag((String) key, tagCompound.getTag((String) key));
				}
			}
			
			tileEntity.onChunkUnload();
			tileEntity.readFromNBT(nbtTagCompoundTileEntity);
			tileEntity.validate();
			tileEntity.markDirty();
			
			JumpBlock.refreshBlockStateOnClient(world, blockPos);
		}
	}
	
	@Override
	public IXmlRepresentableUnit constructor() {
		return new Filler();
	}
	
	@Override
	public boolean equals(final Object object) {
		return object instanceof Filler
			&& (block == null || block.equals(((Filler)object).block))
			&& metadata == ((Filler)object).metadata
			&& (tagCompound == null || tagCompound.equals(((Filler)object).tagCompound));
	}
	
	@Override
	public String toString() {
		return "Filler(" + block.getTranslationKey() + "@" + metadata + ")";
	}

	@Override
	public int hashCode() {
		return Block.getIdFromBlock(block) * 16 + metadata + (tagCompound == null ? 0 : tagCompound.hashCode() * 4096 * 16);
	}
}
