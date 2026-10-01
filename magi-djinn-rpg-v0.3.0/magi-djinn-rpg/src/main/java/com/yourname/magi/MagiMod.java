package com.yourname.magi;

import com.mojang.logging.LogUtils;
import com.yourname.magi.compat.CompatManager;
import com.yourname.magi.config.MagiConfig;
import com.yourname.magi.networking.MagiNetwork;
import com.yourname.magi.magic.MagiArts;
import com.yourname.magi.registry.MagiCreativeTabs;
import com.yourname.magi.registry.MagiEntities;
import com.yourname.magi.registry.MagiSounds;
import com.yourname.magi.registry.MagiItems;
import com.yourname.magi.registry.MagiWeapons;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MagiMod.MODID)
public class MagiMod {
    public static final String MODID = "magi";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MagiMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MagiConfig.SPEC);

        MagiWeapons.init(); // registers weapon items into MagiItems.ITEMS
        MagiItems.ITEMS.register(modBus);
        MagiCreativeTabs.TABS.register(modBus);
        MagiEntities.ENTITIES.register(modBus);
        MagiSounds.SOUNDS.register(modBus);
        MagiArts.init();

        modBus.addListener(this::commonSetup);

        // Must be last: optional modules may hook the mod bus.
        CompatManager.init(modBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(MagiNetwork::register);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
