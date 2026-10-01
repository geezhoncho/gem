package com.yourname.magi.networking;

import com.yourname.magi.magic.ArtCaster;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: "cast this art". Everything (ownership, sword, level, cooldown, cost) is re-checked on the server. */
public record CastArtPacket(ResourceLocation art) {

    public static void encode(CastArtPacket m, FriendlyByteBuf buf) {
        buf.writeResourceLocation(m.art);
    }

    public static CastArtPacket decode(FriendlyByteBuf buf) {
        return new CastArtPacket(buf.readResourceLocation());
    }

    public static void handle(CastArtPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender != null) ArtCaster.tryCast(sender, m.art());
        });
        c.setPacketHandled(true);
    }
}
