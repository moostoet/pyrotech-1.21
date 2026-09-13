package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.tech.basic.TechBasicAttachments;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.effect.CampfireEffectData;
import com.moostoet.pyrotech.tech.basic.effect.RestingEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * The 1.12 potion icons: an 18 pixel texture in place of the vanilla sprite, and under
 * the resting and focused icons a green bar, for the time to the next resting level and
 * for the focused bonus left.
 */
public final class CampfireEffectIcons implements IClientMobEffectExtensions {

    private static final int ICON = 18;
    private static final int BLACK = 0xFF000000;
    private static final int GREEN = 0xFF00FF00;
    private static final int WIDE_BAR = 108;
    private static final int NARROW_BAR = 18;

    /** What the bar shows, 0 to 1, or null for no bar. */
    public enum Bar {
        NONE,
        RESTING,
        FOCUSED
    }

    private final Function<MobEffectInstance, ResourceLocation> texture;
    private final Bar bar;

    public CampfireEffectIcons(String name, Bar bar) {
        this(instance -> texture(name), bar);
    }

    public CampfireEffectIcons(Function<MobEffectInstance, ResourceLocation> texture, Bar bar) {
        this.texture = texture;
        this.bar = bar;
    }

    public static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "textures/potions/" + name + ".png");
    }

    /** Resting's icon steps with its level. */
    public static CampfireEffectIcons resting() {
        return new CampfireEffectIcons(instance -> switch (instance.getAmplifier()) {
            case 1 -> texture("resting2");
            case 2 -> texture("resting3");
            default -> texture("resting");
        }, Bar.RESTING);
    }

    @Override
    public boolean renderInventoryIcon(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, GuiGraphics graphics, int x, int y, int blitOffset) {
        graphics.blit(this.texture.apply(instance), x + 6, y + 7, 0, 0, ICON, ICON, ICON, ICON);
        boolean wide = screen.width - (screen.getGuiLeft() + screen.getXSize() + 2) >= 120;
        this.renderBar(graphics, instance, x + 6, y + 8 + 17, wide ? WIDE_BAR : NARROW_BAR);
        return true;
    }

    @Override
    public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics graphics, int x, int y, float z, float alpha) {
        graphics.blit(this.texture.apply(instance), x + 3, y + 3, 0, 0, ICON, ICON, ICON, ICON);
        this.renderBar(graphics, instance, x + 3, y + 20, NARROW_BAR);
        return true;
    }

    private void renderBar(GuiGraphics graphics, MobEffectInstance instance, int left, int top, int width) {
        Double fill = this.fill(instance);
        if (fill == null) {
            return;
        }
        graphics.fill(left, top, left + width, top + 2, BLACK);
        graphics.fill(left, top, (int) Math.max(left + 1, left + width * fill), top + 1, GREEN);
    }

    @Nullable
    private Double fill(MobEffectInstance instance) {
        if (this.bar == Bar.NONE || Minecraft.getInstance().player == null) {
            return null;
        }
        CampfireEffectData data = TechBasicAttachments.get(Minecraft.getInstance().player);
        if (this.bar == Bar.RESTING) {
            return (data.restingTicks() % RestingEffect.LEVEL_UP_INTERVAL_TICKS) / (double) RestingEffect.LEVEL_UP_INTERVAL_TICKS;
        }
        double max = TechBasicConfig.SERVER.focusedMaximumAccumulatedBonus.get();
        return max <= 0 ? 0 : Math.min(1, data.remainingBonus() / max);
    }
}
