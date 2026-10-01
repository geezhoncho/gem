package com.yourname.magi.magic;

import com.yourname.magi.MagiMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Damage type + area queries shared by all arts (and later by bosses). */
public final class MagiDamage {
    public static final ResourceKey<DamageType> DJINN_LIGHTNING =
            ResourceKey.create(Registries.DAMAGE_TYPE, MagiMod.id("djinn_lightning"));

    public static DamageSource lightning(ServerLevel level, Entity caster) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DJINN_LIGHTNING), caster, caster);
    }

    /** Never the caster, its allies/team, or its own pets. PvP rules are applied by vanilla in Player#hurt. */
    public static boolean canHit(LivingEntity target, Entity caster) {
        if (target == caster || !target.isAlive() || target.isSpectator()) return false;
        if (target.isAlliedTo(caster)) return false;
        return !(target instanceof TamableAnimal tame && caster instanceof LivingEntity owner && tame.isOwnedBy(owner));
    }

    public static List<LivingEntity> sphere(ServerLevel level, Vec3 center, double radius, Entity caster) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius + 2),
                e -> canHit(e, caster) && e.getBoundingBox().getCenter().distanceTo(center) <= radius + e.getBbWidth() * 0.5);
    }

    public static List<LivingEntity> capsule(ServerLevel level, Vec3 a, Vec3 b, double radius, Entity caster) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(a, b).inflate(radius + 2),
                e -> canHit(e, caster) && distToSegment(e.getBoundingBox().getCenter(), a, b) <= radius + e.getBbWidth() * 0.5);
    }

    public static double distToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        double t = len2 < 1.0E-6 ? 0.0 : Mth.clamp(p.subtract(a).dot(ab) / len2, 0.0, 1.0);
        return p.distanceTo(a.add(ab.scale(t)));
    }

    /** Hurts and knocks the target away from 'from'. */
    public static void hit(LivingEntity target, DamageSource source, float amount, Vec3 from, double knockback) {
        if (target.hurt(source, amount) && knockback > 0) {
            Vec3 d = target.position().subtract(from);
            if (d.horizontalDistanceSqr() > 1.0E-4) target.knockback(knockback, -d.x, -d.z);
        }
    }

    private MagiDamage() {}
}
