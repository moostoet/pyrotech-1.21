package com.moostoet.pyrotech.tech.basic.block.entity;

import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.interaction.ScrollInteractable;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.library.inventory.LargeStackHandler;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.block.CompostBinBlock;
import com.moostoet.pyrotech.tech.basic.recipe.CompostValues;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Optional;

/**
 * The compost bin's sixteen layers, as 1.12 ran them. Every item is worth a compost
 * value; sixteen value fills a layer; a wet layer composts over the configured time,
 * faster with more active layers above it, and turns into one output. Layers that
 * finish without an output slot to fill rotate back to the top.
 */
public final class CompostBinBlockEntity extends SyncedBlockEntity implements ScrollInteractable {

    public static final int LAYERS = 16;
    public static final int VALUE_PER_OUTPUT = 16;
    public static final int MAX_VALUE = LAYERS * VALUE_PER_OUTPUT;
    private static final int UPDATE_INTERVAL_TICKS = 20;
    private static final double SPEED_PER_LAYER_ABOVE = 0.2;
    private static final int EVAPORATION_MB = 1;
    private static final int EVAPORATION_TICKS = 48;
    private static final int RAIN_MB = 20;

    private final LargeStackHandler input = new LargeStackHandler(1, MAX_VALUE) {
        @Override
        public int getSlotLimit(int slot) {
            return MAX_VALUE;
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            int value = CompostBinBlockEntity.this.valueOf(stack);
            if (value <= 0) {
                return 0;
            }
            int room = MAX_VALUE - CompostBinBlockEntity.this.totalValue() + this.getStackInSlot(slot).getCount() * value;
            return Math.max(0, room / value);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return CompostBinBlockEntity.this.valueOf(stack) > 0;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return this.isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            CompostBinBlockEntity.this.onChanged();
        }
    };

    private final ItemStackHandler output = new ItemStackHandler(LAYERS) {
        @Override
        protected void onContentsChanged(int slot) {
            CompostBinBlockEntity.this.onChanged();
        }
    };

    private final FluidTank tank = new FluidTank(FluidType.BUCKET_VOLUME, stack -> stack.is(Tags.Fluids.WATER)) {
        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (CompostBinBlockEntity.this.totalValue() <= 0) {
                return 0;
            }
            int filled = super.fill(resource, action);
            if (filled > 0 && action.execute()) {
                CompostBinBlockEntity.this.splash();
            }
            return filled;
        }

        @Override
        protected void onContentsChanged() {
            CompostBinBlockEntity.this.onChanged();
        }
    };

    private final IFluidHandler fillOnlyTank = new FillOnlyTank();

    private int storedValue;
    private ItemStack currentOutput = ItemStack.EMPTY;
    private final float[] layerProgress = new float[LAYERS];
    private final int[] layerOutputSlot = new int[LAYERS];
    private double accumulatedEvaporation;
    private int updateCounter = UPDATE_INTERVAL_TICKS;

    public CompostBinBlockEntity(BlockPos pos, BlockState state) {
        super(TechBasicBlockEntities.COMPOST_BIN.get(), pos, state);
        for (int layer = 0; layer < LAYERS; layer++) {
            this.layerOutputSlot[layer] = layer;
        }
    }

    public LargeStackHandler input() {
        return this.input;
    }

    public ItemStackHandler output() {
        return this.output;
    }

    public FluidTank tank() {
        return this.tank;
    }

    /** The capability view: fills only, as 1.12's {@code canDrain} false had it. */
    public IFluidHandler fillOnlyTank() {
        return this.fillOnlyTank;
    }

    public ItemStack currentOutput() {
        return this.currentOutput;
    }

    public int storedValue() {
        return this.storedValue;
    }

    // -- Values --------------------------------------------------------------

    private Optional<CompostValues.Entry> entryFor(ItemStack stack) {
        if (this.level == null) {
            return Optional.empty();
        }
        Optional<CompostValues.Entry> entry = CompostValues.of(this.level, stack);
        if (entry.isPresent() && !this.currentOutput.isEmpty() && !entry.get().sameResult(this.currentOutput)) {
            return Optional.empty();
        }
        return entry;
    }

    /** One item's value, or 0 when it cannot join what the bin already makes. */
    public int valueOf(ItemStack stack) {
        return this.entryFor(stack).map(CompostValues.Entry::value).orElse(0);
    }

    public int inputValue() {
        ItemStack stack = this.input.getStackInSlot(0);
        return stack.isEmpty() ? 0 : this.valueOf(stack) * stack.getCount();
    }

    public int outputValue() {
        if (this.currentOutput.isEmpty()) {
            return 0;
        }
        int items = 0;
        for (int slot = 0; slot < LAYERS; slot++) {
            items += this.output.getStackInSlot(slot).getCount();
        }
        return items / this.currentOutput.getCount() * VALUE_PER_OUTPUT;
    }

    public int totalValue() {
        return this.inputValue() + this.storedValue + this.outputValue();
    }

    public float moisture() {
        return this.tank.getFluidAmount() / (float) FluidType.BUCKET_VOLUME;
    }

    public boolean isEmpty() {
        return this.input.isEmpty() && this.storedValue == 0 && this.outputCount() == 0;
    }

    private int outputCount() {
        int count = 0;
        for (int slot = 0; slot < LAYERS; slot++) {
            count += this.output.getStackInSlot(slot).getCount();
        }
        return count;
    }

    private static int layerRequiredValue(int layer) {
        return (layer + 1) * VALUE_PER_OUTPUT;
    }

    private boolean isLayerComplete(int layer) {
        return this.layerProgress[layer] > 0.9999f;
    }

    public int activeLayers() {
        int total = this.totalValue();
        int count = 0;
        for (int layer = 0; layer < LAYERS; layer++) {
            if (layerRequiredValue(layer) <= total) {
                count++;
            }
        }
        return count;
    }

    public int completeLayers() {
        int count = 0;
        for (int layer = 0; layer < LAYERS; layer++) {
            if (this.isLayerComplete(layer)) {
                count++;
            }
        }
        return count;
    }

    // -- Interaction ---------------------------------------------------------

    public boolean insert(Player player, ItemStack held) {
        if (this.level == null || !SlotInteraction.insert(this.input, 0, player, held)) {
            return false;
        }
        if (!this.level.isClientSide) {
            this.level.playSound(null, this.worldPosition, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.5f, 1);
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

    /** A shovel takes the topmost finished output and wears one. */
    public boolean takeOutput(Player player, ItemStack shovel, InteractionHand hand) {
        if (this.level == null) {
            return false;
        }
        for (int slot = LAYERS - 1; slot >= 0; slot--) {
            if (this.output.getStackInSlot(slot).isEmpty()) {
                continue;
            }
            if (!this.level.isClientSide) {
                SlotInteraction.giveOrDrop(player, this.output.extractItem(slot, Integer.MAX_VALUE, false), this.worldPosition, true);
                shovel.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return true;
        }
        return false;
    }

    /** A hand-filled bin spills flowing water around it, as 1.12 did, unless it rains. */
    private void splash() {
        if (!(this.level instanceof ServerLevel level) || level.isRainingAt(this.worldPosition.above())) {
            return;
        }
        BlockState water = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 7);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos pos = this.worldPosition.relative(side);
            if (level.getBlockState(pos).canBeReplaced()) {
                level.setBlock(pos, water, Block.UPDATE_ALL);
            }
        }
    }

    // -- Ticking -------------------------------------------------------------

    public static void clientTick(Level level, BlockPos pos, BlockState state, CompostBinBlockEntity bin) {
        if (bin.moisture() > 0 && bin.activeLayers() - bin.completeLayers() > 0 && level.getGameTime() % 20 == 0) {
            ClientProgress.particles(level, 1, pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5, 0.5, 0.15, 0.5);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CompostBinBlockEntity bin) {
        if (--bin.updateCounter > 0) {
            return;
        }
        bin.updateCounter = UPDATE_INTERVAL_TICKS;
        bin.tickMoisture(level, pos);
        bin.tickLayers();
        bin.updateState();
        bin.setChanged();
    }

    private void tickMoisture(Level level, BlockPos pos) {
        if (this.totalValue() == 0) {
            this.tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            return;
        }
        if (level.isRainingAt(pos.above())) {
            this.tank.fill(new FluidStack(Fluids.WATER, RAIN_MB), IFluidHandler.FluidAction.EXECUTE);
        } else if (this.moisture() > 0) {
            this.accumulatedEvaporation += EVAPORATION_MB / (double) EVAPORATION_TICKS * UPDATE_INTERVAL_TICKS;
            while (this.accumulatedEvaporation >= 1) {
                this.accumulatedEvaporation -= 1;
                this.tank.drain(1, IFluidHandler.FluidAction.EXECUTE);
            }
        } else {
            this.accumulatedEvaporation = 0;
        }
    }

    private void tickLayers() {
        int total = this.totalValue();
        int outputValue = this.outputValue();
        int activeLayers = this.activeLayers();
        float increment = Math.min(1, 1f / TechBasicConfig.SERVER.compostDurationTicks.get() * UPDATE_INTERVAL_TICKS);
        for (int layer = LAYERS - 1; layer >= 0; layer--) {
            if (this.isLayerComplete(layer) && this.output.getStackInSlot(this.layerOutputSlot[layer]).isEmpty()) {
                this.rotateLayerToTop(layer);
            }
        }
        for (int layer = 0; layer < LAYERS; layer++) {
            if (this.isLayerComplete(layer)) {
                continue;
            }
            int required = layerRequiredValue(layer);
            if (required > total) {
                this.layerProgress[layer] = 0;
                continue;
            }
            int layersAbove = Math.max(0, activeLayers - layer - 1);
            if (this.moisture() > 0) {
                this.layerProgress[layer] = Math.min(1, this.layerProgress[layer] + increment * (float) (1 + layersAbove * SPEED_PER_LAYER_ABOVE));
            }
            if (this.storedValue + outputValue < required && this.layerProgress[layer] > 0.3f) {
                this.convertInput(required, outputValue);
            }
            if (this.storedValue + outputValue >= required && this.isLayerComplete(layer)) {
                int slot = this.layerOutputSlot[layer];
                if (this.output.getStackInSlot(slot).isEmpty() && !this.currentOutput.isEmpty()) {
                    this.storedValue -= VALUE_PER_OUTPUT;
                    this.output.insertItem(slot, this.currentOutput.copy(), false);
                }
            }
        }
    }

    private void rotateLayerToTop(int layer) {
        float progress = this.layerProgress[layer];
        int slot = this.layerOutputSlot[layer];
        for (int i = layer; i < LAYERS - 1; i++) {
            this.layerProgress[i] = this.layerProgress[i + 1];
            this.layerOutputSlot[i] = this.layerOutputSlot[i + 1];
        }
        this.layerProgress[LAYERS - 1] = 0;
        this.layerOutputSlot[LAYERS - 1] = slot;
        if (progress != 0) {
            this.setChanged();
        }
    }

    private void convertInput(int required, int outputValue) {
        while (this.storedValue + outputValue < required) {
            ItemStack taken = this.input.extractItem(0, 1, false);
            if (taken.isEmpty()) {
                return;
            }
            this.storedValue += this.valueOf(taken);
        }
    }

    private void onChanged() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        if (this.isEmpty()) {
            this.currentOutput = ItemStack.EMPTY;
        } else if (this.currentOutput.isEmpty()) {
            this.currentOutput = this.entryFor(this.input.getStackInSlot(0)).map(entry -> entry.result().copy()).orElse(ItemStack.EMPTY);
        }
        this.updateState();
        this.sync();
    }

    /** The 1.12 {@code getActualState}: ready with output, wet with water, else dry; fill in fifths. */
    private void updateState() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        int state = this.outputCount() > 0 ? CompostBinBlock.READY : this.moisture() > 0 ? CompostBinBlock.WET : CompostBinBlock.DRY;
        int value = Mth.clamp(Mth.ceil(this.totalValue() / (float) MAX_VALUE * 5), 0, 5);
        BlockState current = this.getBlockState();
        if (current.getValue(CompostBinBlock.STATE) != state || current.getValue(CompostBinBlock.COMPOST_VALUE) != value) {
            this.level.setBlock(this.worldPosition,
                current.setValue(CompostBinBlock.STATE, state).setValue(CompostBinBlock.COMPOST_VALUE, value), Block.UPDATE_ALL);
        }
    }

    // -- Breaking ------------------------------------------------------------

    /** The input, every output, and a dirt rock per stored compost value. */
    public void dropContents() {
        if (this.level == null) {
            return;
        }
        Spill.handler(this.level, this.worldPosition, this.input, 0);
        Spill.handler(this.level, this.worldPosition, this.output, 0);
        if (this.storedValue > 0) {
            Spill.onTop(this.level, this.worldPosition, new ItemStack(CoreBlocks.ROCK_DIRT.get().asItem(), this.storedValue));
        }
    }

    // -- Persistence ---------------------------------------------------------

    @Override
    protected void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("input", this.input.serializeNBT(registries));
        tag.put("output", this.output.serializeNBT(registries));
        tag.put("tank", this.tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("storedValue", this.storedValue);
        if (!this.currentOutput.isEmpty()) {
            tag.put("currentOutput", this.currentOutput.save(registries));
        }
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.input.deserializeNBT(registries, tag.getCompound("input"));
        this.output.deserializeNBT(registries, tag.getCompound("output"));
        this.tank.readFromNBT(registries, tag.getCompound("tank"));
        this.storedValue = tag.getInt("storedValue");
        this.currentOutput = tag.contains("currentOutput") ? ItemStack.parseOptional(registries, tag.getCompound("currentOutput")) : ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        int[] progress = new int[LAYERS];
        for (int layer = 0; layer < LAYERS; layer++) {
            progress[layer] = Float.floatToIntBits(this.layerProgress[layer]);
        }
        tag.putIntArray("layerProgress", progress);
        tag.putIntArray("layerOutputSlot", this.layerOutputSlot.clone());
        tag.putDouble("accumulatedEvaporation", this.accumulatedEvaporation);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] progress = tag.getIntArray("layerProgress");
        int[] slots = tag.getIntArray("layerOutputSlot");
        for (int layer = 0; layer < LAYERS; layer++) {
            this.layerProgress[layer] = layer < progress.length ? Float.intBitsToFloat(progress[layer]) : 0;
            this.layerOutputSlot[layer] = layer < slots.length ? slots[layer] : layer;
        }
        this.accumulatedEvaporation = tag.getDouble("accumulatedEvaporation");
    }

    /** The capability view of the tank: everything but a drain. */
    private final class FillOnlyTank implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return CompostBinBlockEntity.this.tank.getFluid();
        }

        @Override
        public int getTankCapacity(int index) {
            return CompostBinBlockEntity.this.tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return CompostBinBlockEntity.this.tank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return CompostBinBlockEntity.this.tank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
