package com.gemcasemod;

import java.util.EnumMap;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import dev.shadowsoffire.apotheosis.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.socket.gem.GemItem;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.Purity;
import dev.shadowsoffire.apotheosis.socket.gem.UnsocketedGem;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class PortableGemStorage {

    public static final ResourceLocation GEM_CASE = ResourceLocation.fromNamespaceAndPath("apotheosis", "gem_case");
    public static final ResourceLocation ENDER_GEM_CASE = ResourceLocation.fromNamespaceAndPath("apotheosis", "ender_gem_case");

    public static final Set<ResourceLocation> GEM_CASE_ITEMS = Set.of(GEM_CASE, ENDER_GEM_CASE);

    private final Object2ObjectMap<DynamicHolder<Gem>, EnumMap<Purity, Integer>> gems = new Object2ObjectLinkedOpenHashMap<>();
    private final int maxCount;

    public PortableGemStorage(int maxCount) {
        this.maxCount = maxCount;
    }

    public static int maxCountFor(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (ENDER_GEM_CASE.equals(id)) {
            return Integer.MAX_VALUE;
        }
        return Short.MAX_VALUE;
    }

    public static boolean isGemCase(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return GEM_CASE_ITEMS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static PortableGemStorage fromStack(ItemStack stack) {
        PortableGemStorage storage = new PortableGemStorage(maxCountFor(stack));
        storage.loadFromStack(stack);
        return storage;
    }

    public void loadFromStack(ItemStack stack) {
        this.gems.clear();
        CustomData data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (!data.isEmpty()) {
            loadGemData(data.copyTag());
        }
    }

    public void saveToStack(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        saveGemData(tag);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.BLOCK_ENTITY_DATA);
        } else {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            ResourceLocation beId = ENDER_GEM_CASE.equals(itemId) ? ENDER_GEM_CASE : GEM_CASE;
            tag.putString("id", beId.toString());
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        }
    }

    public void depositGem(ItemStack stack) {
        UnsocketedGem gem = UnsocketedGem.of(stack);
        if (!gem.isValid()) {
            return;
        }

        Purity purity = gem.purity();
        EnumMap<Purity, Integer> map = getGems(gem.gem());
        map.put(purity, Math.min(this.maxCount, map.get(purity) + stack.getCount()));
    }

    public ItemStack extractGem(DynamicHolder<Gem> gem, Purity purity, int count) {
        EnumMap<Purity, Integer> map = getGems(gem);
        int stored = map.get(purity);
        if (stored < count) {
            count = stored;
        }

        if (count <= 0 || !gem.isBound()) {
            return ItemStack.EMPTY;
        }

        map.put(purity, stored - count);
        return GemItem.createStack(gem.get(), purity, count);
    }

    @Nullable
    public GemUpgradeHelper.Match getUpgradeMatch(DynamicHolder<Gem> gem, Purity purity, Container matInv, Level level) {
        EnumMap<Purity, Integer> map = getGems(gem);
        if (map.get(purity) >= maxCount) {
            return null;
        }
        return GemUpgradeHelper.findMatch(level, purity, map, matInv);
    }

    public boolean upgradeGem(DynamicHolder<Gem> gem, Purity purity, Container matInv, Level level) {
        EnumMap<Purity, Integer> map = getGems(gem);
        return GemUpgradeHelper.upgradeGem(gem, purity, map, matInv, level, maxCount);
    }

    public int getCount(DynamicHolder<Gem> gem, Purity purity) {
        return getGems(gem).get(purity);
    }

    public int getCount(Gem gem, Purity purity) {
        return getCount(GemRegistry.INSTANCE.holder(gem), purity);
    }

    public int getCount(Gem gem) {
        int sum = 0;
        for (Purity p : Purity.ALL_PURITIES) {
            sum += getCount(gem, p);
        }
        return sum;
    }

    protected final EnumMap<Purity, Integer> getGems(DynamicHolder<Gem> gem) {
        return this.gems.computeIfAbsent(gem, g -> {
            EnumMap<Purity, Integer> map = new EnumMap<>(Purity.class);
            for (Purity p : Purity.values()) {
                map.put(p, 0);
            }
            return map;
        });
    }

    public void saveGemData(CompoundTag tag) {
        CompoundTag gemsTag = new CompoundTag();
        for (DynamicHolder<Gem> gem : this.gems.keySet()) {
            EnumMap<Purity, Integer> map = this.gems.get(gem);
            CompoundTag purityTag = new CompoundTag();
            for (Purity p : Purity.values()) {
                int count = map.get(p);
                if (count > 0) {
                    purityTag.putInt(p.getSerializedName(), count);
                }
            }
            if (!purityTag.isEmpty()) {
                gemsTag.put(gem.getId().toString(), purityTag);
            }
        }
        if (!gemsTag.isEmpty()) {
            tag.put("gems", gemsTag);
        }
    }

    public void loadGemData(CompoundTag tag) {
        CompoundTag gemsTag = tag.getCompound("gems");
        for (String key : gemsTag.getAllKeys()) {
            ResourceLocation res = ResourceLocation.tryParse(key);
            if (res == null) {
                continue;
            }
            DynamicHolder<Gem> gem = GemRegistry.INSTANCE.holder(res);
            if (!gem.isBound()) {
                continue;
            }
            CompoundTag purityTag = gemsTag.getCompound(key);
            if (purityTag.isEmpty()) {
                this.gems.remove(gem);
            } else {
                EnumMap<Purity, Integer> map = new EnumMap<>(Purity.class);
                for (Purity p : Purity.values()) {
                    map.put(p, purityTag.getInt(p.getSerializedName()));
                }
                this.gems.put(gem, map);
            }
        }
    }
}
