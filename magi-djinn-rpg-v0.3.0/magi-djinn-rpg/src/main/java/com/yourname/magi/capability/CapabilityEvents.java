package com.yourname.magi.capability;

import com.yourname.magi.MagiMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** FORGE-bus events. */
@Mod.EventBusSubscriber(modid = MagiMod.MODID)
public final class CapabilityEvents {

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerDjinnProvider provider = new PlayerDjinnProvider();
            event.addCapability(MagiMod.id("djinn_data"), provider);
            event.addListener(provider::invalidate);
        }
    }

    /** Fires on death-respawn and on returning from the End. Djinn progression always persists. */
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        MagiCapabilities.get(event.getOriginal()).ifPresent(old ->
                MagiCapabilities.get(event.getEntity()).ifPresent(fresh -> fresh.copyFrom(old)));
        event.getOriginal().invalidateCaps();
    }

    private CapabilityEvents() {}
}
