package com.moostoet.pyrotech.datagen.hunting;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.hunting.HuntingEntities;
import com.moostoet.pyrotech.hunting.HuntingTags;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The mud's spawns: the 1.12 {@code EntityRegistry.addSpawn} at weight 100 in packs of one
 * to three, as an {@code add_spawns} biome modifier over the same tag the spawn rule reads.
 */
public final class HuntingRegistryProvider extends DatapackBuiltinEntriesProvider {

    private static final ResourceKey<BiomeModifier> ADD_MUD_SPAWNS =
        ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "add_mud_spawns"));
    private static final int SPAWN_WEIGHT = 100;
    private static final int SPAWN_COUNT_MIN = 1;
    private static final int SPAWN_COUNT_MAX = 3;

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
        .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, HuntingRegistryProvider::biomeModifiers);

    public HuntingRegistryProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, BUILDER, Set.of(Pyrotech.MOD_ID));
    }

    @Override
    public String getName() {
        return "Hunting Registries";
    }

    private static void biomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        context.register(ADD_MUD_SPAWNS, BiomeModifiers.AddSpawnsBiomeModifier.singleSpawn(
            biomes.getOrThrow(HuntingTags.Biomes.MUD_SPAWN_BIOMES),
            new MobSpawnSettings.SpawnerData(HuntingEntities.MUD.get(), SPAWN_WEIGHT, SPAWN_COUNT_MIN, SPAWN_COUNT_MAX)));
    }
}
