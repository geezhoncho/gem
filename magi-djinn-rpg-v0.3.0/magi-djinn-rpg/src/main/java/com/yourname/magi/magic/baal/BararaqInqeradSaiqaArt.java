package com.yourname.magi.magic.baal;

import com.yourname.magi.MagiMod;
import com.yourname.magi.config.MagiConfig;
import com.yourname.magi.entity.SpectralBladeEntity;
import com.yourname.magi.magic.*;
import com.yourname.magi.registry.MagiEntities;
import com.yourname.magi.registry.MagiSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Extreme Magic: Bararaq Inqerad-Saiqa, "Lightning Sword of Extinction".
 * A magic circle opens in the sky, a lightning storm rages over the target zone, and a colossal spectral blade in
 * the shape of Baal's sword falls from the circle to impale the ground, leaving a huge crater.
 * Every storm strike is marked on the ground 8 ticks before it lands; the impact zone is marked from the start.
 * Timeline: 0 circle + zone marker | 10-85 storm | 40-80 blade falls | 80 impact + crater | 90-175 aftershocks.
 */
public class BararaqInqeradSaiqaArt extends DjinnArt {
    private static final float ZONE = 13.0F;
    private static final float STORM_RADIUS = 18.0F;

    public BararaqInqeradSaiqaArt() { super(MagiMod.id("bararaq_inqerad_saiqa")); }

    @Override public String school() { return "lightning"; }
    @Override public int cooldownTicks() { return 6000; }
    @Override public float manaCost() { return 200.0F; }
    @Override public boolean requiresSword() { return true; }
    @Override public boolean requiresFullEnergy() { return true; }
    @Override public int minDjinnLevel() { return MagiConfig.EXTREME_MAGIC_MIN_LEVEL.get(); }

    @Override
    public ArtSequence cast(ArtContext ctx) {
        return new Extreme(ctx.level(), ctx.caster(), ArtUtil.aimGround(ctx.caster(), 64), ctx.power());
    }

    private record Strike(Vec3 pos, int at) {}

    private static final class Extreme extends ArtSequence {
        private final Vec3 center;
        private final float power;
        private final DamageSource source;
        private final List<Strike> strikes = new ArrayList<>();

        Extreme(ServerLevel level, ServerPlayer caster, Vec3 center, float power) {
            super(level, caster);
            this.center = center;
            this.power = power;
            this.source = MagiDamage.lightning(level, caster);
        }

        @Override public boolean blocksCasting() { return age < 90; }

        @Override
        protected boolean onTick(int age) {
            if (age == 0) begin();
            if (age >= 10 && age <= 85 && age % 3 == 0) {
                double a = level.random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(level.random.nextDouble()) * STORM_RADIUS;
                Vec3 p = ArtUtil.ground(level, center.x + Math.cos(a) * r, center.z + Math.sin(a) * r);
                ArtFx.mark(level, p, 2.5F, 8);
                strikes.add(new Strike(p, age + 8));
            }
            Iterator<Strike> it = strikes.iterator();
            while (it.hasNext()) {
                Strike s = it.next();
                if (s.at() > age) continue;
                ArtFx.bolt(level, s.pos());
                for (LivingEntity e : MagiDamage.sphere(level, s.pos().add(0, 1, 0), 2.5, caster)) {
                    MagiDamage.hit(e, source, 7.0F * power, s.pos(), 0.4);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, true));
                }
                it.remove();
            }
            if (age == SpectralBladeEntity.IMPACT) impact();
            if (age > 90 && age < SpectralBladeEntity.FADE_START && age % 15 == 0) {
                Vec3 p = ArtUtil.ground(level, center.x + level.random.nextGaussian() * 3, center.z + level.random.nextGaussian() * 3);
                ArtFx.bolt(level, p);
                for (LivingEntity e : MagiDamage.sphere(level, p.add(0, 1, 0), 5.0, caster)) {
                    MagiDamage.hit(e, source, 6.0F * power, p, 0.6);
                }
            }
            return age < SpectralBladeEntity.LIFETIME && !(age > SpectralBladeEntity.IMPACT && strikes.isEmpty() && age >= SpectralBladeEntity.FADE_START);
        }

        private void begin() {
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 4, false, false));
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 90, 2, false, false));
            ArtFx.charge(level, caster, 80);
            SpectralBladeEntity blade = MagiEntities.SPECTRAL_BLADE.get().create(level);
            if (blade != null) {
                blade.moveTo(center.x, center.y, center.z, caster.getYRot(), 0.0F);
                level.addFreshEntity(blade);
            }
            ArtFx.ring(level, center.add(0, 0.2, 0), ZONE, SpectralBladeEntity.IMPACT);
            ArtFx.skyFlash(level, center, 256, 0.35F, 20);
            ArtFx.sound(level, caster.position(), MagiSounds.EXTREME_CAST.get(), 6.0F, 0.8F);
            ArtFx.bolt(level, caster.position());
        }

        private void impact() {
            for (LivingEntity e : MagiDamage.sphere(level, center.add(0, 1, 0), ZONE, caster)) {
                double d = e.position().distanceTo(center);
                float falloff = (float) Math.max(0.4, 1.0 - d / (ZONE + 1));
                MagiDamage.hit(e, source, 48.0F * power * falloff, center, 2.2);
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.7, 0));
                e.hurtMarked = true;
            }
            for (int i = 0; i < 10; i++) {
                double a = Math.PI * 2 * i / 10;
                ArtFx.bolt(level, ArtUtil.ground(level, center.x + Math.cos(a) * ZONE, center.z + Math.sin(a) * ZONE));
            }
            ArtFx.bolt(level, center);
            ArtFx.shockwave(level, center.add(0, 0.5, 0), ZONE + 6);
            ArtFx.skyFlash(level, center, 512, 1.0F, 40);
            ArtFx.shake(level, center, 96, 1.0F, 30);
            ArtFx.sound(level, center, MagiSounds.EXTREME_IMPACT.get(), 10.0F, 0.7F);
            int radius = MagiConfig.CRATER_RADIUS.get();
            if (radius > 0 && MagiConfig.ALLOW_BLOCK_DAMAGE.get()) {
                ArtScheduler.add(new CraterCarver(level, caster, BlockPos.containing(center), radius));
            }
        }
    }
}
