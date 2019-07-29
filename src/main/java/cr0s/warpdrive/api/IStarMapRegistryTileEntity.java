package cr0s.warpdrive.api;

import cr0s.warpdrive.api.computer.ICoreSignature;
import cr0s.warpdrive.data.EnumStarMapEntryType;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public interface IStarMapRegistryTileEntity extends ICoreSignature {
	
	// get the registry type
	EnumStarMapEntryType getStarMapType();
	
	// get the area controlled by this tile entity 
	AxisAlignedBB getStarMapArea();
	
	// mass of the multi-block
	int getMass();
	
	// isolation rate from radars
	double getIsolationRate();
	
	// report an update in the area, return false to cancel it
	boolean onBlockUpdatingInArea(@Nullable final Entity entity, final BlockPos blockPos, final IBlockState blockState);
}
