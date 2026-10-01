package com.yourname.magi.djinn;

import com.yourname.magi.MagiMod;
import com.yourname.magi.networking.MagiNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MagiMod.MODID)
public final class DjinnEvents {

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new DjinnManager());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) DjinnEquipmentHandler.onLogin(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) DjinnEquipmentHandler.onLogout(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) DjinnEquipmentHandler.refresh(sp);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) DjinnEquipmentHandler.refresh(sp);
    }

    /**
     * Fires for a joining player (getPlayer() != null) and for everyone after /reload (getPlayer() == null).
     * Clients receive the Djinn definitions in both cases; after a reload, bonuses are re-applied too.
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer joining = event.getPlayer();
        if (joining != null) {
            MagiNetwork.sendDefinitions(joining);
            return;
        }
        for (ServerPlayer sp : event.getPlayerList().getPlayers()) {
            MagiNetwork.sendDefinitions(sp);
            DjinnEquipmentHandler.refresh(sp);
        }
    }

    private DjinnEvents() {}
}
