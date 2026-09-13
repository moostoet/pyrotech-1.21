package com.moostoet.pyrotech.tech.basic.block.entity;

import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.core.network.ProgressParticlesPayload;
import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.interaction.HitSlots;
import com.moostoet.pyrotech.library.interaction.ScrollInteractable;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.block.WorktableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The worktable's grid and shelf, and the hammering that crafts the grid's vanilla
 * recipe. There is no worktable recipe type (tech/basic sign-off, item 6): the grid
 * is a crafting input and the recipe is whatever the crafting manager finds.
 */
public final class WorktableBlockEntity extends SyncedBlockEntity implements ScrollInteractable {

    public static final int GRID_SIZE = 3;
    public static final int GRID_SLOTS = GRID_SIZE * GRID_SIZE;
    public static final int SHELF_SLOTS = 3;
    private static final double GRID_TOP = 14 / 16.0;
    private static final double SHELF_TOP = 5 / 16.0;
    private static final double SHELF_DEPTH = 1 / 3.0;

    private final ItemStackHandler grid;
    private final ItemStackHandler shelf;
    private float progress;
    private int remainingDurability;
    @Nullable
    private ResourceLocation retainedRecipe;

    public WorktableBlockEntity(BlockPos pos, BlockState state) {
        super(TechBasicBlockEntities.WORKTABLE.get(), pos, state);
        WorktableBlock block = (WorktableBlock) state.getBlock();
        this.remainingDurability = block.durability();
        this.grid = new LimitedHandler(GRID_SLOTS, block.gridStackLimit());
        this.shelf = new LimitedHandler(SHELF_SLOTS, block.shelfStackLimit());
    }

    private final class LimitedHandler extends ItemStackHandler {

        private final int limit;

        LimitedHandler(int size, int limit) {
            super(size);
            this.limit = limit;
        }

        @Override
        public int getSlotLimit(int slot) {
            return this.limit;
        }

        @Override
        protected void onContentsChanged(int slot) {
            WorktableBlockEntity.this.sync();
        }
    }

    public WorktableBlock block() {
        return (WorktableBlock) this.getBlockState().getBlock();
    }

    public ItemStackHandler grid() {
        return this.grid;
    }

    public ItemStackHandler shelf() {
        return this.shelf;
    }

    public float progress() {
        return this.progress;
    }

    public int remainingDurability() {
        return this.remainingDurability;
    }

    public Direction facing() {
        return this.getBlockState().getValue(WorktableBlock.FACING);
    }

    // -- Slots ---------------------------------------------------------------

    /** A slot under a hit on the top: 0 to 8 on the grid, 9 to 11 on the shelf, or -1. */
    public int slotAt(BlockHitResult hit) {
        Vec3 local = HitSlots.local(hit, this.worldPosition, this.facing());
        int column = HitSlots.cell(local.x, 0, 1, GRID_SIZE);
        if (local.y >= GRID_TOP) {
            int row = HitSlots.cell(local.z, 0, 1, GRID_SIZE);
            return (GRID_SIZE - 1 - column) + GRID_SIZE * (GRID_SIZE - 1 - row);
        }
        if (local.y <= SHELF_TOP && local.z <= SHELF_DEPTH) {
            return GRID_SLOTS + column;
        }
        return -1;
    }

    private ItemStackHandler handlerFor(int slot) {
        return slot < GRID_SLOTS ? this.grid : this.shelf;
    }

    private int indexFor(int slot) {
        return slot < GRID_SLOTS ? slot : slot - GRID_SLOTS;
    }

    /** Grid cells refuse hammers, as 1.12 did; one item per click. */
    public boolean insert(Player player, ItemStack held, BlockHitResult hit) {
        int slot = this.slotAt(hit);
        if (slot < 0 || slot < GRID_SLOTS && held.is(PyrotechTags.Items.HAMMERS)) {
            return false;
        }
        return SlotInteraction.insert(this.handlerFor(slot), this.indexFor(slot), player, held, 1);
    }

    public boolean extract(Player player, BlockHitResult hit) {
        int slot = this.slotAt(hit);
        return slot >= 0 && SlotInteraction.extract(this.handlerFor(slot), this.indexFor(slot), player, this.worldPosition);
    }

    @Override
    public void scroll(Player player, BlockHitResult hit, boolean up) {
        int slot = this.slotAt(hit);
        if (slot < 0 || up && slot < GRID_SLOTS && player.getMainHandItem().is(PyrotechTags.Items.HAMMERS)) {
            return;
        }
        if (up) {
            SlotInteraction.scrollInsert(this.handlerFor(slot), this.indexFor(slot), player);
        } else {
            SlotInteraction.extractOne(this.handlerFor(slot), this.indexFor(slot), player, this.worldPosition);
        }
    }

    /** Sneak plus an empty hand: the whole grid back to the player. */
    public boolean clearGrid(Player player) {
        boolean any = false;
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            any |= SlotInteraction.extract(this.grid, slot, player, this.worldPosition);
        }
        return any;
    }

    // -- Crafting ------------------------------------------------------------

    private CraftingInput craftingInput() {
        List<ItemStack> items = new ArrayList<>(GRID_SLOTS);
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            items.add(this.grid.getStackInSlot(slot));
        }
        return CraftingInput.of(GRID_SIZE, GRID_SIZE, items);
    }

    /** The vanilla crafting recipe the grid holds, if any. */
    public Optional<RecipeHolder<CraftingRecipe>> recipe() {
        if (this.level == null) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, this.craftingInput(), this.level);
    }

    /** One hammer blow: progress toward the recipe and, on the last, the craft. */
    public boolean hammer(Player player, ItemStack hammer, InteractionHand hand, BlockHitResult hit) {
        if (this.level == null) {
            return false;
        }
        Optional<RecipeHolder<CraftingRecipe>> found = this.recipe();
        if (found.isEmpty() || found.get().value().assemble(this.craftingInput(), this.level.registryAccess()).isEmpty()
            || HungerGate.blocks(player)) {
            return false;
        }
        if (this.level.isClientSide) {
            Vec3 at = hit.getLocation();
            BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, this.block().particleState());
            for (int i = 0; i < 2; i++) {
                this.level.addParticle(particle,
                    at.x + (this.level.random.nextFloat() * 2 - 1) * 0.1, at.y + 0.1, at.z + (this.level.random.nextFloat() * 2 - 1) * 0.1,
                    0, 0, 0);
            }
            return true;
        }
        ServerLevel level = (ServerLevel) this.level;
        BlockPos pos = this.worldPosition;
        WorktableBlock block = this.block();
        level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1, 1);
        this.progress += 1f / block.hitsPerCraft();
        if (block.exhaustionPerHit() > 0) {
            player.causeFoodExhaustion(block.exhaustionPerHit());
        }
        ProgressParticlesPayload.send(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 2);
        if (this.progress >= 0.9999f) {
            this.progress = 0;
            this.craft(level, player, found.get(), hammer, hand);
        }
        this.setChanged();
        return true;
    }

    private void craft(ServerLevel level, Player player, RecipeHolder<CraftingRecipe> holder, ItemStack hammer, InteractionHand hand) {
        WorktableBlock block = this.block();
        this.retainedRecipe = holder.id();
        CraftingInput input = this.craftingInput();
        ItemStack result = holder.value().assemble(input, level.registryAccess()).copy();
        result.onCraftedBy(level, player, result.getCount());
        NonNullList<ItemStack> items = NonNullList.withSize(GRID_SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            items.set(slot, this.grid.getStackInSlot(slot).copy());
        }
        EventHooks.firePlayerCraftingEvent(player, result, new TransientCraftingContainer(DummyMenu.INSTANCE, GRID_SIZE, GRID_SIZE, items));
        NonNullList<ItemStack> remaining = holder.value().getRemainingItems(input);
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            ItemStack left = slot < remaining.size() ? remaining.get(slot) : ItemStack.EMPTY;
            ItemStack inSlot = this.grid.getStackInSlot(slot);
            if (left.isEmpty()) {
                this.grid.extractItem(slot, 1, false);
            } else if (left.getItem() != inSlot.getItem()) {
                Spill.onTop(level, this.worldPosition, left, 0.75);
                this.grid.extractItem(slot, 1, false);
            } else {
                this.grid.setStackInSlot(slot, left);
            }
        }
        Spill.onTop(level, this.worldPosition, result, 0.75);
        if (block.toolDamagePerCraft() > 0) {
            hammer.hurtAndBreak(block.toolDamagePerCraft(), player, LivingEntity.getSlotForHand(hand));
        }
        if (block.usesDurability().get() && --this.remainingDurability <= 0) {
            this.dropContents();
            level.destroyBlock(this.worldPosition, false);
            level.playSound(null, this.worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1, level.random.nextFloat() * 0.4f + 0.8f);
        }
    }

    /**
     * Sneak plus a hammer: the grid's recipe, or the last one crafted, refilled from the
     * player's inventory when every ingredient is there and the grid has room.
     */
    public boolean repeatRecipe(Player player, ItemStack hammer, InteractionHand hand) {
        if (this.level == null || !TechBasicConfig.COMMON.allowRecipeRepeat.get()) {
            return false;
        }
        if (this.level.isClientSide) {
            return true;
        }
        RecipeHolder<CraftingRecipe> holder = this.recipe().orElse(null);
        if (holder == null && this.retainedRecipe != null) {
            holder = this.level.getRecipeManager().byKey(this.retainedRecipe)
                .filter(h -> h.value() instanceof CraftingRecipe)
                .map(h -> {
                    @SuppressWarnings("unchecked")
                    RecipeHolder<CraftingRecipe> crafting = (RecipeHolder<CraftingRecipe>) h;
                    return crafting;
                })
                .orElse(null);
        }
        if (holder == null) {
            return false;
        }
        NonNullList<Ingredient> ingredients = holder.value().getIngredients();
        List<ItemStack> gathered = new ArrayList<>(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                gathered.add(ItemStack.EMPTY);
                continue;
            }
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    gathered.add(stack.copyWithCount(1));
                    stack.shrink(1);
                    break;
                }
            }
        }
        boolean fits = gathered.size() == ingredients.size() && gathered.size() <= GRID_SLOTS;
        for (int slot = 0; fits && slot < gathered.size(); slot++) {
            fits = this.grid.insertItem(slot, gathered.get(slot), true).isEmpty();
        }
        if (!fits) {
            for (ItemStack stack : gathered) {
                SlotInteraction.giveOrDrop(player, stack, this.worldPosition, false);
            }
            return false;
        }
        for (int slot = 0; slot < gathered.size(); slot++) {
            this.grid.insertItem(slot, gathered.get(slot), false);
        }
        int damage = TechBasicConfig.COMMON.recipeRepeatToolDamage.get();
        if (damage > 0) {
            hammer.hurtAndBreak(damage, player, LivingEntity.getSlotForHand(hand));
        }
        return true;
    }

    public void dropContents() {
        if (this.level != null) {
            Spill.handler(this.level, this.worldPosition, this.grid, 0);
            Spill.handler(this.level, this.worldPosition, this.shelf, 0);
        }
    }

    /** The menu the crafting event's container wants; nothing reads it. */
    private static final class DummyMenu extends AbstractContainerMenu {

        static final DummyMenu INSTANCE = new DummyMenu();

        private DummyMenu() {
            super(null, -1);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }
    }

    // -- Persistence ---------------------------------------------------------

    @Override
    protected void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("grid", this.grid.serializeNBT(registries));
        tag.put("shelf", this.shelf.serializeNBT(registries));
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.grid.deserializeNBT(registries, tag.getCompound("grid"));
        this.shelf.deserializeNBT(registries, tag.getCompound("shelf"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("progress", this.progress);
        tag.putInt("remainingDurability", this.remainingDurability);
        if (this.retainedRecipe != null) {
            tag.putString("retainedRecipe", this.retainedRecipe.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getFloat("progress");
        this.remainingDurability = tag.getInt("remainingDurability");
        this.retainedRecipe = tag.contains("retainedRecipe") ? ResourceLocation.tryParse(tag.getString("retainedRecipe")) : null;
    }
}
