package com.yourname.magi.capability;

import com.yourname.magi.MagiMod;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** MOD-bus events. */
@Mod.EventBusSubscriber(modid = MagiMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CapabilityModEvents {

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PlayerDjinnData.class);
    }

    private CapabilityModEvents() {}
}
