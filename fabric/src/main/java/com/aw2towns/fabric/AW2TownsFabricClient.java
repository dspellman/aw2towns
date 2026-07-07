package com.aw2towns.fabric;

import com.aw2towns.AW2TownsClient;
import net.fabricmc.api.ClientModInitializer;

public final class AW2TownsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AW2TownsClient.init();
    }
}
