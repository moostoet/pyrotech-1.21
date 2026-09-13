package com.moostoet.pyrotech.tech.basic.event;

import com.moostoet.pyrotech.tech.basic.TechBasicAttachments;
import com.moostoet.pyrotech.tech.basic.TechBasicEffects;
import com.moostoet.pyrotech.tech.basic.block.entity.CampfireBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Which campfires each player rests by, so comfort and resting end when the player
 * leaves them all or the night ends. The 1.12 {@code CampfireEffectTracker}: transient
 * by design, checked every half second.
 */
public final class CampfireEffectTracker {

    private static final int INTERVAL_TICKS = 10;
    private static final int EFFECTS_START_TIME = 12000;
    private static final int EFFECTS_STOP_TIME = 23000;
    private static final Map<UUID, Set<BlockPos>> TRACKED = new HashMap<>();

    private CampfireEffectTracker() {
    }

    public static void track(Player player, BlockPos campfire) {
        TRACKED.computeIfAbsent(player.getUUID(), id -> new HashSet<>()).add(campfire.immutable());
    }

    public static void untrack(Player player, BlockPos campfire) {
        Set<BlockPos> campfires = TRACKED.get(player.getUUID());
        if (campfires != null) {
            campfires.remove(campfire);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        if (event.getServer().getTickCount() % INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            Set<BlockPos> campfires = TRACKED.get(player.getUUID());
            if (campfires != null) {
                Iterator<BlockPos> iterator = campfires.iterator();
                while (iterator.hasNext()) {
                    BlockPos pos = iterator.next();
                    if (!(player.level().getBlockEntity(pos) instanceof CampfireBlockEntity campfire)
                        || !campfire.isLit() || !campfire.isInEffectRange(player)) {
                        iterator.remove();
                    }
                }
            }
            long dayTime = player.level().getDayTime() % 24000;
            boolean tracked = campfires != null && !campfires.isEmpty();
            if (!tracked || dayTime < EFFECTS_START_TIME || dayTime > EFFECTS_STOP_TIME) {
                boolean had = player.hasEffect(TechBasicEffects.COMFORT) | player.hasEffect(TechBasicEffects.RESTING);
                player.removeEffect(TechBasicEffects.COMFORT);
                player.removeEffect(TechBasicEffects.RESTING);
                if (had) {
                    TechBasicAttachments.set(player, TechBasicAttachments.get(player).withRestingTicks(0));
                }
            }
        }
    }
}
