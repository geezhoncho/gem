package com.yourname.magi.networking;

import com.yourname.magi.client.ClientPacketHandler;
import com.yourname.magi.djinn.DjinnDefinition;
import com.yourname.magi.djinn.DjinnManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C: all Djinn definitions (encoded with the same codec as the JSON), so clients can show names, elements, stats. */
public record SyncDjinnDefinitionsPacket(CompoundTag definitions) {

    public static SyncDjinnDefinitionsPacket fromServer() {
        CompoundTag tag = new CompoundTag();
        DjinnManager.entries().forEach((id, def) -> DjinnDefinition.CODEC.encodeStart(NbtOps.INSTANCE, def)
                .result().ifPresent(encoded -> tag.put(id.toString(), encoded)));
        return new SyncDjinnDefinitionsPacket(tag);
    }

    public static void encode(SyncDjinnDefinitionsPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.definitions);
    }

    public static SyncDjinnDefinitionsPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncDjinnDefinitionsPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncDjinnDefinitionsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleDefinitions(msg.definitions())));
        c.setPacketHandled(true);
    }
}
