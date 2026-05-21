package com.gemcasemod;

import com.gemcasemod.menu.PortableGemCaseMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class GemCaseEvents {
    private GemCaseEvents() {}

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().isShiftKeyDown()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!PortableGemStorage.isGemCase(stack)) {
            return;
        }
        if (event.getLevel().isClientSide) {
            return;
        }
        event.setCanceled(true);
        openPortableMenu((ServerPlayer) event.getEntity(), event.getHand());
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isShiftKeyDown()) {
            return;
        }
        ItemStack stack = event.getEntity().getItemInHand(event.getHand());
        if (!PortableGemStorage.isGemCase(stack)) {
            return;
        }
        event.setCanceled(true);
        if (!event.getLevel().isClientSide) {
            openPortableMenu((ServerPlayer) event.getEntity(), event.getHand());
        }
    }

    private static void openPortableMenu(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return stack.getHoverName();
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player ignored) {
                return new PortableGemCaseMenu(containerId, inventory, hand);
            }
        }, buf -> buf.writeEnum(hand));
    }
}
