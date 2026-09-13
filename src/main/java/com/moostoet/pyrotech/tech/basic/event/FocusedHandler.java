package com.moostoet.pyrotech.tech.basic.event;

import com.moostoet.pyrotech.tech.basic.TechBasicAttachments;
import com.moostoet.pyrotech.tech.basic.TechBasicConfig;
import com.moostoet.pyrotech.tech.basic.TechBasicEffects;
import com.moostoet.pyrotech.tech.basic.effect.CampfireEffectData;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

/**
 * Focused: experience gained is raised by the bonus, and each gain spends the stored
 * bonus by the share of a level it was worth. The 1.12 {@code CampfireFocusEffectEventHandler}
 * on {@code PlayerXpEvent.XpChange} (tech/basic sign-off, item 11).
 */
public final class FocusedHandler {

    private FocusedHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || event.getAmount() <= 0 || !player.hasEffect(TechBasicEffects.FOCUSED)) {
            return;
        }
        CampfireEffectData data = TechBasicAttachments.get(player);
        int amount = (int) (event.getAmount() * (1 + TechBasicConfig.SERVER.focusedBonus.get()));
        event.setAmount(amount);
        double remaining = data.remainingBonus() - amount / (double) Math.max(1, player.getXpNeededForNextLevel());
        if (remaining <= 0) {
            remaining = 0;
            player.removeEffect(TechBasicEffects.FOCUSED);
        }
        TechBasicAttachments.set(player, data.withRemainingBonus(remaining));
    }
}
