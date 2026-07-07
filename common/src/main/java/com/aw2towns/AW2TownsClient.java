package com.aw2towns;

import com.aw2towns.client.gui.TownManagerScreen;
import com.aw2towns.registry.ModScreenHandlers;
import dev.architectury.registry.menu.MenuRegistry;

public final class AW2TownsClient {

    private static boolean initialized;

    private AW2TownsClient() {}

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        MenuRegistry.registerScreenFactory(ModScreenHandlers.TOWN_MANAGER.get(), TownManagerScreen::new);
    }
}
