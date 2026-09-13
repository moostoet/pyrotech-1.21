package com.moostoet.pyrotech.hunting;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.hunting.block.entity.ButchersBlockBlockEntity;
import com.moostoet.pyrotech.hunting.block.entity.CarcassBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Hunting's two block entities: the carcass's contents and the butcher's block's input slot. */
public final class HuntingBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Pyrotech.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CarcassBlockEntity>> CARCASS =
        BLOCK_ENTITY_TYPES.register("carcass", () ->
            BlockEntityType.Builder.of(CarcassBlockEntity::new, HuntingBlocks.CARCASS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ButchersBlockBlockEntity>> BUTCHERS_BLOCK =
        BLOCK_ENTITY_TYPES.register("butchers_block", () ->
            BlockEntityType.Builder.of(ButchersBlockBlockEntity::new, HuntingBlocks.BUTCHERS_BLOCK.get()).build(null));

    private HuntingBlockEntities() {
    }
}
