package com.moostoet.pyrotech.hunting.item;

import com.moostoet.pyrotech.core.item.DurabilityTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * A hunter's or butcher's knife: a sword at half its tier's sword damage, at least one,
 * as the 1.12 override documented and 1.21 now applies (hunting sign-off, item 8). The
 * two kinds differ only in their tags, their data map entries, and their repair toggle.
 */
public final class KnifeItem extends SwordItem {

    private static final float SWORD_BASE_DAMAGE = 3.0f;
    private static final float ATTACK_SPEED = -2.4f;

    private final ModConfigSpec.BooleanValue repairAllowed;

    public KnifeItem(Tier tier, ModConfigSpec.BooleanValue repairAllowed, Properties properties) {
        super(tier, properties.attributes(createAttributes(tier, halfDamage(tier) - tier.getAttackDamageBonus(), ATTACK_SPEED)));
        this.repairAllowed = repairAllowed;
    }

    private static float halfDamage(Tier tier) {
        return Math.max(1, (SWORD_BASE_DAMAGE + tier.getAttackDamageBonus()) / 2);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return this.repairAllowed.get() && super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DurabilityTooltip.appendFull(stack, tooltip);
    }
}
