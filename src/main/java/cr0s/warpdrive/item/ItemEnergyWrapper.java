package cr0s.warpdrive.item;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.block.TileEntityAbstractEnergy;
import cr0s.warpdrive.config.WarpDriveConfig;

import cofh.redstoneflux.api.IEnergyContainerItem;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;

public class ItemEnergyWrapper {
	
	// WarpDrive methods
	public static boolean isEnergyContainer(final ItemStack itemStack) {
		boolean bResult = false;
		// IndustrialCraft2
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			bResult = IC2_isEnergyContainer(itemStack);
		}
		
		// Thermal Expansion
		if (!bResult && WarpDriveConfig.isRedstoneFluxLoaded) {
			bResult = RF_isEnergyContainer(itemStack);
		}
		return bResult;
	}
	
	public static boolean canInput(final ItemStack itemStack) {
		boolean bResult = false;
		// IndustrialCraft2
		if (WarpDriveConfig.isIndustrialCraft2Loaded && IC2_isEnergyContainer(itemStack)) {
			bResult = IC2_canInput(itemStack);
		}
		
		// Thermal Expansion
		if (!bResult && WarpDriveConfig.isRedstoneFluxLoaded && RF_isEnergyContainer(itemStack)) {
			bResult = RF_canInput(itemStack);
		}
		return bResult;
	}
	
	public static boolean canOutput(final ItemStack itemStack) {
		boolean bResult = false;
		// IndustrialCraft2
		if (WarpDriveConfig.isIndustrialCraft2Loaded && IC2_isEnergyContainer(itemStack)) {
			bResult = IC2_canOutput(itemStack);
		}
		
		// Thermal Expansion
		if (!bResult && WarpDriveConfig.isRedstoneFluxLoaded && RF_isEnergyContainer(itemStack)) {
			bResult = RF_canOutput(itemStack);
		}
		return bResult;
	}
	
	public static int getEnergyStored(final ItemStack itemStack) {
		// IndustrialCraft2
		if (WarpDriveConfig.isIndustrialCraft2Loaded && IC2_isEnergyContainer(itemStack)) {
			final double amount_EU = Commons.clamp(0, IC2_getMaxEnergyStorage(itemStack), IC2_getEnergyStored(itemStack));
			return (int) TileEntityAbstractEnergy.convertEUtoInternal_floor(amount_EU);
		}
		
		// Thermal Expansion
		if (WarpDriveConfig.isRedstoneFluxLoaded && RF_isEnergyContainer(itemStack)) {
			final int amount_RF = Commons.clamp(0, RF_getMaxEnergyStorage(itemStack), RF_getEnergyStored(itemStack));
			return (int) TileEntityAbstractEnergy.convertRFtoInternal_floor(amount_RF);
		}
		return 0;
	}
	
	public static int getMaxEnergyStorage(final ItemStack itemStack) {
		// IndustrialCraft2
		if (WarpDriveConfig.isIndustrialCraft2Loaded && IC2_isEnergyContainer(itemStack)) {
			final double amount_EU = IC2_getMaxEnergyStorage(itemStack);
			return (int) TileEntityAbstractEnergy.convertEUtoInternal_floor(amount_EU);
		}
		
		// Thermal Expansion
		if (WarpDriveConfig.isRedstoneFluxLoaded && RF_isEnergyContainer(itemStack)) {
			final int amount_RF = RF_getMaxEnergyStorage(itemStack);
			return (int) TileEntityAbstractEnergy.convertRFtoInternal_floor(amount_RF);
		}
		return 0;
	}
	
	public static ItemStack consume(final ItemStack itemStack, final int amount, final boolean simulate) {
		// IndustrialCraft2
		if (WarpDriveConfig.isIndustrialCraft2Loaded && IC2_isEnergyContainer(itemStack)) {
			final double amount_EU = TileEntityAbstractEnergy.convertInternalToEU_ceil(amount);
			return IC2_consume(itemStack, amount_EU, simulate);
		}
		
		// Thermal Expansion
		if (WarpDriveConfig.isRedstoneFluxLoaded && RF_isEnergyContainer(itemStack)) {
			final int amount_RF = TileEntityAbstractEnergy.convertInternalToRF_ceil(amount);
			return RF_consume(itemStack, amount_RF, simulate);
		}
		return null;
	}
	
	public static ItemStack charge(final ItemStack itemStack, final int amount, final boolean simulate) {
		// IndustrialCraft2
		if (WarpDriveConfig.isIndustrialCraft2Loaded && IC2_isEnergyContainer(itemStack)) {
			final double amount_EU = TileEntityAbstractEnergy.convertInternalToEU_floor(amount);
			return IC2_charge(itemStack, amount_EU, simulate);
		}
		
		// Thermal Expansion
		if (WarpDriveConfig.isRedstoneFluxLoaded && RF_isEnergyContainer(itemStack)) {
			final int amount_RF = TileEntityAbstractEnergy.convertInternalToRF_floor(amount);
			return RF_charge(itemStack, amount_RF, simulate);
		}
		return null;
	}
	
	// IndustrialCraft IElectricItem interface
	@Optional.Method(modid = "ic2")
	private static IElectricItemManager IC2_getManager(final ItemStack itemStack) {
		final Item item = itemStack.getItem();
		if (item == null) {
			return null;
		}
		if (item instanceof ISpecialElectricItem) {
			return ((ISpecialElectricItem) item).getManager(itemStack);
		}
		if (item instanceof IElectricItem) {
			return ElectricItem.rawManager;
		}
		return ElectricItem.getBackupManager(itemStack);
	}
	
	@Optional.Method(modid = "ic2")
	private static boolean IC2_isEnergyContainer(final ItemStack itemStack) {
		return itemStack.getItem() instanceof IElectricItem;
	}
	
	@Optional.Method(modid = "ic2")
	private static boolean IC2_canOutput(final ItemStack itemStack) {
		return ((IElectricItem)itemStack.getItem()).canProvideEnergy(itemStack);
	}
	
	@Optional.Method(modid = "ic2")
	private static boolean IC2_canInput(final ItemStack itemStack) {
		return false;
	}
	
	@Optional.Method(modid = "ic2")
	private static double IC2_getEnergyStored(final ItemStack itemStack) {
		final IElectricItemManager electricItemManager = IC2_getManager(itemStack);
		if (electricItemManager == null) {
			return 0.0D;
		}
		return electricItemManager.getCharge(itemStack);
	}
	
	@Optional.Method(modid = "ic2")
	private static double IC2_getMaxEnergyStorage(final ItemStack itemStack) {
		return ((IElectricItem)itemStack.getItem()).getMaxCharge(itemStack);
	}
	
	@Optional.Method(modid = "ic2")
	private static ItemStack IC2_consume(final ItemStack itemStack, final double amount_EU, final boolean simulate) {
		final IElectricItemManager electricItemManager = IC2_getManager(itemStack);
		if (electricItemManager == null) {
			return null;
		}
		if (amount_EU <= electricItemManager.getCharge(itemStack)) {
			if (!simulate) {
				electricItemManager.discharge(itemStack, amount_EU, ((IElectricItem)itemStack.getItem()).getTier(itemStack), true, true, simulate);
				return itemStack;
			}
		}
		return null;
	}
	
	@Optional.Method(modid = "ic2")
	private static ItemStack IC2_charge(final ItemStack itemStack, final double amount_EU, final boolean simulate) {
		final IElectricItemManager electricItemManager = IC2_getManager(itemStack);
		if (electricItemManager == null) {
			return null;
		}
		if (amount_EU >= IC2_getMaxEnergyStorage(itemStack)) {
			if (!simulate) {
				electricItemManager.charge(itemStack, amount_EU, ((IElectricItem)itemStack.getItem()).getTier(itemStack), true, simulate);
				return itemStack;
			}
		}
		return null;
	}
	
	
	// RedstoneFlux IEnergyContainerItem interface
	@Optional.Method(modid = "redstoneflux")
	private static boolean RF_isEnergyContainer(final ItemStack itemStack) {
		return itemStack.getItem() instanceof IEnergyContainerItem;
	}
	
	@Optional.Method(modid = "redstoneflux")
	private static boolean RF_canOutput(final ItemStack itemStack) {
		return ((IEnergyContainerItem) itemStack.getItem()).getEnergyStored(itemStack) > 0;
	}
	
	@Optional.Method(modid = "redstoneflux")
	private static boolean RF_canInput(final ItemStack itemStack) {
		return ((IEnergyContainerItem) itemStack.getItem()).getEnergyStored(itemStack) < ((IEnergyContainerItem) itemStack.getItem()).getMaxEnergyStored(itemStack);
	}
	
	@Optional.Method(modid = "redstoneflux")
	private static int RF_getEnergyStored(final ItemStack itemStack) {
		return (int) Math.floor( ((IEnergyContainerItem)itemStack.getItem()).getEnergyStored(itemStack) );
	}
	
	@Optional.Method(modid = "redstoneflux")
	private static int RF_getMaxEnergyStorage(final ItemStack itemStack) {
		return (int) Math.floor( ((IEnergyContainerItem)itemStack.getItem()).getMaxEnergyStored(itemStack) );
	}
	
	@Optional.Method(modid = "redstoneflux")
	private static ItemStack RF_consume(final ItemStack itemStack, final int amount_RF, final boolean simulate) {
		if (((IEnergyContainerItem)itemStack.getItem()).extractEnergy(itemStack, amount_RF, simulate) > 0) {
			return itemStack;
		}
		return null;
	}
	
	@Optional.Method(modid = "redstoneflux")
	private static ItemStack RF_charge(final ItemStack itemStack, final int amount_RF, final boolean simulate) {
		if ( ((IEnergyContainerItem)itemStack.getItem()).receiveEnergy(itemStack, amount_RF, simulate) > 0) {
			return itemStack;
		}
		return null;
	}
}
