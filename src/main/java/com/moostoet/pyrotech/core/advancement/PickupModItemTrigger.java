package com.moostoet.pyrotech.core.advancement;

import com.moostoet.pyrotech.core.CoreTriggers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** {@code pyrotech:pickup_mod_item}: the root advancement's criterion (core sign-off, item 6). */
public final class PickupModItemTrigger extends SimpleCriterionTrigger<PickupModItemTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player))
            .apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> pickedUp() {
            return CoreTriggers.PICKUP_MOD_ITEM.get().createCriterion(new TriggerInstance(Optional.empty()));
        }
    }
}
