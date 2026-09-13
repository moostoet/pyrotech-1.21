package com.moostoet.pyrotech.tech.basic.block.entity;

import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.interaction.ScrollInteractable;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.tech.basic.TechBasicAttachments;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.TechBasicEffects;
import com.moostoet.pyrotech.tech.basic.TechBasicItems;
import com.moostoet.pyrotech.tech.basic.TechBasicTags;
import com.moostoet.pyrotech.tech.basic.block.CampfireBlock;
import com.moostoet.pyrotech.tech.basic.block.CampfireVariant;
import com.moostoet.pyrotech.tech.basic.effect.RestingEffect;
import com.moostoet.pyrotech.tech.basic.event.CampfireEffectTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * The campfire: a food slot, an output slot, and eight log slots filled last in, first
 * out. Lit, it burns tinder for six seconds and then a log every two minutes, cooks the
 * food at a pace set by its log count, burns food left too long, drops ash, and grants
 * comfort and resting to players resting beside it at night. Rain or a bucket of water
 * douses it; out of logs, it burns down to ash. The 1.12 combustion worker base is
 * inlined here, its only tech/basic user.
 */
public final class CampfireBlockEntity extends SyncedBlockEntity implements ScrollInteractable {

    public static final int FUEL_SLOTS = 8;
    public static final int TINDER_BURN_TICKS = 120;
    public static final int BURN_TICKS_PER_LOG = 2 * 60 * 20;
    public static final int MAX_ASH = 8;
    private static final int BURNED_FOOD_TICKS = 30 * 20;
    private static final int FUEL_LEVEL_FOR_FULL_COOK_SPEED = 4;
    private static final int RAIN_TICKS_BEFORE_EXTINGUISHED = 10 * 20;
    private static final double ASH_CHANCE = 0.25;
    private static final double FLAMMABLE_FLOOR_CHANCE = 0.05;
    private static final int MINIMUM_LIGHT = 3;
    private static final int MAXIMUM_LIGHT = 11;
    private static final int MINIMUM_EFFECT_RADIUS = 3;
    private static final int MAXIMUM_EFFECT_RADIUS = 6;
    private static final int EFFECT_SCAN_RANGE = 15;
    private static final int EFFECTS_START_TIME = 12000;
    private static final int EFFECTS_STOP_TIME = 23000;
    private static final double PLAYER_BURN_CHANCE = 0.5;
    private static final float PLAYER_LOG_BURN_DAMAGE = 1;
    private static final double DROP_OFFSET = -0.125;

    private final ItemStackHandler input = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return CampfireBlockEntity.this.canCook(stack) && CampfireBlockEntity.this.output.getStackInSlot(0).isEmpty();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return this.isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            CampfireBlockEntity.this.resetCookTime();
            CampfireBlockEntity.this.sync();
        }
    };

    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            if (this.getStackInSlot(slot).isEmpty()) {
                CampfireBlockEntity.this.outputAge = 0;
            }
            CampfireBlockEntity.this.sync();
        }
    };

    private final ItemStackHandler fuel = new ItemStackHandler(FUEL_SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(TechBasicTags.Items.CAMPFIRE_FUELS);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return this.isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            CampfireBlockEntity.this.sync();
            CampfireBlockEntity.this.updateLight();
        }
    };

    private int burnTimeRemaining = TINDER_BURN_TICKS;
    private int cookTime = -1;
    private int cookTimeTotal = -1;
    private int outputAge;
    private int rainTicks = RAIN_TICKS_BEFORE_EXTINGUISHED;
    private boolean extinguishedByRain;

    public CampfireBlockEntity(BlockPos pos, BlockState state) {
        super(TechBasicBlockEntities.CAMPFIRE.get(), pos, state);
    }

    public ItemStackHandler input() {
        return this.input;
    }

    public ItemStackHandler output() {
        return this.output;
    }

    public ItemStackHandler fuel() {
        return this.fuel;
    }

    public boolean isLit() {
        return this.getBlockState().getValue(CampfireBlock.VARIANT) == CampfireVariant.LIT;
    }

    public boolean isDead() {
        return this.getBlockState().getValue(CampfireBlock.VARIANT) == CampfireVariant.ASH;
    }

    /** The number of logs, 0 to 8: the last in, first out stack is filled from slot 0 up. */
    public int fuelCount() {
        for (int slot = 0; slot < FUEL_SLOTS; slot++) {
            if (this.fuel.getStackInSlot(slot).isEmpty()) {
                return slot;
            }
        }
        return FUEL_SLOTS;
    }

    public boolean canCook(ItemStack stack) {
        return this.level != null && CampfireCookList.find(this.level, stack).isPresent();
    }

    // -- Interaction ---------------------------------------------------------

    public boolean insertFood(Player player, ItemStack held) {
        return SlotInteraction.insert(this.input, 0, player, held, 1);
    }

    public boolean extractFood(Player player) {
        return SlotInteraction.extract(this.output, 0, player, this.worldPosition)
            || SlotInteraction.extract(this.input, 0, player, this.worldPosition);
    }

    /** One log from the held stack onto the pile, with the wood place sound. */
    public boolean addLog(Player player, ItemStack held) {
        int count = this.fuelCount();
        if (this.level == null || count >= FUEL_SLOTS || !this.fuel.isItemValid(count, held)) {
            return false;
        }
        if (!this.level.isClientSide) {
            this.fuel.insertItem(count, held.copyWithCount(1), false);
            this.level.playSound(null, this.worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1, 1);
            if (!player.isCreative()) {
                held.shrink(1);
            }
        }
        return true;
    }

    /** The top log into the player's inventory, with half a chance of a burn when lit. */
    public boolean removeLog(Player player, boolean sound) {
        int count = this.fuelCount();
        if (this.level == null || count == 0) {
            return false;
        }
        if (!this.level.isClientSide) {
            ItemStack taken = this.fuel.extractItem(count - 1, 1, false);
            if (this.isLit() && !player.fireImmune() && !player.isSteppingCarefully()
                && this.level.random.nextDouble() < PLAYER_BURN_CHANCE) {
                player.hurt(this.level.damageSources().hotFloor(), PLAYER_LOG_BURN_DAMAGE);
            }
            SlotInteraction.giveOrDrop(player, taken, this.worldPosition, sound);
        }
        return true;
    }

    public boolean shovelAsh(Player player, ItemStack shovel, InteractionHand hand) {
        BlockState state = this.getBlockState();
        int ash = state.getValue(CampfireBlock.ASH);
        if (this.level == null || ash <= 0) {
            return false;
        }
        if (!this.level.isClientSide) {
            this.level.setBlock(this.worldPosition, state.setValue(CampfireBlock.ASH, ash - 1), Block.UPDATE_ALL);
            Spill.onTop(this.level, this.worldPosition, new ItemStack(CoreItems.material(Material.PIT_ASH).get()));
            shovel.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            this.level.playSound(null, this.worldPosition, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1, 1);
        }
        return true;
    }

    @Override
    public void scroll(Player player, BlockHitResult hit, boolean up) {
        if (this.isDead()) {
            return;
        }
        if (up) {
            this.addLog(player, player.getMainHandItem());
        } else {
            this.removeLog(player, false);
        }
    }

    // -- Lighting and dousing ------------------------------------------------

    public void ignite() {
        if (this.getBlockState().getValue(CampfireBlock.VARIANT) == CampfireVariant.NORMAL) {
            this.setLit(true);
        }
    }

    /** Put out by water or rain: the tinder is spent, so a break drops none. */
    public void douse() {
        if (this.isLit()) {
            this.extinguishedByRain = true;
            this.setLit(false);
        }
    }

    private void setLit(boolean lit) {
        if (this.level == null || this.isDead()) {
            return;
        }
        this.rainTicks = RAIN_TICKS_BEFORE_EXTINGUISHED;
        BlockState state = this.getBlockState();
        CampfireVariant variant = lit ? CampfireVariant.LIT : CampfireVariant.NORMAL;
        if (state.getValue(CampfireBlock.VARIANT) != variant) {
            this.level.setBlock(this.worldPosition, state.setValue(CampfireBlock.VARIANT, variant), Block.UPDATE_ALL);
        }
        this.setChanged();
        this.updateLight();
    }

    /** Out of fuel: the fire dies to ash and spits its food. */
    private void die() {
        if (this.level == null) {
            return;
        }
        this.level.setBlock(this.worldPosition, this.getBlockState().setValue(CampfireBlock.VARIANT, CampfireVariant.ASH), Block.UPDATE_ALL);
        this.dropInputAndOutput();
        this.updateLight();
    }

    /** The 1.12 light: three with one log up to eleven with eight, and nothing without logs. */
    public int lightLevel() {
        int fuel = this.fuelCount();
        if (!this.isLit() || fuel == 0) {
            return 0;
        }
        return Mth.clamp((int) ((MAXIMUM_LIGHT - MINIMUM_LIGHT) * (fuel / (float) FUEL_SLOTS) + MINIMUM_LIGHT), 0, 15);
    }

    private void updateLight() {
        AuxiliaryLightManager lights = this.level == null ? null : this.level.getAuxLightManager(this.worldPosition);
        if (lights != null) {
            lights.setLightAt(this.worldPosition, this.lightLevel());
        }
    }

    // -- Effects -------------------------------------------------------------

    /** The comfort radius: three blocks with no logs up to six with eight. */
    public int effectRadius() {
        return Mth.clamp((int) ((MAXIMUM_EFFECT_RADIUS - MINIMUM_EFFECT_RADIUS) * (this.fuelCount() / (float) FUEL_SLOTS) + MINIMUM_EFFECT_RADIUS),
            0, 15);
    }

    public boolean isInEffectRange(Entity entity) {
        int radius = this.effectRadius();
        return entity.distanceToSqr(this.worldPosition.getCenter()) <= radius * radius;
    }

    private void applyEffects(Level level) {
        long dayTime = level.getDayTime() % 24000;
        if (level.getGameTime() % 20 != 0 || dayTime < EFFECTS_START_TIME || dayTime > EFFECTS_STOP_TIME) {
            return;
        }
        boolean comfort = TechBasicConfig.COMMON.comfortEnabled.get();
        boolean resting = TechBasicConfig.COMMON.restingEnabled.get();
        if (!comfort && !resting) {
            return;
        }
        AABB bounds = new AABB(this.worldPosition).inflate(EFFECT_SCAN_RANGE);
        List<Player> players = level.getEntitiesOfClass(Player.class, bounds, this::isInEffectRange);
        for (Player player : players) {
            boolean monsterNear = !level.getEntitiesOfClass(Monster.class, bounds,
                monster -> monster.distanceToSqr(player.position()) < EFFECT_SCAN_RANGE * EFFECT_SCAN_RANGE).isEmpty();
            if (monsterNear) {
                CampfireEffectTracker.untrack(player, this.worldPosition);
                continue;
            }
            if (comfort && !player.hasEffect(TechBasicEffects.COMFORT)) {
                player.addEffect(new MobEffectInstance(TechBasicEffects.COMFORT, MobEffectInstance.INFINITE_DURATION, 0, true, true));
            }
            if (resting && !player.hasEffect(TechBasicEffects.RESTING)) {
                RestingEffect.addTo(player, 0);
                TechBasicAttachments.set(player, TechBasicAttachments.get(player).withRestingTicks(0));
            }
            CampfireEffectTracker.track(player, this.worldPosition);
        }
    }

    // -- Ticking -------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        if (state.getValue(CampfireBlock.VARIANT) != CampfireVariant.LIT) {
            return;
        }
        if (TechBasicConfig.COMMON.campfireExtinguishedByRain.get()) {
            if (level.isRainingAt(pos.above())) {
                if (--campfire.rainTicks <= 0) {
                    campfire.douse();
                    return;
                }
            } else {
                campfire.rainTicks = RAIN_TICKS_BEFORE_EXTINGUISHED;
            }
        }
        if (campfire.burnTimeRemaining <= 0 && !campfire.consumeLog()) {
            campfire.die();
            return;
        }
        campfire.burnTimeRemaining--;
        if (state.getValue(CampfireBlock.ASH) >= MAX_ASH) {
            campfire.setLit(false);
            return;
        }
        BlockPos below = pos.below();
        if (level.random.nextDouble() < FLAMMABLE_FLOOR_CHANCE && level.getBlockState(below).isFlammable(level, below, Direction.UP)) {
            level.setBlock(below, BaseFireBlock.getState(level, below), Block.UPDATE_ALL);
            campfire.setLit(false);
            return;
        }
        campfire.applyEffects(level);
        campfire.cook(level);
        campfire.ageOutput();
        if (campfire.burnTimeRemaining <= 0 && level.random.nextDouble() < ASH_CHANCE) {
            level.setBlock(pos, campfire.getBlockState().setValue(CampfireBlock.ASH, campfire.getBlockState().getValue(CampfireBlock.ASH) + 1),
                Block.UPDATE_ALL);
        }
        campfire.setChanged();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        if (state.getValue(CampfireBlock.VARIANT) != CampfireVariant.LIT) {
            return;
        }
        long time = level.getGameTime();
        if (!campfire.input.getStackInSlot(0).isEmpty() && time % 40 == 0) {
            ClientProgress.particles(level, 1, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5, 0.25, 0.30, 0.25);
        }
        if (!campfire.output.getStackInSlot(0).isEmpty() && time % 5 == 0) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.FLAME,
                    pos.getX() + 0.5 + (level.random.nextDouble() * 2 - 1) * 0.4,
                    pos.getY() + 0.6 + (level.random.nextDouble() * 2 - 1) * 0.4,
                    pos.getZ() + 0.5 + (level.random.nextDouble() * 2 - 1) * 0.4, 0, 0, 0);
            }
        }
    }

    private boolean consumeLog() {
        int count = this.fuelCount();
        if (count == 0) {
            return false;
        }
        this.fuel.extractItem(count - 1, 1, false);
        this.burnTimeRemaining = BURN_TICKS_PER_LOG;
        return true;
    }

    private void resetCookTime() {
        int ticks = this.level == null ? -1 : CampfireCookList.find(this.level, this.input.getStackInSlot(0)).map(CampfireCookList.Cook::ticks).orElse(-1);
        int total = ticks < 0 ? -1 : ticks * FUEL_LEVEL_FOR_FULL_COOK_SPEED;
        this.cookTime = total;
        this.cookTimeTotal = total;
    }

    private void cook(Level level) {
        if (this.cookTime > 0) {
            this.cookTime -= this.fuelCount();
        }
        if (this.cookTime > 0) {
            return;
        }
        ItemStack cooking = this.input.extractItem(0, 1, false);
        if (!cooking.isEmpty()) {
            CampfireCookList.find(level, cooking).ifPresent(cook -> this.output.insertItem(0, cook.result(), false));
        }
    }

    private void ageOutput() {
        ItemStack cooked = this.output.getStackInSlot(0);
        if (cooked.isEmpty() || cooked.is(CoreItems.BURNED_FOOD.get()) || !cooked.has(DataComponents.FOOD)) {
            return;
        }
        this.outputAge += this.fuelCount();
        if (this.outputAge >= BURNED_FOOD_TICKS * FUEL_LEVEL_FOR_FULL_COOK_SPEED) {
            this.outputAge = 0;
            this.output.setStackInSlot(0, new ItemStack(CoreItems.BURNED_FOOD.get()));
        }
    }

    /** A cooking campfire's progress for the renderer and the checks; nothing on the wire reads it. */
    public float progress() {
        return this.cookTimeTotal <= 0 ? 0 : 1 - this.cookTime / (float) this.cookTimeTotal;
    }

    // -- Breaking ------------------------------------------------------------

    /** The 1.12 drops: pit ash for a dead fire, tinder for one never spent, the logs, the food, and the ash. */
    public void dropContents() {
        if (this.level == null) {
            return;
        }
        if (this.isDead()) {
            Spill.onTop(this.level, this.worldPosition, new ItemStack(CoreItems.material(Material.PIT_ASH).get()), DROP_OFFSET);
        } else if (!this.extinguishedByRain && !this.isLit()) {
            Spill.onTop(this.level, this.worldPosition, new ItemStack(TechBasicItems.TINDER.get()), DROP_OFFSET);
        }
        Spill.handler(this.level, this.worldPosition, this.fuel, DROP_OFFSET);
        this.dropInputAndOutput();
        int ash = this.getBlockState().getValue(CampfireBlock.ASH);
        if (ash > 0) {
            Spill.onTop(this.level, this.worldPosition, new ItemStack(CoreItems.material(Material.PIT_ASH).get(), ash), DROP_OFFSET);
        }
    }

    public void dropInputAndOutput() {
        if (this.level != null) {
            Spill.handler(this.level, this.worldPosition, this.input, DROP_OFFSET);
            Spill.handler(this.level, this.worldPosition, this.output, DROP_OFFSET);
        }
    }

    // -- Persistence ---------------------------------------------------------

    @Override
    protected void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("input", this.input.serializeNBT(registries));
        tag.put("output", this.output.serializeNBT(registries));
        tag.put("fuel", this.fuel.serializeNBT(registries));
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.input.deserializeNBT(registries, tag.getCompound("input"));
        this.output.deserializeNBT(registries, tag.getCompound("output"));
        this.fuel.deserializeNBT(registries, tag.getCompound("fuel"));
    }

    @Override
    protected void onSyncedDataUpdate() {
        this.updateLight();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("burnTimeRemaining", this.burnTimeRemaining);
        tag.putInt("cookTime", this.cookTime);
        tag.putInt("cookTimeTotal", this.cookTimeTotal);
        tag.putInt("outputAge", this.outputAge);
        tag.putInt("rainTicks", this.rainTicks);
        tag.putBoolean("extinguishedByRain", this.extinguishedByRain);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.burnTimeRemaining = tag.getInt("burnTimeRemaining");
        this.cookTime = tag.getInt("cookTime");
        this.cookTimeTotal = tag.getInt("cookTimeTotal");
        this.outputAge = tag.getInt("outputAge");
        this.rainTicks = tag.getInt("rainTicks");
        this.extinguishedByRain = tag.getBoolean("extinguishedByRain");
    }
}
