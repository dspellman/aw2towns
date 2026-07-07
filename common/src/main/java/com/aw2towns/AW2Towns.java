package com.aw2towns;

import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AW2Towns {

    public static final String MOD_ID = "aw2towns";
    public static final String MOD_NAME = "AW2 Town Prototype";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static RegistrarManager registrarManager;

    private AW2Towns() {}

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static RegistrarManager registries() {
        if (registrarManager == null) {
            registrarManager = RegistrarManager.get(MOD_ID);
        }
        return registrarManager;
    }

    public static Registrar<Block> blocks() {
        return registries().get(RegistryKeys.BLOCK);
    }

    public static Registrar<Item> items() {
        return registries().get(RegistryKeys.ITEM);
    }

    public static Registrar<ScreenHandlerType<?>> screenHandlers() {
        return registries().get(RegistryKeys.SCREEN_HANDLER);
    }
}
