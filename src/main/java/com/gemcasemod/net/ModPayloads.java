package com.gemcasemod.net;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {
    private ModPayloads() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ModPayloads::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playBidirectional(
                PortableGemCaseSelectPayload.TYPE,
                PortableGemCaseSelectPayload.CODEC,
                PortableGemCaseSelectPayload::handle);
    }
}
