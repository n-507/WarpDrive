package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class CommandRender extends AbstractCommand {
	
	@Nonnull
	@Override
	public String getName() {
		return "wrender";
	}
	
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Nonnull
	@Override
	public String getUsage(@Nonnull final ICommandSender commandSender) {
		return "/" + getName();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void execute(@Nonnull final MinecraftServer server, @Nonnull final ICommandSender commandSender, @Nonnull final String[] args) {
		// parse arguments
		if (args.length > 0) {
			Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
			return;
			
		}
		
		final EntityPlayerMP entityPlayer = commandSender instanceof EntityPlayerMP ? (EntityPlayerMP) commandSender : null;
		
		// validate context
		if (entityPlayer == null) {
			Commons.addChatMessage(commandSender, new WarpDriveText().append(getPrefix())
			                                                         .append(Commons.styleWarning, "warpdrive.command.player_required") );
			return;
		}
		
		// evaluate sub command
		final World world = entityPlayer.getEntityWorld();
		BlockPos blockPos = entityPlayer.getPosition();
		
		//noinspection ConstantConditions
		if (world == null || blockPos == null) {
			Commons.addChatMessage(commandSender, new WarpDriveText().append(getPrefix())
			                                                         .append(Commons.styleWarning, "warpdrive.command.invalid_location") );
			return;
		}
		IBlockState blockState = world.getBlockState(blockPos);
		if (blockState.getBlock().isAir(blockState, world, blockPos)) {
			blockPos = blockPos.down();
			blockState = world.getBlockState(blockPos);
		}
		
		Commons.addChatMessage(commandSender, new WarpDriveText().append(getPrefix())
		                                                         .appendInLine(Commons.styleCorrect, "Dumping render details %s", 
		                                                                 Commons.format(world, blockPos) ) );
		final Block block = blockState.getBlock();
		
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.styleValue, "Blockstate is %s (%s)",
		                                                                 blockState,
		                                                                  block.getMetaFromState(blockState) ));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.styleValue, "Light opacity is %s / useNeighborBrightness is %s",
		                                                                 block.getLightOpacity(blockState),
		                                                                 block.getUseNeighborBrightness(blockState) ));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.styleValue, "isFullBlock is %s / isFullCube is %s",
		                                                                 block.isFullBlock(blockState),
		                                                                 block.isFullCube(blockState) ));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.styleValue, "isBlockNormalCube is %s / isNormalCube is %s",
		                                                                 block.isBlockNormalCube(blockState),
		                                                                 block.isNormalCube(blockState) ));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.styleValue, "isTopSolid is %s / causesSuffocation is %s",
		                                                                 block.isTopSolid(blockState),
		                                                                 block.causesSuffocation(blockState) ));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.styleValue, "Material isOpaque %s / blocksLight %s",
		                                                                 blockState.getMaterial().isOpaque(),
		                                                                 blockState.getMaterial().blocksLight() ));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.styleValue, "Material isLiquid %s / isSolid %s",
		                                                                 blockState.getMaterial().isLiquid(),
		                                                                 blockState.getMaterial().isSolid() ));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.styleValue, "isOpaqueCube is %s  / isTranslucent %s",
		                                                                 blockState.isOpaqueCube(),
		                                                                 world.isRemote ? blockState.isTranslucent() : "???" ));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.styleValue, "renderLayer is %s  / renderType is %s",
		                                                                 world.isRemote ? block.getRenderLayer() : "???",
		                                                                 block.getRenderType(blockState) ));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.styleValue, "isSideSolid D %s, U %s, N %s, S %s, W %s, E %s",
		                                                                 block.isSideSolid(blockState, world, blockPos, EnumFacing.DOWN),
		                                                                 block.isSideSolid(blockState, world, blockPos, EnumFacing.UP),
		                                                                 block.isSideSolid(blockState, world, blockPos, EnumFacing.NORTH),
		                                                                 block.isSideSolid(blockState, world, blockPos, EnumFacing.SOUTH),
		                                                                 block.isSideSolid(blockState, world, blockPos, EnumFacing.WEST),
		                                                                 block.isSideSolid(blockState, world, blockPos, EnumFacing.EAST) ));
	}
}
