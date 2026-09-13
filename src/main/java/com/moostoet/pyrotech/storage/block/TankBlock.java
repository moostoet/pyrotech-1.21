package com.moostoet.pyrotech.storage.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.moostoet.pyrotech.storage.block.entity.TankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * The stone and refractory tanks: {@code capacity} millibuckets each, stacking into a tank
 * group that shares one fluid, with the group's joins as the {@code connection} property. A
 * stone tank breaks on a hot fluid; the brick one holds it. Any fluid container clicked on
 * a face fills or drains the group.
 */
public final class TankBlock extends BaseEntityBlock {

    public static final MapCodec<TankBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.fieldOf("capacity").forGetter(TankBlock::capacity),
        Codec.BOOL.fieldOf("holds_hot_fluids").forGetter(TankBlock::holdsHotFluids),
        propertiesCodec()
    ).apply(instance, TankBlock::new));

    public static final EnumProperty<Connection> CONNECTION = EnumProperty.create("connection", Connection.class);

    private final int capacity;
    private final boolean holdsHotFluids;

    public TankBlock(int capacity, boolean holdsHotFluids, Properties properties) {
        super(properties);
        this.capacity = capacity;
        this.holdsHotFluids = holdsHotFluids;
        this.registerDefaultState(this.stateDefinition.any().setValue(CONNECTION, Connection.NONE));
    }

    public int capacity() {
        return this.capacity;
    }

    public boolean holdsHotFluids() {
        return this.holdsHotFluids;
    }

    @Override
    protected MapCodec<TankBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTION);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TankBlockEntity tank) {
            tank.onNeighborChanged();
        }
    }

    /** The held fluid's own light, as the 1.12 {@code getLightValue} read the fluid's luminosity. */
    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TankBlockEntity tank) {
            FluidStack fluid = tank.tank().getFluid();
            if (!fluid.isEmpty()) {
                return fluid.getFluidType().getLightLevel(fluid);
            }
        }
        return super.getLightEmission(state, level, pos);
    }

    /** A fluid container on any face meets the group handler (storage sign-off, item 8). */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || FluidUtil.getFluidHandler(stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection());
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Pick-block hands out the tank with its fluid, as the loot table does. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof TankBlockEntity tank) {
            stack.applyComponents(tank.collectComponents());
        }
        return stack;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** The joins to the tanks above and below: the 1.12 pair of synced booleans as one property. */
    public enum Connection implements StringRepresentable {
        NONE("none", false, false),
        UP("up", true, false),
        DOWN("down", false, true),
        BOTH("both", true, true);

        private final String name;
        private final boolean up;
        private final boolean down;

        Connection(String name, boolean up, boolean down) {
            this.name = name;
            this.up = up;
            this.down = down;
        }

        public static Connection of(boolean up, boolean down) {
            if (up) {
                return down ? BOTH : UP;
            }
            return down ? DOWN : NONE;
        }

        public boolean up() {
            return this.up;
        }

        public boolean down() {
            return this.down;
        }

        public Connection withUp(boolean up) {
            return of(up, this.down);
        }

        public Connection withDown(boolean down) {
            return of(this.up, down);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
