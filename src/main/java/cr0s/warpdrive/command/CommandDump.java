package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.InventoryWrapper;

import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Collection;

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
		final BlockPos blockPos = commandSender.getPosition();
		
		//noinspection ConstantConditions
		if (world == null || blockPos == null) {
			Commons.addChatMessage(commandSender, getPrefix().appendSibling(new TextComponentTranslation("warpdrive.command.invalid_location").setStyle(Commons.styleWarning)));
			return;
		}
		
		// parse arguments
		if (args.length != 0) {
			Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
			return;
		}
		
		// validate
		final Collection<Object> inventories = InventoryWrapper.getConnectedInventories(world, blockPos);
		if (inventories.isEmpty()) {
			Commons.addChatMessage(commandSender, getPrefix().appendSibling(new TextComponentTranslation("warpdrive.command.no_container").setStyle(Commons.styleWarning)));
			return;
		}
		final Object inventory = inventories.iterator().next();
		
		// actually dump
		WarpDrive.logger.info(String.format("Dumping content from container %s:",
		                                    Commons.format(world, blockPos)));
		for (int indexSlot = 0; indexSlot < InventoryWrapper.getSize(inventory); indexSlot++) {
			final ItemStack itemStack = InventoryWrapper.getStackInSlot(inventory, indexSlot);
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
}
