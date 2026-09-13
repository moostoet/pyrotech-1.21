package com.moostoet.pyrotech.tech.basic.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The 1.12 focused player capability plus the counter its infinite durations no longer
 * provide (tech/basic sign-off, item 2): the experience bonus focused has banked, and the
 * ticks resting has run since it was last added or reset. Synced to the owning player only.
 */
public record CampfireEffectData(double remainingBonus, int restingTicks) {

    public static final CampfireEffectData EMPTY = new CampfireEffectData(0, 0);

    public static final Codec<CampfireEffectData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.optionalFieldOf("remaining_bonus", 0.0).forGetter(CampfireEffectData::remainingBonus),
        Codec.INT.optionalFieldOf("resting_ticks", 0).forGetter(CampfireEffectData::restingTicks)
    ).apply(instance, CampfireEffectData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CampfireEffectData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.DOUBLE, CampfireEffectData::remainingBonus,
        ByteBufCodecs.VAR_INT, CampfireEffectData::restingTicks,
        CampfireEffectData::new);

    public CampfireEffectData withRemainingBonus(double remainingBonus) {
        return new CampfireEffectData(remainingBonus, this.restingTicks);
    }

    public CampfireEffectData withRestingTicks(int restingTicks) {
        return new CampfireEffectData(this.remainingBonus, restingTicks);
    }
}
