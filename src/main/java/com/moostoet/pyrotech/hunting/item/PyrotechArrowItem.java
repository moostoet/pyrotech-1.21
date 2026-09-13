package com.moostoet.pyrotech.hunting.item;

import com.moostoet.pyrotech.hunting.entity.PyrotechArrowEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** A flint or bone arrow item: sixteen to a stack, one damage where vanilla's does two. */
public final class PyrotechArrowItem extends ArrowItem {

    private static final double DAMAGE = 1.0;

    private final Supplier<EntityType<PyrotechArrowEntity>> type;

    public PyrotechArrowItem(Supplier<EntityType<PyrotechArrowEntity>> type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        PyrotechArrowEntity arrow = new PyrotechArrowEntity(this.type.get(), level, shooter, ammo.copyWithCount(1), weapon);
        arrow.setBaseDamage(DAMAGE);
        return arrow;
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        PyrotechArrowEntity arrow = new PyrotechArrowEntity(this.type.get(), level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1));
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        arrow.setBaseDamage(DAMAGE);
        return arrow;
    }
}
