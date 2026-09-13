package com.moostoet.pyrotech.hunting;

import com.moostoet.pyrotech.core.CoreModule;
import com.moostoet.pyrotech.hunting.entity.MudEntity;
import com.moostoet.pyrotech.hunting.event.CarcassDropsHandler;
import com.moostoet.pyrotech.hunting.event.StuckSpearsHandler;
import com.moostoet.pyrotech.hunting.recipe.HuntingRecipeSerializers;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * The hunting module: the animated mud, the carcass and the butcher's block, the pelts and
 * hides, the knives, the spears and arrows, the leather kits, and tannin. Registers on the
 * mod bus once, from the mod constructor.
 */
public final class HuntingModule {

    private HuntingModule() {
    }

    public static void register(IEventBus modEventBus, ModContainer modContainer) {
        HuntingBlocks.BLOCKS.register(modEventBus);
        HuntingItems.ITEMS.register(modEventBus);
        HuntingFluids.FLUID_TYPES.register(modEventBus);
        HuntingFluids.FLUIDS.register(modEventBus);
        HuntingBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        HuntingEntities.ENTITY_TYPES.register(modEventBus);
        HuntingComponents.COMPONENTS.register(modEventBus);
        HuntingAttachments.ATTACHMENT_TYPES.register(modEventBus);
        HuntingRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(HuntingModule::registerAttributes);
        modEventBus.addListener(HuntingModule::registerSpawnPlacements);
        modEventBus.addListener(HuntingModule::registerDataMapTypes);
        modEventBus.addListener(HuntingModule::addToTab);
        NeoForge.EVENT_BUS.register(CarcassDropsHandler.class);
        NeoForge.EVENT_BUS.register(StuckSpearsHandler.class);
        modContainer.registerConfig(ModConfig.Type.COMMON, HuntingConfig.COMMON_SPEC, "pyrotech-hunting.toml");
    }

    /** The slime baseline; {@code Slime.setSize} rewrites health, speed, and damage per size at spawn. */
    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HuntingEntities.MUD.get(), Monster.createMonsterAttributes().build());
    }

    /** Vanilla slime's placement over the tag-swapped rule (hunting sign-off, item 1). */
    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(HuntingEntities.MUD.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            MudEntity::checkMudSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void registerDataMapTypes(RegisterDataMapTypesEvent event) {
        event.register(HuntingDataMaps.CARCASS_DROPS);
        event.register(HuntingDataMaps.KNIFE_EFFICIENCY);
        event.register(HuntingDataMaps.BUTCHERING_TRANSFORMS);
    }

    private static void addToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CoreModule.TAB.getKey())) {
            HuntingItems.addToTab(event);
        }
    }
}
