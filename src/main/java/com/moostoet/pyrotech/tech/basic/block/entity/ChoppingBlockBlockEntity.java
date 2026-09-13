package com.moostoet.pyrotech.tech.basic.block.entity;

import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreConfig;
import com.moostoet.pyrotech.core.ToolLevels;
import com.moostoet.pyrotech.core.network.ProgressParticlesPayload;
import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.block.ChoppingBlockBlock;
import com.moostoet.pyrotech.tech.basic.recipe.ChoppingBlockRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.TechBasicRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The chopping block's one log, its chop count, and its wear. */
public final class ChoppingBlockBlockEntity extends SyncedBlockEntity {

    public static final int MAX_SAWDUST = 5;
    private static final int CHOPS_PER_DAMAGE = 16;
    private static final double WOOD_CHIPS_CHANCE = 0.05;
    private static final float EXHAUSTION_PER_CHOP = 1.5f;
    private static final float EXHAUSTION_PER_SCOOP = 0.5f;

    private final ItemStackHandler input = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ChoppingBlockBlockEntity.this.recipeFor(stack).isPresent();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return this.isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            ChoppingBlockBlockEntity.this.progress = 0;
            ChoppingBlockBlockEntity.this.sync();
        }
    };

    private float progress;
    private int chopsUntilNextDamage = CHOPS_PER_DAMAGE;

    public ChoppingBlockBlockEntity(BlockPos pos, BlockState state) {
        super(TechBasicBlockEntities.CHOPPING_BLOCK.get(), pos, state);
    }

    public ItemStackHandler input() {
        return this.input;
    }

    public float progress() {
        return this.progress;
    }

    public int sawdust() {
        return this.getBlockState().getValue(ChoppingBlockBlock.SAWDUST);
    }

    public int damage() {
        return this.getBlockState().getValue(ChoppingBlockBlock.DAMAGE);
    }

    private Optional<ChoppingBlockRecipe> recipeFor(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(TechBasicRecipeTypes.CHOPPING_BLOCK.get(), new SingleRecipeInput(stack), this.level)
            .map(holder -> holder.value());
    }

    private void setSawdust(int sawdust) {
        if (this.level != null) {
            this.level.setBlock(this.worldPosition, this.getBlockState().setValue(ChoppingBlockBlock.SAWDUST, Mth.clamp(sawdust, 0, MAX_SAWDUST)),
                Block.UPDATE_ALL);
        }
    }

    // -- Interaction ---------------------------------------------------------

    public boolean insert(Player player, ItemStack held) {
        if (this.level == null || !SlotInteraction.insert(this.input, 0, player, held, 1)) {
            return false;
        }
        if (!this.level.isClientSide) {
            this.level.playSound(null, this.worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1, 1);
        }
        return true;
    }

    public boolean extract(Player player) {
        return SlotInteraction.extract(this.input, 0, player, this.worldPosition);
    }

    /**
     * Chips off the block: a shovel takes them as a wood chips rock and wears one; an
     * empty hand takes them too unless core's tweak wants the shovel, in which case it
     * only sweeps them off.
     */
    public boolean scoopSawdust(Player player, ItemStack tool, InteractionHand hand) {
        if (this.level == null || this.sawdust() <= 0) {
            return false;
        }
        if (!this.level.isClientSide) {
            boolean shovel = !tool.isEmpty();
            ItemStack chips = new ItemStack(CoreBlocks.ROCK_WOOD_CHIPS.get().asItem());
            if (shovel) {
                tool.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                SlotInteraction.giveOrDrop(player, chips, this.worldPosition, true);
            } else if (!CoreConfig.COMMON.requireShovelToPickupWoodChips.get()) {
                SlotInteraction.giveOrDrop(player, chips, this.worldPosition, true);
            }
            this.setSawdust(this.sawdust() - 1);
            this.level.playSound(null, this.worldPosition, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1, 1);
            player.causeFoodExhaustion(EXHAUSTION_PER_SCOOP);
        }
        return true;
    }

    /** One axe chop, as the 1.12 {@code InteractionChop.doInteraction} had it. */
    public boolean chop(Player player, ItemStack axe, InteractionHand hand, BlockHitResult hit) {
        if (this.level == null || this.input.getStackInSlot(0).isEmpty() || HungerGate.blocks(player)) {
            return false;
        }
        if (this.level.isClientSide) {
            Vec3 at = hit.getLocation();
            BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, this.getBlockState());
            for (int i = 0; i < 8; i++) {
                this.level.addParticle(particle, at.x, at.y, at.z, 0, 0, 0);
            }
            return true;
        }
        ServerLevel level = (ServerLevel) this.level;
        BlockPos pos = this.worldPosition;
        player.causeFoodExhaustion(EXHAUSTION_PER_CHOP);
        boolean usesDurability = TechBasicConfig.COMMON.choppingBlockUsesDurability.get();
        if (usesDurability && this.chopsUntilNextDamage <= 1) {
            this.chopsUntilNextDamage = CHOPS_PER_DAMAGE;
            int damage = this.damage();
            if (damage + 1 <= ChoppingBlockBlock.MAX_DAMAGE) {
                level.setBlock(pos, this.getBlockState().setValue(ChoppingBlockBlock.DAMAGE, damage + 1), Block.UPDATE_ALL);
            } else {
                Spill.handler(level, pos, this.input, 0);
                Spill.onTop(level, pos, new ItemStack(CoreBlocks.ROCK_WOOD_CHIPS.get().asItem(), this.sawdust()));
                level.destroyBlock(pos, false);
                return true;
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.75f,
            (float) (1 + level.random.nextGaussian() * 0.4));
        this.scatterChips(level);
        if (usesDurability) {
            this.chopsUntilNextDamage--;
        }
        int toolLevel = ToolLevels.of(axe);
        ItemStack stack = this.input.getStackInSlot(0);
        Optional<ChoppingBlockRecipe> recipe = this.recipeFor(stack);
        if (recipe.isPresent()) {
            ProgressParticlesPayload.send(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 2);
            if (this.progress < 1) {
                this.progress += 1f / recipe.get().chops(toolLevel);
            }
            if (this.progress >= 0.9999f) {
                this.input.extractItem(0, 1, false);
                ItemStack output = recipe.get().result().copyWithCount(recipe.get().quantity(toolLevel));
                Spill.onTop(level, pos, output);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1,
                    (float) (1 + level.random.nextGaussian() * 0.4));
            }
        }
        this.setChanged();
        return true;
    }

    /** Twice the chance to add chips on the block, half the chance to drop a chips rock beside it. */
    private void scatterChips(ServerLevel level) {
        if (this.sawdust() < MAX_SAWDUST && level.random.nextDouble() < WOOD_CHIPS_CHANCE * 2) {
            this.setSawdust(this.sawdust() + 1);
        }
        if (level.random.nextDouble() >= WOOD_CHIPS_CHANCE * 0.5) {
            return;
        }
        BlockState rock = CoreBlocks.ROCK_WOOD_CHIPS.get().defaultBlockState();
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(this.worldPosition.offset(-1, -1, -1), this.worldPosition.offset(1, 1, 1))) {
            if (level.isEmptyBlock(pos) && rock.canSurvive(level, pos)) {
                candidates.add(pos.immutable());
            }
        }
        if (!candidates.isEmpty()) {
            level.setBlock(candidates.get(level.random.nextInt(candidates.size())), rock, Block.UPDATE_ALL);
        }
    }

    public void dropContents() {
        if (this.level != null) {
            Spill.handler(this.level, this.worldPosition, this.input, 0);
        }
    }

    // -- Persistence ---------------------------------------------------------

    @Override
    protected void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("input", this.input.serializeNBT(registries));
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.input.deserializeNBT(registries, tag.getCompound("input"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("progress", this.progress);
        tag.putInt("chopsUntilNextDamage", this.chopsUntilNextDamage);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getFloat("progress");
        this.chopsUntilNextDamage = tag.getInt("chopsUntilNextDamage");
    }
}
