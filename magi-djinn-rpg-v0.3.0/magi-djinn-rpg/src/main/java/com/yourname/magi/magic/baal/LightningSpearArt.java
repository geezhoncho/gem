package com.yourname.magi.magic.baal;

import com.yourname.magi.MagiMod;
import com.yourname.magi.magic.*;
import com.yourname.magi.registry.MagiSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Instant piercing javelin of lightning along the line of sight. */
public class LightningSpearArt extends DjinnArt {
    public LightningSpearArt() { super(MagiMod.id("lightning_spear")); }

    @Override public String school() { return "lightning"; }
    @Override public int cooldownTicks() { return 60; }
    @Override public float manaCost() { return 30.0F; }

    @Override
    public ArtSequence cast(ArtContext ctx) {
        ServerLevel level = ctx.level();
        ServerPlayer p = ctx.caster();
        Vec3 start = ArtUtil.hand(p);
        Vec3 end = ArtUtil.aimBlock(p, 34);
        DamageSource src = MagiDamage.lightning(level, p);
        for (LivingEntity t : MagiDamage.capsule(level, start, end, 1.0, p)) {
            MagiDamage.hit(t, src, 10.0F * ctx.power(), p.position(), 0.5);
        }
        ArtFx.beam(level, start, end, 0.35F, 8);
        ArtFx.shockwave(level, end, 2.0F);
        ArtFx.sound(level, start, MagiSounds.LIGHTNING_SPEAR.get(), 1.6F, 1.1F);
        return null;
    }
}
