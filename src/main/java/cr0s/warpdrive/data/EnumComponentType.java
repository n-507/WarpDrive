package cr0s.warpdrive.data;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;

public enum EnumComponentType implements IStringSerializable {
	// processing
	MEMORY_CRYSTAL             ("memory_crystal"),
	CAPACITIVE_CRYSTAL         ("capacitive_crystal"),
	DIAMOND_CRYSTAL            ("diamond_crystal"),
	EMERALD_CRYSTAL            ("emerald_crystal"),
	
	// networking
	ENDER_COIL                 ("ender_coil"),
	DIAMOND_COIL               ("diamond_coil"),
	COMPUTER_INTERFACE         ("computer_interface"),
	
	// breathing
	BONE_CHARCOAL              ("bone_charcoal"),
	ACTIVATED_CARBON           ("activated_carbon"),
	AIR_CANISTER               ("air_canister_empty"),
	
	// human
	// (redstone)
	FLAT_SCREEN                ("flat_screen"),
	HOLOGRAPHIC_PROJECTOR      ("holographic_projector"),
	
	// mechanical
	GLASS_TANK                 ("glass_tank"),
	MOTOR                      ("motor"),
	PUMP                       ("pump"),
	
	// optical
	LENS                       ("lens"),
	ZOOM                       ("zoom"),
	DIFFRACTION_GRATING        ("diffraction_grating"),
	
	// energy
	POWER_INTERFACE            ("power_interface"),
	SUPERCONDUCTOR             ("superconductor"),
	
	// crafting components
	LASER_MEDIUM_EMPTY         ("laser_medium_empty"),
	ELECTROMAGNETIC_PROJECTOR  ("electromagnetic_projector"),
	REACTOR_CORE               ("reactor_core"),
	
	// materials
	RAW_RUBBER                 ("raw_rubber"),
	RUBBER                     ("rubber"),
	;
	
	private final String name;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumComponentType> ID_MAP = new HashMap<>();
	
	static {
		length = EnumComponentType.values().length;
		for (final EnumComponentType componentType : values()) {
			ID_MAP.put(componentType.ordinal(), componentType);
		}
	}
	
	EnumComponentType(final String name) {
		this.name = name;
	}
	
	public static EnumComponentType get(final int damage) {
		return ID_MAP.get(damage);
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
}
