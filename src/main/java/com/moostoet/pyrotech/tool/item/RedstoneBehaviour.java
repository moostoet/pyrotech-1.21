package com.moostoet.pyrotech.tool.item;

import com.moostoet.pyrotech.core.CoreSounds;
import com.moostoet.pyrotech.core.block.DenseRedstoneOreBlock;
import com.moostoet.pyrotech.tool.ToolComponents;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A redstone tool activates by chance when it takes damage, or when its holder stands near
 * redstone ore, and stays active for ten seconds of game time. While active it mines twice
 * as fast, usually skips damage, and the sword hits twice as hard. Standing near redstone
 * ore also lights the ore and may repair the tool. The chances and scalars are the 1.12
 * config defaults, baked in (tool sign-off, item 2).
 */
public final class RedstoneBehaviour implements ActiveBehaviour {

    public static final RedstoneBehaviour INSTANCE = new RedstoneBehaviour();

    static final int ACTIVE_DURATION_TICKS = 200;
    static final float ACTIVE_DAMAGE_CHANCE = 0.125f;
    static final float INACTIVE_ACTIVATION_CHANCE = 0.05f;
    static final float ACTIVE_ACTIVATION_CHANCE = 0.25f;
    static final float SPEED_SCALAR = 2.0f;
    static final float SWORD_DAMAGE_SCALAR = 2.0f;
    static final float PROXIMITY_REPAIR_CHANCE = 0.125f;
    private static final int PROXIMITY_RANGE = 4;

    private RedstoneBehaviour() {
    }

    /** Active until the inventory tick clears the expiry, so no caller needs a level to ask. */
    @Override
    public boolean isActive(ItemStack stack) {
        return stack.has(ToolComponents.REDSTONE_ACTIVE_UNTIL.get());
    }

    @Override
    public float speedScalar() {
        return SPEED_SCALAR;
    }

    @Override
    public float swordDamageScalar() {
        return SWORD_DAMAGE_SCALAR;
    }

    /** Starts, or restarts, the ten seconds. */
    public void activate(ItemStack stack, Level level, @Nullable Entity holder) {
        if (!this.isActive(stack) && holder != null) {
            float pitch = 1.0f + (level.random.nextFloat() * 2 - 1) * 0.05f;
            level.playSound(null, holder.blockPosition(), randomActivationSound(level.random), SoundSource.PLAYERS, 1.0f, pitch);
        }
        stack.set(ToolComponents.REDSTONE_ACTIVE_UNTIL.get(), level.getGameTime() + ACTIVE_DURATION_TICKS);
        ActiveTools.notifyChanged(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder) {
        if (level.isClientSide) {
            return;
        }
        Long until = stack.get(ToolComponents.REDSTONE_ACTIVE_UNTIL.get());
        if (until != null && level.getGameTime() >= until) {
            stack.remove(ToolComponents.REDSTONE_ACTIVE_UNTIL.get());
            ActiveTools.notifyChanged(stack);
        }
        if (holder instanceof Player player && level.getGameTime() % 20 == 0 && player.getMainHandItem() == stack) {
            this.activateNearbyRedstoneOre(stack, level, player);
        }
    }

    /**
     * The 1.12 shuffled cube scan: every dense redstone ore found lights up and every
     * vanilla one turns lit, each with a chance to end the scan there.
     */
    private void activateNearbyRedstoneOre(ItemStack stack, Level level, Player player) {
        BlockPos origin = player.blockPosition();
        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-PROXIMITY_RANGE, -PROXIMITY_RANGE, -PROXIMITY_RANGE),
            origin.offset(PROXIMITY_RANGE, PROXIMITY_RANGE, PROXIMITY_RANGE))) {
            positions.add(pos.immutable());
        }
        Util.shuffle(positions, level.random);
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DenseRedstoneOreBlock ore) {
                ore.activate(state, level, pos);
                this.activateAndRepair(stack, level, player, ore.getProximityRepairAmount());
                if (level.random.nextFloat() >= 0.32f) {
                    return;
                }
            } else if (state.getBlock() instanceof RedStoneOreBlock && !state.getValue(RedStoneOreBlock.LIT)) {
                level.setBlock(pos, state.setValue(RedStoneOreBlock.LIT, true), Block.UPDATE_ALL);
                level.playSound(null, pos, CoreSounds.randomDenseRedstoneOreActivate(level.random), SoundSource.BLOCKS, 1.0f, 1.0f);
                this.activateAndRepair(stack, level, player, 1);
                if (level.random.nextFloat() >= 0.16f) {
                    return;
                }
            }
        }
    }

    private void activateAndRepair(ItemStack stack, Level level, Player player, int amount) {
        if (!this.isActive(stack)) {
            this.activate(stack, level, player);
        }
        if (level.random.nextFloat() < PROXIMITY_REPAIR_CHANCE) {
            stack.setDamageValue(stack.getDamageValue() - amount);
        }
    }

    /**
     * The 1.12 {@code setDamage} chain: an active tool usually ignores the damage, and any
     * damage may start or extend the ten seconds.
     */
    @Override
    public int damageItem(ItemStack stack, int amount, @Nullable LivingEntity holder) {
        if (holder == null) {
            return amount;
        }
        RandomSource random = holder.getRandom();
        boolean active = this.isActive(stack);
        boolean damaged = amount > 0;
        int applied = active && damaged && random.nextFloat() >= ACTIVE_DAMAGE_CHANCE ? 0 : amount;
        if (active || damaged) {
            float chance = active ? ACTIVE_ACTIVATION_CHANCE : INACTIVE_ACTIVATION_CHANCE;
            if (random.nextFloat() < chance) {
                this.activate(stack, holder.level(), holder);
            }
        }
        return applied;
    }

    @Override
    public void appendTooltip(List<Component> tooltip, ToolKind kind) {
        tooltip.add(ActiveTools.line("gui.pyrotech.tooltip.redstone.activation.chance",
            ActiveTools.HIGHLIGHT, (int) (INACTIVE_ACTIVATION_CHANCE * 100), ActiveTools.INFO));
        tooltip.add(ActiveTools.line("gui.pyrotech.tooltip.redstone.active.durability",
            ActiveTools.HIGHLIGHT, (int) ((1 - ACTIVE_DAMAGE_CHANCE) * 100), ActiveTools.INFO));
        tooltip.add(switch (kind) {
            case DIGGER -> ActiveTools.line("gui.pyrotech.tooltip.redstone.active.efficiency",
                ActiveTools.HIGHLIGHT, (int) (SPEED_SCALAR * 100), ActiveTools.INFO);
            case SWORD -> ActiveTools.line("gui.pyrotech.tooltip.redstone.active.damage",
                ActiveTools.HIGHLIGHT, (int) (SWORD_DAMAGE_SCALAR * 100), ActiveTools.INFO);
            case HOE -> ActiveTools.line("gui.pyrotech.tooltip.redstone.active.hoe", ActiveTools.HIGHLIGHT, ActiveTools.INFO);
        });
    }

    private static net.minecraft.sounds.SoundEvent randomActivationSound(RandomSource random) {
        return CoreSounds.REDSTONE_TOOL_ACTIVATE.get(random.nextInt(CoreSounds.REDSTONE_TOOL_ACTIVATE.size())).get();
    }
}
