package com.moostoet.pyrotech.tech.basic;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.tech.basic.block.entity.AnvilBlockEntity;
import com.moostoet.pyrotech.tech.basic.block.entity.BarrelBlockEntity;
import com.moostoet.pyrotech.tech.basic.block.entity.CampfireBlockEntity;
import com.moostoet.pyrotech.tech.basic.block.entity.ChoppingBlockBlockEntity;
import com.moostoet.pyrotech.tech.basic.block.entity.CompactingBinBlockEntity;
import com.moostoet.pyrotech.tech.basic.block.entity.CompostBinBlockEntity;
import com.moostoet.pyrotech.tech.basic.block.entity.DryingRackBlockEntity;
import com.moostoet.pyrotech.tech.basic.block.entity.PitKilnBlockEntity;
import com.moostoet.pyrotech.tech.basic.block.entity.SoakingPotBlockEntity;
import com.moostoet.pyrotech.tech.basic.block.entity.TanningRackBlockEntity;
import com.moostoet.pyrotech.tech.basic.block.entity.WorktableBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Eleven block entity types over the fifteen blocks: the three anvils, the two racks, and the two worktables share theirs. */
public final class TechBasicBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Pyrotech.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnvilBlockEntity>> ANVIL =
        BLOCK_ENTITY_TYPES.register("anvil", () -> BlockEntityType.Builder.of(AnvilBlockEntity::new,
            TechBasicBlocks.ANVIL_GRANITE.get(), TechBasicBlocks.ANVIL_IRON_PLATED.get(), TechBasicBlocks.ANVIL_OBSIDIAN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BarrelBlockEntity>> BARREL =
        BLOCK_ENTITY_TYPES.register("barrel", () -> BlockEntityType.Builder.of(BarrelBlockEntity::new, TechBasicBlocks.BARREL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CampfireBlockEntity>> CAMPFIRE =
        BLOCK_ENTITY_TYPES.register("campfire", () -> BlockEntityType.Builder.of(CampfireBlockEntity::new, TechBasicBlocks.CAMPFIRE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChoppingBlockBlockEntity>> CHOPPING_BLOCK =
        BLOCK_ENTITY_TYPES.register("chopping_block", () ->
            BlockEntityType.Builder.of(ChoppingBlockBlockEntity::new, TechBasicBlocks.CHOPPING_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompactingBinBlockEntity>> COMPACTING_BIN =
        BLOCK_ENTITY_TYPES.register("compacting_bin", () ->
            BlockEntityType.Builder.of(CompactingBinBlockEntity::new, TechBasicBlocks.COMPACTING_BIN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompostBinBlockEntity>> COMPOST_BIN =
        BLOCK_ENTITY_TYPES.register("compost_bin", () ->
            BlockEntityType.Builder.of(CompostBinBlockEntity::new, TechBasicBlocks.COMPOST_BIN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DryingRackBlockEntity>> DRYING_RACK =
        BLOCK_ENTITY_TYPES.register("drying_rack", () -> BlockEntityType.Builder.of(DryingRackBlockEntity::new,
            TechBasicBlocks.DRYING_RACK_CRUDE.get(), TechBasicBlocks.DRYING_RACK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PitKilnBlockEntity>> KILN_PIT =
        BLOCK_ENTITY_TYPES.register("kiln_pit", () -> BlockEntityType.Builder.of(PitKilnBlockEntity::new, TechBasicBlocks.KILN_PIT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoakingPotBlockEntity>> SOAKING_POT =
        BLOCK_ENTITY_TYPES.register("soaking_pot", () ->
            BlockEntityType.Builder.of(SoakingPotBlockEntity::new, TechBasicBlocks.SOAKING_POT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TanningRackBlockEntity>> TANNING_RACK =
        BLOCK_ENTITY_TYPES.register("tanning_rack", () ->
            BlockEntityType.Builder.of(TanningRackBlockEntity::new, TechBasicBlocks.TANNING_RACK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorktableBlockEntity>> WORKTABLE =
        BLOCK_ENTITY_TYPES.register("worktable", () -> BlockEntityType.Builder.of(WorktableBlockEntity::new,
            TechBasicBlocks.WORKTABLE.get(), TechBasicBlocks.WORKTABLE_STONE.get()).build(null));

    private TechBasicBlockEntities() {
    }
}
