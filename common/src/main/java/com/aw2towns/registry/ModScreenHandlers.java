package com.aw2towns.registry;

import com.aw2towns.AW2Towns;
import com.aw2towns.screen.TownManagerScreenHandler;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.screen.ScreenHandlerType;

public final class ModScreenHandlers {

    public static final RegistrySupplier<ScreenHandlerType<TownManagerScreenHandler>> TOWN_MANAGER = AW2Towns.screenHandlers().register(
            AW2Towns.id("town_manager"),
            () -> MenuRegistry.ofExtended(TownManagerScreenHandler::new)
    );

    private ModScreenHandlers() {}

    public static void init() {}
}
