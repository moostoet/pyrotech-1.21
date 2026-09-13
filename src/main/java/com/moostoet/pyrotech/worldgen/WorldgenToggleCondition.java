package com.moostoet.pyrotech.worldgen;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * The worldgen toggle as a datapack condition: {@code pyrotech:worldgen_toggle} with a
 * {@code generator} field. It sits on the generator's biome modifier entry, so a false
 * toggle makes the entry fail to load and the generator never runs.
 */
public record WorldgenToggleCondition(Generator generator) implements ICondition {

    public static final MapCodec<WorldgenToggleCondition> CODEC = Generator.CODEC.fieldOf("generator")
        .xmap(WorldgenToggleCondition::new, WorldgenToggleCondition::generator);

    @Override
    public boolean test(IContext context) {
        return WorldgenConfig.COMMON.isEnabled(this.generator);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
