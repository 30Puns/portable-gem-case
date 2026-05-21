package com.gemcasemod;

import com.gemcasemod.client.GemCaseModClient;
import com.gemcasemod.net.ModPayloads;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(GemCaseMod.MODID)
public class GemCaseMod {
    public static final String MODID = "gemcasemod";

    public GemCaseMod(IEventBus modBus) {
        ModMenuTypes.MENUS.register(modBus);
        ModPayloads.register(modBus);

        if (FMLEnvironment.dist.isClient()) {
            modBus.addListener(GemCaseModClient::registerScreens);
        }

        NeoForge.EVENT_BUS.addListener(GemCaseEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(GemCaseEvents::onRightClickBlock);
    }
}
