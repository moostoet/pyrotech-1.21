package com.moostoet.pyrotech.tool.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * A Pyrotech sword: vanilla's, with the durability tooltip and an optional active behaviour.
 * An active sword swaps its attribute modifiers for the scaled set, so the scalar the 1.12
 * tooltip promised now lands on a hit (tool sign-off, item 10).
 */
public class PyrotechSwordItem extends SwordItem implements ActiveTool {

    private static final float ATTACK_DAMAGE = 3.0f;
    private static final float ATTACK_SPEED = -2.4f;

    @Nullable
    private final ActiveBehaviour active;
    private final ItemAttributeModifiers baseAttributes;
    private final ItemAttributeModifiers activeAttributes;

    public PyrotechSwordItem(Tier tier, @Nullable ActiveBehaviour active, Properties properties) {
        super(tier, properties.attributes(createAttributes(tier, ATTACK_DAMAGE, ATTACK_SPEED)));
        this.active = active;
        this.baseAttributes = createAttributes(tier, ATTACK_DAMAGE, ATTACK_SPEED);
        // 1.12 scaled the whole attack damage, the base 3 plus the tier bonus.
        float scalar = active == null ? 1.0f : active.swordDamageScalar();
        float total = ATTACK_DAMAGE + tier.getAttackDamageBonus();
        this.activeAttributes = createAttributes(tier, total * scalar - tier.getAttackDamageBonus(), ATTACK_SPEED);
    }

    @Override
    @Nullable
    public ActiveBehaviour activeBehaviour() {
        return this.active;
    }

    @Override
    public void onActiveChanged(ItemStack stack) {
        boolean isActive = this.active != null && this.active.isActive(stack);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, isActive ? this.activeAttributes : this.baseAttributes);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return ActiveTools.destroySpeed(this.active, stack, super.getDestroySpeed(stack, state), false);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        ActiveTools.inventoryTick(this.active, stack, level, entity);
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return ActiveTools.damageItem(this.active, stack, amount, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ActiveTools.appendHoverText(this.active, stack, tooltip, ActiveBehaviour.ToolKind.SWORD);
    }
}
