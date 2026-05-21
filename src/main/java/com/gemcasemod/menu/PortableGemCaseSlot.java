package com.gemcasemod.menu;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PortableGemCaseSlot extends Slot {

    private static final Container EMPTY = new SimpleContainer(0);

    private final PortableGemCaseMenu menu;
    public final Purity purity;

    public PortableGemCaseSlot(PortableGemCaseMenu menu, Purity purity, int x, int y) {
        super(EMPTY, -1, x, y);
        this.menu = menu;
        this.purity = purity;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        if (!stack.isEmpty()) {
            DynamicHolder<Gem> gem = GemItem.getGem(stack);
            Purity purity = GemItem.getPurity(stack);
            if (!gem.isBound() || gem.get() != this.menu.selectedGem || purity != this.purity) {
                Apotheosis.LOGGER.warn("Player {} tried to take a gem that doesn't match the selected gem or purity!", player.getName().getString());
                return;
            }
            this.menu.extractGem(this.purity, stack.getCount());
        }
        this.setChanged();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack getItem() {
        Gem gem = this.menu.selectedGem;
        if (gem == null) {
            return ItemStack.EMPTY;
        }
        int count = this.menu.getGemCount(gem, purity);
        return GemItem.createStack(gem, purity, Math.min(count, 64));
    }

    @Override
    public boolean hasItem() {
        Gem gem = this.menu.selectedGem;
        return gem != null && this.menu.getGemCount(gem, purity) > 0;
    }

    @Override
    public void setByPlayer(ItemStack stack) {}

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {}

    @Override
    public void set(ItemStack stack) {}

    @Override
    public ItemStack remove(int amount) {
        Gem gem = this.menu.selectedGem;
        if (gem == null) {
            return ItemStack.EMPTY;
        }
        int count = this.menu.getGemCount(gem, purity);
        int toExtract = Math.min(count, amount);
        if (toExtract <= 0) {
            return ItemStack.EMPTY;
        }
        return GemItem.createStack(gem, purity, toExtract);
    }

    @Override
    public boolean mayPickup(Player player) {
        return this.hasItem();
    }

    @Override
    public boolean isActive() {
        Gem gem = this.menu.selectedGem;
        return gem != null && this.purity.isAtLeast(gem.getMinPurity());
    }

    @Override
    public boolean isSameInventory(Slot other) {
        return false;
    }

    @Override
    public boolean allowModification(Player player) {
        return false;
    }

    @Override
    public boolean isHighlightable() {
        return true;
    }

    @Override
    public boolean isFake() {
        return false;
    }
}
