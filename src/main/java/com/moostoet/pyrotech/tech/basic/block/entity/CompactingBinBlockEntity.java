package com.moostoet.pyrotech.tech.basic.block.entity;

import com.moostoet.pyrotech.core.ToolLevels;
import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.interaction.ScrollInteractable;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.library.inventory.LargeStackHandler;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.recipe.CompactingBinRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.TechBasicRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

/**
 * The compacting bin's one heap: up to four recipes' worth of one material, packed a
 * shovel stroke at a time. The heap and its recipe are public for the machine module's
 * mechanical bin.
 */
public final class CompactingBinBlockEntity extends SyncedBlockEntity implements ScrollInteractable {

    public static final int RECIPES_PER_HEAP = 4;
    private static final int TOOL_DAMAGE_PER_CRAFT = 1;
    private static final float EXHAUSTION_PER_HIT = 1;

    private final LargeStackHandler input = new LargeStackHandler(1, Integer.MAX_VALUE / 64) {
        @Override
        public int getSlotLimit(int slot) {
            return CompactingBinBlockEntity.this.capacity(this.getStackInSlot(slot));
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return CompactingBinBlockEntity.this.capacity(stack);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return CompactingBinBlockEntity.this.recipeFor(stack).isPresent();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return this.isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (this.getStackInSlot(slot).isEmpty()) {
                CompactingBinBlockEntity.this.progress = 0;
            }
            CompactingBinBlockEntity.this.sync();
        }
    };

    private float progress;

    public CompactingBinBlockEntity(BlockPos pos, BlockState state) {
        super(TechBasicBlockEntities.COMPACTING_BIN.get(), pos, state);
    }

    public LargeStackHandler input() {
        return this.input;
    }

    public float progress() {
        return this.progress;
    }

    public Optional<CompactingBinRecipe> recipeFor(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(TechBasicRecipeTypes.COMPACTING_BIN.get(), new SingleRecipeInput(stack), this.level)
            .map(holder -> holder.value());
    }

    /** The recipe of what the bin holds, or nothing while it is empty. */
    public Optional<CompactingBinRecipe> currentRecipe() {
        return this.recipeFor(this.input.getStackInSlot(0));
    }

    /** Four recipes' worth of the item, or nothing for an item without a recipe. */
    private int capacity(ItemStack stack) {
        return this.recipeFor(stack).map(recipe -> RECIPES_PER_HEAP * recipe.amount()).orElse(0);
    }

    // -- Interaction ---------------------------------------------------------

    public boolean insert(Player player, ItemStack held) {
        if (this.level == null || !SlotInteraction.insert(this.input, 0, player, held)) {
            return false;
        }
        if (!this.level.isClientSide) {
            this.level.playSound(null, this.worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.5f, 1);
        }
        return true;
    }

    public boolean extract(Player player) {
        return SlotInteraction.extract(this.input, 0, player, this.worldPosition);
    }

    @Override
    public void scroll(Player player, BlockHitResult hit, boolean up) {
        if (up) {
            SlotInteraction.scrollInsert(this.input, 0, player);
        } else {
            SlotInteraction.extractOne(this.input, 0, player, this.worldPosition);
        }
    }

    /** One shovel stroke: progress by the recipe's uses at the shovel's level, then a block out. */
    public boolean pack(Player player, ItemStack shovel, InteractionHand hand) {
        if (this.level == null) {
            return false;
        }
        Optional<CompactingBinRecipe> current = this.currentRecipe();
        if (current.isEmpty() || current.get().amount() > this.input.totalCount() || HungerGate.blocks(player)) {
            return false;
        }
        if (this.level.isClientSide) {
            return true;
        }
        CompactingBinRecipe recipe = current.get();
        player.causeFoodExhaustion(EXHAUSTION_PER_HIT);
        this.progress += 1f / recipe.toolUses(ToolLevels.of(shovel));
        if (this.progress > 0.9999f) {
            Spill.onTop(this.level, this.worldPosition, recipe.result().copy(), 1);
            this.input.extractItem(0, recipe.amount(), false);
            shovel.hurtAndBreak(TOOL_DAMAGE_PER_CRAFT, player, LivingEntity.getSlotForHand(hand));
            this.progress = 0;
        }
        this.setChanged();
        return true;
    }

    public void dropContents() {
        if (this.level != null) {
            Spill.handler(this.level, this.worldPosition, this.input, 0);
        }
    }

    // -- Persistence ---------------------------------------------------------

    @Override
    protected void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("input", this.input.serializeNBT(registries));
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.input.deserializeNBT(registries, tag.getCompound("input"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("progress", this.progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getFloat("progress");
    }
}
