package com.aw2towns.economy;

import java.util.Locale;
import net.minecraft.text.Text;

public enum ResourceType {
    WHEAT("wheat"),
    IRON("iron"),
    OAK_PLANKS("oak_planks"),
    PICKAXE("pickaxe"),
    AXE("axe"),
    HOE("hoe"),
    SWORD("sword");

    private final String id;

    ResourceType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Text displayName() {
        return Text.translatable("resource.aw2towns." + id);
    }

    public static ResourceType byId(String id) {
        for (ResourceType resource : values()) {
            if (resource.id.equals(id)) {
                return resource;
            }
        }
        return ResourceType.valueOf(id.toUpperCase(Locale.ROOT));
    }
}
