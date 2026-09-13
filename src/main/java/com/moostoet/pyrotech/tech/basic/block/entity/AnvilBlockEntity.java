package com.moostoet.pyrotech.tech.basic.block.entity;

import com.moostoet.pyrotech.core.ToolLevels;
import com.moostoet.pyrotech.core.network.ProgressParticlesPayload;
import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.block.AnvilBlock;
import com.moostoet.pyrotech.tech.basic.network.AnvilHitPayload;
import com.moostoet.pyrotech.tech.basic.recipe.AnvilRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.AnvilTier;
import com.moostoet.pyrotech.tech.basic.recipe.AnvilToolType;
import com.moostoet.pyrotech.tech.basic.recipe.ExtendedAnvilRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The anvil's one item, its recipe, and its wear. {@link #hit} is public because the
 * machine module's trip hammer strikes it with no player.
 */
public final class AnvilBlockEntity extends SyncedBlockEntity {

    private final ItemStackHandler input = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return AnvilBlockEntity.this.level != null && AnvilRecipe.hasRecipe(AnvilBlockEntity.this.level, stack, AnvilBlockEntity.this.tier());
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return this.isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            AnvilBlockEntity.this.progress = 0;
            AnvilBlockEntity.this.recipe = null;
            AnvilBlockEntity.this.sync();
            AnvilBlockEntity.this.updateLight();
        }
    };

    private float progress;
    private int durabilityUntilNextDamage;
    @Nullable
    private RecipeHolder<AnvilRecipe> recipe;

    public AnvilBlockEntity(BlockPos pos, BlockState state) {
        super(TechBasicBlockEntities.ANVIL.get(), pos, state);
        this.durabilityUntilNextDamage = this.block().hitsPerDamage();
    }

    public AnvilBlock block() {
        return (AnvilBlock) this.getBlockState().getBlock();
    }

    public AnvilTier tier() {
        return this.block().tier();
    }

    public ItemStackHandler input() {
        return this.input;
    }

    public float progress() {
        return this.progress;
    }

    public int damage() {
        return this.getBlockState().getValue(AnvilBlock.DAMAGE);
    }

    public boolean usesDurability() {
        return this.block().usesDurability().get();
    }

    public int durabilityUntilNextDamage() {
        return this.durabilityUntilNextDamage;
    }

    public void setDurabilityUntilNextDamage(int durability) {
        this.durabilityUntilNextDamage = durability;
        this.setChanged();
    }

    // -- Interaction ---------------------------------------------------------

    public boolean insert(Player player, ItemStack held) {
        if (this.level == null || !SlotInteraction.insert(this.input, 0, player, held, 1)) {
            return false;
        }
        if (!this.level.isClientSide) {
            this.level.playSound(null, this.worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.5f,
                (float) (1 + this.level.random.nextGaussian() * 0.4));
        }
        return true;
    }

    public boolean extract(Player player) {
        return SlotInteraction.extract(this.input, 0, player, this.worldPosition);
    }

    /** True when the held item is a hammer or pickaxe with a recipe for the item on the anvil. */
    public boolean canHitWith(ItemStack tool) {
        AnvilToolType type = AnvilToolType.of(tool);
        return type != null && this.level != null
            && AnvilRecipe.find(this.level, this.input.getStackInSlot(0), this.tier(), type).isPresent();
    }

    /**
     * One strike, as the 1.12 {@code doInteraction} had it: wear, the hit sound, progress by
     * the recipe's hits less the tool's level, and the outputs on completion. Server only.
     */
    public List<ItemStack> hit(ItemStack tool, @Nullable Player player, Vec3 hitVec) {
        if (!(this.level instanceof ServerLevel level) || this.input.getStackInSlot(0).isEmpty() || tool.isEmpty()) {
            return List.of();
        }
        List<ItemStack> result = new ArrayList<>();
        ItemStack stack = this.input.getStackInSlot(0);
        AnvilToolType type = AnvilToolType.of(tool);
        RecipeHolder<AnvilRecipe> matched = type == null ? null : AnvilRecipe.find(level, stack, this.tier(), type).orElse(null);
        if (matched == null || this.recipe == null || !matched.id().equals(this.recipe.id())) {
            this.progress = 0;
            this.recipe = matched;
        }
        AnvilRecipe recipe = matched == null ? null : matched.value();
        ExtendedAnvilRecipe extended = recipe instanceof ExtendedAnvilRecipe e ? e : null;
        if (player != null && this.block().exhaustionPerHit() > 0) {
            player.causeFoodExhaustion(this.block().exhaustionPerHit());
        }
        if (this.usesDurability() && this.durabilityUntilNextDamage <= 1) {
            this.durabilityUntilNextDamage = this.block().hitsPerDamage();
            int damage = this.damage();
            if (damage + 1 <= AnvilBlock.MAX_DAMAGE) {
                level.setBlock(this.worldPosition, this.getBlockState().setValue(AnvilBlock.DAMAGE, damage + 1), Block.UPDATE_ALL);
            } else {
                if (extended != null) {
                    extended.onAnvilDurabilityExpired(level, this, hitVec);
                } else {
                    Spill.handler(level, this.worldPosition, this.input, 0);
                    level.destroyBlock(this.worldPosition, false);
                }
                return List.of();
            }
        }
        level.playSound(null, this.worldPosition, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.75f,
            (float) (1 + level.random.nextGaussian() * 0.4));
        if (recipe == null) {
            this.setChanged();
            return result;
        }
        if (extended != null) {
            extended.applyDamage(level, this);
        } else if (this.usesDurability()) {
            this.durabilityUntilNextDamage--;
        }
        if (this.progress < 1) {
            int hitReduction = type == AnvilToolType.PICKAXE
                ? ToolLevels.of(tool)
                : TechBasicConfig.COMMON.hitReduction(ToolLevels.of(tool));
            int hits = Math.max(1, recipe.hits() - hitReduction);
            float increment = 1f / hits;
            if (extended != null) {
                Vec3 hammerPos = player != null
                    ? new Vec3(player.getX(), player.getY() + player.getEyeHeight() * 0.5, player.getZ())
                    : Vec3.atLowerCornerOf(this.worldPosition);
                increment = extended.modifiedProgressIncrement(increment, this.worldPosition, hammerPos, tool, player);
            }
            this.progress += increment;
            if (increment > 0) {
                ProgressParticlesPayload.send(level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1,
                    this.worldPosition.getZ() + 0.5, 2);
            }
        }
        if (this.progress >= 0.9999f) {
            if (extended != null) {
                result.addAll(extended.onRecipeCompleted(this, level, tool));
            } else {
                this.input.extractItem(0, 1, false);
                result.add(recipe.result().copy());
            }
            level.playSound(null, this.worldPosition, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1,
                (float) (1 + level.random.nextGaussian() * 0.4));
            this.recipe = null;
            this.progress = 0;
        }
        AnvilHitPayload.send(level, this.worldPosition, hitVec);
        this.setChanged();
        return result;
    }

    /** The recipe the client resolves for its hit particles: the item's recipe for any tool. */
    public Optional<AnvilRecipe> clientRecipe() {
        if (this.level == null) {
            return Optional.empty();
        }
        return AnvilRecipe.find(this.level, this.input.getStackInSlot(0), this.tier(), null).map(RecipeHolder::value);
    }

    // -- Light ---------------------------------------------------------------

    /** The 1.12 {@code getLightValue}: a block item on the anvil lights it as its block would. */
    private void updateLight() {
        AuxiliaryLightManager lights = this.level == null ? null : this.level.getAuxLightManager(this.worldPosition);
        if (lights == null) {
            return;
        }
        ItemStack stack = this.input.getStackInSlot(0);
        int light = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState().getLightEmission() : 0;
        lights.setLightAt(this.worldPosition, light);
    }

    @Override
    protected void onSyncedDataUpdate() {
        this.updateLight();
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
        tag.putInt("durabilityUntilNextDamage", this.durabilityUntilNextDamage);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getFloat("progress");
        this.durabilityUntilNextDamage = tag.getInt("durabilityUntilNextDamage");
    }
}
