package com.moostoet.pyrotech.hunting.entity;

import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.hunting.HuntingEntities;
import com.moostoet.pyrotech.hunting.HuntingItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A flint or bone arrow: vanilla's arrow that may break when it lands, at odds that grow
 * with how far it moved into the block, spilling its stick, its shard, and its fletching
 * each at half chance. One class serves both 1.12 entity ids; the type says which.
 */
public final class PyrotechArrowEntity extends AbstractArrow {

    private static final float BREAK_ON_HIT_CHANCE = 0.75f;
    private static final float MATERIAL_DROP_CHANCE = 0.5f;

    public enum Kind {
        FLINT(Material.FLINT_SHARD),
        BONE(Material.BONE_SHARD);

        private final Material shard;

        Kind(Material shard) {
            this.shard = shard;
        }

        public Item shard() {
            return CoreItems.material(this.shard).get();
        }

        public Item arrow() {
            return this == BONE ? HuntingItems.BONE_ARROW.get() : HuntingItems.FLINT_ARROW.get();
        }
    }

    public PyrotechArrowEntity(EntityType<? extends PyrotechArrowEntity> type, Level level) {
        super(type, level);
    }

    public PyrotechArrowEntity(EntityType<? extends PyrotechArrowEntity> type, Level level, LivingEntity shooter, ItemStack ammo,
                               @Nullable ItemStack weapon) {
        super(type, shooter, level, ammo, weapon);
    }

    public PyrotechArrowEntity(EntityType<? extends PyrotechArrowEntity> type, Level level, double x, double y, double z, ItemStack ammo) {
        super(type, x, y, z, level, ammo, null);
    }

    public Kind kind() {
        return this.getType() == HuntingEntities.BONE_ARROW.get() ? Kind.BONE : Kind.FLINT;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(this.kind().arrow());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide || !this.inGround) {
            return;
        }
        float travel = (float) this.getDeltaMovement().length();
        double breakOnHit = 1 - Mth.clamp(BREAK_ON_HIT_CHANCE, 0, 1);
        if (this.random.nextFloat() * travel < breakOnHit) {
            return;
        }
        this.discard();
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.5f, 1);
        for (ItemStack drop : List.of(new ItemStack(Items.STICK), new ItemStack(this.kind().shard()),
            new ItemStack(CoreItems.material(Material.FLETCHING).get()))) {
            if (this.random.nextFloat() < MATERIAL_DROP_CHANCE) {
                this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), drop, 0, 0.1, 0));
            }
        }
    }
}
