package com.yourname.magi.networking;

import com.yourname.magi.client.ClientFx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C: one packet per visual effect; the client builds particles/flash/shake locally (cheap on bandwidth). */
public record ArtFxPacket(byte type, Vec3 a, Vec3 b, float p1, float p2, int entityId) {
    public static final byte BEAM = 0, SKY_FLASH = 1, SHAKE = 2, SHOCKWAVE = 3, CHARGE = 4, ARC = 5, RING = 6, MARK = 7;

    public static void encode(ArtFxPacket m, FriendlyByteBuf buf) {
        buf.writeByte(m.type);
        buf.writeDouble(m.a.x); buf.writeDouble(m.a.y); buf.writeDouble(m.a.z);
        buf.writeDouble(m.b.x); buf.writeDouble(m.b.y); buf.writeDouble(m.b.z);
        buf.writeFloat(m.p1); buf.writeFloat(m.p2);
        buf.writeVarInt(m.entityId);
    }

    public static ArtFxPacket decode(FriendlyByteBuf buf) {
        byte t = buf.readByte();
        Vec3 a = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 b = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new ArtFxPacket(t, a, b, buf.readFloat(), buf.readFloat(), buf.readVarInt());
    }

    public static void handle(ArtFxPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFx.handle(m)));
        c.setPacketHandled(true);
    }
}
