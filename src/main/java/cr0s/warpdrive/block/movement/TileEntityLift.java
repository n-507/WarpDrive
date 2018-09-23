package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.computer.ILift;
import cr0s.warpdrive.block.TileEntityAbstractEnergyConsumer;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumLiftMode;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.fml.common.Optional;

public class TileEntityLift extends TileEntityAbstractEnergyConsumer implements ILift {
	
	private static final double LIFT_GRAB_RADIUS = 0.4D;
	
	// persistent properties
	private EnumLiftMode mode = EnumLiftMode.INACTIVE;
	private EnumLiftMode computerMode = EnumLiftMode.REDSTONE;
	
	// computed properties
	private int updateTicks = 0;
	private boolean isActive = false;
	private boolean isValid = false;
	private int firstUncoveredY;
	
	public TileEntityLift() {
		super();
		
		IC2_sinkTier = 2;
		IC2_sourceTier = 2;
		peripheralName = "warpdriveLift";
		addMethods(new String[] {
				"mode",
				"state"
		});
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		updateTicks--;
		if (updateTicks < 0) {
			updateTicks = WarpDriveConfig.LIFT_UPDATE_INTERVAL_TICKS;
			
			// Switching mode
			if (  computerMode == EnumLiftMode.DOWN
			  || (computerMode == EnumLiftMode.REDSTONE && world.getRedstonePowerFromNeighbors(pos) > 0)) {
				mode = EnumLiftMode.DOWN;
			} else {
				mode = EnumLiftMode.UP;
			}
			
			isValid = isPassableBlock(pos.getY() + 1)
			       && isPassableBlock(pos.getY() + 2)
			       && isPassableBlock(pos.getY() - 1)
			       && isPassableBlock(pos.getY() - 2);
			isActive = isEnabled && isValid;
			
			final IBlockState blockState = world.getBlockState(pos);
			if (energy_getEnergyStored() < WarpDriveConfig.LIFT_ENERGY_PER_ENTITY || !isActive) {
				mode = EnumLiftMode.INACTIVE;
				if (blockState.getValue(BlockLift.MODE) != EnumLiftMode.INACTIVE) {
					world.setBlockState(pos, blockState.withProperty(BlockLift.MODE, EnumLiftMode.INACTIVE));
				}
				return;
			}
			
			if (blockState.getValue(BlockLift.MODE) != mode) {
				world.setBlockState(pos, blockState.withProperty(BlockLift.MODE, mode));
			}
			
			// Launch a beam: search non-air blocks under lift
			for (int ny = pos.getY() - 2; ny > 0; ny--) {
				if (!isPassableBlock(ny)) {
					firstUncoveredY = ny + 1;
					break;
				}
			}
			
			if (pos.getY() - firstUncoveredY >= 2) {
				if (mode == EnumLiftMode.UP) {
					PacketHandler.sendBeamPacket(world,
							new Vector3(pos.getX() + 0.5D, firstUncoveredY, pos.getZ() + 0.5D),
							new Vector3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D),
							0f, 1f, 0f, 40, 0, 100);
				} else if (mode == EnumLiftMode.DOWN) {
					PacketHandler.sendBeamPacket(world,
							new Vector3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D),
							new Vector3(pos.getX() + 0.5D, firstUncoveredY, pos.getZ() + 0.5D), 0f,
							0f, 1f, 40, 0, 100);
				}
				
				if (liftEntity()) {
					updateTicks = WarpDriveConfig.LIFT_ENTITY_COOLDOWN_TICKS;
				}
			}
		}
	}
	
	private boolean isPassableBlock(final int yPosition) {
		final BlockPos blockPos = new BlockPos(pos.getX(), yPosition, pos.getZ());
		final IBlockState blockState = world.getBlockState(blockPos);
		return blockState.getBlock() == Blocks.AIR
			|| world.isAirBlock(blockPos)
			|| blockState.getCollisionBoundingBox(world, blockPos) == null;
	}
	
	private boolean liftEntity() {
		final double xMin = pos.getX() + 0.5 - LIFT_GRAB_RADIUS;
		final double xMax = pos.getX() + 0.5 + LIFT_GRAB_RADIUS;
		final double zMin = pos.getZ() + 0.5 - LIFT_GRAB_RADIUS;
		final double zMax = pos.getZ() + 0.5 + LIFT_GRAB_RADIUS;
		boolean isTransferDone = false; 
		
		// Lift up
		if (mode == EnumLiftMode.UP) {
			final AxisAlignedBB aabb = new AxisAlignedBB(
					xMin, firstUncoveredY, zMin,
					xMax, pos.getY(), zMax);
			final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, aabb);
			for (final Entity entity : list) {
				if ( entity instanceof EntityLivingBase
				  && energy_consume(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, true)) {
					entity.setPositionAndUpdate(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
					PacketHandler.sendBeamPacket(world,
							new Vector3(pos.getX() + 0.5D, firstUncoveredY, pos.getZ() + 0.5D),
							new Vector3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D),
							1F, 1F, 0F, 40, 0, 100);
					world.playSound(null, pos, SoundEvents.LASER_HIGH, SoundCategory.AMBIENT, 4.0F, 1.0F);
					energy_consume(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, false);
					isTransferDone = true;
				}
			}
			
		} else if (mode == EnumLiftMode.DOWN) {
			final AxisAlignedBB aabb = new AxisAlignedBB(
					xMin, Math.min(firstUncoveredY + 4.0D, pos.getY()), zMin,
					xMax, pos.getY() + 2.0D, zMax);
			final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, aabb);
			for (final Entity entity : list) {
	  			if ( entity instanceof EntityLivingBase
            && energy_consume(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, true)) {
            entity.setPositionAndUpdate(pos.getX() + 0.5D, firstUncoveredY, pos.getZ() + 0.5D);
            PacketHandler.sendBeamPacket(world,
	  						new Vector3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D),
	  						new Vector3(pos.getX() + 0.5D, firstUncoveredY, pos.getZ() + 0.5D), 1F, 1F, 0F, 40, 0, 100);
	  				world.playSound(null, pos, SoundEvents.LASER_HIGH, SoundCategory.AMBIENT, 4.0F, 1.0F);
	  				energy_consume(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, false);
	  				isTransferDone = true;
				}
			}
		}
		
		return isTransferDone;
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		if (tagCompound.hasKey("mode")) {
			final byte byteValue = tagCompound.getByte("mode");
			mode = EnumLiftMode.get(Commons.clamp(0, 3, byteValue == -1 ? 3 : byteValue));
		}
		if (tagCompound.hasKey("computerMode")) {
			final byte byteValue = tagCompound.getByte("computerMode");
			computerMode = EnumLiftMode.get(Commons.clamp(0, 3, byteValue == -1 ? 3 : byteValue));
		}
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		tagCompound.setByte("mode", (byte) mode.ordinal());
		tagCompound.setByte("computerMode", (byte) computerMode.ordinal());
		return tagCompound;
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		tagCompound.removeTag("mode");
		return tagCompound;
	}
	
	@Override
	public int energy_getMaxStorage() {
		return WarpDriveConfig.LIFT_MAX_ENERGY_STORED;
	}
	
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		return true;
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getEnergyRequired() {
		return new Object[] { WarpDriveConfig.LIFT_ENERGY_PER_ENTITY };
	}
	
	@Override
	public Object[] mode(final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] instanceof String) {
			final String stringValue = (String) arguments[0];
			if (stringValue.equalsIgnoreCase("up")) {
				computerMode = EnumLiftMode.UP;
			} else if (stringValue.equalsIgnoreCase("down")) {
				computerMode = EnumLiftMode.DOWN;
			} else {
				computerMode = EnumLiftMode.REDSTONE;
			}
			markDirty();
		}
		
		return new Object[] { computerMode.getName() };
	}
	
	@Override
	public Object[] state() {
		final int energy = energy_getEnergyStored();
		final String status = getStatusHeaderInPureText();
		return new Object[] { status, isActive, energy, isValid, isEnabled, computerMode.getName() };
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] mode(final Context context, final Arguments arguments) {
		return mode(
			new Object[] {
				arguments.checkString(0)
			}
		);
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] state(final Context context, final Arguments arguments) {
		return state();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	public Object[] callMethod(@Nonnull final IComputerAccess computer, @Nonnull final ILuaContext context, final int method, @Nonnull final Object[] arguments) {
		final String methodName = CC_getMethodNameAndLogCall(method, arguments);
		
		switch (methodName) {
		case "mode":
			return mode(arguments);
			
		case "state":
			return state();
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
}
