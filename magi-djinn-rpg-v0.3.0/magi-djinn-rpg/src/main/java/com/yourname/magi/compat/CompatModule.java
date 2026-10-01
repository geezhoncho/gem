package com.yourname.magi.compat;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * An optional integration. Implementations may import the target mod's classes, but must ONLY be
 * instantiated through CompatManager after ModList confirms the mod is present.
 */
public interface CompatModule {
    String modId();

    default void onLoad(IEventBus modBus) {}

    default void onCommonSetup(FMLCommonSetupEvent event) {}
}
