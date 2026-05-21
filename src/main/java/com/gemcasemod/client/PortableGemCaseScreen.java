package com.gemcasemod.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.gemcasemod.menu.PortableGemCaseMenu;
import com.gemcasemod.menu.PortableGemCaseSlot;
import com.mojang.blaze3d.platform.InputConstants;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.salvaging.SalvagingScreen;
import dev.shadowsoffire.apotheosis.client.GhostVertexBuilder;
import dev.shadowsoffire.apotheosis.client.SimpleTexButton;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import com.gemcasemod.GemFormatUtil;
import com.gemcasemod.GemUpgradeHelper;
import dev.shadowsoffire.placebo.payloads.ButtonClickPayload;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.util.DrawsOnLeft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;

public class PortableGemCaseScreen extends AbstractContainerScreen<PortableGemCaseMenu> implements DrawsOnLeft {

    public static final ResourceLocation TEXTURES = Apotheosis.loc("textures/gui/gem_case.png");
    public static final int MAX_ROWS = 3;
    public static final int SLOTS_PER_ROW = 6;

    protected float scrollOffs;
    protected boolean scrolling;
    protected int startIndex;
    protected List<SafeSlot> data = new ArrayList<>();
    protected List<SimpleTexButton> upgradeButtons = new ArrayList<>();

    @Nullable
    protected EditBox filter;

    public PortableGemCaseScreen(PortableGemCaseMenu container, Inventory inv, Component title) {
        super(container, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 230;
        this.containerChanged();
        container.setNotifier(this::containerChanged);
    }

    @Override
    protected void init() {
        super.init();
        this.filter = this.addRenderableWidget(new EditBox(this.font, this.getGuiLeft() + 16, this.getGuiTop() + 16, 110, 11, this.filter, Component.literal("")));
        this.filter.setBordered(false);
        this.filter.setTextColor(0x97714F);
        this.filter.setResponder(t -> this.containerChanged());
        this.setFocused(this.filter);
        for (int i = 0; i < MAX_ROWS * SLOTS_PER_ROW; i++) {
            this.addRenderableWidget(new PortableGemCaseSelectButton(this, i, this.getGuiLeft() + 21 + (i % SLOTS_PER_ROW) * 18, this.getGuiTop() + 31 + (i / SLOTS_PER_ROW) * 19));
        }

        this.upgradeButtons.clear();
        Purity[] purities = Purity.values();
        for (int i = 1; i < purities.length; i++) {
            Purity prev = purities[i - 1];
            Purity purity = purities[i];
            var btn = SimpleTexButton.builder()
                    .size(16, 16)
                    .texture(TEXTURES)
                    .texSize(307, 256)
                    .texPos(291, 29)
                    .pos(this.getGuiLeft() + 30 + (i - 1) * 18, this.getGuiTop() + 109)
                    .message(Apotheosis.lang("button", "gem_case.upgrade", prev.toComponent(), purity.toComponent()))
                    .inactiveMessage(Apotheosis.lang("button", "gem_case.upgrade_no_materials"))
                    .action(tryUpgrade(purity))
                    .build();
            this.upgradeButtons.add(btn);
            this.addRenderableWidget(btn);
        }
        this.containerChanged();
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        InputConstants.Key mouseKey = InputConstants.getKey(pKeyCode, pScanCode);
        if (this.minecraft.options.keyInventory.isActiveAndMatches(mouseKey) && this.getFocused() == this.filter) {
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        super.render(gfx, mouseX, mouseY, partialTicks);
        this.renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
        super.renderTooltip(gfx, mouseX, mouseY);
        Gem selected = this.getSelectedGem();
        if (selected == null) {
            return;
        }
        for (Purity p : Purity.ALL_PURITIES) {
            if (!p.isAtLeast(selected.getMinPurity())) {
                continue;
            }
            int count = this.menu.getGemCount(selected, p);
            if (count == 0) {
                int slotIndex = p.ordinal();
                int x = this.getGuiLeft() + 21 + slotIndex * 18;
                int y = this.getGuiTop() + 91;
                if (this.isHovering(x - this.getGuiLeft(), y - this.getGuiTop(), 16, 16, mouseX, mouseY) && this.menu.getCarried().isEmpty()) {
                    ItemStack stack = selected.toStack(p);
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(stack.getHoverName());
                    tooltip.add(Apotheosis.lang("tooltip", "gem_case.none_owned").withStyle(ChatFormatting.RED));
                    tooltip.add(CommonComponents.SPACE);
                    stack.getItem().appendHoverText(stack, TooltipContext.of(Minecraft.getInstance().level), tooltip, TooltipFlag.NORMAL);
                    gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partial, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        gfx.blit(TEXTURES, left, top, 0, 0, this.imageWidth, this.imageHeight, 307, 256);
        int scrollbarPos = (int) (90F * this.scrollOffs);
        gfx.blit(TEXTURES, left + 13, top + 29 + scrollbarPos, 303, this.isScrollBarActive() ? 0 : 12, 4, 12, 307, 256);
        gfx.blit(TEXTURES, left - 65, top + 16, 198, 0, 65, 193, 307, 256);

        Gem selected = this.getSelectedGem();
        if (selected != null) {
            for (Purity p : Purity.ALL_PURITIES) {
                if (!p.isAtLeast(selected.getMinPurity())) {
                    continue;
                }
                if (this.menu.getGemCount(selected, p) == 0) {
                    ItemStack stack = selected.toStack(p);
                    int slotIndex = p.ordinal();
                    Function<MultiBufferSource, MultiBufferSource> wrapper = GhostVertexBuilder.wrapper(0x44);
                    SalvagingScreen.renderGuiItem(gfx, stack, this.getGuiLeft() + 21 + slotIndex * 18, this.getGuiTop() + 91, wrapper);
                }
            }
        }
    }

    @Override
    protected void renderSlotContents(GuiGraphics gfx, ItemStack stack, Slot slot, @Nullable String stackCount) {
        if (slot instanceof PortableGemCaseSlot gss) {
            gfx.renderFakeItem(stack, slot.x, slot.y);
            Gem gem = this.menu.selectedGem;
            if (gem != null) {
                int count = this.menu.getGemCount(gem, gss.purity);
                if (count > 1) {
                    String countStr = GemFormatUtil.formatCount(count);
                    float scale = countStr.length() > 2 ? 2.0f / countStr.length() : 1.0f;
                    gfx.pose().pushPose();
                    gfx.pose().scale(scale, scale, 1);
                    gfx.pose().translate(0.0f, 0.0f, 200.0f);
                    float textX = (slot.x + 16 - (this.font.width(countStr) - 1) * scale) / scale;
                    float textY = (slot.y + 16 - (this.font.lineHeight - 2) * scale) / scale;
                    gfx.drawString(this.font, countStr, textX, textY, 0xFFFFFF, true);
                    gfx.pose().popPose();
                }
            }
        } else {
            super.renderSlotContents(gfx, stack, slot, stackCount);
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        this.scrolling = false;
        if (this.isHovering(14, 29, 4, 103, pMouseX, pMouseY)) {
            this.scrolling = true;
            this.mouseDragged(pMouseX, pMouseY, pButton, 0, 0);
            return true;
        }
        if (this.filter.isHovered() && pButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.filter.setValue("");
            return true;
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (this.scrolling && this.isScrollBarActive()) {
            int barTop = this.topPos + 14;
            int barBot = barTop + 103;
            this.scrollOffs = Mth.clamp(((float) pMouseY - barTop - 6F) / (barBot - barTop - 12F) - 0.12F, 0.0F, 1.0F);
            this.startIndex = (int) (this.scrollOffs * this.getOffscreenRows() + 0.5D);
            return true;
        }
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        if (this.isScrollBarActive()) {
            this.scrollOffs = Mth.clamp(this.scrollOffs - (float) (pScrollY / this.getOffscreenRows()), 0.0F, 1.0F);
            this.startIndex = (int) (this.scrollOffs * this.getOffscreenRows() + 0.5D);
        }
        return true;
    }

    @Nullable
    public Gem getSelectedGem() {
        return this.menu.selectedGem;
    }

    private boolean isScrollBarActive() {
        return this.data.size() > MAX_ROWS * SLOTS_PER_ROW;
    }

    protected int getOffscreenRows() {
        return Math.max(1, Math.ceilDiv(this.data.size() - MAX_ROWS * SLOTS_PER_ROW, SLOTS_PER_ROW));
    }

    private void containerChanged() {
        this.menu.syncFromStack();
        this.data.clear();
        for (Gem gem : GemRegistry.INSTANCE.getValues()) {
            var slot = new SafeSlot(gem, this.menu.getGemCount(gem), new ItemStack(Apoth.Items.GEM));
            GemItem.setGem(slot.displayStack, gem);
            this.data.add(slot);
        }
        this.data = filter(new ArrayList<>(this.data));
        if (!this.isScrollBarActive()) {
            this.scrollOffs = 0.0F;
            this.startIndex = 0;
        }
        Collections.sort(this.data, Comparator.comparing((SafeSlot slot) -> slot.count <= 0).thenComparing(slot -> slot.gem.getId().toString()));

        for (int i = 0; i < upgradeButtons.size(); i++) {
            Purity prev = Purity.values()[i];
            Purity purity = prev.next();
            GemUpgradeHelper.Match match = this.menu.getUpgradeMatch(purity);
            SimpleTexButton button = this.upgradeButtons.get(i);
            if (match != null) {
                button.active = true;
                ItemStack leftMat = this.menu.upgradeMatInv.getItem(match.leftSlot());
                ItemStack rightMat = this.menu.upgradeMatInv.getItem(match.rightSlot());
                int leftCount = match.leftIng().count();
                int rightCount = match.rightIng().count();
                button.setTooltipProvider((btn, tooltip) -> {
                    tooltip.accept(Apotheosis.lang("button", "gem_case.upgrade_cost", leftCount, leftMat.getHoverName(), rightCount, rightMat.getHoverName()));
                    if (Screen.hasShiftDown()) {
                        tooltip.accept(Apotheosis.lang("button", "gem_case.upgrade_all").withStyle(ChatFormatting.YELLOW));
                    }
                });
            } else {
                button.active = false;
                if (this.menu.getGemCount(this.getSelectedGem(), prev) < 2) {
                    button.setInactiveMessage(Apotheosis.lang("button", "gem_case.upgrade_no_gems").withStyle(ChatFormatting.RED));
                } else {
                    button.setInactiveMessage(Apotheosis.lang("button", "gem_case.upgrade_no_materials").withStyle(ChatFormatting.RED));
                }
            }
        }
    }

    private List<SafeSlot> filter(List<SafeSlot> list) {
        Iterator<SafeSlot> iter = list.iterator();
        while (iter.hasNext()) {
            SafeSlot slot = iter.next();
            if (!isAllowedByItem(slot) || !isAllowedBySearch(slot)) {
                iter.remove();
            }
        }
        return list;
    }

    private boolean isAllowedByItem(SafeSlot slot) {
        ItemStack stack = this.menu.ioInv.getItem(1);
        return stack.isEmpty() || slot.gem.getBonus(LootCategory.forItem(stack)) != null;
    }

    private boolean isAllowedBySearch(SafeSlot slot) {
        String name = slot.displayStack.getDisplayName().getString().toLowerCase(Locale.ROOT);
        String search = this.filter == null ? "" : this.filter.getValue().trim().toLowerCase(Locale.ROOT);
        return search.isEmpty() || ChatFormatting.stripFormatting(name).contains(search);
    }

    private OnPress tryUpgrade(Purity purity) {
        return btn -> {
            int value = purity.ordinal() | (Screen.hasShiftDown() ? 0x1000 : 0);
            PacketDistributor.sendToServer(new ButtonClickPayload(value));
        };
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {}

    @Override
    public int getSlotColor(int index) {
        return 0x40FFFFFF;
    }

    public static void handleSelectedGem(DynamicHolder<Gem> gem) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PortableGemCaseScreen screen) {
            screen.menu.setSelectedGem(gem);
            screen.containerChanged();
        }
    }

    public record SafeSlot(Gem gem, int count, ItemStack displayStack) {}
}
