package com.moostoet.pyrotech.bucket.item;

import com.moostoet.pyrotech.bucket.BucketComponents;
import com.moostoet.pyrotech.bucket.BucketConfig;
import com.moostoet.pyrotech.bucket.BucketTier;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A Pyrotech bucket. One item per tier holds any one fluid, a whole bucket at a time, in
 * the fluid component, and counts its uses down in the uses component; at zero it is gone.
 * The fluid moves through the item fluid handler, so cauldrons, tanks, and crucibles see
 * one bucket (bucket sign-off, items 2 and 7). Milk is NeoForge's milk fluid (item 1).
 */
public final class PyrotechBucketItem extends Item {

    private static final int DRINK_DURATION = 32;

    private final BucketTier tier;

    public PyrotechBucketItem(BucketTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public BucketTier tier() {
        return this.tier;
    }

    // -- Contents ---------------------------------------------------------------

    public FluidStack fluid(ItemStack stack) {
        return stack.getOrDefault(BucketComponents.FLUID, SimpleFluidContent.EMPTY).copy();
    }

    public boolean hasFluid(ItemStack stack) {
        return !stack.getOrDefault(BucketComponents.FLUID, SimpleFluidContent.EMPTY).isEmpty();
    }

    private boolean holdsMilk(ItemStack stack) {
        return stack.getOrDefault(BucketComponents.FLUID, SimpleFluidContent.EMPTY).is(NeoForgeMod.MILK.get());
    }

    /** One bucket of {@code fluid} with the uses of {@code stack}. */
    private ItemStack withFluid(ItemStack stack, FluidStack fluid) {
        ItemStack filled = stack.copyWithCount(1);
        filled.set(BucketComponents.FLUID, SimpleFluidContent.copyOf(fluid));
        return filled;
    }

    public int uses(ItemStack stack) {
        return stack.getOrDefault(BucketComponents.USES, this.tier.uses());
    }

    private void setUses(ItemStack stack, int uses) {
        if (uses >= this.tier.uses()) {
            stack.remove(BucketComponents.USES);
        } else {
            stack.set(BucketComponents.USES, uses);
        }
    }

    /**
     * Empties {@code stack} and spends one use, in place. False when that was the last use:
     * the caller drops the bucket, since there is no broken bucket item.
     */
    public boolean spendUse(ItemStack stack) {
        stack.remove(BucketComponents.FLUID);
        int uses = this.uses(stack) - 1;
        if (uses <= 0) {
            return false;
        }
        this.setUses(stack, uses);
        return true;
    }

    /** An empty copy of one of {@code stack} with one use spent, or nothing once that was the last use. */
    private ItemStack spent(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        return this.spendUse(copy) ? copy : ItemStack.EMPTY;
    }

    // -- Using the bucket -------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.holdsMilk(stack)) {
            return ItemUtils.startUsingInstantly(level, player, hand);
        }
        boolean empty = !this.hasFluid(stack);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, empty ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos pos = hit.getBlockPos();
        Direction side = hit.getDirection();
        if (!level.mayInteract(player, pos)) {
            return InteractionResultHolder.fail(stack);
        }
        if (FluidUtil.getFluidHandler(level, pos, side).isPresent()) {
            return this.useOnFluidHandler(level, player, hand, pos, side);
        }
        return empty ? this.pickUp(level, player, stack, pos, side) : this.place(level, player, hand, stack, pos, side);
    }

    /**
     * A block with a fluid handler, such as a cauldron or a tank, takes or gives a whole
     * bucket through the handler and never through placement (bucket sign-off, item 2).
     */
    private InteractionResultHolder<ItemStack> useOnFluidHandler(Level level, Player player, InteractionHand hand,
                                                                 BlockPos pos, Direction side) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        boolean lastUse = this.hasFluid(stack) && this.uses(stack) <= 1;
        if (!FluidUtil.interactWithFluidHandler(player, hand, level, pos, side)) {
            return InteractionResultHolder.fail(stack);
        }
        if (lastUse) {
            playBreak(level, player);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), false);
    }

    private InteractionResultHolder<ItemStack> pickUp(Level level, Player player, ItemStack stack, BlockPos pos, Direction side) {
        FluidActionResult result = FluidUtil.tryPickUpFluid(stack.copyWithCount(1), player, level, pos, side);
        if (!result.isSuccess()) {
            return InteractionResultHolder.fail(stack);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        return InteractionResultHolder.sidedSuccess(afterUse(stack, player, result.getResult()), level.isClientSide);
    }

    private InteractionResultHolder<ItemStack> place(Level level, Player player, InteractionHand hand, ItemStack stack,
                                                     BlockPos pos, Direction side) {
        FluidStack fluid = this.fluid(stack);
        BlockState state = level.getBlockState(pos);
        boolean intoBlock = state.getBlock() instanceof LiquidBlockContainer container
            && container.canPlaceLiquid(player, level, pos, state, fluid.getFluid());
        BlockPos target = intoBlock ? pos : pos.relative(side);
        if (!player.mayUseItemAt(target, side, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        FluidActionResult result = FluidUtil.tryPlaceFluid(player, level, hand, target, stack, fluid);
        if (!result.isSuccess()) {
            return InteractionResultHolder.fail(stack);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (result.getResult().isEmpty()) {
            playBreak(level, player);
        }
        return InteractionResultHolder.sidedSuccess(afterUse(stack, player, result.getResult()), level.isClientSide);
    }

    /**
     * What the hand holds after one bucket of {@code stack} became {@code result}: the
     * 1.12 rule, where a creative player's stack never changes and a stack of empties
     * shrinks and hands the result to the inventory.
     */
    private static ItemStack afterUse(ItemStack stack, Player player, ItemStack result) {
        if (player.hasInfiniteMaterials()) {
            return stack;
        }
        if (stack.getCount() == 1) {
            return result;
        }
        stack.shrink(1);
        if (!result.isEmpty() && !player.getInventory().add(result)) {
            player.drop(result, false);
        }
        return stack;
    }

    private static void playBreak(Level level, Entity entity) {
        if (!level.isClientSide) {
            level.playSound(null, entity.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    // -- Milk ----------------------------------------------------------------------------

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return this.holdsMilk(stack) ? UseAnim.DRINK : UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return this.holdsMilk(stack) ? DRINK_DURATION : 0;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!this.holdsMilk(stack)) {
            return stack;
        }
        if (!level.isClientSide) {
            entity.removeEffectsCuredBy(EffectCures.MILK);
        }
        if (!(entity instanceof Player player)) {
            return stack;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        ItemStack empty = this.spent(stack);
        if (empty.isEmpty()) {
            playBreak(level, player);
        }
        return afterUse(stack, player, empty);
    }

    /** Milking a cow, which vanilla's cow leaves to the item for anything but its own bucket. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!BucketConfig.COMMON.enableCowMilk(this.tier) || this.hasFluid(stack) || player.hasInfiniteMaterials()) {
            return InteractionResult.PASS;
        }
        if (!(target instanceof Cow cow) || cow.isBaby()) {
            return InteractionResult.PASS;
        }
        player.playSound(SoundEvents.COW_MILK, 1.0f, 1.0f);
        ItemStack milk = this.withFluid(stack, new FluidStack(NeoForgeMod.MILK.get(), FluidType.BUCKET_VOLUME));
        player.setItemInHand(hand, afterUse(stack, player, milk));
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    // -- The damage tick -------------------------------------------------------------

    /**
     * Once a second, on the tick every filled bucket in the game shares (bucket sign-off,
     * item 10): a full bucket wears, a hot one wears more and burns its holder, and a
     * bucket worn out this way spills at the holder's feet (item 8).
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || level.getGameTime() % 20 != 0) {
            return;
        }
        FluidStack fluid = this.fluid(stack);
        if (fluid.isEmpty()) {
            return;
        }
        int damage = this.tier.fullContainerDamagePerSecond();
        if (fluid.getFluidType().getTemperature(fluid) >= BucketTier.HOT_TEMPERATURE) {
            int playerDamage = this.tier.hotPlayerDamagePerSecond();
            if (playerDamage > 0) {
                entity.hurt(level.damageSources().inFire(), playerDamage);
                entity.igniteForSeconds(1);
            }
            damage += this.tier.hotContainerDamagePerSecond();
        }
        if (damage == 0) {
            return;
        }
        int uses = this.uses(stack) - damage;
        if (uses > 0) {
            this.setUses(stack, uses);
            return;
        }
        stack.setCount(0);
        if (entity instanceof Player player) {
            this.spill(level, player, fluid);
        }
        playBreak(level, entity);
    }

    private void spill(Level level, Player player, FluidStack fluid) {
        BlockPos pos = player.blockPosition();
        if (!level.mayInteract(player, pos)) {
            return;
        }
        ItemStack source = this.withFluid(new ItemStack(this), fluid);
        if (!FluidUtil.tryPlaceFluid(player, level, InteractionHand.MAIN_HAND, pos, source, fluid).isSuccess()) {
            return;
        }
        if (!BucketConfig.COMMON.dropSourceOnBreak(this.tier)) {
            return;
        }
        BlockState placed = level.getBlockState(pos);
        if (placed.getBlock() instanceof LiquidBlock) {
            level.setBlock(pos, placed.setValue(LiquidBlock.LEVEL, 5), Block.UPDATE_ALL);
        }
    }

    // -- Crafting, fuel, stacking -------------------------------------------------------

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return this.spent(stack);
    }

    /** The held fluid's own bucket entry in {@code furnace_fuels} (refractory sign-off, item 4). */
    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        FluidStack fluid = this.fluid(stack);
        if (fluid.isEmpty()) {
            return 0;
        }
        FurnaceFuel fuel = BuiltInRegistries.ITEM.wrapAsHolder(fluid.getFluid().getBucket()).getData(NeoForgeDataMaps.FURNACE_FUELS);
        return fuel == null ? 0 : fuel.burnTime();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return this.hasFluid(stack) ? 1 : super.getMaxStackSize(stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (oldStack.getItem() != newStack.getItem()) {
            return true;
        }
        FluidStack oldFluid = this.fluid(oldStack);
        FluidStack newFluid = this.fluid(newStack);
        if (oldFluid.isEmpty() && newFluid.isEmpty()) {
            return slotChanged;
        }
        if (oldFluid.isEmpty() || newFluid.isEmpty()) {
            return true;
        }
        return slotChanged || oldFluid.getFluid() != newFluid.getFluid();
    }

    // -- Display -------------------------------------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return this.uses(stack) < this.tier.uses();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * this.uses(stack) / this.tier.uses());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0.0f, (float) this.uses(stack) / this.tier.uses()) / 3.0f, 1.0f, 1.0f);
    }

    /** The two 1.12 lines, with no client flag in front of them (bucket sign-off, item 9). */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int uses = this.uses(stack);
        tooltip.add(uses == this.tier.uses()
            ? Component.translatable("gui.pyrotech.tooltip.uses.full", uses)
            : Component.translatable("gui.pyrotech.tooltip.uses", uses, this.tier.uses()));
        tooltip.add(this.tier.hotContainerDamagePerSecond() <= 0
            ? Component.translatable("gui.pyrotech.tooltip.hot.fluids.true").withStyle(ChatFormatting.GREEN)
            : Component.translatable("gui.pyrotech.tooltip.hot.fluids.false").withStyle(ChatFormatting.RED));
    }

    /** The 1.12 names: an empty key, a key of the fluid's own where the lang has one (milk), else the fluid's name filled in. */
    @Override
    public Component getName(ItemStack stack) {
        FluidStack fluid = this.fluid(stack);
        if (fluid.isEmpty()) {
            return Component.translatable(this.getDescriptionId() + "_empty");
        }
        String own = this.getDescriptionId() + "_" + BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getPath();
        if (Language.getInstance().has(own)) {
            return Component.translatable(own);
        }
        return Component.translatable(this.getDescriptionId(), fluid.getHoverName());
    }

    /** Every filled variant, milk first and then each source fluid: 1.12's sub-items behind {@code SHOW_ALL_BUCKETS}. */
    public void addFilledVariants(CreativeModeTab.Output output) {
        Fluid milk = NeoForgeMod.MILK.get();
        output.accept(this.withFluid(new ItemStack(this), new FluidStack(milk, FluidType.BUCKET_VOLUME)));
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid != Fluids.EMPTY && fluid != milk && fluid.isSource(fluid.defaultFluidState())) {
                output.accept(this.withFluid(new ItemStack(this), new FluidStack(fluid, FluidType.BUCKET_VOLUME)));
            }
        }
    }
}
