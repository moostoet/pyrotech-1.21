package com.moostoet.pyrotech.storage.block.entity;

import com.moostoet.pyrotech.core.CombustParticles;
import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.fluid.HotFluidTank;
import com.moostoet.pyrotech.storage.StorageBlockEntities;
import com.moostoet.pyrotech.storage.StorageComponents;
import com.moostoet.pyrotech.storage.block.TankBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * A tank in a tank group: its own {@link HotFluidTank}, and a handler over the whole
 * column that fills from the bottom up, drains from the top down, and refuses a second
 * fluid. The joins are the block's {@code connection} property; the group is walked from
 * it whenever it is needed. Fluid settles downward after every neighbour change.
 */
public final class TankBlockEntity extends SyncedBlockEntity {

    private final HotFluidTank tank;
    private final IFluidHandler groupHandler = new GroupHandler();

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(StorageBlockEntities.TANK.get(), pos, state);
        TankBlock block = (TankBlock) state.getBlock();
        this.tank = new HotFluidTank(block.capacity(), block.holdsHotFluids(), this::onHotFluid) {
            @Override
            protected void onContentsChanged() {
                TankBlockEntity.this.onFluidChanged();
            }
        };
    }

    public HotFluidTank tank() {
        return this.tank;
    }

    public IFluidHandler groupHandler() {
        return this.groupHandler;
    }

    public TankBlock.Connection connection() {
        return this.getBlockState().getValue(TankBlock.CONNECTION);
    }

    private Block tankBlock() {
        return this.getBlockState().getBlock();
    }

    // -- The group -----------------------------------------------------------

    /** The column this tank belongs to, lowest first. */
    public List<TankBlockEntity> group() {
        TankBlockEntity lowest = this;
        while (lowest.connection().down() && lowest.neighbour(Direction.DOWN) instanceof TankBlockEntity below) {
            lowest = below;
        }
        List<TankBlockEntity> group = new ArrayList<>();
        group.add(lowest);
        TankBlockEntity current = lowest;
        while (current.connection().up() && current.neighbour(Direction.UP) instanceof TankBlockEntity above) {
            group.add(above);
            current = above;
        }
        return group;
    }

    /** The tank of the same kind beside this one, or null. */
    private TankBlockEntity neighbour(Direction direction) {
        if (this.level == null) {
            return null;
        }
        BlockPos pos = this.worldPosition.relative(direction);
        return this.level.getBlockState(pos).is(this.tankBlock()) && this.level.getBlockEntity(pos) instanceof TankBlockEntity tank
            ? tank : null;
    }

    public int groupAmount(List<TankBlockEntity> group) {
        int amount = 0;
        for (TankBlockEntity tank : group) {
            amount += tank.tank.getFluidAmount();
        }
        return amount;
    }

    public int groupCapacity(List<TankBlockEntity> group) {
        return group.size() * this.tank.getCapacity();
    }

    // -- Connections ---------------------------------------------------------

    /**
     * The 1.12 placement rule: a tank joins the tank below before the tank above, or the
     * other way round when placed against an underside, and only where the fluids agree.
     */
    public void updateConnectionsForPlacement(Direction clickedFace) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        FluidStack mine = this.tank.getFluid();
        FluidStack below = this.nearestFluidBelow();
        FluidStack above = this.fluidAbove();
        boolean upFirst = clickedFace == Direction.DOWN;
        boolean joinUp = mine.isEmpty() ? upFirst || below.isEmpty() || same(below, above) : above.isEmpty() || same(mine, above);
        boolean joinDown = mine.isEmpty() ? !upFirst || below.isEmpty() || same(below, above) : below.isEmpty() || same(mine, below);
        if (upFirst) {
            if (joinUp) {
                this.connect(Direction.UP);
            }
            if (joinDown) {
                this.connect(Direction.DOWN);
            }
        } else {
            if (joinDown) {
                this.connect(Direction.DOWN);
            }
            if (joinUp) {
                this.connect(Direction.UP);
            }
        }
    }

    private static boolean same(FluidStack a, FluidStack b) {
        return !a.isEmpty() && FluidStack.isSameFluidSameComponents(a, b);
    }

    private void connect(Direction direction) {
        TankBlockEntity other = this.neighbour(direction);
        if (other == null) {
            return;
        }
        boolean up = direction == Direction.UP;
        this.setConnection(up ? this.connection().withUp(true) : this.connection().withDown(true));
        other.setConnection(up ? other.connection().withDown(true) : other.connection().withUp(true));
    }

    private void setConnection(TankBlock.Connection connection) {
        BlockState state = this.getBlockState();
        if (this.level != null && state.getValue(TankBlock.CONNECTION) != connection) {
            this.level.setBlock(this.worldPosition, state.setValue(TankBlock.CONNECTION, connection), Block.UPDATE_ALL);
        }
    }

    private FluidStack fluidAbove() {
        return this.level != null && this.level.getBlockEntity(this.worldPosition.above()) instanceof TankBlockEntity above
            ? above.tank.getFluid() : FluidStack.EMPTY;
    }

    /** The first fluid down the column of joined tanks below, or empty. */
    private FluidStack nearestFluidBelow() {
        if (this.level == null) {
            return FluidStack.EMPTY;
        }
        for (BlockPos pos = this.worldPosition.below(); this.level.getBlockEntity(pos) instanceof TankBlockEntity tank; pos = pos.below()) {
            if (!tank.tank.isEmpty()) {
                return tank.tank.getFluid();
            }
            if (!tank.connection().down()) {
                break;
            }
        }
        return FluidStack.EMPTY;
    }

    /** A neighbour changed: drop a join whose tank is gone, then let the fluid settle. */
    public void onNeighborChanged() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        TankBlock.Connection connection = this.connection();
        if (!(this.level.getBlockEntity(this.worldPosition.above()) instanceof TankBlockEntity)) {
            connection = connection.withUp(false);
        }
        if (!(this.level.getBlockEntity(this.worldPosition.below()) instanceof TankBlockEntity)) {
            connection = connection.withDown(false);
        }
        this.setConnection(connection);
        this.settleFluids();
    }

    /** Fluid in the upper tanks moves into the lower ones until they are full. */
    public void settleFluids() {
        List<TankBlockEntity> group = this.group();
        for (int drainIndex = group.size() - 1; drainIndex > 0; drainIndex--) {
            HotFluidTank toDrain = group.get(drainIndex).tank;
            int amount = toDrain.getFluidAmount();
            if (amount == 0) {
                continue;
            }
            for (int fillIndex = 0; fillIndex < drainIndex; fillIndex++) {
                HotFluidTank toFill = group.get(fillIndex).tank;
                if (toFill.getFluidAmount() == toFill.getCapacity()) {
                    continue;
                }
                int filled = toFill.fill(toDrain.drain(amount, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.SIMULATE);
                if (filled > 0) {
                    toFill.fill(toDrain.drain(filled, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    // -- Fluid changes -------------------------------------------------------

    private void onFluidChanged() {
        if (this.level == null || this.isRemoved()) {
            return;
        }
        this.sync();
        this.updateLight();
    }

    /** The fluid's light into the chunk's light manager, which relights the block on both sides. */
    private void updateLight() {
        AuxiliaryLightManager lights = this.level == null ? null : this.level.getAuxLightManager(this.worldPosition);
        if (lights != null) {
            FluidStack fluid = this.tank.getFluid();
            lights.setLightAt(this.worldPosition, fluid.isEmpty() ? 0 : fluid.getFluidType().getLightLevel(fluid));
        }
    }

    /** A hot fluid in a stone tank: the tank breaks, spills what it held, and smokes. */
    private void onHotFluid(FluidStack resource) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = this.worldPosition;
        serverLevel.removeBlock(pos, false);
        serverLevel.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1, 1);
        FluidUtil.tryPlaceFluid(null, serverLevel, InteractionHand.MAIN_HAND, pos, this.tank, resource);
        CombustParticles.spawn(serverLevel, pos, 0.5);
    }

    @Override
    protected void onSyncedDataUpdate() {
        this.updateLight();
    }

    // -- The item round trip -------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        this.tank.setFluid(input.getOrDefault(StorageComponents.TANK_FLUID, SimpleFluidContent.EMPTY).copy());
        this.onFluidChanged();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!this.tank.isEmpty()) {
            components.set(StorageComponents.TANK_FLUID, SimpleFluidContent.copyOf(this.tank.getFluid()));
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("tank");
    }

    @Override
    protected void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("tank", this.tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.tank.readFromNBT(registries, tag.getCompound("tank"));
    }

    /** The 1.12 {@code FluidHandler}: one tank the size of the group, bottom up in and top down out. */
    private final class GroupHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int slot) {
            List<TankBlockEntity> group = TankBlockEntity.this.group();
            FluidStack lowest = group.get(0).tank.getFluid();
            return lowest.isEmpty() ? FluidStack.EMPTY : lowest.copyWithAmount(TankBlockEntity.this.groupAmount(group));
        }

        @Override
        public int getTankCapacity(int slot) {
            return TankBlockEntity.this.groupCapacity(TankBlockEntity.this.group());
        }

        @Override
        public boolean isFluidValid(int slot, FluidStack stack) {
            FluidStack lowest = TankBlockEntity.this.group().get(0).tank.getFluid();
            return lowest.isEmpty() || FluidStack.isSameFluidSameComponents(lowest, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            List<TankBlockEntity> group = TankBlockEntity.this.group();
            FluidStack lowest = group.get(0).tank.getFluid();
            if (!lowest.isEmpty() && !FluidStack.isSameFluidSameComponents(lowest, resource)) {
                return 0;
            }
            int remaining = resource.getAmount();
            for (TankBlockEntity tank : group) {
                remaining -= tank.tank.fill(resource.copyWithAmount(remaining), action);
                if (remaining <= 0) {
                    break;
                }
            }
            return resource.getAmount() - remaining;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            List<TankBlockEntity> group = TankBlockEntity.this.group();
            FluidStack result = FluidStack.EMPTY;
            int remaining = resource.getAmount();
            for (int i = group.size() - 1; i >= 0 && remaining > 0; i--) {
                FluidStack drained = group.get(i).tank.drain(resource.copyWithAmount(remaining), action);
                result = add(result, drained);
                remaining -= drained.getAmount();
            }
            return result;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            List<TankBlockEntity> group = TankBlockEntity.this.group();
            FluidStack result = FluidStack.EMPTY;
            int remaining = maxDrain;
            for (int i = group.size() - 1; i >= 0 && remaining > 0; i--) {
                FluidStack drained = group.get(i).tank.drain(remaining, action);
                result = add(result, drained);
                remaining -= drained.getAmount();
            }
            return result;
        }

        private static FluidStack add(FluidStack total, FluidStack drained) {
            if (drained.isEmpty()) {
                return total;
            }
            return total.isEmpty() ? drained : total.copyWithAmount(total.getAmount() + drained.getAmount());
        }
    }
}
