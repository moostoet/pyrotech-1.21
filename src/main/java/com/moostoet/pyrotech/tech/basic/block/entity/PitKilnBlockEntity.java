package com.moostoet.pyrotech.tech.basic.block.entity;

import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.core.RefractoryBlocks;
import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.interaction.HitSlots;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.block.PitKilnBlock;
import com.moostoet.pyrotech.tech.basic.block.PitKilnVariant;
import com.moostoet.pyrotech.tech.basic.recipe.KilnRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.TechBasicRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Optional;

/**
 * The pit kiln's burn: the 1.12 burnable base with its one stage inlined. The ware is
 * one stack of up to eight; the burn time shrinks with a partial stack. Every second
 * the walls and floor are checked; a bad wall resets the burn and, after five seconds,
 * ruins the ware into its failure items or ash. Refractory walls always pass and lower
 * the failure chance a fifth each.
 */
public final class PitKilnBlockEntity extends SyncedBlockEntity {

    public static final int MAX_INPUT = 8;
    public static final int OUTPUT_SLOTS = 9;
    public static final int LOG_SLOTS = 3;
    private static final int DEFAULT_TOTAL_BURN_TICKS = 1000;
    private static final int STRUCTURE_VALIDATION_INTERVAL = 20;
    private static final int MAX_INVALID_TICKS = 100;
    private static final int RAIN_TICKS_BEFORE_EXTINGUISHED = 200;
    private static final double LOG_ZONE_BOTTOM = 2 / 3.0;

    private final ItemStackHandler input = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return MAX_INPUT;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return PitKilnBlockEntity.this.recipeFor(stack).isPresent();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return this.isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            PitKilnBlockEntity.this.updateBurnTime();
            PitKilnBlockEntity.this.sync();
        }
    };

    private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            PitKilnBlockEntity.this.sync();
        }
    };

    private final ItemStackHandler logs = new ItemStackHandler(LOG_SLOTS) {
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
            return this.isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            PitKilnBlockEntity.this.sync();
        }
    };

    private boolean active;
    private int totalBurnTicks = DEFAULT_TOTAL_BURN_TICKS;
    private int burnTicksRemaining = DEFAULT_TOTAL_BURN_TICKS;
    private int rainTicksRemaining = RAIN_TICKS_BEFORE_EXTINGUISHED;
    private boolean needStructureValidation = true;
    private int nextStructureValidationTicks = STRUCTURE_VALIDATION_INTERVAL;
    private int invalidTicks;

    public PitKilnBlockEntity(BlockPos pos, BlockState state) {
        super(TechBasicBlockEntities.KILN_PIT.get(), pos, state);
    }

    public ItemStackHandler input() {
        return this.input;
    }

    public ItemStackHandler output() {
        return this.output;
    }

    public ItemStackHandler logs() {
        return this.logs;
    }

    public boolean isActive() {
        return this.active;
    }

    public PitKilnVariant variant() {
        return this.getBlockState().getValue(PitKilnBlock.VARIANT);
    }

    private Optional<KilnRecipe> recipeFor(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(TechBasicRecipeTypes.PIT_KILN.get(), new SingleRecipeInput(stack), this.level)
            .map(holder -> holder.value());
    }

    /** The burn's progress, 0 to 1, for the checks. */
    public float progress() {
        return !this.active || this.totalBurnTicks <= 0 ? 0 : 1 - this.burnTicksRemaining / (float) this.totalBurnTicks;
    }

    // -- Interaction ---------------------------------------------------------

    public boolean insertInput(Player player, ItemStack held) {
        return SlotInteraction.insert(this.input, 0, player, held);
    }

    /** Every output, then the ware, into the player's inventory. */
    public boolean extractAll(Player player) {
        boolean any = false;
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            any |= SlotInteraction.extract(this.output, slot, player, this.worldPosition);
        }
        if (!any) {
            any = SlotInteraction.extract(this.input, 0, player, this.worldPosition);
        }
        return any;
    }

    /** The log slot a hit on the upper third targets, by world x, or -1. */
    public int logSlotAt(BlockHitResult hit) {
        Vec3 local = HitSlots.local(hit, this.worldPosition, Direction.NORTH);
        if (local.y < LOG_ZONE_BOTTOM && hit.getDirection() != Direction.UP) {
            return -1;
        }
        return HitSlots.cell(local.x, 0, 1, LOG_SLOTS);
    }

    public boolean insertLog(Player player, ItemStack held, BlockHitResult hit) {
        int slot = this.logSlotAt(hit);
        if (slot < 0 || this.level == null || !SlotInteraction.insert(this.logs, slot, player, held, 1)) {
            return false;
        }
        if (!this.level.isClientSide) {
            this.level.playSound(null, this.worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1, 1);
            if (this.logCount() == LOG_SLOTS) {
                this.setVariant(PitKilnVariant.WOOD);
            }
        }
        return true;
    }

    public boolean extractLog(Player player, BlockHitResult hit) {
        int slot = this.logSlotAt(hit);
        if (slot < 0 || this.level == null || !SlotInteraction.extract(this.logs, slot, player, this.worldPosition)) {
            return false;
        }
        if (!this.level.isClientSide && this.variant() == PitKilnVariant.WOOD) {
            this.setVariant(PitKilnVariant.THATCH);
        }
        return true;
    }

    private int logCount() {
        int count = 0;
        for (int slot = 0; slot < LOG_SLOTS; slot++) {
            if (!this.logs.getStackInSlot(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void setVariant(PitKilnVariant variant) {
        if (this.level != null && this.variant() != variant) {
            this.level.setBlock(this.worldPosition, this.getBlockState().setValue(PitKilnBlock.VARIANT, variant), Block.UPDATE_ALL);
        }
    }

    // -- The burn ------------------------------------------------------------

    /** Fire on the logs: the logs are spent and the burn starts. */
    public void activate() {
        this.setActive(true);
        this.setVariant(PitKilnVariant.ACTIVE);
    }

    private void setActive(boolean active) {
        this.active = active;
        this.rainTicksRemaining = RAIN_TICKS_BEFORE_EXTINGUISHED;
        if (active) {
            for (int slot = 0; slot < LOG_SLOTS; slot++) {
                this.logs.setStackInSlot(slot, ItemStack.EMPTY);
            }
            this.needStructureValidation = true;
            this.invalidTicks = 0;
            this.resetBurn();
        }
        this.setChanged();
    }

    public void requestStructureValidation() {
        this.needStructureValidation = true;
    }

    private void updateBurnTime() {
        ItemStack stack = this.input.getStackInSlot(0);
        Optional<KilnRecipe> recipe = this.recipeFor(stack);
        if (recipe.isEmpty()) {
            return;
        }
        int burnTicks = TechBasicConfig.Server.scaled(recipe.get().burnTime(), TechBasicConfig.SERVER.pitKilnDurationModifier);
        double n = TechBasicConfig.SERVER.pitKilnVariableSpeedModifier.get();
        double x = stack.getCount() == 1 ? 0 : (stack.getCount() - 1) / (double) (MAX_INPUT - 1);
        this.totalBurnTicks = (int) (burnTicks * ((1 - n) * x + n));
        this.resetBurn();
        this.setChanged();
    }

    private void resetBurn() {
        this.needStructureValidation = true;
        this.burnTicksRemaining = this.totalBurnTicks;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PitKilnBlockEntity kiln) {
        if (!kiln.active) {
            return;
        }
        BlockPos above = pos.above();
        if (TechBasicConfig.COMMON.pitKilnExtinguishedByRain.get() && level.isRainingAt(above)) {
            if (--kiln.rainTicksRemaining <= 0) {
                kiln.setVariant(PitKilnVariant.THATCH);
                kiln.clearFireAbove(level, above);
                kiln.setActive(false);
                return;
            }
        } else {
            kiln.rainTicksRemaining = RAIN_TICKS_BEFORE_EXTINGUISHED;
        }
        if (!kiln.needStructureValidation && --kiln.nextStructureValidationTicks <= 0) {
            kiln.nextStructureValidationTicks = STRUCTURE_VALIDATION_INTERVAL;
            kiln.needStructureValidation = true;
        }
        if (kiln.needStructureValidation && kiln.isStructureValid(level)) {
            kiln.invalidTicks = 0;
            kiln.needStructureValidation = false;
            kiln.nextStructureValidationTicks = STRUCTURE_VALIDATION_INTERVAL;
        }
        if (!kiln.needStructureValidation) {
            kiln.keepFireAbove(level, above);
        } else if (kiln.invalidTicks < MAX_INVALID_TICKS) {
            kiln.invalidTicks++;
            kiln.resetBurn();
        } else {
            kiln.fail(level);
            return;
        }
        if (kiln.burnTicksRemaining > 0) {
            kiln.burnTicksRemaining--;
        } else {
            kiln.complete(level);
        }
    }

    private void keepFireAbove(Level level, BlockPos above) {
        BlockState state = level.getBlockState(above);
        if (!state.is(BlockTags.FIRE) && (state.isAir() || state.canBeReplaced())) {
            level.setBlock(above, BaseFireBlock.getState(level, above), Block.UPDATE_ALL);
        }
    }

    private void clearFireAbove(Level level, BlockPos above) {
        if (level.getBlockState(above).is(BlockTags.FIRE)) {
            level.removeBlock(above, false);
        }
    }

    private boolean isStructureValid(Level level) {
        PitKilnVariant variant = this.variant();
        if (variant != PitKilnVariant.WOOD && variant != PitKilnVariant.ACTIVE) {
            return false;
        }
        BlockPos above = this.worldPosition.above();
        BlockState aboveState = level.getBlockState(above);
        if (!aboveState.isAir() && !aboveState.canBeReplaced() && !aboveState.is(BlockTags.FIRE)) {
            return false;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (!this.isValidWall(level, this.worldPosition.relative(side), side.getOpposite())) {
                return false;
            }
        }
        return this.isValidWall(level, this.worldPosition.below(), Direction.UP);
    }

    /** Solid and unburnable on the face toward the kiln, or refractory. */
    private boolean isValidWall(Level level, BlockPos pos, Direction faceTowardKiln) {
        BlockState state = level.getBlockState(pos);
        return RefractoryBlocks.isRefractory(state)
            || state.isFaceSturdy(level, pos, faceTowardKiln) && !state.isFlammable(level, pos, faceTowardKiln);
    }

    private int refractoryWalls(Level level) {
        int count = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (RefractoryBlocks.isRefractory(level.getBlockState(this.worldPosition.relative(side)))) {
                count++;
            }
        }
        if (RefractoryBlocks.isRefractory(level.getBlockState(this.worldPosition.below()))) {
            count++;
        }
        return count;
    }

    /** A broken wall for too long: every piece of ware becomes a failure item, or ash. */
    private void fail(Level level) {
        ItemStack stack = this.input.getStackInSlot(0);
        Optional<KilnRecipe> recipe = this.recipeFor(stack);
        if (recipe.isPresent()) {
            for (int i = 0; i < stack.getCount(); i++) {
                this.insertOutput(this.failureItem(level, recipe.get()));
            }
            this.input.setStackInSlot(0, ItemStack.EMPTY);
        }
        this.finish(level);
    }

    private void complete(Level level) {
        ItemStack stack = this.input.getStackInSlot(0);
        Optional<KilnRecipe> recipe = this.recipeFor(stack);
        if (recipe.isPresent()) {
            float failureChance = recipe.get().failureChance() * (1 - this.refractoryWalls(level) / 5f);
            this.input.setStackInSlot(0, ItemStack.EMPTY);
            for (int i = 0; i < stack.getCount(); i++) {
                if (level.random.nextFloat() < failureChance) {
                    this.insertOutput(this.failureItem(level, recipe.get()));
                } else {
                    this.insertOutput(recipe.get().result().copy());
                }
            }
        }
        this.insertOutput(new ItemStack(CoreItems.material(Material.PIT_ASH).get(), level.random.nextInt(3) + 1));
        this.finish(level);
    }

    private ItemStack failureItem(Level level, KilnRecipe recipe) {
        List<ItemStack> failureItems = recipe.failureItems();
        if (failureItems.isEmpty()) {
            return new ItemStack(CoreItems.material(Material.PIT_ASH).get());
        }
        return failureItems.get(level.random.nextInt(failureItems.size())).copyWithCount(1);
    }

    private void finish(Level level) {
        this.setActive(false);
        this.setVariant(PitKilnVariant.COMPLETE);
        this.clearFireAbove(level, this.worldPosition.above());
    }

    private void insertOutput(ItemStack stack) {
        ItemHandlerHelper.insertItem(this.output, stack, false);
    }

    // -- Breaking ------------------------------------------------------------

    public void dropContents() {
        if (this.level != null) {
            Spill.handler(this.level, this.worldPosition, this.input, 0);
            Spill.handler(this.level, this.worldPosition, this.output, 0);
            Spill.handler(this.level, this.worldPosition, this.logs, 0);
        }
    }

    // -- Persistence ---------------------------------------------------------

    @Override
    protected void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("input", this.input.serializeNBT(registries));
        tag.put("output", this.output.serializeNBT(registries));
        tag.put("logs", this.logs.serializeNBT(registries));
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.input.deserializeNBT(registries, tag.getCompound("input"));
        this.output.deserializeNBT(registries, tag.getCompound("output"));
        this.logs.deserializeNBT(registries, tag.getCompound("logs"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("active", this.active);
        tag.putInt("totalBurnTicks", this.totalBurnTicks);
        tag.putInt("burnTicksRemaining", this.burnTicksRemaining);
        tag.putInt("rainTicksRemaining", this.rainTicksRemaining);
        tag.putInt("invalidTicks", this.invalidTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.active = tag.getBoolean("active");
        this.totalBurnTicks = tag.getInt("totalBurnTicks");
        this.burnTicksRemaining = tag.getInt("burnTicksRemaining");
        this.rainTicksRemaining = tag.getInt("rainTicksRemaining");
        this.invalidTicks = tag.getInt("invalidTicks");
    }
}
