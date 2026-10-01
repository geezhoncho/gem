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

/**
 * Bararaq Saiqa: call lightning down from the sky into your sword, hold it, then release it as an enormous
 * blast that lights up the whole sky. Requires Baal and a sword in the main hand.
 * Timeline: 0 charge starts | 4 the sky strikes your blade | 4-30 charging (slowed, visible to everyone) | 30 release.
 */
public class BararaqSaiqaArt extends DjinnArt {
    static final int RELEASE = 30;

    public BararaqSaiqaArt() { super(MagiMod.id("bararaq_saiqa")); }

    @Override public String school() { return "lightning"; }
    @Override public int cooldownTicks() { return 900; }
    @Override public float manaCost() { return 120.0F; }
    @Override public boolean requiresSword() { return true; }

    @Override
    public ArtSequence cast(ArtContext ctx) {
        return new Saiqa(ctx.level(), ctx.caster(), ctx.power());
    }

    private static final class Saiqa extends ArtSequence {
        private final float power;

        Saiqa(ServerLevel level, ServerPlayer caster, float power) {
            super(level, caster);
            this.power = power;
        }

        @Override public boolean blocksCasting() { return age <= RELEASE; }

        @Override
        protected boolean onTick(int age) {
            if (casterGone()) return false;
            if (age == 0) {
                caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, RELEASE + 4, 2, false, false));
                caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, RELEASE + 4, 0, false, false));
                ArtFx.charge(level, caster, RELEASE);
                ArtFx.ring(level, caster.position().add(0, 3.6, 0), 2.4F, RELEASE);
                ArtFx.sound(level, caster.position(), MagiSounds.SAIQA_CHARGE.get(), 2.0F, 1.0F);
            }
            if (age == 4) ArtFx.bolt(level, caster.position());           // the sky strikes the sword
            if (age == 18) ArtFx.bolt(level, caster.position());
            if (age < RELEASE) return true;

            Vec3 start = ArtUtil.hand(caster);
            Vec3 end = ArtUtil.aimBlock(caster, 56);
            DamageSource src = MagiDamage.lightning(level, caster);
            for (LivingEntity t : MagiDamage.capsule(level, start, end, 3.0, caster)) {
                MagiDamage.hit(t, src, 26.0F * power, caster.position(), 1.4);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3, false, true));
            }
            Vec3 dir = end.subtract(start);
            for (int i = 1; i <= 4; i++) ArtFx.bolt(level, start.add(dir.scale(i / 4.0)));
            ArtFx.beam(level, start, end, 2.8F, 16);
            ArtFx.shockwave(level, end, 6.0F);
            ArtFx.skyFlash(level, start, 256, 0.85F, 24);
            ArtFx.shake(level, start, 64, 0.6F, 14);
            ArtFx.sound(level, start, MagiSounds.SAIQA_RELEASE.get(), 5.0F, 0.9F);
            return false;
        }
    }
}
