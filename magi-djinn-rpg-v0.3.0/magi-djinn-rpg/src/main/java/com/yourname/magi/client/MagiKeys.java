package com.yourname.magi.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.yourname.magi.MagiMod;
import com.yourname.magi.djinn.DjinnDefinition;
import com.yourname.magi.magic.MagiArts;
import com.yourname.magi.networking.CastArtPacket;
import com.yourname.magi.networking.MagiNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Keys (rebindable under Controls > Magi): G cast selected art, H next art, J Djinn signature ability. */
public final class MagiKeys {
    private static final String CATEGORY = "key.categories.magi";
    public static final KeyMapping CAST = new KeyMapping("key.magi.cast_art", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping NEXT = new KeyMapping("key.magi.next_art", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping SIGNATURE = new KeyMapping("key.magi.signature", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);

    private static int selected;

    /** The equipped Djinn's castable arts, in JSON order. */
    public static List<ResourceLocation> arts() {
        DjinnDefinition def = equippedDefinition();
        if (def == null) return List.of();
        return def.abilities().stream().filter(id -> MagiArts.get(id) != null).toList();
    }

    @Nullable
    public static ResourceLocation selectedArt() {
        List<ResourceLocation> arts = arts();
        if (arts.isEmpty()) return null;
        return arts.get(Math.floorMod(selected, arts.size()));
    }

    @Nullable
    public static ResourceLocation signature() {
        DjinnDefinition def = equippedDefinition();
        return def == null ? null : def.signature().filter(id -> MagiArts.get(id) != null).orElse(null);
    }

    @Nullable
    public static DjinnDefinition equippedDefinition() {
        ResourceLocation eq = ClientDjinnState.DATA.getEquipped();
        return eq == null ? null : ClientDjinnDefinitions.get(eq);
    }

    @Mod.EventBusSubscriber(modid = MagiMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(CAST);
            event.register(NEXT);
            event.register(SIGNATURE);
        }
    }

    @Mod.EventBusSubscriber(modid = MagiMod.MODID, value = Dist.CLIENT)
    public static final class Handler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;
            while (NEXT.consumeClick()) selected++;
            while (CAST.consumeClick()) {
                ResourceLocation art = selectedArt();
                if (art != null) MagiNetwork.CHANNEL.sendToServer(new CastArtPacket(art));
            }
            while (SIGNATURE.consumeClick()) {
                ResourceLocation art = signature();
                if (art != null) MagiNetwork.CHANNEL.sendToServer(new CastArtPacket(art));
            }
        }
    }

    private MagiKeys() {}
}
