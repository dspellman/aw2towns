package com.aw2towns.economy;

import java.util.Locale;
import net.minecraft.text.Text;

public enum WorkstationType {
    FARM("farm"),
    MINE("mine"),
    LUMBER_MILL("lumber_mill"),
    BLACKSMITH("blacksmith");

    private final String id;

    WorkstationType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Text displayName() {
        return Text.translatable("workstation.aw2towns." + id);
    }

    public static WorkstationType byId(String id) {
        for (WorkstationType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return WorkstationType.valueOf(id.toUpperCase(Locale.ROOT));
    }
}
