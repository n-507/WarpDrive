package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;

public enum EnumReactorFace implements IStringSerializable {
	
	//                                     tier              inst name               x   y   z  facingLaserProperty
	UNKNOWN                               (null             , -1, "unknown"       ,  0,  0,  0, null            ),
	
	BASIC_LASER_SOUTH                     (EnumTier.BASIC   ,  0, "laser_south"   ,  0,  0, -2, EnumFacing.NORTH),
	BASIC_LASER_NORTH                     (EnumTier.BASIC   ,  1, "laser_north"   ,  0,  0,  2, EnumFacing.SOUTH),
	BASIC_LASER_EAST                      (EnumTier.BASIC   ,  2, "laser_east"    , -2,  0,  0, EnumFacing.WEST ),
	BASIC_LASER_WEST                      (EnumTier.BASIC   ,  3, "laser_west"    ,  2,  0,  0, EnumFacing.EAST ),
	BASIC_LENS_NORTH                      (EnumTier.BASIC   , -1, "lens_north"    ,  0,  0, -1, null            ),
	BASIC_LENS_SOUTH                      (EnumTier.BASIC   , -1, "lens_south"    ,  0,  0,  1, null            ),
	BASIC_LENS_EAST                       (EnumTier.BASIC   , -1, "lens_east"     , -1,  0,  0, null            ),
	BASIC_LENS_WEST                       (EnumTier.BASIC   , -1, "lens_west"     ,  1,  0,  0, null            ),
	
	ADVANCED_LASER_SOUTH_BOTTOM           (EnumTier.ADVANCED,  0, "laser_south"   ,  1,  2, -3, EnumFacing.NORTH),
	ADVANCED_LASER_SOUTH_TOP              (EnumTier.ADVANCED,  1, "laser_south"   , -1,  4, -3, EnumFacing.NORTH),
	ADVANCED_LASER_NORTH_BOTTOM           (EnumTier.ADVANCED,  2, "laser_north"   , -1,  2,  3, EnumFacing.SOUTH),
	ADVANCED_LASER_NORTH_TOP              (EnumTier.ADVANCED,  3, "laser_north"   ,  1,  4,  3, EnumFacing.SOUTH),
	ADVANCED_LASER_EAST_BOTTOM            (EnumTier.ADVANCED,  4, "laser_east"    , -3,  2, -1, EnumFacing.WEST ),
	ADVANCED_LASER_EAST_TOP               (EnumTier.ADVANCED,  5, "laser_east"    , -3,  4,  1, EnumFacing.WEST ),
	ADVANCED_LASER_WEST_BOTTOM            (EnumTier.ADVANCED,  6, "laser_west"    ,  3,  2,  1, EnumFacing.EAST ),
	ADVANCED_LASER_WEST_TOP               (EnumTier.ADVANCED,  7, "laser_west"    ,  3,  4, -1, EnumFacing.EAST ),
	ADVANCED_LENS_SOUTH_BOTTOM            (EnumTier.ADVANCED, -1, "lens_south"    ,  1,  2, -2, null            ),
	ADVANCED_LENS_SOUTH_TOP               (EnumTier.ADVANCED, -1, "lens_south"    , -1,  4, -2, null            ),
	ADVANCED_LENS_NORTH_BOTTOM            (EnumTier.ADVANCED, -1, "lens_north"    , -1,  2,  2, null            ),
	ADVANCED_LENS_NORTH_TOP               (EnumTier.ADVANCED, -1, "lens_north"    ,  1,  4,  2, null            ),
	ADVANCED_LENS_EAST_BOTTOM             (EnumTier.ADVANCED, -1, "lens_east"     , -2,  2, -1, null            ),
	ADVANCED_LENS_EAST_TOP                (EnumTier.ADVANCED, -1, "lens_east"     , -2,  4,  1, null            ),
	ADVANCED_LENS_WEST_BOTTOM             (EnumTier.ADVANCED, -1, "lens_west"     ,  2,  2,  1, null            ),
	ADVANCED_LENS_WEST_TOP                (EnumTier.ADVANCED, -1, "lens_west"     ,  2,  4, -1, null            ),
	ADVANCED_CORE_CENTER                  (EnumTier.ADVANCED, -1, "core_center"   ,  0,  3, -1, null            ),
	ADVANCED_CORE_BOTTOM                  (EnumTier.ADVANCED, -1, "core_bottom"   ,  0,  2,  0, null            ),
	ADVANCED_CORE_TOP                     (EnumTier.ADVANCED, -1, "core_top"      ,  0,  4,  0, null            ),
	ADVANCED_CORE_NORTH                   (EnumTier.ADVANCED, -1, "core_north"    ,  0,  3, -1, null            ),
	ADVANCED_CORE_SOUTH                   (EnumTier.ADVANCED, -1, "core_south"    ,  0,  3,  1, null            ),
	ADVANCED_CORE_EAST                    (EnumTier.ADVANCED, -1, "core_east"     , -1,  3,  0, null            ),
	ADVANCED_CORE_WEST                    (EnumTier.ADVANCED, -1, "core_west"     ,  1,  3,  0, null            ),
	ADVANCED_SHELL1_BOTTOM_NORTH          (EnumTier.ADVANCED, -1, "shell_inner"   ,  0,  2, -1, null            ),
	ADVANCED_SHELL1_BOTTOM_SOUTH          (EnumTier.ADVANCED, -1, "shell_inner"   ,  0,  2,  1, null            ),
	ADVANCED_SHELL1_BOTTOM_EAST           (EnumTier.ADVANCED, -1, "shell_inner"   , -1,  2,  0, null            ),
	ADVANCED_SHELL1_BOTTOM_WEST           (EnumTier.ADVANCED, -1, "shell_inner"   ,  1,  2,  0, null            ),
	ADVANCED_SHELL1_TOP_NORTH             (EnumTier.ADVANCED, -1, "shell_inner"   ,  0,  4, -1, null            ),
	ADVANCED_SHELL1_TOP_SOUTH             (EnumTier.ADVANCED, -1, "shell_inner"   ,  0,  4,  1, null            ),
	ADVANCED_SHELL1_TOP_EAST              (EnumTier.ADVANCED, -1, "shell_inner"   , -1,  4,  0, null            ),
	ADVANCED_SHELL1_TOP_WEST              (EnumTier.ADVANCED, -1, "shell_inner"   ,  1,  4,  0, null            ),
	ADVANCED_SHELL1_NORTH_EAST            (EnumTier.ADVANCED, -1, "shell_inner"   , -1,  3, -1, null            ),
	ADVANCED_SHELL1_NORTH_WEST            (EnumTier.ADVANCED, -1, "shell_inner"   ,  1,  3, -1, null            ),
	ADVANCED_SHELL1_SOUTH_EAST            (EnumTier.ADVANCED, -1, "shell_inner"   , -1,  3,  1, null            ),
	ADVANCED_SHELL1_SOUTH_WEST            (EnumTier.ADVANCED, -1, "shell_inner"   ,  1,  3,  1, null            ),
	ADVANCED_SHELL2_BOTTOM                (EnumTier.ADVANCED, -1, "shell_outer"   ,  0,  1,  0, null            ),
	ADVANCED_SHELL2_TOP                   (EnumTier.ADVANCED, -1, "shell_outer"   ,  0,  5,  0, null            ),
	ADVANCED_SHELL2_NORTH                 (EnumTier.ADVANCED, -1, "shell_outer"   ,  0,  3, -2, null            ),
	ADVANCED_SHELL2_SOUTH                 (EnumTier.ADVANCED, -1, "shell_outer"   ,  0,  3,  2, null            ),
	ADVANCED_SHELL2_EAST                  (EnumTier.ADVANCED, -1, "shell_outer"   , -2,  3,  0, null            ),
	ADVANCED_SHELL2_WEST                  (EnumTier.ADVANCED, -1, "shell_outer"   ,  2,  3,  0, null            ),
	ADVANCED_SHELL2_BOTTOM_NORTH_EAST     (EnumTier.ADVANCED, -1, "shell_outer"   , -1,  2, -1, null            ),
	ADVANCED_SHELL2_BOTTOM_NORTH_WEST     (EnumTier.ADVANCED, -1, "shell_outer"   ,  1,  2, -1, null            ),
	ADVANCED_SHELL2_BOTTOM_SOUTH_EAST     (EnumTier.ADVANCED, -1, "shell_outer"   , -1,  2,  1, null            ),
	ADVANCED_SHELL2_BOTTOM_SOUTH_WEST     (EnumTier.ADVANCED, -1, "shell_outer"   ,  1,  2,  1, null            ),
	ADVANCED_SHELL2_TOP_NORTH_EAST        (EnumTier.ADVANCED, -1, "shell_outer"   , -1,  4, -1, null            ),
	ADVANCED_SHELL2_TOP_NORTH_WEST        (EnumTier.ADVANCED, -1, "shell_outer"   ,  1,  4, -1, null            ),
	ADVANCED_SHELL2_TOP_SOUTH_EAST        (EnumTier.ADVANCED, -1, "shell_outer"   , -1,  4,  1, null            ),
	ADVANCED_SHELL2_TOP_SOUTH_WEST        (EnumTier.ADVANCED, -1, "shell_outer"   ,  1,  4,  1, null            ),
	;
	
	public final EnumTier enumTier;
	public final int indexStability;
	public final String name;
	public final int x;
	public final int y;
	public final int z;
	public final EnumFacing facingLaserProperty;
	
	// cached values
	public static final int length;
	public static final int maxInstabilities;
	private static final HashMap<Integer, EnumReactorFace> ID_MAP = new HashMap<>();
	private static final HashMap<EnumTier, EnumReactorFace[]> TIER_ALL = new HashMap<>(EnumTier.length);
	private static final HashMap<EnumTier, EnumReactorFace[]> TIER_LASERS = new HashMap<>(EnumTier.length);
	
	static {
		length = EnumReactorFace.values().length;
		for (final EnumReactorFace reactorFace : values()) {
			ID_MAP.put(reactorFace.ordinal(), reactorFace);
		}
		
		// pre-build the list of lasers in the structure
		final HashMap<EnumTier, ArrayList<EnumReactorFace>> tierAll = new HashMap<>(EnumTier.length);
		final HashMap<EnumTier, ArrayList<EnumReactorFace>> tierLasers = new HashMap<>(EnumTier.length);
		for (final EnumTier tierLoop : EnumTier.values()) {
			tierAll.put(tierLoop, new ArrayList<>(16));
			tierLasers.put(tierLoop, new ArrayList<>(16));
		}
		for (final EnumReactorFace reactorFace : values()) {
			if (reactorFace.enumTier == null) {
				continue;
			}
			tierAll.get(reactorFace.enumTier).add(reactorFace);
			if (reactorFace.indexStability >= 0) {
				tierLasers.get(reactorFace.enumTier).add(reactorFace);
			}
		}
		for (final Entry<EnumTier, ArrayList<EnumReactorFace>> entry : tierAll.entrySet()) {
			TIER_ALL.put(entry.getKey(), entry.getValue().toArray(new EnumReactorFace[0]));
		}
		int max = 0;
		for (final Entry<EnumTier, ArrayList<EnumReactorFace>> entry : tierLasers.entrySet()) {
			TIER_LASERS.put(entry.getKey(), entry.getValue().toArray(new EnumReactorFace[0]));
			max = Math.max(max, entry.getValue().size());
		}
		maxInstabilities = max;
	}
	
	EnumReactorFace(final EnumTier enumTier, final int indexStability, final String name,
	                final int x, final int y, final int z,
	                final EnumFacing facingLaserProperty) {
		this.enumTier = enumTier;
		this.indexStability = indexStability;
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.facingLaserProperty = facingLaserProperty;
	}
	
	public static EnumReactorFace[] get(final EnumTier tier) {
		return TIER_ALL.get(tier);
	}
	
	public static EnumReactorFace[] getLasers(final EnumTier tier) {
		return TIER_LASERS.get(tier);
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
	
	public static EnumReactorFace get(final int ordinal) {
		return ID_MAP.get(ordinal);
	}
}
