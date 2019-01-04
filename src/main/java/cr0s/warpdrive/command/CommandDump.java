package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CommandDump extends AbstractCommand {
	
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Nonnull
	@Override
	public String getName() {
		return "wdump";
	}
	
	@Override
	public void execute(@Nonnull final MinecraftServer server, @Nonnull final ICommandSender commandSender, @Nonnull final String[] args) {
		final World world = commandSender.getEntityWorld();
		final BlockPos coordinates = commandSender.getPosition();
		
		//noinspection ConstantConditions
		if (world == null || coordinates == null) {
			Commons.addChatMessage(commandSender, getPrefix().appendSibling(new TextComponentTranslation("warpdrive.command.invalid_location").setStyle(Commons.styleWarning)));
			return;
		}
		int x = coordinates.getX();
		int y = coordinates.getY();
		int z = coordinates.getZ();
		
		// parse arguments
		if (args.length != 0) {
			Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
			return;
		}
		
		// validate
		IInventory inventory = null;
		for (final EnumFacing direction : EnumFacing.values()) {
			inventory = getInventory(world, x + direction.getXOffset(), y + direction.getYOffset(), z + direction.getZOffset());
			if (inventory != null) {
				x += direction.getXOffset();
				y += direction.getYOffset();
				z += direction.getZOffset();
				break;
			}
		}
		if (inventory == null) {
			Commons.addChatMessage(commandSender, getPrefix().appendSibling(new TextComponentTranslation("warpdrive.command.no_container").setStyle(Commons.styleWarning)));
			return;
		}
		
		// actually dump
		WarpDrive.logger.info(String.format("Dumping content from container %s:",
		                                    Commons.format(world, x, y, z)));
		for (int indexSlot = 0; indexSlot < inventory.getSizeInventory(); indexSlot++) {
			final ItemStack itemStack = inventory.getStackInSlot(indexSlot);
			if (itemStack != ItemStack.EMPTY && !itemStack.isEmpty()) {
				final ResourceLocation uniqueIdentifier = itemStack.getItem().getRegistryName();
				assert uniqueIdentifier != null;
				final String stringDamage = itemStack.getItemDamage() == 0 ? "" : String.format(" damage=\"%d\"", itemStack.getItemDamage());
				final String stringNBT = !itemStack.hasTagCompound() ? "" : String.format(" nbt=\"%s\"", itemStack.getTagCompound());
				WarpDrive.logger.info(String.format("Slot %3d is <loot item=\"%s:%s\"%s minQuantity=\"%d\" minQuantity=\"%d\"%s weight=\"1\" /><!-- %s -->",
				                                    indexSlot,
				                                    uniqueIdentifier.getNamespace(), uniqueIdentifier.getPath(),
				                                    stringDamage,
				                                    itemStack.getCount(), itemStack.getCount(),
				                                    stringNBT,
				                                    itemStack.getDisplayName()));
			}
		}
	}
	
	@Nonnull
	@Override
	public String getUsage(@Nonnull final ICommandSender icommandsender) {
		return "/wdump: write loot table in console for item container below or next to player";
	}
	
	@Nullable
	private IInventory getInventory(@Nonnull final World world, final int x, final int y, final int z) {
		final BlockPos blockPos = new BlockPos(x, y, z);
		final IBlockState blockState = world.getBlockState(blockPos);
		if (blockState.getBlock() instanceof ITileEntityProvider) {
			if (blockState.getBlock().hasTileEntity(blockState)) {
				final TileEntity tileEntity = world.getTileEntity(blockPos);
				if (tileEntity instanceof IInventory) {
					if (((IInventory) tileEntity).getSizeInventory() > 0) {
						return (IInventory) tileEntity;
					}
				}
			}
		}
		return null;
	}
}
