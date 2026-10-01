package com.yourname.magi.magic.baal;

import com.yourname.magi.MagiMod;
import com.yourname.magi.magic.*;
import com.yourname.magi.registry.MagiSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/** Lightning that leaps from the target to nearby enemies, a little weaker each jump. */
public class ChainLightningArt extends DjinnArt {
    public ChainLightningArt() { super(MagiMod.id("chain_lightning")); }

    @Override public String school() { return "lightning"; }
    @Override public int cooldownTicks() { return 120; }
    @Override public float manaCost() { return 55.0F; }

    @Override
    public ArtSequence cast(ArtContext ctx) {
        ServerLevel level = ctx.level();
        ServerPlayer p = ctx.caster();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = ArtUtil.aimBlock(p, 24);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, p, eye, end,
                p.getBoundingBox().expandTowards(look.scale(24)).inflate(1.5),
                e -> e instanceof LivingEntity le && MagiDamage.canHit(le, p));
        LivingEntity first = hit != null ? (LivingEntity) hit.getEntity() : nearest(level, end, 6.0, p, Set.of());
        ArtFx.sound(level, eye, MagiSounds.CHAIN_LIGHTNING.get(), 1.5F, 1.0F);
        if (first == null) {
            ArtFx.arc(level, ArtUtil.hand(p), end);
            return null;
        }
        return new Chain(level, p, first, ctx.power(), 4 + ctx.djinnLevel() / 3);
    }

    @Nullable
    static LivingEntity nearest(ServerLevel level, Vec3 at, double radius, ServerPlayer caster, Set<LivingEntity> exclude) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : MagiDamage.sphere(level, at, radius, caster)) {
            if (exclude.contains(e)) continue;
            double d = e.position().distanceToSqr(at);
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }

    private static final class Chain extends ArtSequence {
        private final Set<LivingEntity> hit = new HashSet<>();
        private final DamageSource source;
        private final int maxJumps;
        private LivingEntity current;
        private Vec3 from;
        private float damage;
        private int jumps;

        Chain(ServerLevel level, ServerPlayer caster, LivingEntity first, float power, int maxJumps) {
            super(level, caster);
            this.current = first;
            this.from = ArtUtil.hand(caster);
            this.damage = 8.0F * power;
            this.maxJumps = maxJumps;
            this.source = MagiDamage.lightning(level, caster);
        }

        @Override
        protected boolean onTick(int age) {
            if (age % 3 != 0) return true;
            if (current == null || !current.isAlive() || jumps > maxJumps) return false;
            Vec3 to = current.getBoundingBox().getCenter();
            ArtFx.arc(level, from, to);
            MagiDamage.hit(current, source, damage, from, 0.3);
            ArtFx.sound(level, to, MagiSounds.CHAIN_LIGHTNING.get(), 0.8F, 1.3F + jumps * 0.08F);
            hit.add(current);
            from = to;
            damage *= 0.82F;
            jumps++;
            current = nearest(level, to, 8.0, caster, hit);
            return true;
        }
    }
}
