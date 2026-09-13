package com.moostoet.pyrotech.tech.basic.block.entity;

import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.interaction.HitSlots;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.block.DryingRackBlock;
import com.moostoet.pyrotech.tech.basic.recipe.DryingRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.TechBasicRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Optional;

/**
 * A drying rack's slots and timers, as the 1.12 worker had them: each slot dries on its
 * own clock at the rack's shared speed, which the weather sets every second. Rain
 * directly on the rack runs the clocks backward.
 */
public final class DryingRackBlockEntity extends SyncedBlockEntity {

    public static final int NORMAL_SLOTS = 4;
    private static final int SPEED_CHECK_INTERVAL_TICKS = 20;
    private static final double HIGH_HUMIDITY = 0.85;
    private static final double CLIMB_SPEED = 0.1;

    private final int slots;
    private final boolean crude;
    private final ItemStackHandler input;
    private final ItemStackHandler output;
    private final int[] dryTimeTotal;
    private final int[] dryTimeRemaining;
    private final double[] partialTicks;
    private float speed;
    private int speedCheckTicks;

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(TechBasicBlockEntities.DRYING_RACK.get(), pos, state);
        DryingRackBlock block = (DryingRackBlock) state.getBlock();
        this.crude = block.isCrude();
        this.slots = block.slots();
        this.dryTimeTotal = new int[this.slots];
        this.dryTimeRemaining = new int[this.slots];
        this.partialTicks = new double[this.slots];
        java.util.Arrays.fill(this.dryTimeTotal, -1);
        java.util.Arrays.fill(this.dryTimeRemaining, -1);
        this.input = new ItemStackHandler(this.slots) {
            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return DryingRackBlockEntity.this.recipeFor(stack).isPresent();
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return this.isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            protected void onContentsChanged(int slot) {
                DryingRackBlockEntity.this.startSlot(slot);
                DryingRackBlockEntity.this.sync();
            }
        };
        this.output = new ItemStackHandler(this.slots) {
            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                DryingRackBlockEntity.this.sync();
            }
        };
    }

    public int slots() {
        return this.slots;
    }

    public boolean isCrude() {
        return this.crude;
    }

    public ItemStackHandler input() {
        return this.input;
    }

    public ItemStackHandler output() {
        return this.output;
    }

    public float speed() {
        return this.speed;
    }

    public Direction facing() {
        return this.getBlockState().getValue(DryingRackBlock.FACING);
    }

    private RecipeType<DryingRecipe> recipeType() {
        return this.crude ? TechBasicRecipeTypes.CRUDE_DRYING_RACK.get() : TechBasicRecipeTypes.DRYING_RACK.get();
    }

    public Optional<DryingRecipe> recipeFor(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(this.recipeType(), new SingleRecipeInput(stack), this.level).map(holder -> holder.value());
    }

    public float progress(int slot) {
        return this.dryTimeTotal[slot] <= 0 ? 0 : 1 - this.dryTimeRemaining[slot] / (float) this.dryTimeTotal[slot];
    }

    // -- Interaction ---------------------------------------------------------

    /** The slot under a hit: the crude rack's one, or the 2 by 2 cell on the frame's top. */
    public int slotAt(BlockHitResult hit) {
        if (this.crude) {
            return 0;
        }
        if (hit.getDirection() != Direction.UP) {
            return -1;
        }
        Vec3 local = HitSlots.local(hit, this.worldPosition, this.facing());
        return HitSlots.cell(local.x, 0, 1, 2) + 2 * HitSlots.cell(local.z, 0, 1, 2);
    }

    /** Into the input when the item dries, else onto the output as a plain shelf, whichever is free. */
    public boolean insert(Player player, ItemStack held, BlockHitResult hit) {
        int slot = this.slotAt(hit);
        if (slot < 0) {
            return false;
        }
        if (!this.input.getStackInSlot(slot).isEmpty()) {
            return false;
        }
        if (!this.output.getStackInSlot(slot).isEmpty()) {
            return false;
        }
        ItemStackHandler target = this.recipeFor(held).isPresent() ? this.input : this.output;
        return SlotInteraction.insert(target, slot, player, held, 1);
    }

    public boolean extract(Player player, BlockHitResult hit) {
        int slot = this.slotAt(hit);
        return slot >= 0 && (SlotInteraction.extract(this.output, slot, player, this.worldPosition)
            || SlotInteraction.extract(this.input, slot, player, this.worldPosition));
    }

    // -- Drying --------------------------------------------------------------

    private void startSlot(int slot) {
        ItemStack stack = this.input.getStackInSlot(slot);
        Optional<DryingRecipe> recipe = this.recipeFor(stack);
        int ticks = recipe.map(r -> TechBasicConfig.Server.scaled(r.dryTime(), this.crude
            ? TechBasicConfig.SERVER.crudeDryingRackDurationModifier
            : TechBasicConfig.SERVER.dryingRackDurationModifier)).orElse(-1);
        this.dryTimeTotal[slot] = ticks;
        this.dryTimeRemaining[slot] = ticks;
        this.partialTicks[slot] = 0;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, DryingRackBlockEntity rack) {
        rack.tryClimb(level, pos, state);
        if (rack.speed > 0 && rack.hasInput() && level.getGameTime() % 40 == 0) {
            ClientProgress.particles(level, 1, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, 0.5, 0.15, 0.5);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DryingRackBlockEntity rack) {
        rack.tryClimb(level, pos, state);
        if (++rack.speedCheckTicks >= SPEED_CHECK_INTERVAL_TICKS) {
            rack.speedCheckTicks = 0;
            float speed = rack.calculateSpeed(level, pos);
            if (speed != rack.speed) {
                rack.speed = speed;
                rack.sync();
            }
        }
        boolean dirty = false;
        for (int slot = 0; slot < rack.slots; slot++) {
            if (rack.input.getStackInSlot(slot).isEmpty()) {
                continue;
            }
            if (rack.dryTimeRemaining[slot] > 0) {
                rack.partialTicks[slot] += rack.speed;
                double fullTicks = Math.floor(rack.partialTicks[slot]);
                if (fullTicks != 0) {
                    rack.partialTicks[slot] -= fullTicks;
                    rack.dryTimeRemaining[slot] = (int) Math.min(rack.dryTimeTotal[slot], Math.max(0, rack.dryTimeRemaining[slot] - fullTicks));
                    dirty = true;
                }
            }
            if (rack.dryTimeRemaining[slot] == 0) {
                ItemStack stack = rack.input.extractItem(slot, 64, false);
                rack.dryTimeTotal[slot] = -1;
                rack.dryTimeRemaining[slot] = -1;
                rack.partialTicks[slot] = 0;
                Optional<DryingRecipe> recipe = rack.recipeFor(stack);
                if (recipe.isPresent()) {
                    rack.output.insertItem(slot, recipe.get().result().copy(), false);
                }
            }
        }
        if (dirty) {
            rack.setChanged();
        }
    }

    private boolean hasInput() {
        for (int slot = 0; slot < this.slots; slot++) {
            if (!this.input.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** The 1.12 {@code updateSpeed}: the weather, the biome, nearby fire, and daylight. */
    private float calculateSpeed(Level level, BlockPos pos) {
        TechBasicConfig.DryingRackModifiers modifiers = this.crude
            ? TechBasicConfig.SERVER.crudeDryingRackModifiers
            : TechBasicConfig.SERVER.dryingRackModifiers;
        Holder<Biome> biome = level.getBiome(pos);
        boolean canRain = biome.value().hasPrecipitation();
        double speed;
        if (canRain && level.isRainingAt(pos.above())) {
            speed = modifiers.directRain.get();
        } else if (canRain && level.isRaining() || biome.value().getModifiedClimateSettings().downfall() > HIGH_HUMIDITY) {
            speed = modifiers.indirectRain.get();
        } else if (level.dimension() == Level.NETHER) {
            speed = modifiers.nether.get();
        } else {
            speed = modifiers.baseDerived.get();
            if (biome.is(Tags.Biomes.IS_HOT)) {
                speed += modifiers.derivedHot.get();
            }
            if (biome.is(Tags.Biomes.IS_DRY)) {
                speed += modifiers.derivedDry.get();
            }
            if (biome.is(Tags.Biomes.IS_COLD)) {
                speed += modifiers.derivedCold.get();
            }
            if (biome.is(Tags.Biomes.IS_WET)) {
                speed += modifiers.derivedWet.get();
            }
        }
        int range = modifiers.fireSourceBonusRange.get();
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-range, -range, -range), pos.offset(range, range, range))) {
            BlockState state = level.getBlockState(near);
            if (state.is(BlockTags.FIRE) || state.isFireSource(level, near, Direction.UP)) {
                speed += modifiers.fireSourceBonus.get();
            }
        }
        long dayTime = level.getDayTime() % 24000;
        if (!level.isRaining() && level.canSeeSky(pos.above()) && dayTime > 3000 && dayTime < 9000) {
            speed += modifiers.daytime.get();
        }
        double multiplier = this.crude
            ? TechBasicConfig.SERVER.crudeDryingRackSpeedModifier.get()
            : TechBasicConfig.SERVER.dryingRackSpeedModifier.get();
        return (float) (speed * multiplier);
    }

    // -- The ladder ----------------------------------------------------------

    /** The 1.12 {@code tryClimb}: a rack stacked on a rack nudges players up it (tech/basic sign-off, item 10). */
    private void tryClimb(Level level, BlockPos pos, BlockState state) {
        if (this.crude || !TechBasicConfig.COMMON.useAsLadder.get()) {
            return;
        }
        BlockState below = level.getBlockState(pos.below());
        boolean rackBelow = below.getBlock() instanceof DryingRackBlock rack && !rack.isCrude();
        if (!state.getValue(DryingRackBlock.STACKED) && !rackBelow) {
            return;
        }
        AABB bounds = new AABB(pos).inflate(0.6, 0.2, 0.6);
        for (Player player : level.getEntitiesOfClass(Player.class, bounds)) {
            Vec3 motion = player.getDeltaMovement();
            if (player.isShiftKeyDown()) {
                player.setDeltaMovement(motion.x, 0, motion.z);
            } else if (player.horizontalCollision) {
                if (player.zza > 0 && motion.y < CLIMB_SPEED) {
                    player.setDeltaMovement(motion.x, CLIMB_SPEED, motion.z);
                }
            } else if (motion.y < -CLIMB_SPEED) {
                player.setDeltaMovement(motion.x, -CLIMB_SPEED, motion.z);
            }
            player.fallDistance = 0;
        }
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
        tag.putFloat("speed", this.speed);
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.input.deserializeNBT(registries, tag.getCompound("input"));
        this.output.deserializeNBT(registries, tag.getCompound("output"));
        this.speed = tag.getFloat("speed");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("dryTimeTotal", this.dryTimeTotal.clone());
        tag.putIntArray("dryTimeRemaining", this.dryTimeRemaining.clone());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] total = tag.getIntArray("dryTimeTotal");
        int[] remaining = tag.getIntArray("dryTimeRemaining");
        for (int slot = 0; slot < this.slots; slot++) {
            this.dryTimeTotal[slot] = slot < total.length ? total[slot] : -1;
            this.dryTimeRemaining[slot] = slot < remaining.length ? remaining[slot] : -1;
        }
    }
}
