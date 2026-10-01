package com.yourname.magi.client;

import com.yourname.magi.MagiMod;
import com.yourname.magi.client.render.SpectralBladeRenderer;
import com.yourname.magi.registry.MagiEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MagiMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRenderers {
    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MagiEntities.SPECTRAL_BLADE.get(), SpectralBladeRenderer::new);
    }

    private ClientRenderers() {}
}
