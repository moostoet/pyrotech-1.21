package com.moostoet.pyrotech.hunting;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.hunting.entity.StuckSpears;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The spears stuck in a living entity: the 1.12 spear capability and its two sync packets
 * as one attachment, saved while it holds anything and pushed to every tracking client.
 */
public final class HuntingAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Pyrotech.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<StuckSpears>> STUCK_SPEARS =
        ATTACHMENT_TYPES.register("stuck_spears", () -> AttachmentType.builder(() -> StuckSpears.EMPTY)
            .serialize(StuckSpears.CODEC, spears -> !spears.isEmpty())
            .sync(StuckSpears.STREAM_CODEC)
            .build());

    private HuntingAttachments() {
    }
}
