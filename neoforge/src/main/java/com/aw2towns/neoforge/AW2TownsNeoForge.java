package com.aw2towns.neoforge;

import com.aw2towns.AW2Towns;
import com.aw2towns.AW2TownsClient;
import com.aw2towns.AW2TownsCommon;
import com.aw2towns.client.gui.TownManagerScreen;
import com.aw2towns.economy.TownSimulationManager;
import com.aw2towns.registry.ModScreenHandlers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(AW2Towns.MOD_ID)
public final class AW2TownsNeoForge {

    public AW2TownsNeoForge(IEventBus modEventBus) {
        AW2TownsCommon.init();
        if (!FMLEnvironment.dist.isDedicatedServer()) {
            modEventBus.addListener(this::clientSetup);
            modEventBus.addListener(this::registerScreens);
        }
        NeoForge.EVENT_BUS.addListener(this::serverTick);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(AW2TownsClient::init);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModScreenHandlers.TOWN_MANAGER.get(), TownManagerScreen::new);
    }

    private void serverTick(ServerTickEvent.Post event) {
        TownSimulationManager.onServerTick(event.getServer());
    }
}
