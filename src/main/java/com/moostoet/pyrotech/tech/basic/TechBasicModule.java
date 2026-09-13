package com.moostoet.pyrotech.tech.basic;

import com.moostoet.pyrotech.core.CoreModule;
import com.moostoet.pyrotech.tech.basic.client.AnvilHitParticles;
import com.moostoet.pyrotech.tech.basic.event.CampfireEffectTracker;
import com.moostoet.pyrotech.tech.basic.event.ComfortHandler;
import com.moostoet.pyrotech.tech.basic.event.FocusedHandler;
import com.moostoet.pyrotech.tech.basic.event.RestingHandler;
import com.moostoet.pyrotech.tech.basic.event.WellFedHandler;
import com.moostoet.pyrotech.tech.basic.event.WorktableRepeatHandler;
import com.moostoet.pyrotech.tech.basic.network.AnvilHitPayload;
import com.moostoet.pyrotech.tech.basic.recipe.TechBasicRecipeSerializers;
import com.moostoet.pyrotech.tech.basic.recipe.TechBasicRecipeTypes;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The tech/basic module: the campfire, the pit kiln, the chopping block, the three
 * anvils, the barrel, the compacting bin, the compost bin, the two drying racks, the
 * soaking pot, the tanning rack, the two worktables, tinder, the marshmallows, and the
 * five campfire effects. Registers on the mod bus once, from the mod constructor.
 */
public final class TechBasicModule {

    private TechBasicModule() {
    }

    public static void register(IEventBus modEventBus, ModContainer modContainer) {
        TechBasicBlocks.BLOCKS.register(modEventBus);
        TechBasicItems.ITEMS.register(modEventBus);
        TechBasicBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        TechBasicComponents.COMPONENTS.register(modEventBus);
        TechBasicAttachments.ATTACHMENT_TYPES.register(modEventBus);
        TechBasicEffects.MOB_EFFECTS.register(modEventBus);
        TechBasicRecipeTypes.RECIPE_TYPES.register(modEventBus);
        TechBasicRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(TechBasicModule::registerCapabilities);
        modEventBus.addListener(TechBasicModule::registerPayloads);
        modEventBus.addListener(TechBasicModule::addToTab);
        NeoForge.EVENT_BUS.register(CampfireEffectTracker.class);
        NeoForge.EVENT_BUS.register(ComfortHandler.class);
        NeoForge.EVENT_BUS.register(RestingHandler.class);
        NeoForge.EVENT_BUS.register(WellFedHandler.class);
        NeoForge.EVENT_BUS.register(FocusedHandler.class);
        NeoForge.EVENT_BUS.register(WorktableRepeatHandler.class);
        modContainer.registerConfig(ModConfig.Type.COMMON, TechBasicConfig.COMMON_SPEC, "pyrotech-tech-basic.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, TechBasicConfig.SERVER_SPEC, "pyrotech-tech-basic-server.toml");
    }

    /** Every inventory is automatable, as 1.12 had it with {@code ALLOW_AUTOMATION} on; the barrel only from above and open. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TechBasicBlockEntities.CAMPFIRE.get(),
            (campfire, side) -> side == Direction.DOWN ? campfire.output() : campfire.input());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TechBasicBlockEntities.KILN_PIT.get(),
            (kiln, side) -> side == Direction.DOWN ? kiln.output() : kiln.input());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TechBasicBlockEntities.CHOPPING_BLOCK.get(), (block, side) -> block.input());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TechBasicBlockEntities.ANVIL.get(), (anvil, side) -> anvil.input());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TechBasicBlockEntities.COMPACTING_BIN.get(), (bin, side) -> bin.input());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TechBasicBlockEntities.COMPOST_BIN.get(),
            (bin, side) -> side == Direction.DOWN ? bin.output() : bin.input());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TechBasicBlockEntities.COMPOST_BIN.get(), (bin, side) -> bin.fillOnlyTank());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TechBasicBlockEntities.DRYING_RACK.get(),
            (rack, side) -> side == Direction.DOWN ? rack.output() : rack.input());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TechBasicBlockEntities.SOAKING_POT.get(),
            (pot, side) -> side == Direction.DOWN ? pot.output() : pot.input());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TechBasicBlockEntities.SOAKING_POT.get(), (pot, side) -> pot.tank());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TechBasicBlockEntities.TANNING_RACK.get(),
            (rack, side) -> side == Direction.DOWN ? rack.output() : rack.input());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TechBasicBlockEntities.WORKTABLE.get(), (table, side) -> table.grid());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TechBasicBlockEntities.BARREL.get(),
            (barrel, side) -> side == Direction.UP && !barrel.isSealed() ? barrel.input() : null);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TechBasicBlockEntities.BARREL.get(),
            (barrel, side) -> side == Direction.UP && !barrel.isSealed() ? barrel.tank() : null);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(AnvilHitPayload.TYPE, AnvilHitPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> AnvilHitParticles.spawn(payload)));
    }

    private static void addToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CoreModule.TAB.getKey())) {
            TechBasicItems.addToTab(event);
        }
    }
}
