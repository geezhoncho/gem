package com.yourname.magi.networking;

import com.yourname.magi.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C: full snapshot of the player's Djinn data. Sent only on state change. */
public record SyncDjinnDataPacket(CompoundTag data) {

    public static void encode(SyncDjinnDataPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static SyncDjinnDataPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncDjinnDataPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncDjinnDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleSync(msg.data())));
        c.setPacketHandled(true);
    }
}
