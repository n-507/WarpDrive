package cr0s.warpdrive.compat;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.InvocationTargetException;

public class CompatEnergyControl implements IBlockTransformer {
	
	//only require rotation
	private static Class<?> classThermalMonitor;            //Thermal Monitor
	private static Class<?> classRemoteThermalMonitor;      //Remote Thermal Monitor
	
	//rotation and coordinate
	private static Class<?> classInfoPanel;                 //(Advanced) Info Panel
	private static Class<?> classInfoPanelExtender;         //(Advanced) Info Panel Extender
	private static Class<?> classHoloPanel;                 //Holographic Panel
	private static Class<?> classHoloPanelExtender;         //Holo Extender
	
	public static void register(){
		try{
			classThermalMonitor = Class.forName("com.zuxelus.energycontrol.blocks.ThermalMonitor");
			classRemoteThermalMonitor = Class.forName("com.zuxelus.energycontrol.blocks.RemoteThermalMonitor");
			classInfoPanel = Class.forName("com.zuxelus.energycontrol.blocks.InfoPanel");
			classInfoPanelExtender = Class.forName("com.zuxelus.energycontrol.blocks.InfoPanelExtender");
			classHoloPanel = Class.forName("com.zuxelus.energycontrol.blocks.HoloPanel");
			classHoloPanelExtender = Class.forName("com.zuxelus.energycontrol.blocks.HoloPanelExtender");
			WarpDriveConfig.registerBlockTransformer("energycontrol", new CompatEnergyControl());
		}catch(final ClassNotFoundException exception){
			WarpDrive.logger.error(exception);
		}
	}
	
	@Override
	public boolean isApplicable(Block block, int metadata, TileEntity tileEntity) {
		return (
			classThermalMonitor.isInstance(block) ||
			classRemoteThermalMonitor.isInstance(block) ||
			classInfoPanel.isInstance(block) ||
			classInfoPanelExtender.isInstance(block) ||
			classHoloPanel.isInstance(block) ||
			classHoloPanelExtender.isInstance(block)
		);
	}
	
	@Override
	public boolean isJumpReady(Block block, int metadata, TileEntity tileEntity, WarpDriveText reason) {
		//nothing to do
		return true;
	}
	
	@Override
	public NBTBase saveExternals(World world, int x, int y, int z, Block block, int blockMeta, TileEntity tileEntity) {
		if(
			classInfoPanel.isInstance(block) ||
			classHoloPanel.isInstance(block) ||
			classInfoPanelExtender.isInstance(block) ||
			classHoloPanelExtender.isInstance(block)
		){
			//trick the system to call restoreExternals later
			return new NBTTagCompound();
		}
		return null;
	}
	
	@Override
	public void removeExternals(World world, int x, int y, int z, Block block, int blockMeta, TileEntity tileEntity) {
		//nothing to do
	}
	
	//Rotation ID/metadata                                  0   1   2   3   4   5   6   7   8   9   10  11  12  13  14  15
	private static final byte[] panelRotFacing      = {     0,  1,  5,  4,  2,  3,  6,  7,  11, 10, 8,  9,  12, 13, 14, 15};
	private static final byte[] nbtRotFacing        = {     0,  1,  5,  4,  2,  3,  0,  1,  2,  3,  4,  5,  12, 13, 14, 15};
	private static final byte[] holoRotFacing       = {     1,  2,  3,  0,  5,  6,  7,  4,  8,  9,  10, 11, 12, 13, 14, 15};
	
	@Override
	public int rotate(Block block, int metadata, NBTTagCompound nbtTileEntity, ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		
		
		//Fix screen data for display panels
		if(
			classInfoPanel.isInstance(block) ||
			classHoloPanel.isInstance(block)
		){
			NBTTagCompound screenData = nbtTileEntity.getCompoundTag("screenData");
			int maxX = screenData.getInteger("maxX");
			int maxY = screenData.getInteger("maxY");
			int maxZ = screenData.getInteger("maxZ");
			int minX = screenData.getInteger("minX");
			int minY = screenData.getInteger("minY");
			int minZ = screenData.getInteger("minZ");
			BlockPos newMax = transformation.apply(maxX, maxY, maxZ);
			BlockPos newMin = transformation.apply(minX, minY, minZ);
			int newMaxX = Math.max(newMax.getX(), newMin.getX());
			int newMaxY = Math.max(newMax.getY(), newMin.getY());
			int newMaxZ = Math.max(newMax.getZ(), newMin.getZ());
			int newMinX = Math.min(newMax.getX(), newMin.getX());
			int newMinY = Math.min(newMax.getY(), newMin.getY());
			int newMinZ = Math.min(newMax.getZ(), newMin.getZ());
			
			screenData.setInteger("maxX", newMaxX);
			screenData.setInteger("maxY", newMaxY);
			screenData.setInteger("maxZ", newMaxZ);
			screenData.setInteger("minX", newMinX);
			screenData.setInteger("minY", newMinY);
			screenData.setInteger("minZ", newMinZ);
			nbtTileEntity.setTag("screenData", screenData);
		}

		
		//handle data cards (transform absolute coordinates)
		if(
			classHoloPanel.isInstance(block) ||
			classInfoPanel.isInstance(block) ||
			classRemoteThermalMonitor.isInstance(block)
		){
			NBTTagList items = nbtTileEntity.getTagList("Items", 10);
			NBTTagList itemsNew = items.copy();
			boolean anyChange = false;
			for(int index = 0; index < items.tagCount(); index++){
				NBTTagCompound item = items.getCompoundTagAt(index);
				if(!item.hasKey("tag")){ continue; }  //must have tag
				NBTTagCompound itemTag = item.getCompoundTag("tag");
				if(!(itemTag.hasKey("x") &&
				     itemTag.hasKey("y") &&
				     itemTag.hasKey("z"))){
					continue;
				}   //must have the coordinate data
				
				int x = itemTag.getInteger("x");
				int y = itemTag.getInteger("y");
				int z = itemTag.getInteger("z");
				
				if(!transformation.isInside(x, y, z)){ continue; }//only convert if inside the ship
				BlockPos result = transformation.apply(x, y, z);
				itemTag.setInteger("x", result.getX());
				itemTag.setInteger("y", result.getY());
				itemTag.setInteger("z", result.getZ());
				item.setTag("tag", itemTag);
				itemsNew.set(index, item);
				anyChange = true;
			}
			if(anyChange){
				nbtTileEntity.setTag("Items", itemsNew);
			}
		}
		
		
		//Redirect core x/y/z for extended screens
		if(
			classInfoPanelExtender.isInstance(block)||
			classHoloPanelExtender.isInstance(block)
		){
			byte partOfScreen = nbtTileEntity.getByte("partOfScreen");
			//only do the modification if it is part of a screen
			if(partOfScreen == 1){
				int x = nbtTileEntity.getInteger("coreX");
				int y = nbtTileEntity.getInteger("coreY");
				int z = nbtTileEntity.getInteger("coreZ");
				BlockPos result = transformation.apply(x, y, z);
				nbtTileEntity.setInteger("coreX", result.getX());
				nbtTileEntity.setInteger("coreY", result.getY());
				nbtTileEntity.setInteger("coreZ", result.getZ());
			}
		}
		
		
		//handle rotation in NBT
		//6 sided type (block screens and monitors) - both facing and rotation
		if(
			classThermalMonitor.isInstance(block) ||
			classRemoteThermalMonitor.isInstance(block) ||
			classInfoPanel.isInstance(block) ||
			classInfoPanelExtender.isInstance(block)
		){
			int facing = nbtTileEntity.getInteger("facing");
			int rotation = 15;
			//only panels have rotation
			//NOTE there is no rotation of 15, so we use it as a marker of "Not exist"
			if(nbtTileEntity.hasKey("rotation")){
				rotation = nbtTileEntity.getInteger("rotation");
			}
			switch (rotationSteps) {
				//without break, so it rotate an additional time for each step.
				case 3:
					metadata = panelRotFacing[metadata];
					facing = nbtRotFacing[facing];
					rotation = nbtRotFacing[rotation];
				case 2:
					metadata = panelRotFacing[metadata];
					facing = nbtRotFacing[facing];
					rotation = nbtRotFacing[rotation];
				case 1:
					metadata = panelRotFacing[metadata];
					facing = nbtRotFacing[facing];
					rotation = nbtRotFacing[rotation];
				default:
					break;
			}
			nbtTileEntity.setInteger("facing", facing);
			if(rotation != 15){
				nbtTileEntity.setInteger("rotation", rotation);
			}
		}
		
		
		//4 sided type (holo displays) - facing only
		if(
			classHoloPanel.isInstance(block) ||
			classHoloPanelExtender.isInstance(block)
		){
			int facing = nbtTileEntity.getInteger("facing");
			switch (rotationSteps) {
				//without break, so it rotate an additional time for each step.
				case 3:
					metadata = holoRotFacing[metadata];
					facing = nbtRotFacing[facing];
				case 2:
					metadata = holoRotFacing[metadata];
					facing = nbtRotFacing[facing];
				case 1:
					metadata = holoRotFacing[metadata];
					facing = nbtRotFacing[facing];
				default:
					break;
			}
			nbtTileEntity.setInteger("facing", facing);
		}
		
		return metadata;
	}
	
	@SuppressWarnings("all")    //Suppress error related to invoke with args
	@Override
	public void restoreExternals(World world, BlockPos blockPos, IBlockState blockState, TileEntity tileEntity, ITransformation transformation, NBTBase nbtBase) {
		try {
			//Request an update to the block state
			//Necessary. Otherwise screen sometimes do not connect.
			if(classInfoPanel.isInstance(blockState.getBlock()) || classHoloPanel.isInstance(blockState.getBlock())){
				tileEntity.getClass().getMethod("updateBlockState").invoke(tileEntity, blockState);
			}
			if(classInfoPanelExtender.isInstance(blockState.getBlock()) || classHoloPanelExtender.isInstance(blockState.getBlock())){
				tileEntity.getClass().getMethod("update").invoke(tileEntity);
			}
		}catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e){
			WarpDrive.logger.warn(String.format("Failed to call update on a Energy Control panel %s at %s. The screen may become split.", blockState, blockPos));
			WarpDrive.logger.warn(e);
		}
	}
}
