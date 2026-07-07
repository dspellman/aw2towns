package com.aw2towns;

import com.aw2towns.registry.ModBlocks;
import com.aw2towns.registry.ModCreativeTabs;
import com.aw2towns.registry.ModGameRules;
import com.aw2towns.registry.ModItems;
import com.aw2towns.registry.ModScreenHandlers;

public final class AW2TownsCommon {

    private static boolean initialized;

    private AW2TownsCommon() {}

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ModBlocks.init();
        ModItems.init();
        ModScreenHandlers.init();
        ModCreativeTabs.init();
        ModGameRules.init();

        AW2Towns.LOGGER.info("Initialized {}", AW2Towns.MOD_NAME);
    }
}
