package com.aw2towns.registry;

import net.minecraft.world.GameRules;

public final class ModGameRules {

    public static final int DEFAULT_TEST_DAY_SECONDS = 10;
    public static final int DEFAULT_TEST_NIGHT_SECONDS = 10;
    public static final int REAL_DAY_SECONDS = 600;
    public static final int REAL_NIGHT_SECONDS = 600;

    public static final GameRules.Key<GameRules.IntRule> TOWN_SIMULATION_DAY_SECONDS = GameRules.register(
            "aw2townsSimulationDaySeconds",
            GameRules.Category.MISC,
            GameRules.IntRule.create(DEFAULT_TEST_DAY_SECONDS));

    public static final GameRules.Key<GameRules.IntRule> TOWN_SIMULATION_NIGHT_SECONDS = GameRules.register(
            "aw2townsSimulationNightSeconds",
            GameRules.Category.MISC,
            GameRules.IntRule.create(DEFAULT_TEST_NIGHT_SECONDS));

    private ModGameRules() {}

    public static void init() {}
}
