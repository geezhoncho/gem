package com.yourname.magi.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class ArtUtil {

    /** Roughly where the sword hand is. */
    public static Vec3 hand(ServerPlayer p) {
        return p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, -0.35, 0);
    }

    /** First block hit along the look direction (or the point at max range). */
    public static Vec3 aimBlock(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        return p.level().clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getLocation();
    }

    /** Top of the terrain column at x/z. */
    public static Vec3 ground(ServerLevel level, double x, double z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
        return new Vec3(x, y, z);
    }

    public static Vec3 aimGround(ServerPlayer p, double range) {
        Vec3 hit = aimBlock(p, range);
        return ground(p.serverLevel(), hit.x, hit.z);
    }

    private ArtUtil() {}
}
