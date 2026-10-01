package com.yourname.magi.magic;

import com.yourname.magi.networking.ArtFxPacket;
import com.yourname.magi.networking.MagiNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/** Server-side helpers that trigger client visuals. Nothing here affects gameplay. */
public final class ArtFx {

    private static void send(ServerLevel level, Vec3 at, double range, ArtFxPacket pkt) {
        MagiNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() ->
                new PacketDistributor.TargetPoint(at.x, at.y, at.z, range, level.dimension())), pkt);
    }

    public static void beam(ServerLevel level, Vec3 from, Vec3 to, float width, int ticks) {
        send(level, from.add(to).scale(0.5), 192, new ArtFxPacket(ArtFxPacket.BEAM, from, to, width, ticks, -1));
    }

    public static void skyFlash(ServerLevel level, Vec3 at, double range, float intensity, int ticks) {
        send(level, at, range, new ArtFxPacket(ArtFxPacket.SKY_FLASH, at, new Vec3(range, 0, 0), intensity, ticks, -1));
    }

    public static void shake(ServerLevel level, Vec3 at, double range, float strength, int ticks) {
        send(level, at, range, new ArtFxPacket(ArtFxPacket.SHAKE, at, new Vec3(range, 0, 0), strength, ticks, -1));
    }

    public static void shockwave(ServerLevel level, Vec3 at, float radius) {
        send(level, at, 160, new ArtFxPacket(ArtFxPacket.SHOCKWAVE, at, at, radius, 0, -1));
    }

    public static void charge(ServerLevel level, Entity entity, int ticks) {
        send(level, entity.position(), 128, new ArtFxPacket(ArtFxPacket.CHARGE, entity.position(), entity.position(), 0, ticks, entity.getId()));
    }

    public static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        send(level, from, 128, new ArtFxPacket(ArtFxPacket.ARC, from, to, 0, 3, -1));
    }

    public static void ring(ServerLevel level, Vec3 center, float radius, int ticks) {
        send(level, center, 160, new ArtFxPacket(ArtFxPacket.RING, center, center, radius, ticks, -1));
    }

    /** Ground warning a few ticks before a strike lands there (readable attacks). */
    public static void mark(ServerLevel level, Vec3 at, float radius, int ticks) {
        send(level, at, 160, new ArtFxPacket(ArtFxPacket.MARK, at, at, radius, ticks, -1));
    }

    /** Vanilla lightning visuals and thunder, but no fire, no damage, no mob conversion. */
    public static void bolt(ServerLevel level, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    public static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private ArtFx() {}
}
