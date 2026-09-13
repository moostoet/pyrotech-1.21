package com.moostoet.pyrotech.tech.basic.block.entity;

import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.block.TanningRackBlock;
import com.moostoet.pyrotech.tech.basic.recipe.TanningRackRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.TechBasicRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Optional;

/** The tanning rack's hide and its tanned result. */
public final class TanningRackBlockEntity extends SyncedBlockEntity {

    private static final int DAY_END = 12000;

    private final ItemStackHandler input = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return TanningRackBlockEntity.this.recipeFor(stack).isPresent();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return this.isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            TanningRackBlockEntity.this.progress = 0;
            TanningRackBlockEntity.this.rainTicks = 0;
            TanningRackBlockEntity.this.sync();
        }
    };

    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            TanningRackBlockEntity.this.sync();
        }
    };

    private float progress;
    private int rainTicks;

    public TanningRackBlockEntity(BlockPos pos, BlockState state) {
        super(TechBasicBlockEntities.TANNING_RACK.get(), pos, state);
    }

    public ItemStackHandler input() {
        return this.input;
    }

    public ItemStackHandler output() {
        return this.output;
    }

    public float progress() {
        return this.progress;
    }

    public Direction facing() {
        return this.getBlockState().getValue(TanningRackBlock.FACING);
    }

    public Optional<TanningRackRecipe> recipeFor(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(TechBasicRecipeTypes.TANNING_RACK.get(), new SingleRecipeInput(stack), this.level)
            .map(holder -> holder.value());
    }

    /** The item on the rack: the result once tanned, else the hide. */
    public ItemStack displayed() {
        ItemStack result = this.output.getStackInSlot(0);
        return result.isEmpty() ? this.input.getStackInSlot(0) : result;
    }

    // -- Interaction ---------------------------------------------------------

    public boolean insert(Player player, ItemStack held) {
        return this.output.getStackInSlot(0).isEmpty() && SlotInteraction.insert(this.input, 0, player, held, 1);
    }

    public boolean extract(Player player) {
        return SlotInteraction.extract(this.output, 0, player, this.worldPosition)
            || SlotInteraction.extract(this.input, 0, player, this.worldPosition);
    }

    // -- Ticking -------------------------------------------------------------

    private static boolean isDay(Level level) {
        long time = level.getDayTime() % 24000;
        return time >= 0 && time <= DAY_END;
    }

    private static boolean rainRuins() {
        return TechBasicConfig.COMMON.recipeRuinRainTicks.get() >= 0;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TanningRackBlockEntity rack) {
        if (rack.recipeFor(rack.input.getStackInSlot(0)).isPresent() && level.getGameTime() % 40 == 0
            && !(rainRuins() && level.isRainingAt(pos.above())) && level.canSeeSky(pos) && isDay(level)) {
            ClientProgress.particles(level, 1, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5, 0.5, 0.25, 0.5);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TanningRackBlockEntity rack) {
        Optional<TanningRackRecipe> found = rack.recipeFor(rack.input.getStackInSlot(0));
        if (found.isEmpty() || !level.canSeeSky(pos)) {
            rack.progress = 0;
            return;
        }
        TanningRackRecipe recipe = found.get();
        if (rainRuins() && level.isRainingAt(pos.above())) {
            if (recipe.rainFailure().isPresent() && ++rack.rainTicks >= TechBasicConfig.COMMON.recipeRuinRainTicks.get()) {
                rack.output.setStackInSlot(0, recipe.rainFailure().get().copy());
                rack.input.extractItem(0, 1, false);
                rack.progress = 0;
            }
        } else if (isDay(level)) {
            int ticks = TechBasicConfig.Server.scaled(recipe.time(), TechBasicConfig.SERVER.tanningRackDurationModifier);
            rack.progress += 1f / ticks;
            if (rack.progress >= 0.9999f) {
                rack.output.setStackInSlot(0, recipe.result().copy());
                rack.input.extractItem(0, 1, false);
                rack.progress = 0;
            }
        }
        rack.setChanged();
    }

    public void dropContents() {
        if (this.level != null) {
            Spill.handler(this.level, this.worldPosition, this.input, 0);
            Spill.handler(this.level, this.worldPosition, this.output, 0);
        }
    }

    // -- Persistence ---------------------------------------------------------

    @Override
    protected void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("input", this.input.serializeNBT(registries));
        tag.put("output", this.output.serializeNBT(registries));
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.input.deserializeNBT(registries, tag.getCompound("input"));
        this.output.deserializeNBT(registries, tag.getCompound("output"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("progress", this.progress);
        tag.putInt("rainTicks", this.rainTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getFloat("progress");
        this.rainTicks = tag.getInt("rainTicks");
    }
}
