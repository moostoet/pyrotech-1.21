package com.moostoet.pyrotech.tech.basic.block.entity;

import com.moostoet.pyrotech.core.CombustParticles;
import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.fluid.HotFluidTank;
import com.moostoet.pyrotech.library.interaction.HitSlots;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.TechBasicComponents;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.TechBasicItems;
import com.moostoet.pyrotech.tech.basic.block.BarrelBlock;
import com.moostoet.pyrotech.tech.basic.recipe.BarrelRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The barrel's bucket of fluid, its four items, and its lid. Open, it catches rain and
 * rain slowly turns whatever it holds into water. Sealed, the recipe for its items and
 * fluid runs and replaces the fluid. A sealed barrel breaks into an item that carries
 * everything.
 */
public final class BarrelBlockEntity extends SyncedBlockEntity {

    public static final int SLOTS = BarrelRecipe.SLOTS;
    private static final int RAIN_FILL_INTERVAL_TICKS = 20;
    private static final int RAIN_FILL_MB = 5;
    private static final int RAIN_CONVERSION_TICKS = 2400;
    private static final double GRID_MIN = 2 / 16.0;
    private static final double GRID_MAX = 14 / 16.0;

    private final HotFluidTank tank = new HotFluidTank(FluidType.BUCKET_VOLUME, false, this::onHotFluid) {
        @Override
        protected void onContentsChanged() {
            BarrelBlockEntity.this.onChanged();
        }
    };

    private final ItemStackHandler input = new ItemStackHandler(SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return BarrelBlockEntity.this.level != null && !BarrelBlockEntity.this.isSealed()
                && !BarrelBlockEntity.this.tank.isEmpty()
                && BarrelRecipe.isValidItem(BarrelBlockEntity.this.level, stack, BarrelBlockEntity.this.tank.getFluid());
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return this.isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            BarrelBlockEntity.this.onChanged();
        }
    };

    private final ItemStackHandler lid = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(TechBasicItems.BARREL_LID.get());
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return this.isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            BarrelBlockEntity.this.onLidChanged();
        }
    };

    private float progress;
    private int rainFillTicks;
    private int rainConversionTicks;
    @Nullable
    private RecipeHolder<BarrelRecipe> recipe;

    public BarrelBlockEntity(BlockPos pos, BlockState state) {
        super(TechBasicBlockEntities.BARREL.get(), pos, state);
        this.tank.setValidator(stack -> !this.isSealed());
    }

    public HotFluidTank tank() {
        return this.tank;
    }

    public ItemStackHandler input() {
        return this.input;
    }

    public ItemStackHandler lid() {
        return this.lid;
    }

    public boolean isSealed() {
        return this.getBlockState().getValue(BarrelBlock.SEALED);
    }

    public float progress() {
        return this.progress;
    }

    public boolean hasRecipe() {
        return this.recipe != null;
    }

    // -- Interaction ---------------------------------------------------------

    /** The 2 by 2 cell a hit on the top targets, within the 2 to 14 pixel square. */
    public int slotAt(BlockHitResult hit) {
        Vec3 local = HitSlots.local(hit, this.worldPosition, Direction.NORTH);
        return HitSlots.cell(local.x, GRID_MIN, GRID_MAX, 2) + 2 * HitSlots.cell(local.z, GRID_MIN, GRID_MAX, 2);
    }

    public boolean insertItem(Player player, ItemStack held, BlockHitResult hit) {
        return SlotInteraction.insert(this.input, this.slotAt(hit), player, held, 1);
    }

    public boolean extractItem(Player player, BlockHitResult hit) {
        return SlotInteraction.extract(this.input, this.slotAt(hit), player, this.worldPosition);
    }

    public boolean insertLid(Player player, ItemStack held) {
        if (this.level == null || !SlotInteraction.insert(this.lid, 0, player, held, 1)) {
            return false;
        }
        if (!this.level.isClientSide) {
            this.level.playSound(null, this.worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1, 1);
        }
        return true;
    }

    public boolean extractLid(Player player) {
        return SlotInteraction.extract(this.lid, 0, player, this.worldPosition);
    }

    // -- Changes -------------------------------------------------------------

    private void onChanged() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        this.updateRecipe();
        this.sync();
    }

    /** The lid seals and unseals the block; unsealing forgets the progress. */
    private void onLidChanged() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        boolean sealed = !this.lid.getStackInSlot(0).isEmpty();
        if (!sealed) {
            this.progress = 0;
        }
        if (this.isSealed() != sealed) {
            this.level.setBlock(this.worldPosition, this.getBlockState().setValue(BarrelBlock.SEALED, sealed), Block.UPDATE_ALL);
        }
        this.onChanged();
    }

    private void updateRecipe() {
        if (this.level == null) {
            return;
        }
        List<ItemStack> items = new ArrayList<>(SLOTS);
        for (int slot = 0; slot < SLOTS; slot++) {
            items.add(this.input.getStackInSlot(slot));
        }
        this.recipe = BarrelRecipe.find(this.level, items, this.tank.getFluid()).orElse(null);
    }

    /** A hot fluid poured in: the barrel breaks, as the tank does (storage sign-off, item 6). */
    private void onHotFluid(FluidStack resource) {
        if (this.level instanceof ServerLevel level) {
            level.destroyBlock(this.worldPosition, true);
            CombustParticles.spawn(level, this.worldPosition, 0.5);
        }
    }

    // -- Ticking -------------------------------------------------------------

    public static void clientTick(Level level, BlockPos pos, BlockState state, BarrelBlockEntity barrel) {
        if (barrel.isSealed() && barrel.recipe != null && level.getGameTime() % 40 == 0) {
            ClientProgress.particles(level, 1, pos.getX() + 0.5, pos.getY() + 1.25, pos.getZ() + 0.5, 0.5, 0.25, 0.5);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BarrelBlockEntity barrel) {
        if (!barrel.isSealed()) {
            barrel.tickRain(level, pos);
            return;
        }
        barrel.rainConversionTicks = 0;
        if (barrel.recipe == null) {
            barrel.progress = 0;
            return;
        }
        BarrelRecipe recipe = barrel.recipe.value();
        int ticks = TechBasicConfig.Server.scaled(recipe.time(), TechBasicConfig.SERVER.barrelDurationModifier);
        barrel.progress += 1f / ticks;
        if (barrel.progress >= 0.9999f) {
            for (int slot = 0; slot < SLOTS; slot++) {
                barrel.input.setStackInSlot(slot, ItemStack.EMPTY);
            }
            if (!FluidStack.isSameFluidSameComponents(recipe.result(), barrel.tank.getFluid())) {
                barrel.tank.setFluid(recipe.result().copy());
            }
            barrel.progress = 0;
            barrel.onChanged();
        }
        barrel.setChanged();
    }

    /** Rain on an open barrel: five millibuckets every second up to a bucket, and any other fluid turns to water in two minutes. */
    private void tickRain(Level level, BlockPos pos) {
        if (!level.isRainingAt(pos.above())) {
            this.rainConversionTicks = 0;
            return;
        }
        if (++this.rainFillTicks >= RAIN_FILL_INTERVAL_TICKS) {
            this.rainFillTicks = 0;
            if (this.tank.getFluidAmount() < FluidType.BUCKET_VOLUME) {
                this.tank.fill(new FluidStack(Fluids.WATER, RAIN_FILL_MB), IFluidHandler.FluidAction.EXECUTE);
            }
        }
        if (++this.rainConversionTicks >= RAIN_CONVERSION_TICKS) {
            this.rainConversionTicks = 0;
            FluidStack fluid = this.tank.getFluid();
            if (!fluid.isEmpty() && !fluid.is(Fluids.WATER)) {
                this.tank.setFluid(new FluidStack(Fluids.WATER, fluid.getAmount()));
                this.onChanged();
            }
        }
    }

    // -- The sealed item -----------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        this.tank.setFluid(input.getOrDefault(TechBasicComponents.BARREL_FLUID.get(), SimpleFluidContent.EMPTY).copy());
        ItemContainerContents contents = input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        for (int slot = 0; slot < SLOTS && slot < contents.getSlots(); slot++) {
            this.input.setStackInSlot(slot, contents.getStackInSlot(slot));
        }
        if (contents.getSlots() > SLOTS) {
            this.lid.setStackInSlot(0, contents.getStackInSlot(SLOTS));
        }
        input.get(DataComponents.MAX_STACK_SIZE);
        input.get(DataComponents.ITEM_NAME);
        this.updateRecipe();
    }

    /** A sealed barrel's item: the fluid, the four items and the lid, one to a stack, named as sealed. */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!this.isSealed()) {
            return;
        }
        List<ItemStack> items = new ArrayList<>(SLOTS + 1);
        for (int slot = 0; slot < SLOTS; slot++) {
            items.add(this.input.getStackInSlot(slot).copy());
        }
        items.add(this.lid.getStackInSlot(0).copy());
        components.set(TechBasicComponents.BARREL_FLUID.get(), SimpleFluidContent.copyOf(this.tank.getFluid()));
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        components.set(DataComponents.MAX_STACK_SIZE, 1);
        components.set(DataComponents.ITEM_NAME, Component.translatable("block.pyrotech.barrel_sealed"));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("tank");
        tag.remove("input");
        tag.remove("lid");
    }

    public void dropContents() {
        if (this.level != null) {
            Spill.handler(this.level, this.worldPosition, this.input, 0);
            Spill.handler(this.level, this.worldPosition, this.lid, 0);
        }
    }

    // -- Persistence ---------------------------------------------------------

    @Override
    protected void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("tank", this.tank.writeToNBT(registries, new CompoundTag()));
        tag.put("input", this.input.serializeNBT(registries));
        tag.put("lid", this.lid.serializeNBT(registries));
        tag.putBoolean("hasRecipe", this.recipe != null);
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.tank.readFromNBT(registries, tag.getCompound("tank"));
        this.input.deserializeNBT(registries, tag.getCompound("input"));
        this.lid.deserializeNBT(registries, tag.getCompound("lid"));
        if (this.level != null && !this.level.isClientSide) {
            this.updateRecipe();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("progress", this.progress);
        tag.putInt("rainFillTicks", this.rainFillTicks);
        tag.putInt("rainConversionTicks", this.rainConversionTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getFloat("progress");
        this.rainFillTicks = tag.getInt("rainFillTicks");
        this.rainConversionTicks = tag.getInt("rainConversionTicks");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide) {
            this.updateRecipe();
        }
    }
}
