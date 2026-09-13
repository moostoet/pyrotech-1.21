package com.moostoet.pyrotech.tech.basic.block.entity;

import com.moostoet.pyrotech.core.CombustParticles;
import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.fluid.HotFluidTank;
import com.moostoet.pyrotech.library.interaction.ScrollInteractable;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.library.inventory.LargeStackHandler;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.block.SoakingPotBlock;
import com.moostoet.pyrotech.tech.basic.recipe.SoakingPotRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The soaking pot's tank, input, and output. The input takes only as many items as the
 * fluid can pay for, and spits the surplus when the fluid drops. The recipe drains its
 * fluid per item on completion.
 */
public final class SoakingPotBlockEntity extends SyncedBlockEntity implements ScrollInteractable {

    public static final int CAPACITY = 4 * FluidType.BUCKET_VOLUME;
    public static final int MAX_INPUT = 8;
    private static final int OUTPUT_STACKS = 10;

    private final HotFluidTank tank = new HotFluidTank(CAPACITY, false, this::onHotFluid) {
        @Override
        protected void onContentsChanged() {
            SoakingPotBlockEntity.this.onChanged();
            SoakingPotBlockEntity.this.ejectOverfill();
        }
    };

    private final ItemStackHandler input = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return MAX_INPUT;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            SoakingPotBlockEntity pot = SoakingPotBlockEntity.this;
            if (pot.level == null || !pot.output.getStackInSlot(0).isEmpty() || pot.tank.isEmpty()) {
                return stack;
            }
            FluidStack fluid = pot.tank.getFluid();
            SoakingPotRecipe recipe = SoakingPotRecipe.find(pot.level, stack, fluid).map(RecipeHolder::value).orElse(null);
            if (recipe == null) {
                return stack;
            }
            int perItem = recipe.fluid().amount();
            int existing = this.getStackInSlot(0).getCount();
            if (fluid.getAmount() < perItem * existing) {
                return stack;
            }
            int room = stack.getCount() - super.insertItem(slot, stack, true).getCount();
            int accepted = 0;
            while (accepted < room && fluid.getAmount() >= perItem * (existing + accepted + 1)) {
                accepted++;
            }
            if (accepted == 0) {
                return stack;
            }
            super.insertItem(slot, stack.copyWithCount(accepted), simulate);
            return stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        protected void onContentsChanged(int slot) {
            SoakingPotBlockEntity.this.onChanged();
        }
    };

    private final LargeStackHandler output = new LargeStackHandler(1, OUTPUT_STACKS) {
        @Override
        protected void onContentsChanged(int slot) {
            SoakingPotBlockEntity.this.sync();
        }
    };

    private float progress;
    @Nullable
    private RecipeHolder<SoakingPotRecipe> recipe;

    public SoakingPotBlockEntity(BlockPos pos, BlockState state) {
        super(TechBasicBlockEntities.SOAKING_POT.get(), pos, state);
    }

    public HotFluidTank tank() {
        return this.tank;
    }

    public ItemStackHandler input() {
        return this.input;
    }

    public LargeStackHandler output() {
        return this.output;
    }

    public float progress() {
        return this.progress;
    }

    public boolean hasRecipe() {
        return this.recipe != null;
    }

    public boolean isOverCampfire() {
        return this.getBlockState().getValue(SoakingPotBlock.CAMPFIRE);
    }

    // -- Interaction ---------------------------------------------------------

    public boolean insert(Player player, ItemStack held) {
        return SlotInteraction.insert(this.input, 0, player, held);
    }

    public boolean extract(Player player) {
        return SlotInteraction.extract(this.output, 0, player, this.worldPosition)
            || SlotInteraction.extract(this.input, 0, player, this.worldPosition);
    }

    @Override
    public void scroll(Player player, BlockHitResult hit, boolean up) {
        if (up) {
            SlotInteraction.scrollInsert(this.input, 0, player);
        } else if (!SlotInteraction.extractOne(this.output, 0, player, this.worldPosition)) {
            SlotInteraction.extractOne(this.input, 0, player, this.worldPosition);
        }
    }

    // -- Changes -------------------------------------------------------------

    private void onChanged() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        this.updateRecipe();
        this.sync();
    }

    private void updateRecipe() {
        if (this.level == null) {
            return;
        }
        RecipeHolder<SoakingPotRecipe> found = SoakingPotRecipe.find(this.level, this.input.getStackInSlot(0), this.tank.getFluid()).orElse(null);
        if (found == null || this.recipe == null || !found.id().equals(this.recipe.id())) {
            this.progress = 0;
        }
        this.recipe = found;
    }

    /** Items the fluid can no longer pay for jump out. */
    private void ejectOverfill() {
        if (this.recipe == null || this.level == null || this.level.isClientSide) {
            return;
        }
        int perItem = this.recipe.value().fluid().amount();
        int count = this.input.getStackInSlot(0).getCount();
        int affordable = this.tank.getFluidAmount() / perItem;
        if (affordable < count) {
            Spill.onTop(this.level, this.worldPosition, this.input.extractItem(0, count - affordable, false), 0.5);
        }
    }

    private void onHotFluid(FluidStack resource) {
        if (this.level instanceof ServerLevel level) {
            level.destroyBlock(this.worldPosition, true);
            CombustParticles.spawn(level, this.worldPosition, 0.5);
        }
    }

    // -- Ticking -------------------------------------------------------------

    public static void clientTick(Level level, BlockPos pos, BlockState state, SoakingPotBlockEntity pot) {
        if (pot.recipe == null && !pot.input.getStackInSlot(0).isEmpty() && pot.level != null) {
            pot.recipe = SoakingPotRecipe.find(level, pot.input.getStackInSlot(0), pot.tank.getFluid()).orElse(null);
        }
        if (pot.recipe != null && !pot.input.getStackInSlot(0).isEmpty() && level.getGameTime() % 40 == 0
            && (!pot.recipe.value().campfireRequired() || pot.hasLitCampfireBelow(level, pos))) {
            double y = pos.getY() + (pot.isOverCampfire() ? 0.5 : 0.75);
            ClientProgress.particles(level, 1, pos.getX() + 0.5, y, pos.getZ() + 0.5, 0.25, 0.25, 0.25);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SoakingPotBlockEntity pot) {
        if (pot.recipe == null) {
            pot.progress = 0;
            return;
        }
        SoakingPotRecipe recipe = pot.recipe.value();
        if (recipe.campfireRequired() && !pot.hasLitCampfireBelow(level, pos)) {
            return;
        }
        ItemStack stack = pot.input.getStackInSlot(0);
        int drain = recipe.fluid().amount() * stack.getCount();
        if (pot.tank.drain(drain, IFluidHandler.FluidAction.SIMULATE).getAmount() != drain) {
            return;
        }
        int ticks = TechBasicConfig.Server.scaled(recipe.time(), TechBasicConfig.SERVER.soakingPotDurationModifier);
        pot.progress += 1f / ticks;
        if (pot.progress >= 0.9999f) {
            ItemStack soaked = pot.input.extractItem(0, MAX_INPUT, false);
            pot.tank.drain(recipe.fluid().amount() * soaked.getCount(), IFluidHandler.FluidAction.EXECUTE);
            ItemStack result = recipe.result().copy();
            result.setCount(soaked.getCount() * result.getCount());
            pot.output.insertItem(0, result, false);
            pot.progress = 0;
            pot.updateRecipe();
        }
        pot.setChanged();
    }

    private boolean hasLitCampfireBelow(Level level, BlockPos pos) {
        return level.getBlockEntity(pos.below()) instanceof CampfireBlockEntity campfire && campfire.isLit();
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
        tag.put("tank", this.tank.writeToNBT(registries, new CompoundTag()));
        tag.put("input", this.input.serializeNBT(registries));
        tag.put("output", this.output.serializeNBT(registries));
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.tank.readFromNBT(registries, tag.getCompound("tank"));
        this.input.deserializeNBT(registries, tag.getCompound("input"));
        this.output.deserializeNBT(registries, tag.getCompound("output"));
        this.recipe = null;
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

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide) {
            float progress = this.progress;
            this.updateRecipe();
            this.progress = progress;
        }
    }
}
