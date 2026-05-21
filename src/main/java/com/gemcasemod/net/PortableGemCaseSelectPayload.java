package com.gemcasemod.net;

import com.gemcasemod.GemCaseMod;
import com.gemcasemod.client.PortableGemCaseScreen;
import com.gemcasemod.menu.PortableGemCaseMenu;

import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PortableGemCaseSelectPayload(DynamicHolder<Gem> gem) implements CustomPacketPayload {
    public static final Type<PortableGemCaseSelectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GemCaseMod.MODID, "gem_case_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortableGemCaseSelectPayload> CODEC = StreamCodec.composite(
            GemRegistry.INSTANCE.holderStreamCodec(),
            PortableGemCaseSelectPayload::gem,
            PortableGemCaseSelectPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PortableGemCaseSelectPayload msg, IPayloadContext ctx) {
        if (ctx.flow().isClientbound()) {
            PortableGemCaseScreen.handleSelectedGem(msg.gem());
        } else if (ctx.player().containerMenu instanceof PortableGemCaseMenu menu) {
            menu.setSelectedGem(msg.gem());
        }
    }
}
