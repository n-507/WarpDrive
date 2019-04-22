package cr0s.warpdrive;

import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.block.BlockChunkLoader;
import cr0s.warpdrive.block.BlockLaser;
import cr0s.warpdrive.block.BlockLaserMedium;
import cr0s.warpdrive.block.BlockSecurityStation;
import cr0s.warpdrive.block.TileEntityChunkLoader;
import cr0s.warpdrive.block.TileEntityLaser;
import cr0s.warpdrive.block.TileEntityLaserMedium;
import cr0s.warpdrive.block.atomic.BlockAcceleratorControlPoint;
import cr0s.warpdrive.block.atomic.BlockAcceleratorController;
import cr0s.warpdrive.block.atomic.BlockChiller;
import cr0s.warpdrive.block.atomic.BlockElectromagnetGlass;
import cr0s.warpdrive.block.atomic.BlockElectromagnetPlain;
import cr0s.warpdrive.block.atomic.BlockParticlesCollider;
import cr0s.warpdrive.block.atomic.BlockParticlesInjector;
import cr0s.warpdrive.block.atomic.BlockVoidShellGlass;
import cr0s.warpdrive.block.atomic.BlockVoidShellPlain;
import cr0s.warpdrive.block.atomic.TileEntityAcceleratorControlPoint;
import cr0s.warpdrive.block.atomic.TileEntityAcceleratorController;
import cr0s.warpdrive.block.atomic.TileEntityParticlesInjector;
import cr0s.warpdrive.block.breathing.BlockAirFlow;
import cr0s.warpdrive.block.breathing.BlockAirGeneratorTiered;
import cr0s.warpdrive.block.breathing.BlockAirShield;
import cr0s.warpdrive.block.breathing.BlockAirSource;
import cr0s.warpdrive.block.breathing.TileEntityAirGeneratorTiered;
import cr0s.warpdrive.block.building.BlockShipScanner;
import cr0s.warpdrive.block.building.TileEntityShipScanner;
import cr0s.warpdrive.block.collection.BlockLaserTreeFarm;
import cr0s.warpdrive.block.collection.BlockMiningLaser;
import cr0s.warpdrive.block.collection.TileEntityLaserTreeFarm;
import cr0s.warpdrive.block.collection.TileEntityMiningLaser;
import cr0s.warpdrive.block.decoration.BlockBedrockGlass;
import cr0s.warpdrive.block.decoration.BlockDecorative;
import cr0s.warpdrive.block.decoration.BlockGas;
import cr0s.warpdrive.block.decoration.BlockLamp_bubble;
import cr0s.warpdrive.block.decoration.BlockLamp_flat;
import cr0s.warpdrive.block.decoration.BlockLamp_long;
import cr0s.warpdrive.block.detection.BlockCamera;
import cr0s.warpdrive.block.detection.BlockCloakingCoil;
import cr0s.warpdrive.block.detection.BlockCloakingCore;
import cr0s.warpdrive.block.detection.BlockMonitor;
import cr0s.warpdrive.block.detection.BlockRadar;
import cr0s.warpdrive.block.detection.BlockSiren;
import cr0s.warpdrive.block.detection.BlockWarpIsolation;
import cr0s.warpdrive.block.detection.TileEntityCamera;
import cr0s.warpdrive.block.detection.TileEntityCloakingCore;
import cr0s.warpdrive.block.detection.TileEntityMonitor;
import cr0s.warpdrive.block.detection.TileEntityRadar;
import cr0s.warpdrive.block.detection.TileEntitySiren;
import cr0s.warpdrive.block.energy.BlockCapacitor;
import cr0s.warpdrive.block.energy.BlockEnanReactorCore;
import cr0s.warpdrive.block.energy.BlockEnanReactorLaser;
import cr0s.warpdrive.block.energy.BlockIC2reactorLaserCooler;
import cr0s.warpdrive.block.energy.TileEntityCapacitor;
import cr0s.warpdrive.block.energy.TileEntityEnanReactorCore;
import cr0s.warpdrive.block.energy.TileEntityEnanReactorLaser;
import cr0s.warpdrive.block.energy.TileEntityIC2reactorLaserMonitor;
import cr0s.warpdrive.block.forcefield.BlockForceField;
import cr0s.warpdrive.block.forcefield.BlockForceFieldProjector;
import cr0s.warpdrive.block.forcefield.BlockForceFieldRelay;
import cr0s.warpdrive.block.forcefield.TileEntityForceField;
import cr0s.warpdrive.block.forcefield.TileEntityForceFieldProjector;
import cr0s.warpdrive.block.forcefield.TileEntityForceFieldRelay;
import cr0s.warpdrive.block.hull.BlockHullGlass;
import cr0s.warpdrive.block.hull.BlockHullOmnipanel;
import cr0s.warpdrive.block.hull.BlockHullPlain;
import cr0s.warpdrive.block.hull.BlockHullSlab;
import cr0s.warpdrive.block.hull.BlockHullStairs;
import cr0s.warpdrive.block.movement.BlockLift;
import cr0s.warpdrive.block.movement.BlockShipController;
import cr0s.warpdrive.block.movement.BlockShipCore;
import cr0s.warpdrive.block.movement.BlockTransporterBeacon;
import cr0s.warpdrive.block.movement.BlockTransporterContainment;
import cr0s.warpdrive.block.movement.BlockTransporterCore;
import cr0s.warpdrive.block.movement.BlockTransporterScanner;
import cr0s.warpdrive.block.movement.TileEntityJumpGateCore;
import cr0s.warpdrive.block.movement.TileEntityLift;
import cr0s.warpdrive.block.movement.TileEntityShipController;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.block.movement.TileEntityTransporterBeacon;
import cr0s.warpdrive.block.movement.TileEntityTransporterCore;
import cr0s.warpdrive.block.passive.BlockHighlyAdvancedMachine;
import cr0s.warpdrive.block.passive.BlockIridium;
import cr0s.warpdrive.block.weapon.BlockLaserCamera;
import cr0s.warpdrive.block.weapon.BlockWeaponController;
import cr0s.warpdrive.block.weapon.TileEntityLaserCamera;
import cr0s.warpdrive.block.weapon.TileEntityWeaponController;
import cr0s.warpdrive.client.CreativeTabHull;
import cr0s.warpdrive.client.CreativeTabMain;
import cr0s.warpdrive.command.CommandDebug;
import cr0s.warpdrive.command.CommandDump;
import cr0s.warpdrive.command.CommandEntity;
import cr0s.warpdrive.command.CommandFind;
import cr0s.warpdrive.command.CommandGenerate;
import cr0s.warpdrive.command.CommandBed;
import cr0s.warpdrive.command.CommandInvisible;
import cr0s.warpdrive.command.CommandReload;
import cr0s.warpdrive.command.CommandSpace;
import cr0s.warpdrive.config.Recipes;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.damage.DamageAsphyxia;
import cr0s.warpdrive.damage.DamageCold;
import cr0s.warpdrive.damage.DamageIrradiation;
import cr0s.warpdrive.damage.DamageLaser;
import cr0s.warpdrive.damage.DamageShock;
import cr0s.warpdrive.damage.DamageTeleportation;
import cr0s.warpdrive.damage.DamageWarm;
import cr0s.warpdrive.data.CamerasRegistry;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.CloakManager;
import cr0s.warpdrive.data.EnumHullPlainType;
import cr0s.warpdrive.data.StarMapRegistry;
import cr0s.warpdrive.entity.EntityParticleBunch;
import cr0s.warpdrive.event.ChunkHandler;
import cr0s.warpdrive.event.ChunkLoadingHandler;
import cr0s.warpdrive.event.CommonWorldGenerator;
import cr0s.warpdrive.event.WorldHandler;
import cr0s.warpdrive.item.ItemAirTank;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.item.ItemElectromagneticCell;
import cr0s.warpdrive.item.ItemForceFieldShape;
import cr0s.warpdrive.item.ItemForceFieldUpgrade;
import cr0s.warpdrive.item.ItemIC2reactorLaserFocus;
import cr0s.warpdrive.item.ItemShipToken;
import cr0s.warpdrive.item.ItemTuningDriver;
import cr0s.warpdrive.item.ItemTuningFork;
import cr0s.warpdrive.item.ItemWarpArmor;
import cr0s.warpdrive.network.PacketHandler;
import cr0s.warpdrive.render.EntityCamera;
import cr0s.warpdrive.world.BiomeSpace;
import cr0s.warpdrive.world.EntitySphereGen;
import cr0s.warpdrive.world.EntityStarCore;
import cr0s.warpdrive.world.HyperSpaceWorldProvider;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;

import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.apache.logging.log4j.Logger;

import com.mojang.authlib.GameProfile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mod(modid = WarpDrive.MODID,
     name = "WarpDrive",
     version = WarpDrive.VERSION,
     dependencies = "after:ic2;"
                  + "after:cofhcore;"
                  + "after:computercraft;"
                  + "after:opencomputer;"
                  + "after:ccturtle;"
                  + "after:gregtech;"
                  + "after:appliedenergistics;"
                  + "after:enderio;"
                  + "after:defensetech;"
                  + "after:icbmclassic;",
     certificateFingerprint = "f7be6b40743c6a8205df86c5e57547d578605d8a"
)
public class WarpDrive {
	public static final String MODID = "warpdrive";
	public static final String VERSION = "@version@";
	@SuppressWarnings("ConstantConditions")
	public static final boolean isDev = VERSION.equals("@" + "version" + "@") || VERSION.contains("-dev");
	public static GameProfile gameProfile = new GameProfile(UUID.nameUUIDFromBytes("[WarpDrive]".getBytes()), "[WarpDrive]");
	
	public static Block blockShipCore;
	public static Block blockShipController;
	public static Block blockRadar;
	public static Block blockWarpIsolation;
	public static Block blockAirGenerator;
	public static Block[] blockAirGeneratorTiered = new Block[3];
	public static Block blockLaser;
	public static Block blockLaserCamera;
	public static Block blockWeaponController;
	public static Block blockCamera;
	public static Block blockMonitor;
	public static Block blockLaserMedium;
	public static Block blockMiningLaser;
	public static Block blockLaserTreeFarm;
	public static Block blockLift;
	public static Block blockShipScanner;
	public static Block blockCloakingCore;
	public static Block blockCloakingCoil;
	public static Block blockTransporterBeacon;
	public static Block blockTransporterCore;
	public static Block blockTransporterContainment;
	public static Block blockTransporterScanner;
	public static Block blockIC2reactorLaserCooler;
	public static Block blockEnanReactorCore;
	public static Block blockEnanReactorLaser;
	public static Block blockEnergyBank;
	public static Block blockAir;
	public static Block blockAirFlow;
	public static Block blockAirSource;
	public static Block blockAirShield;
	public static Block blockBedrockGlass;
	public static Block blockGas;
	public static Block blockIridium;
	public static Block blockLamp_bubble;
	public static Block blockLamp_flat;
	public static Block blockLamp_long;
	public static Block blockHighlyAdvancedMachine;
	public static Block blockChunkLoader;
	public static Block[] blockForceFields;
	public static Block[] blockForceFieldProjectors;
	public static Block[] blockForceFieldRelays;
	public static Block blockSecurityStation;
	public static Block blockAcceleratorController;
	public static Block blockAcceleratorControlPoint;
	public static Block blockParticlesCollider;
	public static Block blockParticlesInjector;
	public static Block blockVoidShellPlain;
	public static Block blockVoidShellGlass;
	public static Block[] blockElectromagnetPlain;
	public static Block[] blockElectromagnetGlass;
	public static Block[] blockChillers;
	public static Block blockDecorative;
	public static Block[][] blockHulls_plain;
	public static Block[] blockHulls_glass;
	public static Block[] blockHulls_omnipanel;
	public static Block[][] blockHulls_stairs;
	public static Block[][] blockHulls_slab;
	public static Block blockSiren;
	
	public static Item itemIC2reactorLaserFocus;
	public static ItemComponent itemComponent;
	public static ItemShipToken itemShipToken;
	public static ItemTuningFork itemTuningFork;
	public static ItemTuningDriver itemTuningDriver;
	public static ItemForceFieldShape itemForceFieldShape;
	public static ItemForceFieldUpgrade itemForceFieldUpgrade;
	public static ItemElectromagneticCell itemElectromagneticCell;
	
	public static final ArmorMaterial armorMaterial = EnumHelper.addArmorMaterial("WARP", "warp", 18, new int[] { 2, 6, 5, 2 }, 9, SoundEvents.ITEM_ARMOR_EQUIP_IRON, 0.0F);
	public static ItemArmor[] itemWarpArmor;
	public static ItemAirTank[] itemAirTanks;
	
	public static DamageAsphyxia damageAsphyxia;
	public static DamageCold damageCold;
	public static DamageIrradiation damageIrradiation;
	public static DamageLaser damageLaser;
	public static DamageShock damageShock;
	public static DamageTeleportation damageTeleportation;
	public static DamageWarm damageWarm;
	
	// world generation
	public static Biome biomeSpace;
	public static DimensionType dimensionTypeSpace;
	public static DimensionType dimensionTypeHyperSpace;
	@SuppressWarnings("FieldCanBeLocal")
	private CommonWorldGenerator commonWorldGenerator;
	
	public static Field fieldBlockHardness = null;
	public static Method methodBlock_getSilkTouch = null;
	
	// Client settings
	public static final CreativeTabs creativeTabMain = new CreativeTabMain(MODID.toLowerCase() + ".main");
	public static final CreativeTabs creativeTabHull = new CreativeTabHull(MODID.toLowerCase() + ".hull");
	
	@Instance(WarpDrive.MODID)
	public static WarpDrive instance;
	@SidedProxy(clientSide = "cr0s.warpdrive.client.ClientProxy", serverSide = "cr0s.warpdrive.CommonProxy")
	public static CommonProxy proxy;
	
	public static StarMapRegistry starMap;
	public static CloakManager cloaks;
	public static CamerasRegistry cameras;
	
	@SuppressWarnings("FieldCanBeLocal")
	private static WarpDrivePeripheralHandler peripheralHandler = null;
	
	public static Logger logger;
	
	@EventHandler
	public void onFMLPreInitialization(final FMLPreInitializationEvent event) {
		logger = event.getModLog();
		
		WarpDriveConfig.onFMLpreInitialization(event.getModConfigurationDirectory().getAbsolutePath());
		
		// open access to Block.blockHardness
		fieldBlockHardness = Commons.getField(Block.class, "blockHardness", "field_149782_v");
		methodBlock_getSilkTouch = ReflectionHelper.findMethod(Block.class, "getSilkTouchDrop", "func_180643_i", IBlockState.class);
		
		// common blocks
		blockChunkLoader = new BlockChunkLoader("block_chunk_loader");
		blockLaser = new BlockLaser("block_laser");
		blockLaserMedium = new BlockLaserMedium("block_laser_medium");
		
		// atomic blocks
		if (WarpDriveConfig.ACCELERATOR_ENABLE) {
			blockAcceleratorController = new BlockAcceleratorController("block_accelerator_controller");
			blockAcceleratorControlPoint = new BlockAcceleratorControlPoint("block_accelerator_controlPoint");
			blockParticlesCollider = new BlockParticlesCollider("block_particles_collider");
			blockParticlesInjector = new BlockParticlesInjector("block_particles_injector");
			blockVoidShellPlain = new BlockVoidShellPlain("block_void_shell_plain");
			blockVoidShellGlass = new BlockVoidShellGlass("block_void_shell_glass");
			
			blockElectromagnetPlain = new Block[3];
			blockElectromagnetGlass = new Block[3];
			blockChillers = new Block[3];
			for(byte tier = 1; tier <= 3; tier++) {
				final int index = tier - 1;
				blockElectromagnetPlain[index] = new BlockElectromagnetPlain("block_electromagnet_plain" + tier, tier);
				blockElectromagnetGlass[index] = new BlockElectromagnetGlass("block_electromagnet_glass" + tier, tier);
				blockChillers[index] = new BlockChiller("block_chiller" + tier, tier);
			}
			
			itemElectromagneticCell = new ItemElectromagneticCell("item_electromagnetic_cell");
		}
		
		// building blocks
		blockShipScanner = new BlockShipScanner("block_ship_scanner");
		
		// breathing blocks
		blockAirFlow = new BlockAirFlow("block_air_flow");
		blockAirSource = new BlockAirSource("block_air_source");
		blockAirShield = new BlockAirShield("block_air_shield");
		
		blockAirGeneratorTiered = new Block[3];
		for (byte tier = 1; tier <= 3; tier++) {
			final int index = tier - 1;
			blockAirGeneratorTiered[index] = new BlockAirGeneratorTiered("block_air_generator" + tier, tier);
		}
		
		// collection blocks
		blockMiningLaser = new BlockMiningLaser("block_mining_laser");
		blockLaserTreeFarm = new BlockLaserTreeFarm("block_laser_tree_farm");
		
		// decorative
		blockDecorative = new BlockDecorative("block_decorative");
		blockGas = new BlockGas("block_gas");
		blockLamp_bubble = new BlockLamp_bubble("block_lamp_bubble");
		blockLamp_flat = new BlockLamp_flat("block_lamp_flat");
		blockLamp_long = new BlockLamp_long("block_lamp_long");
		
		// detection blocks
		blockCamera = new BlockCamera("block_camera");
		blockCloakingCore = new BlockCloakingCore("block_cloaking_core");
		blockCloakingCoil = new BlockCloakingCoil("block_cloaking_coil");
		blockMonitor = new BlockMonitor("block_monitor");
		blockRadar = new BlockRadar("block_radar");
		blockSiren = new BlockSiren("block_siren");
		blockWarpIsolation = new BlockWarpIsolation("block_warp_isolation");
		
		// energy blocks and items
		blockEnanReactorCore = new BlockEnanReactorCore("block_enan_reactor_core");
		blockEnanReactorLaser = new BlockEnanReactorLaser("block_enan_reactor_laser");
		blockEnergyBank = new BlockEnergyBank("block_subspace_capacitor");
		
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			blockIC2reactorLaserCooler = new BlockIC2reactorLaserCooler("block_ic2_reactor_laser_cooler");
			itemIC2reactorLaserFocus = new ItemIC2reactorLaserFocus("item_ic2_reactor_laser_focus");
		}
		
		// force field blocks and items
		blockForceFields = new Block[3];
		blockForceFieldProjectors = new Block[3];
		blockForceFieldRelays = new Block[3];
		for (byte tier = 1; tier <= 3; tier++) {
			int index = tier - 1;
			blockForceFields[index] = new BlockForceField("block_force_field" + tier, tier);
			blockForceFieldProjectors[index] = new BlockForceFieldProjector("block_projector" + tier, tier);
			blockForceFieldRelays[index] = new BlockForceFieldRelay("block_force_field_relay" + tier, tier);
		}
		blockSecurityStation = new BlockSecurityStation("block_security_station");
		itemForceFieldShape = new ItemForceFieldShape("item_force_field_shape");
		itemForceFieldUpgrade = new ItemForceFieldUpgrade("item_force_field_upgrade");
		
		// hull blocks
		blockHulls_plain = new Block[3][EnumHullPlainType.length];
		blockHulls_glass = new Block[3];
		blockHulls_omnipanel = new Block[3];
		blockHulls_stairs = new Block[3][16];
		blockHulls_slab = new Block[3][16];
		
		for (byte tier = 1; tier <= 3; tier++) {
			final int index = tier - 1;
			for (final EnumHullPlainType hullPlainType : EnumHullPlainType.values()) {
				blockHulls_plain[index][hullPlainType.ordinal()] = new BlockHullPlain("block_hull" + tier + "_" + hullPlainType.getName(), tier, hullPlainType);
			}
			blockHulls_glass[index] = new BlockHullGlass("block_hull" + tier + "_glass", tier);
			blockHulls_omnipanel[index] = new BlockHullOmnipanel("block_hull" + tier + "_omnipanel", tier);
			for (final EnumDyeColor enumDyeColor : EnumDyeColor.values()) {
				blockHulls_stairs[index][enumDyeColor.getMetadata()] = new BlockHullStairs("block_hull" + tier + "_stairs_" + enumDyeColor.getName(), blockHulls_plain[index][0].getStateFromMeta(enumDyeColor.getMetadata()), tier);
				blockHulls_slab[index][enumDyeColor.getMetadata()] = new BlockHullSlab("block_hull" + tier + "_slab_" + enumDyeColor.getName(), blockHulls_plain[index][0].getStateFromMeta(enumDyeColor.getMetadata()), tier);
			}
		}
		
		// movement blocks
		blockLift = new BlockLift("block_lift");
		blockShipController = new BlockShipController("block_ship_controller");
		blockShipCore = new BlockShipCore("block_ship_core");
		blockTransporterBeacon = new BlockTransporterBeacon("block_transporter_beacon");
		blockTransporterContainment = new BlockTransporterContainment("block_transporter_containment");
		blockTransporterCore = new BlockTransporterCore("block_transporter_core");
		blockTransporterScanner = new BlockTransporterScanner("block_transporter_scanner");
		
		// passive blocks
		blockBedrockGlass = new BlockBedrockGlass("block_bedrock_glass");
		blockHighlyAdvancedMachine = new BlockHighlyAdvancedMachine("block_highly_advanced_machine");
		blockIridium = new BlockIridium("block_iridium");
		
		// weapon blocks
		blockLaserCamera = new BlockLaserCamera("block_laser_camera");
		blockWeaponController = new BlockWeaponController("block_weapon_controller");
		
		// component items
		itemComponent = new ItemComponent("item_component");
		itemShipToken = new ItemShipToken("item_ship_token");
		
		// warp armor
		itemWarpArmor = new ItemArmor[4];
		itemWarpArmor[EntityEquipmentSlot.HEAD.getIndex() ] = new ItemWarpArmor("item_warp_armor_" + ItemWarpArmor.suffixes[EntityEquipmentSlot.HEAD.getIndex() ], armorMaterial, 3, EntityEquipmentSlot.HEAD );
		itemWarpArmor[EntityEquipmentSlot.CHEST.getIndex()] = new ItemWarpArmor("item_warp_armor_" + ItemWarpArmor.suffixes[EntityEquipmentSlot.CHEST.getIndex()], armorMaterial, 3, EntityEquipmentSlot.CHEST);
		itemWarpArmor[EntityEquipmentSlot.LEGS.getIndex() ] = new ItemWarpArmor("item_warp_armor_" + ItemWarpArmor.suffixes[EntityEquipmentSlot.LEGS.getIndex() ], armorMaterial, 3, EntityEquipmentSlot.LEGS );
		itemWarpArmor[EntityEquipmentSlot.FEET.getIndex() ] = new ItemWarpArmor("item_warp_armor_" + ItemWarpArmor.suffixes[EntityEquipmentSlot.FEET.getIndex() ], armorMaterial, 3, EntityEquipmentSlot.FEET );
		
		itemAirTanks = new ItemAirTank[4];
		for (int index = 0; index < 4; index++) {
			itemAirTanks[index] = new ItemAirTank((byte) index, "item_air_tank" + index);
		}
		
		// tool items
		itemTuningFork = new ItemTuningFork("item_tuning_fork");
		itemTuningDriver = new ItemTuningDriver("item_tuning_driver");
		
		// damage sources
		damageAsphyxia = new DamageAsphyxia();
		damageCold = new DamageCold();
		damageIrradiation = new DamageIrradiation();
		damageLaser = new DamageLaser();
		damageShock = new DamageShock();
		damageTeleportation = new DamageTeleportation();
		damageWarm = new DamageWarm();
		
		// entities
		// (done in the event handler)
		
		// world generation
		final Biome.BiomeProperties biomeProperties = new Biome.BiomeProperties("space").setRainDisabled().setWaterColor(0);
		biomeSpace = new BiomeSpace(biomeProperties);
		register(biomeSpace);
		
		// chunk loading
		ForgeChunkManager.setForcedChunkLoadingCallback(instance, ChunkLoadingHandler.INSTANCE);
		
		// Event handlers
		MinecraftForge.EVENT_BUS.register(this);
		
		proxy.onForgePreInitialisation();
	}
	
	@EventHandler
	public void onFMLInitialization(final FMLInitializationEvent event) {
		PacketHandler.init();
		
		WarpDriveConfig.onFMLInitialization();
		
		// world generation
		commonWorldGenerator = new CommonWorldGenerator();
		GameRegistry.registerWorldGenerator(commonWorldGenerator, 0);
		
		dimensionTypeSpace = DimensionType.register("space", "_space", WarpDriveConfig.G_SPACE_PROVIDER_ID, SpaceWorldProvider.class, true);
		dimensionTypeHyperSpace = DimensionType.register("hyperspace", "_hyperspace", WarpDriveConfig.G_HYPERSPACE_PROVIDER_ID, HyperSpaceWorldProvider.class, true);
		
		// Registers
		starMap = new StarMapRegistry();
		cloaks = new CloakManager();
		cameras = new CamerasRegistry();
		
		CelestialObjectManager.onFMLInitialization();
		
		proxy.onForgeInitialisation();
	}
	
	@EventHandler
	public void onFMLPostInitialization(final FMLPostInitializationEvent event) {
		/* @TODO not sure why it would be needed, disabling for now
		// load all owned dimensions at boot
		for (final CelestialObject celestialObject : CelestialObjectManager.celestialObjects) {
			if (celestialObject.provider.equals(CelestialObject.PROVIDER_OTHER)) {
				DimensionManager.getWorld(celestialObject.dimensionId);
			}
		}
		/**/
		
		WarpDriveConfig.onFMLPostInitialization();
		
		if (WarpDriveConfig.isComputerCraftLoaded) {
			peripheralHandler = new WarpDrivePeripheralHandler();
			peripheralHandler.register();
		}
		
		final WorldHandler worldHandler = new WorldHandler();
		MinecraftForge.EVENT_BUS.register(worldHandler);
		
		final ChunkHandler chunkHandler = new ChunkHandler();
		MinecraftForge.EVENT_BUS.register(chunkHandler);
	}
	
	@EventHandler
	public void onFMLServerStarting(final FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandDebug());
		event.registerServerCommand(new CommandDump());
		event.registerServerCommand(new CommandEntity());
		event.registerServerCommand(new CommandFind());
		event.registerServerCommand(new CommandGenerate());
		event.registerServerCommand(new CommandBed());
		event.registerServerCommand(new CommandInvisible());
		event.registerServerCommand(new CommandReload());
		event.registerServerCommand(new CommandSpace());
	}
	
	/* @TODO MC1.12
	@SuppressWarnings("ConstantConditions")
	@Mod.EventHandler
	public void onFMLMissingMappings(final FMLMissingMappingsEvent event) {
		for (final FMLMissingMappingsEvent.MissingMapping mapping: event.get()) {
			if (mapping.type == GameRegistry.Type.ITEM) {
				switch (mapping.name) {
					case "WarpDrive:airBlock":
						mapping.remap(Item.getItemFromBlock(blockAir));
						break;
					case "WarpDrive:airCanisterFull":
					case "WarpDrive:itemAirCanisterFull":
					case "WarpDrive:itemAirTank":
						mapping.remap(itemAirTanks[0]);
						break;
					case "WarpDrive:airgenBlock":
						mapping.remap(Item.getItemFromBlock(blockAirGenerator));
						break;
					case "WarpDrive:blockHAMachine":
						mapping.remap(Item.getItemFromBlock(blockHighlyAdvancedMachine));
						break;
					case "WarpDrive:boosterBlock":
						mapping.remap(Item.getItemFromBlock(blockLaserMedium));
						break;
					case "WarpDrive:cameraBlock":
						mapping.remap(Item.getItemFromBlock(blockCamera));
						break;
					case "WarpDrive:chunkLoader":
						mapping.remap(Item.getItemFromBlock(blockChunkLoader));
						break;
					case "WarpDrive:cloakBlock":
						mapping.remap(Item.getItemFromBlock(blockCloakingCore));
						break;
					case "WarpDrive:cloakCoilBlock":
						mapping.remap(Item.getItemFromBlock(blockCloakingCoil));
						break;
					case "WarpDrive:component":
						mapping.remap(itemComponent);
						break;
					case "WarpDrive:decorative":
						mapping.remap(Item.getItemFromBlock(blockDecorative));
						break;
					case "WarpDrive:gasBlock":
						mapping.remap(Item.getItemFromBlock(blockGas));
						break;
					case "WarpDrive:helmet":
					case "WarpDrive:itemHelmet":
						mapping.remap(itemWarpArmor[3]);
						break;
					case "WarpDrive:iridiumBlock":
						mapping.remap(Item.getItemFromBlock(blockIridium));
						break;
					case "WarpDrive:isolationBlock":
						mapping.remap(Item.getItemFromBlock(blockWarpIsolation));
						break;
					case "WarpDrive:laserBlock":
						mapping.remap(Item.getItemFromBlock(blockLaser));
						break;
					case "WarpDrive:laserCamBlock":
						mapping.remap(Item.getItemFromBlock(blockLaserCamera));
						break;
					case "WarpDrive:laserTreeFarmBlock":
						mapping.remap(Item.getItemFromBlock(blockLaserTreeFarm));
						break;
					case "WarpDrive:liftBlock":
						mapping.remap(Item.getItemFromBlock(blockLift));
						break;
					case "WarpDrive:miningLaserBlock":
						mapping.remap(Item.getItemFromBlock(blockMiningLaser));
						break;
					case "WarpDrive:monitorBlock":
						mapping.remap(Item.getItemFromBlock(blockMonitor));
						break;
					case "WarpDrive:powerLaser":
						mapping.remap(Item.getItemFromBlock(blockEnanReactorLaser));
						break;
					case "WarpDrive:powerReactor":
						mapping.remap(Item.getItemFromBlock(blockEnanReactorCore));
						break;
					case "WarpDrive:powerStore":
						mapping.remap(Item.getItemFromBlock(blockEnergyBank));
						break;
					case "WarpDrive:protocolBlock":
						mapping.remap(Item.getItemFromBlock(blockShipController));
						break;
					case "WarpDrive:radarBlock":
						mapping.remap(Item.getItemFromBlock(blockRadar));
						break;
					case "WarpDrive:reactorLaserFocus":
						mapping.remap(itemIC2reactorLaserFocus);
						break;
					case "WarpDrive:reactorMonitor":
						mapping.remap(Item.getItemFromBlock(blockIC2reactorLaserCooler));
						break;
					case "WarpDrive:scannerBlock":
						mapping.remap(Item.getItemFromBlock(blockShipScanner));
						break;
					case "WarpDrive:transportBeacon":
					case "WarpDrive:blockTransportBeacon":
						mapping.remap(Item.getItemFromBlock(blockTransporterBeacon));
						break;
					case "WarpDrive:transporter":
					case "WarpDrive:blockTransporter":
						mapping.remap(Item.getItemFromBlock(blockTransporterCore));
						break;
					case "WarpDrive:warpCore":
						mapping.remap(Item.getItemFromBlock(blockShipCore));
						break;
					case "WarpDrive:itemTuningRod":
						mapping.remap(itemTuningFork);
						break;
					case "WarpDrive:itemCrystalToken":
						mapping.remap(itemShipToken);
						break;
				}
				
			} else if (mapping.type == GameRegistry.Type.BLOCK) {
				switch (mapping.name) {
					case "WarpDrive:airBlock":
						mapping.remap(blockAir);
						break;
					case "WarpDrive:airgenBlock":
						mapping.remap(blockAirGenerator);
						break;
					case "WarpDrive:blockHAMachine":
						mapping.remap(blockHighlyAdvancedMachine);
						break;
					case "WarpDrive:boosterBlock":
						mapping.remap(blockLaserMedium);
						break;
					case "WarpDrive:cameraBlock":
						mapping.remap(blockCamera);
						break;
					case "WarpDrive:chunkLoader":
						mapping.remap(blockChunkLoader);
						break;
					case "WarpDrive:cloakBlock":
						mapping.remap(blockCloakingCore);
						break;
					case "WarpDrive:cloakCoilBlock":
						mapping.remap(blockCloakingCoil);
						break;
					case "WarpDrive:decorative":
						mapping.remap(blockDecorative);
						break;
					case "WarpDrive:gasBlock":
						mapping.remap(blockGas);
						break;
					case "WarpDrive:iridiumBlock":
						mapping.remap(blockIridium);
						break;
					case "WarpDrive:isolationBlock":
						mapping.remap(blockWarpIsolation);
						break;
					case "WarpDrive:laserBlock":
						mapping.remap(blockLaser);
						break;
					case "WarpDrive:laserCamBlock":
						mapping.remap(blockLaserCamera);
						break;
					case "WarpDrive:laserTreeFarmBlock":
						mapping.remap(blockLaserTreeFarm);
						break;
					case "WarpDrive:liftBlock":
						mapping.remap(blockLift);
						break;
					case "WarpDrive:miningLaserBlock":
						mapping.remap(blockMiningLaser);
						break;
					case "WarpDrive:monitorBlock":
						mapping.remap(blockMonitor);
						break;
					case "WarpDrive:powerLaser":
						mapping.remap(blockEnanReactorLaser);
						break;
					case "WarpDrive:powerReactor":
						mapping.remap(blockEnanReactorCore);
						break;
					case "WarpDrive:powerStore":
						mapping.remap(blockEnergyBank);
						break;
					case "WarpDrive:protocolBlock":
						mapping.remap(blockShipController);
						break;
					case "WarpDrive:radarBlock":
						mapping.remap(blockRadar);
						break;
					case "WarpDrive:reactorMonitor":
						mapping.remap(blockIC2reactorLaserCooler);
						break;
					case "WarpDrive:scannerBlock":
						mapping.remap(blockShipScanner);
						break;
					case "WarpDrive:transportBeacon":
					case "WarpDrive:blockTransportBeacon":
						mapping.remap(blockTransporterBeacon);
						break;
					case "WarpDrive:transporter":
					case "WarpDrive:blockTransporter":
						mapping.remap(blockTransporterCore);
						break;
					case "WarpDrive:warpCore":
						mapping.remap(blockShipCore);
						break;
				}
			}
		}
	}
	/**/
	
	final public static ArrayList<Biome> biomes = new ArrayList<>(10);
	final public static ArrayList<Block> blocks = new ArrayList<>(100);
	final public static ArrayList<Enchantment> enchantments = new ArrayList<>(10);
	final public static ArrayList<Item> items = new ArrayList<>(50);
	final public static ArrayList<Potion> potions = new ArrayList<>(10);
	final public static ArrayList<PotionType> potionTypes = new ArrayList<>(10);
	final public static ArrayList<SoundEvent> soundEvents = new ArrayList<>(100);
	final public static HashMap<ResourceLocation, IRecipe> recipes = new HashMap<>(100);
	final public static ArrayList<VillagerProfession> villagerProfessions = new ArrayList<>(10);
	
	// Register a Biome.
	public static <BIOME extends Biome> BIOME register(@Nonnull final BIOME biome) {
		biomes.add(biome);
		return biome;
	}
	
	// Register a Block with the default ItemBlock class.
	public static <BLOCK extends Block> BLOCK register(@Nonnull final BLOCK block) {
		assert block instanceof IBlockBase;
		return register(block, ((IBlockBase) block).createItemBlock());
	}
	
	// Register a Block with a custom ItemBlock class.
	public static <BLOCK extends Block> BLOCK register(@Nonnull final BLOCK block, @Nullable final ItemBlock itemBlock) {
		final ResourceLocation resourceLocation = block.getRegistryName();
		if (resourceLocation == null) {
			WarpDrive.logger.error(String.format("Missing registry name for block %s, ignoring registration...",
			                                     block));
			return block;
		}
		
		blocks.add(block);
		
		if (itemBlock != null) {
			itemBlock.setRegistryName(resourceLocation);
			register(itemBlock);
		}
		
		return block;
	}
	
	// Register an Enchantment.
	public static <ENCHANTMENT extends Enchantment> ENCHANTMENT register(@Nonnull final ENCHANTMENT enchantment) {
		enchantments.add(enchantment);
		return enchantment;
	}
	
	// Register an Item.
	public static <ITEM extends Item> ITEM register(@Nonnull final ITEM item) {
		items.add(item);
		return item;
	}
	
	// Register an Potion.
	public static <POTION extends Potion> POTION register(@Nonnull final POTION potion) {
		potions.add(potion);
		return potion;
	}
	
	// Register an PotionType.
	public static <POTION_TYPE extends PotionType> POTION_TYPE register(@Nonnull final POTION_TYPE potionType) {
		potionTypes.add(potionType);
		return potionType;
	}
	
	// Register a recipe.
	public static <RECIPE extends IRecipe> RECIPE register(@Nonnull final RECIPE recipe) {
		return register(recipe, "");
	}
	public static <RECIPE extends IRecipe> RECIPE register(@Nonnull final RECIPE recipe, final String suffix) {
		ResourceLocation registryName = recipe.getRegistryName();
		if (registryName == null) {
			final String path;
			final ItemStack itemStackOutput = recipe.getRecipeOutput();
			assert itemStackOutput.getItem().getRegistryName() != null;
			if (itemStackOutput.isEmpty()) {
				path = recipe.toString();
			} else if (itemStackOutput.getCount() == 1) {
				path = String.format("%s@%d%s",
				                     itemStackOutput.getItem().getRegistryName().getPath(),
				                     itemStackOutput.getItemDamage(),
				                     suffix );
			} else {
				path = String.format("%s@%dx%d%s",
				                     itemStackOutput.getItem().getRegistryName().getPath(),
				                     itemStackOutput.getItemDamage(),
				                     itemStackOutput.getCount(),
				                     suffix );
			}
			registryName = new ResourceLocation(MODID, path);
			if (recipes.containsKey(registryName)) {
				logger.error(String.format("Overlapping recipe detected, please report this to the mod author %s",
				                           registryName));
				registryName = new ResourceLocation(MODID, path + "!" + System.nanoTime());
				try {
					Thread.sleep(10000);
				} catch (final Exception exception) {
					// ignored
				}
				assert false;
			}
			recipe.setRegistryName(registryName);
		}
		
		recipes.put(registryName, recipe);
		return recipe;
	}
	
	// Register a SoundEvent.
	public static <SOUND_EVENT extends SoundEvent> SOUND_EVENT register(@Nonnull final SOUND_EVENT soundEvent) {
		soundEvents.add(soundEvent);
		return soundEvent;
	}
	
	// Register a VillagerProfession.
	public static <VILLAGER_PROFESSION extends VillagerProfession> VILLAGER_PROFESSION register(@Nonnull final VILLAGER_PROFESSION villagerProfession) {
		villagerProfessions.add(villagerProfession);
		return villagerProfession;
	}
	
	@SubscribeEvent
	public void onRegisterBiomes(@Nonnull final RegistryEvent.Register<Biome> event) {
		WarpDrive.logger.debug(String.format("Registering %s", event.getName()));
		for (final Biome biome : biomes) {
			event.getRegistry().register(biome);
		}
		
		BiomeDictionary.addTypes(biomeSpace, BiomeDictionary.Type.DEAD, BiomeDictionary.Type.WASTELAND);
	}
	
	@SubscribeEvent
	public void onRegisterBlocks(@Nonnull final RegistryEvent.Register<Block> event) {
		WarpDrive.logger.debug(String.format("Registering %s", event.getName()));
		for (final Block block : blocks) {
			event.getRegistry().register(block);
		}
		
		GameRegistry.registerTileEntity(TileEntityAcceleratorController.class, new ResourceLocation(WarpDrive.MODID, "accelerator_controller"));
		GameRegistry.registerTileEntity(TileEntityAcceleratorControlPoint.class, new ResourceLocation(WarpDrive.MODID, "accelerator_control_point"));
		GameRegistry.registerTileEntity(TileEntityAirGeneratorTiered.class, new ResourceLocation(WarpDrive.MODID, "air_generator"));
		GameRegistry.registerTileEntity(TileEntityCamera.class, new ResourceLocation(WarpDrive.MODID, "camera"));
		GameRegistry.registerTileEntity(TileEntityCapacitor.class, new ResourceLocation(WarpDrive.MODID, "capacitor"));
		GameRegistry.registerTileEntity(TileEntityChunkLoader.class, new ResourceLocation(WarpDrive.MODID, "chunk_loader"));
		GameRegistry.registerTileEntity(TileEntityCloakingCore.class, new ResourceLocation(WarpDrive.MODID, "cloaking_core"));
		GameRegistry.registerTileEntity(TileEntityEnanReactorCore.class, new ResourceLocation(WarpDrive.MODID, "enan_reactor_core"));
		GameRegistry.registerTileEntity(TileEntityEnanReactorLaser.class, new ResourceLocation(WarpDrive.MODID, "enan_reactor_laser"));
		GameRegistry.registerTileEntity(TileEntityForceField.class, new ResourceLocation(WarpDrive.MODID, "force_field"));
		GameRegistry.registerTileEntity(TileEntityForceFieldProjector.class, new ResourceLocation(WarpDrive.MODID, "force_field_projector"));
		GameRegistry.registerTileEntity(TileEntityForceFieldRelay.class, new ResourceLocation(WarpDrive.MODID, "force_field_relay"));
		GameRegistry.registerTileEntity(TileEntityIC2reactorLaserMonitor.class, new ResourceLocation(WarpDrive.MODID, "ic2_reactor_laser_monitor"));
		GameRegistry.registerTileEntity(TileEntityJumpGateCore.class, new ResourceLocation(WarpDrive.MODID, "jump_gate_core"));
		GameRegistry.registerTileEntity(TileEntityLaser.class, new ResourceLocation(WarpDrive.MODID, "laser"));
		GameRegistry.registerTileEntity(TileEntityLaserCamera.class, new ResourceLocation(WarpDrive.MODID, "laser_camera"));
		GameRegistry.registerTileEntity(TileEntityLaserMedium.class, new ResourceLocation(WarpDrive.MODID, "laser_medium"));
		GameRegistry.registerTileEntity(TileEntityLaserTreeFarm.class, new ResourceLocation(WarpDrive.MODID, "laser_tree_farm"));
		GameRegistry.registerTileEntity(TileEntityLift.class, new ResourceLocation(WarpDrive.MODID, "lift"));
		GameRegistry.registerTileEntity(TileEntityMiningLaser.class, new ResourceLocation(WarpDrive.MODID, "mining_laser"));
		GameRegistry.registerTileEntity(TileEntityMonitor.class, new ResourceLocation(WarpDrive.MODID, "monitor"));
		GameRegistry.registerTileEntity(TileEntityParticlesInjector.class, new ResourceLocation(WarpDrive.MODID, "particles_injector"));
		GameRegistry.registerTileEntity(TileEntityRadar.class, new ResourceLocation(WarpDrive.MODID, "radar"));
		GameRegistry.registerTileEntity(TileEntitySecurityStation.class, new ResourceLocation(WarpDrive.MODID, "security_station"));
		GameRegistry.registerTileEntity(TileEntityShipController.class, new ResourceLocation(WarpDrive.MODID, "ship_controller"));
		GameRegistry.registerTileEntity(TileEntityShipCore.class, new ResourceLocation(WarpDrive.MODID, "ship_core"));
		GameRegistry.registerTileEntity(TileEntityShipScanner.class, new ResourceLocation(WarpDrive.MODID, "ship_scanner"));
		GameRegistry.registerTileEntity(TileEntitySiren.class, new ResourceLocation(WarpDrive.MODID, "siren"));
		GameRegistry.registerTileEntity(TileEntityTransporterBeacon.class, new ResourceLocation(WarpDrive.MODID, "transporter_beacon"));
		GameRegistry.registerTileEntity(TileEntityTransporterCore.class, new ResourceLocation(WarpDrive.MODID, "transporter_core"));
		GameRegistry.registerTileEntity(TileEntityWeaponController.class, new ResourceLocation(WarpDrive.MODID, "weapon_controller"));
	}
	
	@SubscribeEvent
	public void onRegisterEnchantments(@Nonnull final RegistryEvent.Register<Enchantment> event) {
		WarpDrive.logger.debug(String.format("Registering %s", event.getName()));
		for (final Enchantment enchantment : enchantments) {
			event.getRegistry().register(enchantment);
		}
	}
	
	@SubscribeEvent
	public void onRegisterEntities(@Nonnull final RegistryEvent.Register<EntityEntry> event) {
		WarpDrive.logger.debug(String.format("Registering %s", event.getName()));
		
		EntityEntry entityEntry;
		
		entityEntry = EntityEntryBuilder.create()
		                                .entity(EntitySphereGen.class).factory(EntitySphereGen::new)
		                                .tracker(200, 1, false)
		                                .id("entitySphereGenerator", WarpDriveConfig.G_ENTITY_SPHERE_GENERATOR_ID).name("EntitySphereGenerator")
		                                .build();
		event.getRegistry().register(entityEntry);
		
		entityEntry = EntityEntryBuilder.create()
		                                .entity(EntityStarCore.class).factory(EntityStarCore::new)
		                                .tracker(300, 1, false)
		                                .id("entityStarCore", WarpDriveConfig.G_ENTITY_STAR_CORE_ID).name("EntityStarCore")
		                                .build();
		event.getRegistry().register(entityEntry);
		
		entityEntry = EntityEntryBuilder.create()
		                                .entity(EntityCamera.class).factory(EntityCamera::new)
		                                .tracker(300, 1, false)
		                                .id("entityCamera", WarpDriveConfig.G_ENTITY_CAMERA_ID).name("EntityCamera")
		                                .build();
		event.getRegistry().register(entityEntry);
		
		entityEntry = EntityEntryBuilder.create()
		                                .entity(EntityParticleBunch.class).factory(EntityParticleBunch::new)
		                                .tracker(300, 1, false)
		                                .id("entityParticleBunch", WarpDriveConfig.G_ENTITY_PARTICLE_BUNCH_ID).name("EntityParticleBunch")
		                                .build();
		event.getRegistry().register(entityEntry);
	}
	
	@SubscribeEvent
	public void onRegisterItems(@Nonnull final RegistryEvent.Register<Item> event) {
		WarpDrive.logger.debug(String.format("Registering %s", event.getName()));
		for (final Item item : items) {
			event.getRegistry().register(item);
			proxy.onModelInitialisation(item);
		}
		for (final Block block : blocks) {
			proxy.onModelInitialisation(block);
		}
	}
	
	@SubscribeEvent
	public void onRegisterPotions(@Nonnull final RegistryEvent.Register<Potion> event) {
		WarpDrive.logger.debug(String.format("Registering %s", event.getName()));
		for (final Potion potion : potions) {
			event.getRegistry().register(potion);
		}
	}
	
	@SubscribeEvent
	public void onRegisterPotionTypes(@Nonnull final RegistryEvent.Register<PotionType> event) {
		WarpDrive.logger.debug(String.format("Registering %s", event.getName()));
		for (final PotionType potionType : potionTypes) {
			event.getRegistry().register(potionType);
		}
	}
	
	@SubscribeEvent
	public void onRegisterRecipes(@Nonnull final RegistryEvent.Register<IRecipe> event) {
		WarpDrive.logger.debug(String.format("Registering %s", event.getName()));
		
		Recipes.initOreDictionary();
		
		Recipes.initDynamic();
		
		for (final IRecipe recipe : recipes.values()) {
			event.getRegistry().register(recipe);
		}
	}
	
	@SubscribeEvent
	public void onRegisterSoundEvents(@Nonnull final RegistryEvent.Register<SoundEvent> event) {
		WarpDrive.logger.debug(String.format("Registering %s", event.getName()));
		cr0s.warpdrive.data.SoundEvents.registerSounds();
		for (final SoundEvent soundEvent : soundEvents) {
			event.getRegistry().register(soundEvent);
		}
	}
	
	@SubscribeEvent
	public void onRegisterVillagerProfessions(@Nonnull final RegistryEvent.Register<VillagerProfession> event) {
		WarpDrive.logger.debug(String.format("Registering %s", event.getName()));
		for (final VillagerProfession villagerProfession : villagerProfessions) {
			event.getRegistry().register(villagerProfession);
		}
	}
}