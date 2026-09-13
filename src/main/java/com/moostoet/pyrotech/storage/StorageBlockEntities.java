package com.moostoet.pyrotech.storage;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.storage.block.entity.BagBlockEntity;
import com.moostoet.pyrotech.storage.block.entity.CrateBlockEntity;
import com.moostoet.pyrotech.storage.block.entity.FaucetBlockEntity;
import com.moostoet.pyrotech.storage.block.entity.ShelfBlockEntity;
import com.moostoet.pyrotech.storage.block.entity.StashBlockEntity;
import com.moostoet.pyrotech.storage.block.entity.TankBlockEntity;
import com.moostoet.pyrotech.storage.block.entity.WoodRackBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Seven block entity types for the thirteen blocks: each durable twin reads its numbers from its block. */
public final class StorageBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Pyrotech.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StashBlockEntity>> STASH =
        BLOCK_ENTITY_TYPES.register("stash", () ->
            BlockEntityType.Builder.of(StashBlockEntity::new, StorageBlocks.STASH.get(), StorageBlocks.STASH_STONE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShelfBlockEntity>> SHELF =
        BLOCK_ENTITY_TYPES.register("shelf", () ->
            BlockEntityType.Builder.of(ShelfBlockEntity::new, StorageBlocks.SHELF.get(), StorageBlocks.SHELF_STONE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE =
        BLOCK_ENTITY_TYPES.register("crate", () ->
            BlockEntityType.Builder.of(CrateBlockEntity::new, StorageBlocks.CRATE.get(), StorageBlocks.CRATE_STONE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WoodRackBlockEntity>> WOOD_RACK =
        BLOCK_ENTITY_TYPES.register("wood_rack", () ->
            BlockEntityType.Builder.of(WoodRackBlockEntity::new, StorageBlocks.WOOD_RACK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BagBlockEntity>> BAG =
        BLOCK_ENTITY_TYPES.register("bag", () ->
            BlockEntityType.Builder.of(BagBlockEntity::new, StorageBlocks.BAG_SIMPLE.get(), StorageBlocks.BAG_DURABLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK =
        BLOCK_ENTITY_TYPES.register("tank", () ->
            BlockEntityType.Builder.of(TankBlockEntity::new, StorageBlocks.STONE_TANK.get(), StorageBlocks.BRICK_TANK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FaucetBlockEntity>> FAUCET =
        BLOCK_ENTITY_TYPES.register("faucet", () ->
            BlockEntityType.Builder.of(FaucetBlockEntity::new, StorageBlocks.FAUCET_STONE.get(), StorageBlocks.FAUCET_BRICK.get()).build(null));

    private StorageBlockEntities() {
    }

    /** The type that serves a block entity class, for a block's {@code getTicker} checks. */
    public static boolean is(BlockEntityType<?> type, DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<? extends BlockEntity>> holder) {
        return type == holder.get();
    }
}
