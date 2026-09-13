package com.moostoet.pyrotech.worldgen;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.worldgen.feature.CaveFloorClusterFeature;
import com.moostoet.pyrotech.worldgen.feature.DenseCoalFeature;
import com.moostoet.pyrotech.worldgen.feature.MudFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The three feature types that keep 1.12 logic. The other eight generators are vanilla
 * {@code minecraft:ore} and {@code minecraft:random_patch} features in the generated JSON.
 */
public final class WorldgenFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Pyrotech.MOD_ID);

    public static final DeferredHolder<Feature<?>, DenseCoalFeature> DENSE_COAL =
        FEATURES.register("dense_coal", DenseCoalFeature::new);
    public static final DeferredHolder<Feature<?>, CaveFloorClusterFeature> CAVE_FLOOR_CLUSTER =
        FEATURES.register("cave_floor_cluster", CaveFloorClusterFeature::new);
    public static final DeferredHolder<Feature<?>, MudFeature> MUD =
        FEATURES.register("mud", MudFeature::new);

    private WorldgenFeatures() {
    }
}
