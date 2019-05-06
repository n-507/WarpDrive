package cr0s.warpdrive.api;

import cr0s.warpdrive.api.computer.ICoreSignature;
import cr0s.warpdrive.data.EnumStarMapEntryType;
import cr0s.warpdrive.data.VectorI;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;

public interface IStarMapRegistryTileEntity extends ICoreSignature {
	
	// get the registry type
	EnumStarMapEntryType getStarMapType();
	
	// get the area controlled by this tile entity 
	AxisAlignedBB getStarMapArea();
	
	// mass of the multi-block
	int getMass();
	
	// isolation rate from radars
	double getIsolationRate();
	
	// report an update in the area
	void onBlockUpdatedInArea(final VectorI vector, final IBlockState blockState);
}
