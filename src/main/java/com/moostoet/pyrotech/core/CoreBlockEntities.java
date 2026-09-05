package com.moostoet.pyrotech.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.block.entity.MulchedFarmlandBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Core's block entities: only the mulched farmland's charge counter. */
public final class CoreBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Pyrotech.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MulchedFarmlandBlockEntity>> FARMLAND_MULCHED =
        BLOCK_ENTITY_TYPES.register("farmland_mulched", () ->
            BlockEntityType.Builder.of(MulchedFarmlandBlockEntity::new, CoreBlocks.FARMLAND_MULCHED.get()).build(null));

    private CoreBlockEntities() {
    }
}
