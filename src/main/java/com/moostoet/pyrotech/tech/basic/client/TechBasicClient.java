package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.TechBasicBlocks;
import com.moostoet.pyrotech.tech.basic.TechBasicEffects;
import com.moostoet.pyrotech.tech.basic.TechBasicItems;
import com.moostoet.pyrotech.tech.basic.block.AnvilBlock;
import com.moostoet.pyrotech.tech.basic.block.BarrelBlock;
import com.moostoet.pyrotech.tech.basic.block.ChoppingBlockBlock;
import com.moostoet.pyrotech.tech.basic.block.WearStage;
import com.moostoet.pyrotech.tech.basic.item.MarshmallowStickItem;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Tech/basic's client registrations: the eleven block entity renderers, the item model predicates, and the effect icons. */
@EventBusSubscriber(modid = Pyrotech.MOD_ID, value = Dist.CLIENT)
public final class TechBasicClient {

    public static final ResourceLocation MARSHMALLOW_TYPE_PREDICATE = ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "marshmallow_type");
    public static final ResourceLocation SEALED_PREDICATE = ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "sealed");

    private TechBasicClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TechBasicBlockEntities.CAMPFIRE.get(), CampfireRenderer::new);
        event.registerBlockEntityRenderer(TechBasicBlockEntities.KILN_PIT.get(), PitKilnRenderer::new);
        event.registerBlockEntityRenderer(TechBasicBlockEntities.CHOPPING_BLOCK.get(), ChoppingBlockRenderer::new);
        event.registerBlockEntityRenderer(TechBasicBlockEntities.ANVIL.get(), AnvilRenderer::new);
        event.registerBlockEntityRenderer(TechBasicBlockEntities.BARREL.get(), BarrelRenderer::new);
        event.registerBlockEntityRenderer(TechBasicBlockEntities.COMPACTING_BIN.get(), CompactingBinRenderer::new);
        event.registerBlockEntityRenderer(TechBasicBlockEntities.COMPOST_BIN.get(), CompostBinRenderer::new);
        event.registerBlockEntityRenderer(TechBasicBlockEntities.DRYING_RACK.get(), DryingRackRenderer::new);
        event.registerBlockEntityRenderer(TechBasicBlockEntities.SOAKING_POT.get(), SoakingPotRenderer::new);
        event.registerBlockEntityRenderer(TechBasicBlockEntities.TANNING_RACK.get(), TanningRackRenderer::new);
        event.registerBlockEntityRenderer(TechBasicBlockEntities.WORKTABLE.get(), WorktableRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(TechBasicItems.MARSHMALLOW_STICK.get(), MARSHMALLOW_TYPE_PREDICATE,
                (ClampedItemPropertyFunction) (stack, level, entity, seed) -> MarshmallowStickItem.type(stack).ordinal());
            ItemProperties.register(TechBasicBlocks.CHOPPING_BLOCK_ITEM.get(), WearStage.PREDICATE,
                (ClampedItemPropertyFunction) (stack, level, entity, seed) -> WearStage.of(stack, ChoppingBlockBlock.DAMAGE));
            ItemProperties.register(TechBasicBlocks.ANVIL_GRANITE_ITEM.get(), WearStage.PREDICATE, anvilDamage());
            ItemProperties.register(TechBasicBlocks.ANVIL_IRON_PLATED_ITEM.get(), WearStage.PREDICATE, anvilDamage());
            ItemProperties.register(TechBasicBlocks.ANVIL_OBSIDIAN_ITEM.get(), WearStage.PREDICATE, anvilDamage());
            ItemProperties.register(TechBasicBlocks.BARREL_ITEM.get(), SEALED_PREDICATE,
                (ClampedItemPropertyFunction) (stack, level, entity, seed) -> {
                    BlockItemStateProperties properties = stack.get(DataComponents.BLOCK_STATE);
                    Boolean sealed = properties == null ? null : properties.get(BarrelBlock.SEALED);
                    return sealed != null && sealed ? 1 : 0;
                });
        });
    }

    private static ClampedItemPropertyFunction anvilDamage() {
        return (stack, level, entity, seed) -> WearStage.of(stack, AnvilBlock.DAMAGE);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerMobEffect(new CampfireEffectIcons("comfort", CampfireEffectIcons.Bar.NONE), TechBasicEffects.COMFORT.get());
        event.registerMobEffect(new CampfireEffectIcons("well_fed", CampfireEffectIcons.Bar.NONE), TechBasicEffects.WELL_FED.get());
        event.registerMobEffect(CampfireEffectIcons.resting(), TechBasicEffects.RESTING.get());
        event.registerMobEffect(new CampfireEffectIcons("well_rested", CampfireEffectIcons.Bar.NONE), TechBasicEffects.WELL_RESTED.get());
        event.registerMobEffect(new CampfireEffectIcons("focused", CampfireEffectIcons.Bar.FOCUSED), TechBasicEffects.FOCUSED.get());
    }
}
