package com.aw2towns.registry;

import com.aw2towns.AW2Towns;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;

public final class ModItems {

    public static final RegistrySupplier<Item> TOWN_MANAGER = AW2Towns.items().register(
            AW2Towns.id("town_manager"),
            () -> new BlockItem(ModBlocks.TOWN_MANAGER.get(), new Item.Settings())
    );

    private ModItems() {}

    public static void init() {}
}
