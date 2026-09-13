package com.moostoet.pyrotech.tool.client;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.tool.ToolItems;
import com.moostoet.pyrotech.tool.item.ActiveBehaviour;
import com.moostoet.pyrotech.tool.item.ActiveTool;
import com.moostoet.pyrotech.tool.item.PyrotechShieldItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

/**
 * Tool's client registrations: the model predicates the item models switch on, and the
 * shield models and renderer. Vanilla registers {@code cast} and {@code blocking} for its
 * own rod and shield only, so the Pyrotech ones get the same functions here.
 */
@EventBusSubscriber(modid = Pyrotech.MOD_ID, value = Dist.CLIENT)
public final class ToolClient {

    private static final ResourceLocation CAST = ResourceLocation.withDefaultNamespace("cast");
    private static final ResourceLocation BLOCKING = ResourceLocation.withDefaultNamespace("blocking");

    private ToolClient() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (DeferredItem<? extends Item> holder : ToolItems.ACTIVE_TOOLS) {
                Item item = holder.get();
                ActiveBehaviour behaviour = ((ActiveTool) item).activeBehaviour();
                ItemProperties.register(item, ActiveBehaviour.MODEL_PREDICATE,
                    (ClampedItemPropertyFunction) (stack, level, entity, seed) -> behaviour.isActive(stack) ? 1.0f : 0.0f);
            }
            ItemProperties.register(ToolItems.CRUDE_FISHING_ROD.get(), CAST, (ClampedItemPropertyFunction) ToolClient::cast);
            for (DeferredItem<PyrotechShieldItem> shield : ToolItems.SHIELDS) {
                ItemProperties.register(shield.get(), BLOCKING, (ClampedItemPropertyFunction) (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0f : 0.0f);
            }
        });
    }

    private static float cast(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        if (entity == null) {
            return 0.0f;
        }
        boolean mainHand = entity.getMainHandItem() == stack;
        boolean offHand = entity.getOffhandItem() == stack && !(entity.getMainHandItem().getItem() instanceof FishingRodItem);
        return (mainHand || offHand) && entity instanceof Player player && player.fishing != null ? 1.0f : 0.0f;
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PyrotechShieldModel.CRUDE_LAYER, PyrotechShieldModel::createCrudeLayer);
        event.registerLayerDefinition(PyrotechShieldModel.DURABLE_LAYER, PyrotechShieldModel::createDurableLayer);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(ShieldItemRenderer.extensions(PyrotechShieldModel.CRUDE_LAYER, texture("crude_shield")),
            ToolItems.CRUDE_SHIELD.get());
        event.registerItem(ShieldItemRenderer.extensions(PyrotechShieldModel.DURABLE_LAYER, texture("durable_shield")),
            ToolItems.DURABLE_SHIELD.get());
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "textures/entity/" + name + ".png");
    }
}
