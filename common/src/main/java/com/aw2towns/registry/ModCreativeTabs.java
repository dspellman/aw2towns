package com.aw2towns.registry;

import dev.architectury.registry.CreativeTabRegistry;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;

public final class ModCreativeTabs {

    private ModCreativeTabs() {}

    public static void init() {
        CreativeTabRegistry.appendStack(ItemGroups.FUNCTIONAL, () -> new ItemStack(ModItems.TOWN_MANAGER.get()));
    }
}
