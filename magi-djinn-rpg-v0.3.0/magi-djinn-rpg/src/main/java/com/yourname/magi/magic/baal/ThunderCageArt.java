package com.yourname.magi.magic.baal;

import com.yourname.magi.MagiMod;
import com.yourname.magi.magic.*;
import com.yourname.magi.registry.MagiSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** A ring of lightning pillars: everything inside is shocked and slowed; anything trying to leave is thrown back. */
public class ThunderCageArt extends DjinnArt {
    private static final float RADIUS = 5.5F;
    private static final int DURATION = 100;

    public ThunderCageArt() { super(MagiMod.id("thunder_cage")); }

    @Override public String school() { return "lightning"; }
    @Override public int cooldownTicks() { return 360; }
    @Override public float manaCost() { return 80.0F; }

    @Override
    public ArtSequence cast(ArtContext ctx) {
        return new Cage(ctx.level(), ctx.caster(), ArtUtil.aimGround(ctx.caster(), 24), ctx.power());
    }

    private static final class Cage extends ArtSequence {
        private final Vec3 center;
        private final float power;
        private final DamageSource source;

        Cage(ServerLevel level, ServerPlayer caster, Vec3 center, float power) {
            super(level, caster);
            this.center = center;
            this.power = power;
            this.source = MagiDamage.lightning(level, caster);
        }

        private void pillars() {
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                ArtFx.bolt(level, ArtUtil.ground(level, center.x + Math.cos(a) * RADIUS, center.z + Math.sin(a) * RADIUS));
            }
        }

        @Override
        protected boolean onTick(int age) {
            Vec3 mid = center.add(0, 1.2, 0);
            if (age == 0) {
                pillars();
                ArtFx.ring(level, center.add(0, 0.15, 0), RADIUS, DURATION);
                ArtFx.sound(level, center, MagiSounds.THUNDER_CAGE.get(), 2.0F, 1.0F);
            }
            for (LivingEntity e : MagiDamage.sphere(level, mid, RADIUS + 1.5, caster)) {
                Vec3 off = e.position().subtract(center);
                double d = Math.sqrt(off.x * off.x + off.z * off.z);
                if (d > RADIUS - 0.8 && d > 0.01) {
                    e.setDeltaMovement(e.getDeltaMovement().add(-off.x / d * 0.3, 0.05, -off.z / d * 0.3));
                    e.hurtMarked = true;
                }
                if (age > 0 && age % 10 == 0) {
                    MagiDamage.hit(e, source, 3.0F * power, center, 0.0);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, true));
                }
            }
            if (age % 20 == 10) {
                double a = level.random.nextDouble() * Math.PI * 2;
                ArtFx.bolt(level, ArtUtil.ground(level, center.x + Math.cos(a) * RADIUS, center.z + Math.sin(a) * RADIUS));
            }
            if (age >= DURATION) {
                pillars();
                for (LivingEntity e : MagiDamage.sphere(level, mid, RADIUS + 0.5, caster)) {
                    MagiDamage.hit(e, source, 7.0F * power, center, 0.8);
                }
                ArtFx.shockwave(level, center.add(0, 0.5, 0), RADIUS + 2);
                return false;
            }
            return true;
        }
    }
}
