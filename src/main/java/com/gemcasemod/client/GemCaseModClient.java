package com.gemcasemod.client;

import com.gemcasemod.ModMenuTypes;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class GemCaseModClient {
    private GemCaseModClient() {}

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.PORTABLE_GEM_CASE.get(), PortableGemCaseScreen::new);
    }
}
