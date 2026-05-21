package com.gemcasemod;

import com.gemcasemod.menu.PortableGemCaseMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, GemCaseMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<PortableGemCaseMenu>> PORTABLE_GEM_CASE = MENUS.register(
            "portable_gem_case",
            () -> IMenuTypeExtension.create(PortableGemCaseMenu::fromNetwork));

    private ModMenuTypes() {}
}
