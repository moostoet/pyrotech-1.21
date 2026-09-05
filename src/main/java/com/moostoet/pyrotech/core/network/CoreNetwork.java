package com.moostoet.pyrotech.core.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Core owns the one payload registrar; every module's payloads register under version 1. */
public final class CoreNetwork {

    private CoreNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(NoHungerPayload.TYPE, NoHungerPayload.STREAM_CODEC, NoHungerPayload::handle);
    }
}
