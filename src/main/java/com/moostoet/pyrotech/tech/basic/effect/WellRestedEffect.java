package com.moostoet.pyrotech.tech.basic.effect;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Well rested: four absorption half hearts, added when the effect starts and taken away
 * with the attribute modifier when it ends, the way vanilla's absorption works.
 */
public final class WellRestedEffect extends CampfireEffect {

    public static final int ABSORPTION_HALF_HEARTS = 4;

    public WellRestedEffect() {
        super(true);
        this.addAttributeModifier(Attributes.MAX_ABSORPTION, ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "effect.well_rested"),
            ABSORPTION_HALF_HEARTS, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        super.onEffectStarted(entity, amplifier);
        entity.setAbsorptionAmount(Math.min(entity.getMaxAbsorption(), entity.getAbsorptionAmount() + ABSORPTION_HALF_HEARTS));
    }
}
