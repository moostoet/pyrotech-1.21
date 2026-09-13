package com.moostoet.pyrotech.tech.basic;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.tech.basic.effect.CampfireEffect;
import com.moostoet.pyrotech.tech.basic.effect.RestingEffect;
import com.moostoet.pyrotech.tech.basic.effect.WellRestedEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The five campfire effects, in the 1.12 registration order. Comfort, resting, and focused
 * are granted as infinite instances; well fed and well rested last five minutes.
 */
public final class TechBasicEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Pyrotech.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> COMFORT = MOB_EFFECTS.register("comfort", () -> new CampfireEffect(false));
    public static final DeferredHolder<MobEffect, MobEffect> WELL_FED = MOB_EFFECTS.register("well_fed", () -> new CampfireEffect(true));
    public static final DeferredHolder<MobEffect, MobEffect> RESTING = MOB_EFFECTS.register("resting", RestingEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> WELL_RESTED = MOB_EFFECTS.register("well_rested", WellRestedEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> FOCUSED = MOB_EFFECTS.register("focused", () -> new CampfireEffect(true));

    public static final List<DeferredHolder<MobEffect, MobEffect>> ALL = List.of(COMFORT, WELL_FED, RESTING, WELL_RESTED, FOCUSED);

    private TechBasicEffects() {
    }
}
