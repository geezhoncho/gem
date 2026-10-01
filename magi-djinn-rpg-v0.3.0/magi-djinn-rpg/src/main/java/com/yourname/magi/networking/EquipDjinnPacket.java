package com.yourname.magi.networking;

import com.yourname.magi.capability.MagiCapabilities;
import com.yourname.magi.djinn.DjinnEquipmentHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** C2S: request to equip a Djinn (id == null means unequip). The server validates ownership. */
public record EquipDjinnPacket(@Nullable ResourceLocation id) {

    public static void encode(EquipDjinnPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.id != null);
        if (msg.id != null) buf.writeResourceLocation(msg.id);
    }

    public static EquipDjinnPacket decode(FriendlyByteBuf buf) {
        return new EquipDjinnPacket(buf.readBoolean() ? buf.readResourceLocation() : null);
    }

    public static void handle(EquipDjinnPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sender = c.getSender();
            if (sender == null) return;
            long now = sender.level().getGameTime();
            boolean allowed = MagiCapabilities.get(sender).map(d -> d.tryEquipRequest(now, 10L)).orElse(false);
            if (allowed) DjinnEquipmentHandler.equip(sender, msg.id());
        });
        c.setPacketHandled(true);
    }
}
