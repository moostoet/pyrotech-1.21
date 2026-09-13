package com.moostoet.pyrotech.tech.basic;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.tech.basic.effect.CampfireEffectData;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The campfire effect data on every player: the 1.12 focused capability and its sync
 * packet as one attachment. It is not copied on death, as 1.12 registered no clone handler.
 */
public final class TechBasicAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Pyrotech.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CampfireEffectData>> CAMPFIRE_EFFECTS =
        ATTACHMENT_TYPES.register("campfire_effects", () -> AttachmentType.builder(() -> CampfireEffectData.EMPTY)
            .serialize(CampfireEffectData.CODEC)
            .sync((holder, player) -> holder == player, CampfireEffectData.STREAM_CODEC)
            .build());

    private TechBasicAttachments() {
    }

    public static CampfireEffectData get(Player player) {
        return player.getData(CAMPFIRE_EFFECTS);
    }

    /** Writes the data and pushes it to the owning client. */
    public static void set(Player player, CampfireEffectData data) {
        player.setData(CAMPFIRE_EFFECTS, data);
        if (!player.level().isClientSide) {
            player.syncData(CAMPFIRE_EFFECTS);
        }
    }
}
