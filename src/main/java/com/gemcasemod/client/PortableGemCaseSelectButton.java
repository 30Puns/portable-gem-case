package com.gemcasemod.client;

import org.jetbrains.annotations.Nullable;

import com.gemcasemod.net.PortableGemCaseSelectPayload;

import dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingScreen;
import dev.shadowsoffire.apotheosis.client.GhostVertexBuilder;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class PortableGemCaseSelectButton extends AbstractButton {

    protected final PortableGemCaseScreen screen;
    protected final int index;

    public PortableGemCaseSelectButton(PortableGemCaseScreen screen, int index, int x, int y) {
        super(x, y, 16, 16, CommonComponents.EMPTY);
        this.screen = screen;
        this.index = index;
    }

    @Override
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        PortableGemCaseScreen.SafeSlot slot = this.getSafeSlot();
        if (slot == null) {
            return;
        }

        int count = this.screen.getMenu().getGemCount(slot.gem());
        java.util.function.Function<net.minecraft.client.renderer.MultiBufferSource, net.minecraft.client.renderer.MultiBufferSource> wrapper;
        if (count == 0) {
            wrapper = GhostVertexBuilder.wrapper(0x44);
        } else {
            wrapper = java.util.function.Function.identity();
        }
        SalvagingScreen.renderGuiItem(gfx, slot.displayStack(), this.getX(), this.getY(), wrapper);

        if (count > 1) {
            String countStr = com.gemcasemod.GemFormatUtil.formatCount(count);
            float scale = countStr.length() > 2 ? 2.0f / countStr.length() : 1.0f;
            gfx.pose().pushPose();
            gfx.pose().scale(scale, scale, 1);
            gfx.pose().translate(0.0f, 0.0f, 200.0f);
            float textX = (this.getX() + 16 - (mc.font.width(countStr) - 1) * scale) / scale;
            float textY = (this.getY() + 16 - (mc.font.lineHeight - 2) * scale) / scale;
            gfx.drawString(mc.font, countStr, textX, textY, 0xAAFFFFFF, true);
            gfx.pose().popPose();
        }

        if (this.isHovered()) {
            gfx.pose().pushPose();
            gfx.pose().translate(0.0f, 0.0f, 200.0f);
            gfx.fill(this.getX(), this.getY(), this.getX() + 16, this.getY() + 16, 0x40FFFFFF);
            gfx.pose().popPose();
            Component desc = Component.translatable(slot.displayStack().getDescriptionId());
            gfx.renderTooltip(mc.font, desc, mouseX, mouseY);
        }
    }

    @Override
    public void onPress() {
        PortableGemCaseScreen.SafeSlot slot = this.getSafeSlot();
        if (slot != null) {
            DynamicHolder<dev.shadowsoffire.apotheosis.socket.gem.Gem> holder = GemRegistry.INSTANCE.holder(slot.gem());
            this.screen.getMenu().setSelectedGem(holder);
            PacketDistributor.sendToServer(new PortableGemCaseSelectPayload(holder));
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    @Nullable
    private PortableGemCaseScreen.SafeSlot getSafeSlot() {
        int idx = this.screen.startIndex * PortableGemCaseScreen.SLOTS_PER_ROW + this.index;
        if (idx >= 0 && idx < this.screen.data.size()) {
            return this.screen.data.get(idx);
        }
        return null;
    }
}
