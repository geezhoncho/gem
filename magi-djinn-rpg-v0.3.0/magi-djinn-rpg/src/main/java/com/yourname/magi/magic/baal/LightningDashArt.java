package com.yourname.magi.magic.baal;

import com.yourname.magi.MagiMod;
import com.yourname.magi.magic.*;
import com.yourname.magi.registry.MagiSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/** Baal's signature ability: become lightning for a moment, dash forward and shock everything you pass through. */
public class LightningDashArt extends DjinnArt {
    public LightningDashArt() { super(MagiMod.id("lightning_dash")); }

    @Override public String school() { return "lightning"; }
    @Override public int cooldownTicks() { return 50; }
    @Override public double energyCost() { return 20.0; }

    @Override
    public ArtSequence cast(ArtContext ctx) {
        ServerPlayer p = ctx.caster();
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, Mth.clamp(look.y, -0.1, 0.35), look.z).normalize();
        p.setDeltaMovement(dir.scale(2.2));
        p.hurtMarked = true;
        p.fallDistance = 0.0F;
        p.invulnerableTime = Math.max(p.invulnerableTime, 12);
        ArtFx.sound(ctx.level(), p.position(), MagiSounds.LIGHTNING_DASH.get(), 1.4F, 1.2F);
        return new Dash(ctx.level(), p, ctx.power());
    }

    private static final class Dash extends ArtSequence {
        private final Set<LivingEntity> hit = new HashSet<>();
        private final DamageSource source;
        private final float power;
        private Vec3 last;

        Dash(ServerLevel level, ServerPlayer caster, float power) {
            super(level, caster);
            this.power = power;
            this.last = caster.position().add(0, 1, 0);
            this.source = MagiDamage.lightning(level, caster);
        }

        @Override
        protected boolean onTick(int age) {
            if (casterGone() || age > 8) return false;
            caster.fallDistance = 0.0F;
            Vec3 now = caster.position().add(0, 1, 0);
            ArtFx.arc(level, last, now);
            for (LivingEntity e : MagiDamage.capsule(level, last, now, 1.6, caster)) {
                if (hit.add(e)) MagiDamage.hit(e, source, 6.0F * power, now, 0.6);
            }
            last = now;
            return true;
        }
    }
}
