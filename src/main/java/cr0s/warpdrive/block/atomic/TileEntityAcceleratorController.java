package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.api.IStarMapRegistryTileEntity;
import cr0s.warpdrive.block.TileEntityAbstractEnergyCoreOrController;
import cr0s.warpdrive.data.EnumStarMapEntryType;
import cr0s.warpdrive.data.VectorI;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;

public class TileEntityAcceleratorController extends TileEntityAbstractEnergyCoreOrController implements IStarMapRegistryTileEntity {
	
	public TileEntityAcceleratorController() {
		super();
	}
	
	@Override
	public void update() {
		super.update();
	}
	
	// IStarMapRegistryTileEntity overrides
	@Override
	public EnumStarMapEntryType getStarMapType() {
		return EnumStarMapEntryType.ACCELERATOR;
	}
	
	@Override
	public AxisAlignedBB getStarMapArea() {
		return null;
	}
	
	@Override
	public int getMass() {
		return 0;
	}
	
	@Override
	public double getIsolationRate() {
		return 0.0;
	}
	
	@Override
	public void onBlockUpdatedInArea(final VectorI vector, final IBlockState blockState) {
		
	}
	
	@Override
	public Object[] getEnergyRequired() {
		return new Object[0];
	}
}
