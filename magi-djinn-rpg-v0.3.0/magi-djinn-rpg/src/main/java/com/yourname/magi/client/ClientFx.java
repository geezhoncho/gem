package com.yourname.magi.client;

import com.yourname.magi.MagiMod;
import com.yourname.magi.networking.ArtFxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Client-only visuals for Djinn arts: particles, sky flash, screen flash and camera shake. */
@Mod.EventBusSubscriber(modid = MagiMod.MODID, value = Dist.CLIENT)
public final class ClientFx {
    private interface Effect { boolean tick(ClientLevel level, int age); }
    private record Running(Effect effect, int[] age) {}

    private static final List<Running> EFFECTS = new ArrayList<>();
    private static float flash, flashDecay;
    private static float shake;
    private static int shakeTicks;

    public static void handle(ArtFxPacket p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        switch (p.type()) {
            case ArtFxPacket.BEAM -> add((lvl, age) -> { beam(lvl, p.a(), p.b(), p.p1()); return age < (int) p.p2(); });
            case ArtFxPacket.ARC -> add((lvl, age) -> { jagged(lvl, p.a(), p.b(), 0.35, 4); return age < (int) p.p2(); });
            case ArtFxPacket.SKY_FLASH -> {
                double range = p.b().x;
                float f = p.p1() * (float) Mth.clamp(1.0 - mc.player.position().distanceTo(p.a()) / range, 0.25, 1.0);
                flash = Math.max(flash, f);
                flashDecay = flash / Math.max(1.0F, p.p2());
                level.setSkyFlashTime(Math.max(2, (int) p.p2() / 2));
            }
            case ArtFxPacket.SHAKE -> {
                double range = p.b().x;
                float f = p.p1() * (float) Mth.clamp(1.0 - mc.player.position().distanceTo(p.a()) / range, 0.0, 1.0);
                if (f > 0.02F) { shake = Math.max(shake, f); shakeTicks = Math.max(shakeTicks, (int) p.p2()); }
            }
            case ArtFxPacket.SHOCKWAVE -> shockwave(level, p.a(), p.p1());
            case ArtFxPacket.CHARGE -> add((lvl, age) -> {
                Entity e = lvl.getEntity(p.entityId());
                if (e == null) return false;
                charge(lvl, e, age);
                return age < (int) p.p2();
            });
            case ArtFxPacket.RING -> add((lvl, age) -> {
                if (age % 2 == 0) ring(lvl, p.a(), p.p1(), 48, ParticleTypes.ELECTRIC_SPARK);
                return age < (int) p.p2();
            });
            case ArtFxPacket.MARK -> add((lvl, age) -> {
                ring(lvl, p.a().add(0, 0.1, 0), p.p1() * (1.0F - age / Math.max(1.0F, p.p2())), 14, ParticleTypes.ELECTRIC_SPARK);
                if (age % 2 == 0) spawn(lvl, ParticleTypes.END_ROD, p.a().add(0, 0.2, 0), new Vec3(0, 0.25, 0));
                return age < (int) p.p2();
            });
            default -> MagiMod.LOGGER.debug("Unknown art fx {}", p.type());
        }
    }

    private static void add(Effect e) {
        if (EFFECTS.size() < 256) EFFECTS.add(new Running(e, new int[]{0}));
    }

    private static void spawn(ClientLevel level, ParticleOptions type, Vec3 at, Vec3 vel) {
        level.addAlwaysVisibleParticle(type, true, at.x, at.y, at.z, vel.x, vel.y, vel.z);
    }

    private static void beam(ClientLevel level, Vec3 a, Vec3 b, float width) {
        RandomSource r = level.random;
        Vec3 d = b.subtract(a);
        double len = d.length();
        int steps = (int) Math.min(220, len * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.add(d.scale(i / (double) steps));
            Vec3 off = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).scale(width * 0.45);
            spawn(level, ParticleTypes.ELECTRIC_SPARK, p.add(off), off.scale(0.05));
            if (i % 3 == 0) spawn(level, ParticleTypes.END_ROD, p, Vec3.ZERO);
        }
        if (width > 1.0F) {
            for (int k = 0; k < 2; k++) jagged(level, a, b, width * 0.5, (int) Math.max(6, len / 2));
            if (r.nextInt(3) == 0) spawn(level, ParticleTypes.FLASH, a.add(d.scale(r.nextDouble())), Vec3.ZERO);
        }
    }

    private static void jagged(ClientLevel level, Vec3 a, Vec3 b, double amplitude, int segments) {
        RandomSource r = level.random;
        Vec3 prev = a;
        for (int s = 1; s <= segments; s++) {
            Vec3 next = a.add(b.subtract(a).scale(s / (double) segments));
            if (s < segments) next = next.add(r.nextGaussian() * amplitude, r.nextGaussian() * amplitude, r.nextGaussian() * amplitude);
            Vec3 seg = next.subtract(prev);
            int n = (int) Math.max(2, seg.length() * 5);
            for (int i = 0; i < n; i++) spawn(level, ParticleTypes.ELECTRIC_SPARK, prev.add(seg.scale(i / (double) n)), Vec3.ZERO);
            prev = next;
        }
    }

    private static void ring(ClientLevel level, Vec3 c, float radius, int points, ParticleOptions type) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points + level.random.nextDouble() * 0.1;
            spawn(level, type, c.add(Math.cos(a) * radius, 0.05, Math.sin(a) * radius), new Vec3(0, 0.03, 0));
        }
    }

    private static void shockwave(ClientLevel level, Vec3 c, float radius) {
        spawn(level, ParticleTypes.EXPLOSION_EMITTER, c, Vec3.ZERO);
        int n = (int) Mth.clamp(radius * 10, 24, 200);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            Vec3 dir = new Vec3(Math.cos(a), 0.05, Math.sin(a));
            spawn(level, ParticleTypes.CLOUD, c.add(dir), dir.scale(radius * 0.06));
            spawn(level, ParticleTypes.ELECTRIC_SPARK, c.add(dir.scale(0.5)), dir.scale(radius * 0.09));
        }
    }

    private static void charge(ClientLevel level, Entity e, int age) {
        RandomSource r = level.random;
        Vec3 look = e.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        Vec3 sword = e.getEyePosition().add(look.scale(0.5)).add(right.scale(0.35)).add(0, -0.1, 0);
        for (int i = 0; i < 4; i++) {
            Vec3 tip = sword.add(0, 0.4 + r.nextDouble() * 1.2, 0);
            spawn(level, ParticleTypes.ELECTRIC_SPARK, tip, new Vec3(r.nextGaussian() * 0.08, r.nextGaussian() * 0.08, r.nextGaussian() * 0.08));
        }
        double a = age * 0.6;
        spawn(level, ParticleTypes.END_ROD, e.position().add(Math.cos(a) * 1.2, 0.2 + (age % 20) * 0.1, Math.sin(a) * 1.2), Vec3.ZERO);
        if (age % 6 == 0) jagged(level, sword.add(0, 6 + r.nextDouble() * 4, 0), sword.add(0, 1.2, 0), 0.5, 6);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) {
            if (mc.level == null) { EFFECTS.clear(); flash = 0; shakeTicks = 0; }
            return;
        }
        Iterator<Running> it = EFFECTS.iterator();
        while (it.hasNext()) {
            Running r = it.next();
            boolean keep = r.effect().tick(mc.level, r.age()[0]++);
            if (!keep) it.remove();
        }
        if (flash > 0) flash = Math.max(0, flash - flashDecay);
        if (shakeTicks > 0 && --shakeTicks == 0) shake = 0;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeTicks <= 0 || shake <= 0) return;
        RandomSource r = RandomSource.create();
        float s = shake * Math.min(1.0F, shakeTicks / 10.0F) * 2.2F;
        event.setPitch(event.getPitch() + (r.nextFloat() - 0.5F) * s);
        event.setYaw(event.getYaw() + (r.nextFloat() - 0.5F) * s);
        event.setRoll(event.getRoll() + (r.nextFloat() - 0.5F) * s * 0.6F);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (flash <= 0) return;
        float f = Math.min(1.0F, flash);
        event.setRed(Mth.lerp(f, event.getRed(), 0.80F));
        event.setGreen(Mth.lerp(f, event.getGreen(), 0.90F));
        event.setBlue(Mth.lerp(f, event.getBlue(), 1.00F));
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (flash <= 0.01F) return;
        int alpha = (int) (Mth.clamp(flash, 0, 1) * 150);
        int w = event.getWindow().getGuiScaledWidth(), h = event.getWindow().getGuiScaledHeight();
        event.getGuiGraphics().fill(0, 0, w, h, (alpha << 24) | 0xE6F2FF);
    }

    private ClientFx() {}
}
