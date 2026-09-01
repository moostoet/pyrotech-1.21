package com.moostoet.pyrotech.prototype.campfire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CampfireBlockEntity extends SyncedBlockEntity {

    static final int TINDER_BURN_TICKS = 400;
    static final int BURN_TICKS_PER_LOG = 1200;
    static final int BURNED_FOOD_TICKS = 2400;
    static final int FUEL_SLOTS = 8;
    static final double ASH_CHANCE = 0.35;

    private final ItemStackHandler input = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return CampfireBlockEntity.this.findRecipe(stack) != null
                && CampfireBlockEntity.this.output.getStackInSlot(0).isEmpty();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!this.isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            CampfireBlockEntity.this.resetCookTime();
            CampfireBlockEntity.this.sync();
        }
    };

    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            CampfireBlockEntity.this.outputAge = 0;
            CampfireBlockEntity.this.sync();
        }
    };

    private final ItemStackHandler fuel = new ItemStackHandler(FUEL_SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ItemTags.LOGS_THAT_BURN);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!this.isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            CampfireBlockEntity.this.sync();
            CampfireBlockEntity.this.checkLight();
        }
    };

    private final RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> recipeCheck =
        RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);

    private int burnTimeRemaining;
    private int cookTime = -1;
    private int cookTimeTotal = -1;
    private int outputAge;

    public CampfireBlockEntity(BlockPos pos, BlockState state) {
        super(PrototypeCampfire.CAMPFIRE_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStackHandler getInput() {
        return this.input;
    }

    public ItemStackHandler getOutput() {
        return this.output;
    }

    public ItemStackHandler getFuel() {
        return this.fuel;
    }

    public int getFuelCount() {
        for (int i = 0; i < FUEL_SLOTS; i++) {
            if (this.fuel.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return FUEL_SLOTS;
    }

    // ---------------------------------------------------------------------
    // Interaction entry points, called from CampfireBlock and the scroll payload
    // ---------------------------------------------------------------------

    public boolean insertFood(Player player, ItemStack held) {
        if (this.findRecipe(held) == null) {
            return false;
        }
        ItemStack remainder = this.input.insertItem(0, held.copyWithCount(1), false);
        if (remainder.isEmpty()) {
            if (!player.isCreative()) {
                held.shrink(1);
            }
            return true;
        }
        return false;
    }

    public boolean extractFoodTo(Player player) {
        for (ItemStackHandler handler : new ItemStackHandler[]{this.input, this.output}) {
            ItemStack taken = handler.extractItem(0, 1, false);
            if (!taken.isEmpty()) {
                if (!player.addItem(taken)) {
                    Containers.dropItemStack(this.level, player.getX(), player.getY(), player.getZ(), taken);
                }
                return true;
            }
        }
        return false;
    }

    public boolean addLogFrom(Player player, ItemStack held) {
        int count = this.getFuelCount();
        if (count >= FUEL_SLOTS || !this.fuel.isItemValid(count, held)) {
            return false;
        }
        this.fuel.insertItem(count, held.copyWithCount(1), false);
        if (!player.isCreative()) {
            held.shrink(1);
        }
        this.level.playSound(null, this.worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1, 1);
        return true;
    }

    public boolean removeLogTo(Player player) {
        int count = this.getFuelCount();
        if (count == 0) {
            return false;
        }
        ItemStack taken = this.fuel.extractItem(count - 1, 1, false);
        if (this.getBlockState().getValue(CampfireBlock.VARIANT) == CampfireVariant.LIT
            && !player.fireImmune()
            && this.level.random.nextFloat() < 0.3f) {
            player.hurt(this.level.damageSources().hotFloor(), 1);
        }
        if (!player.addItem(taken)) {
            Containers.dropItemStack(this.level, player.getX(), player.getY(), player.getZ(), taken);
        }
        return true;
    }

    public boolean shovelAsh(Player player) {
        BlockState state = this.getBlockState();
        int ash = state.getValue(CampfireBlock.ASH);
        if (ash <= 0) {
            return false;
        }
        this.level.setBlock(this.worldPosition, state.setValue(CampfireBlock.ASH, ash - 1), 3);
        Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5,
            this.worldPosition.getZ() + 0.5, new ItemStack(Items.BONE_MEAL));
        this.level.playSound(null, this.worldPosition, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1, 1);
        return true;
    }

    public void ignite() {
        BlockState state = this.getBlockState();
        if (state.getValue(CampfireBlock.VARIANT) != CampfireVariant.NORMAL) {
            return;
        }
        this.burnTimeRemaining = TINDER_BURN_TICKS;
        this.level.setBlock(this.worldPosition, state.setValue(CampfireBlock.VARIANT, CampfireVariant.LIT), 3);
    }

    // ---------------------------------------------------------------------
    // Server tick
    // ---------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        if (state.getValue(CampfireBlock.VARIANT) != CampfireVariant.LIT) {
            return;
        }

        campfire.burnTimeRemaining -= 1;
        if (campfire.burnTimeRemaining <= 0 && !campfire.consumeLog(level, pos, state)) {
            return;
        }

        campfire.cook(level);
        campfire.ageOutput();
        campfire.setChanged();
    }

    private boolean consumeLog(Level level, BlockPos pos, BlockState state) {
        int count = this.getFuelCount();
        if (count == 0) {
            this.die(level, pos, state);
            return false;
        }

        this.fuel.extractItem(count - 1, 1, false);
        this.burnTimeRemaining += BURN_TICKS_PER_LOG;

        if (level.random.nextDouble() < ASH_CHANCE) {
            int ash = Math.min(8, state.getValue(CampfireBlock.ASH) + 1);
            if (ash == 8) {
                level.setBlock(pos, state.setValue(CampfireBlock.ASH, 8)
                    .setValue(CampfireBlock.VARIANT, CampfireVariant.NORMAL), 3);
                return false;
            }
            level.setBlock(pos, state.setValue(CampfireBlock.ASH, ash), 3);
        }
        return true;
    }

    private void die(Level level, BlockPos pos, BlockState state) {
        for (ItemStackHandler handler : new ItemStackHandler[]{this.input, this.output}) {
            ItemStack stack = handler.extractItem(0, 64, false);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
        }
        level.setBlock(pos, state.setValue(CampfireBlock.VARIANT, CampfireVariant.ASH), 3);
    }

    private void cook(Level level) {
        ItemStack cooking = this.input.getStackInSlot(0);
        if (cooking.isEmpty()) {
            return;
        }

        this.cookTime -= this.getFuelCount();
        if (this.cookTime > 0) {
            return;
        }

        CampfireCookingRecipe recipe = this.findRecipe(cooking);
        this.input.extractItem(0, 1, false);
        if (recipe != null) {
            this.output.insertItem(0, recipe.getResultItem(level.registryAccess()).copy(), false);
        }
    }

    private void ageOutput() {
        ItemStack cooked = this.output.getStackInSlot(0);
        if (cooked.isEmpty() || cooked.is(Items.CHARCOAL)) {
            return;
        }
        this.outputAge += this.getFuelCount();
        if (this.outputAge >= BURNED_FOOD_TICKS * FUEL_SLOTS) {
            this.output.setStackInSlot(0, new ItemStack(Items.CHARCOAL));
        }
    }

    private CampfireCookingRecipe findRecipe(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return null;
        }
        return this.recipeCheck.getRecipeFor(new SingleRecipeInput(stack), this.level)
            .map(holder -> holder.value())
            .orElse(null);
    }

    private void resetCookTime() {
        CampfireCookingRecipe recipe = this.findRecipe(this.input.getStackInSlot(0));
        int ticks = (recipe == null) ? -1 : recipe.getCookingTime() * FUEL_SLOTS;
        this.cookTime = ticks;
        this.cookTimeTotal = ticks;
    }

    public void dropContents(Level level, BlockPos pos) {
        BlockState state = this.getBlockState();
        if (state.getValue(CampfireBlock.VARIANT) == CampfireVariant.ASH) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                new ItemStack(Items.BONE_MEAL, Math.max(1, state.getValue(CampfireBlock.ASH))));
        }
        for (ItemStackHandler handler : new ItemStackHandler[]{this.input, this.output, this.fuel}) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                }
            }
        }
    }

    private void checkLight() {
        if (this.level != null) {
            this.level.getChunkSource().getLightEngine().checkBlock(this.worldPosition);
        }
    }

    // ---------------------------------------------------------------------
    // Serialization
    // ---------------------------------------------------------------------

    @Override
    protected void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("input", this.input.serializeNBT(registries));
        tag.put("output", this.output.serializeNBT(registries));
        tag.put("fuel", this.fuel.serializeNBT(registries));
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.input.deserializeNBT(registries, tag.getCompound("input"));
        this.output.deserializeNBT(registries, tag.getCompound("output"));
        this.fuel.deserializeNBT(registries, tag.getCompound("fuel"));
    }

    @Override
    protected void onSyncedDataUpdate() {
        this.checkLight();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("burnTimeRemaining", this.burnTimeRemaining);
        tag.putInt("cookTime", this.cookTime);
        tag.putInt("cookTimeTotal", this.cookTimeTotal);
        tag.putInt("outputAge", this.outputAge);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.burnTimeRemaining = tag.getInt("burnTimeRemaining");
        this.cookTime = tag.getInt("cookTime");
        this.cookTimeTotal = tag.getInt("cookTimeTotal");
        this.outputAge = tag.getInt("outputAge");
    }

    public int getLightLevel() {
        int fuelCount = this.getFuelCount();
        if (fuelCount == 0) {
            return 0;
        }
        return Mth.clamp(4 + (int) (11 * (fuelCount / (float) FUEL_SLOTS)), 0, 15);
    }
}
