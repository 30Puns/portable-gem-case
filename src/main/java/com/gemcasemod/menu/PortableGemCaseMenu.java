package com.gemcasemod.menu;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.gemcasemod.ModMenuTypes;
import com.gemcasemod.PortableGemStorage;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.socket.gem.cutting.GemCuttingMenu;
import dev.shadowsoffire.apotheosis.socket.gem.cutting.GemCuttingRecipe;
import dev.shadowsoffire.apotheosis.socket.gem.cutting.PurityUpgradeRecipe;
import com.gemcasemod.GemUpgradeHelper;
import dev.shadowsoffire.placebo.menu.PlaceboContainerMenu;
import dev.shadowsoffire.placebo.payloads.ButtonClickPayload.IButtonContainer;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class PortableGemCaseMenu extends PlaceboContainerMenu implements IButtonContainer {

    public static final int INPUT_SLOT = 0;
    public static final int FILTER_SLOT = 1;
    public static final int FIRST_GEM_SLOT = 2;
    public static final int FIRST_UPGRADE_MAT_SLOT = 8;

    protected final PortableGemStorage storage;
    protected final InteractionHand hand;
    protected final Player player;

    public SimpleContainer ioInv = new SimpleContainer(2);
    public SimpleContainer upgradeMatInv = new SimpleContainer(6) {
        @Override
        public void setChanged() {
            super.setChanged();
            PortableGemCaseMenu.this.onChanged();
        }
    };

    protected Runnable notifier = null;

    @Nullable
    public Gem selectedGem = null;

    public PortableGemCaseMenu(int id, Inventory inv, InteractionHand hand) {
        super(ModMenuTypes.PORTABLE_GEM_CASE.get(), id, inv);
        this.hand = hand;
        this.player = inv.player;
        this.storage = PortableGemStorage.fromStack(inv.player.getItemInHand(hand));
        initCommon(inv);
    }

    public static PortableGemCaseMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        return new PortableGemCaseMenu(id, inv, hand);
    }

    public ItemStack getCaseStack() {
        return this.player.getItemInHand(this.hand);
    }

    public void syncFromStack() {
        this.storage.loadFromStack(getCaseStack());
    }

    public void persistToStack() {
        if (!this.player.level().isClientSide) {
            this.storage.saveToStack(getCaseStack());
        }
    }

    public void setSelectedGem(DynamicHolder<Gem> gem) {
        this.selectedGem = gem.isBound() ? gem.get() : null;
        this.onChanged();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.persistToStack();
        this.clearContainer(player, this.ioInv);
        this.clearContainer(player, this.upgradeMatInv);
    }

    @Override
    public boolean stillValid(Player player) {
        return PortableGemStorage.isGemCase(player.getItemInHand(this.hand));
    }

    void initCommon(Inventory inv) {
        this.addSlot(new Slot(this.ioInv, 0, 142, 99) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Apoth.Items.GEM);
            }

            @Override
            public int getMaxStackSize() {
                return 64;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                if (!PortableGemCaseMenu.this.level.isClientSide && !this.getItem().isEmpty()) {
                    PortableGemCaseMenu.this.storage.depositGem(this.getItem());
                    PortableGemCaseMenu.this.persistToStack();
                }
                if (!this.getItem().isEmpty() && PortableGemCaseMenu.this.level.isClientSide) {
                    inv.player.level().playSound(inv.player, inv.player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.NEUTRAL, 0.5F, 0.7F);
                }
                PortableGemCaseMenu.this.ioInv.setItem(0, ItemStack.EMPTY);
                PortableGemCaseMenu.this.broadcastChanges();
                PortableGemCaseMenu.this.onChanged();
            }
        });
        this.addSlot(new Slot(this.ioInv, 1, 142, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !LootCategory.forItem(stack).isNone();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                PortableGemCaseMenu.this.onChanged();
            }
        });

        for (Purity p : Purity.ALL_PURITIES) {
            this.addSlot(new PortableGemCaseSlot(this, p, 21 + p.ordinal() * 18, 91));
        }

        for (int i = 0; i < this.upgradeMatInv.getContainerSize(); i++) {
            this.addSlot(new Slot(this.upgradeMatInv, i, -45 + 18 * (i % 2), 37 + 18 * (i / 2)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return PortableGemCaseMenu.this.isValidUpgradeMaterial(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 64;
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    PortableGemCaseMenu.this.onChanged();
                }
            });
        }

        this.addPlayerSlots(inv, 8, 148);

        this.mover.registerRule((stack, slot) -> slot == FILTER_SLOT, this.playerInvStart, this.slots.size());
        this.mover.registerRule((stack, slot) -> slot >= FIRST_GEM_SLOT && slot < FIRST_UPGRADE_MAT_SLOT, this.playerInvStart, this.slots.size());
        this.mover.registerRule((stack, slot) -> slot >= FIRST_UPGRADE_MAT_SLOT && slot < FIRST_UPGRADE_MAT_SLOT + 6, this.playerInvStart, this.slots.size());
        this.mover.registerRule((stack, slot) -> slot >= this.playerInvStart && stack.is(Apoth.Items.GEM), INPUT_SLOT, INPUT_SLOT + 1);
        this.mover.registerRule((stack, slot) -> slot >= this.playerInvStart && isValidUpgradeMaterial(stack), FIRST_UPGRADE_MAT_SLOT, FIRST_UPGRADE_MAT_SLOT + 6);
        this.mover.registerRule((stack, slot) -> !LootCategory.forItem(stack).isNone(), FILTER_SLOT, FILTER_SLOT + 1);
        this.registerInvShuffleRules();
    }

    public void setNotifier(Runnable r) {
        this.notifier = r;
    }

    public void onChanged() {
        if (!this.level.isClientSide) {
            this.persistToStack();
            this.broadcastChanges();
        } else {
            this.syncFromStack();
        }
        if (this.notifier != null) {
            this.notifier.run();
        }
    }

    public int getGemCount(Gem gem) {
        return this.storage.getCount(gem);
    }

    public int getGemCount(Gem gem, Purity p) {
        return this.storage.getCount(gem, p);
    }

    public ItemStack extractGem(Purity p, int count) {
        if (this.selectedGem == null) {
            return ItemStack.EMPTY;
        }
        DynamicHolder<Gem> holder = GemRegistry.INSTANCE.holder(this.selectedGem);
        ItemStack extracted = this.storage.extractGem(holder, p, count);
        if (!extracted.isEmpty()) {
            this.persistToStack();
            this.broadcastChanges();
            this.onChanged();
        }
        return extracted;
    }

    @Nullable
    public GemUpgradeHelper.Match getUpgradeMatch(Purity purity) {
        if (this.selectedGem == null) {
            return null;
        }
        return this.storage.getUpgradeMatch(GemRegistry.INSTANCE.holder(this.selectedGem), purity, this.upgradeMatInv, this.level);
    }

    @Override
    public void onQuickMove(ItemStack original, ItemStack remaining, Slot slot) {
        if (slot instanceof PortableGemCaseSlot gss) {
            int amount = original.getCount() - remaining.getCount();
            this.storage.extractGem(GemRegistry.INSTANCE.holder(this.selectedGem), gss.purity, amount);
            this.persistToStack();
        }
        slot.setChanged();
        this.onChanged();
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        Slot slot = this.getSlot(pIndex);
        if (slot instanceof PortableGemCaseSlot) {
            this.mover.quickMoveStack(this, pPlayer, pIndex);
            return ItemStack.EMPTY;
        }
        return this.mover.quickMoveStack(this, pPlayer, pIndex);
    }

    public boolean isValidUpgradeMaterial(ItemStack stack) {
        for (RecipeHolder<GemCuttingRecipe> rec : GemCuttingMenu.getRecipes(this.level)) {
            if (rec.value() instanceof PurityUpgradeRecipe purRec) {
                if (purRec.isValidLeftItem(null, stack) || purRec.isValidRightItem(null, stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onButtonClick(int id) {
        boolean shift = (id & 0x1000) != 0;
        Purity purity = Purity.BY_ID.apply(id & 0xFFF);

        if (this.selectedGem == null || purity == Purity.CRACKED) {
            return;
        }

        DynamicHolder<Gem> holder = GemRegistry.INSTANCE.holder(this.selectedGem);
        int tries = shift ? 64 : 1;

        while (tries-- > 0) {
            boolean result = this.storage.upgradeGem(holder, purity, this.upgradeMatInv, this.level);
            if (!result) {
                break;
            }
            this.level.playSound(null, this.player.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1, 1.5F + 0.35F * (1 - 2 * this.level.random.nextFloat()));
        }

        this.persistToStack();
        this.broadcastChanges();
        this.onChanged();
    }
}
