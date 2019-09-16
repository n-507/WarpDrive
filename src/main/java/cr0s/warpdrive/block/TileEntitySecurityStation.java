package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.api.computer.ISecurityStation;
import cr0s.warpdrive.block.TileEntityAbstractMachine;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.DamageSource;

import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.Optional;

public class TileEntitySecurityStation extends TileEntityAbstractMachine implements ISecurityStation {
	
	// persistent properties
	public final ArrayList<String> players = new ArrayList<>();
	
	public TileEntitySecurityStation() {
		super();
		
		// (abstract) peripheralName = "warpdriveSecurityStation";
		addMethods(new String[] {
				"getAttachedPlayers"
				});
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		players.clear();
		final NBTTagList tagListPlayers = tagCompound.getTagList("players", Constants.NBT.TAG_STRING);
		for (int index = 0; index < tagListPlayers.tagCount(); index++) {
			final String namePlayer = tagListPlayers.getStringTagAt(index);
			players.add(namePlayer);
		}
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		final NBTTagList tagListPlayers = new NBTTagList();
		for (final String namePlayer : players) {
			final NBTTagString tagStringPlayer = new NBTTagString(namePlayer);
			tagListPlayers.appendTag(tagStringPlayer);
		}
		tagCompound.setTag("players", tagListPlayers);
		
		return tagCompound;
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		tagCompound.removeTag("players");
		
		return tagCompound;
	}
	
	@Override
	public WarpDriveText getStatus() {
		return super.getStatus()
		            .append(null, "warpdrive.security_station.guide.registered_players",
		                    getAttachedPlayersList());
	}
	
	public WarpDriveText attachPlayer(final EntityPlayer entityPlayer) {
		for (int i = 0; i < players.size(); i++) {
			final String name = players.get(i);
			
			if (entityPlayer.getName().equals(name)) {
				players.remove(i);
				WarpDriveText text = Commons.getChatPrefix(getBlockType());
				text.appendSibling(new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.security_station.guide.player_unregistered",
				                                     getAttachedPlayersList()));
				return text;
			}
		}
		
		entityPlayer.attackEntityFrom(DamageSource.GENERIC, 1);
		players.add(entityPlayer.getName());
		WarpDriveText text = Commons.getChatPrefix(getBlockType());
		text.appendSibling(new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.security_station.guide.player_registered",
		                                     getAttachedPlayersList()));
		return text;
	}
	
	protected String getAttachedPlayersList() {
		if (players.isEmpty()) {
			return "<nobody>";
		}
		
		final StringBuilder list = new StringBuilder();
		
		for (int i = 0; i < players.size(); i++) {
			final String nick = players.get(i);
			list.append(nick).append(((i == players.size() - 1) ? "" : ", "));
		}
		
		return list.toString();
	}
	
	public String getFirstOnlinePlayer() {
		if (players == null || players.isEmpty()) {// no crew defined
			return null;
		}
		
		for (final String namePlayer : players) {
			final EntityPlayer entityPlayer = Commons.getOnlinePlayerByName(namePlayer);
			if (entityPlayer != null) {// crew member is online
				return namePlayer;
			}
		}
		
		// all cleared
		return null;
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getAttachedPlayers() {
		final StringBuilder list = new StringBuilder();
		
		if (!players.isEmpty()) {
			for (int i = 0; i < players.size(); i++) {
				final String nick = players.get(i);
				list.append(nick).append((i == players.size() - 1) ? "" : ",");
			}
		}
		
		return new Object[] { list.toString(), players.toArray() };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getAttachedPlayers(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getAttachedPlayers();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "getAttachedPlayers":
			return getAttachedPlayers();
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
}
