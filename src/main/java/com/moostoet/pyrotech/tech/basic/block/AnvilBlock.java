package com.moostoet.pyrotech.tech.basic.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.block.entity.AnvilBlockEntity;
import com.moostoet.pyrotech.tech.basic.block.entity.HungerGate;
import com.moostoet.pyrotech.tech.basic.block.entity.Spill;
import com.moostoet.pyrotech.tech.basic.recipe.AnvilTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;

/**
 * The three anvils on one class: granite, ironclad, and obsidian differ in their tier,
 * how many hits wear them a stage, how a hot item on them wears them, and what a hit
 * costs the smith. An item on the anvil lights the block as the item's own block would,
 * and a hot item burns whoever walks over it.
 */
public final class AnvilBlock extends BaseEntityBlock {

    public static final MapCodec<AnvilBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        AnvilTier.CODEC.fieldOf("tier").forGetter(AnvilBlock::tier),
        propertiesCodec()
    ).apply(instance, (tier, properties) -> new AnvilBlock(tier, properties)));

    public static final int MAX_DAMAGE = 3;
    public static final IntegerProperty DAMAGE = IntegerProperty.create("damage", 0, MAX_DAMAGE);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    private final AnvilTier tier;
    private final int hitsPerDamage;
    private final int hotItemExtraDamagePerHit;
    private final double hotItemExtraDamageChance;
    private final float exhaustionPerHit;

    public AnvilBlock(AnvilTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        switch (tier) {
            case GRANITE -> {
                this.hitsPerDamage = 64;
                this.hotItemExtraDamagePerHit = 1;
                this.hotItemExtraDamageChance = 0.5;
                this.exhaustionPerHit = 0.5f;
            }
            case IRONCLAD -> {
                this.hitsPerDamage = 256;
                this.hotItemExtraDamagePerHit = 1;
                this.hotItemExtraDamageChance = 0.05;
                this.exhaustionPerHit = 0.5f;
            }
            default -> {
                this.hitsPerDamage = 2048;
                this.hotItemExtraDamagePerHit = 0;
                this.hotItemExtraDamageChance = 0;
                this.exhaustionPerHit = 0.25f;
            }
        }
        this.registerDefaultState(this.stateDefinition.any().setValue(DAMAGE, 0));
    }

    public AnvilTier tier() {
        return this.tier;
    }

    public int hitsPerDamage() {
        return this.hitsPerDamage;
    }

    /** The extra wear a hot item's hit may add, for bloomery's recipe. */
    public int hotItemExtraDamagePerHit() {
        return this.hotItemExtraDamagePerHit;
    }

    public double hotItemExtraDamageChance() {
        return this.hotItemExtraDamageChance;
    }

    public float exhaustionPerHit() {
        return this.exhaustionPerHit;
    }

    public ModConfigSpec.BooleanValue usesDurability() {
        return switch (this.tier) {
            case GRANITE -> TechBasicConfig.COMMON.graniteAnvilUseDurability;
            case IRONCLAD -> TechBasicConfig.COMMON.ironcladAnvilUseDurability;
            case OBSIDIAN -> TechBasicConfig.COMMON.obsidianAnvilUseDurability;
        };
    }

    @Override
    protected MapCodec<AnvilBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DAMAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // -- The item on the anvil -----------------------------------------------

    @Override
    public boolean hasDynamicLightEmission(BlockState state) {
        return true;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        AuxiliaryLightManager lights = level.getAuxLightManager(pos);
        return lights == null ? 0 : lights.getLightAt(pos);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil
            && anvil.input().getStackInSlot(0).getItem() instanceof AnvilHotItem hot && hot.walkDamage() > 0
            && !entity.fireImmune() && entity instanceof LivingEntity && !entity.isSteppingCarefully()) {
            entity.hurt(level.damageSources().hotFloor(), hot.walkDamage());
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil)
            || !(anvil.input().getStackInSlot(0).getItem() instanceof AnvilHotItem)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 10 / 16.0 + random.nextDouble() * 2 / 16.0;
        double z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, pos.getY(), z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1, 1, false);
        }
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.FLAME,
                x + (random.nextDouble() * 2 - 1) * 0.2, y + (random.nextDouble() * 2 - 1) * 0.2, z + (random.nextDouble() * 2 - 1) * 0.2,
                0, 0, 0);
        }
    }

    // -- Interaction ---------------------------------------------------------

    /** A tool with a recipe for the item hits it; an item with a recipe goes on; anything else passes, so tongs can work. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP || !(level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (anvil.canHitWith(stack)) {
            if (HungerGate.blocks(player)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                for (ItemStack output : anvil.hit(stack, player, hit.getLocation())) {
                    Spill.onTop(level, pos, output);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (anvil.insert(player, stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (hit.getDirection() == Direction.UP && level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil && anvil.extract(player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return WearStage.with(super.getCloneItemStack(level, pos, state), DAMAGE, state.getValue(DAMAGE));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil) {
            anvil.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnvilBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
