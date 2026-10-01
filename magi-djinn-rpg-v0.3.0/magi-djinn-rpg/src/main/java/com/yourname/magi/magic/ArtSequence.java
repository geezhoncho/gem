package com.yourname.magi.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** A server-side timeline (charge, telegraph, release, aftermath). Ticked by ArtScheduler. */
public abstract class ArtSequence {
    protected final ServerLevel level;
    protected final ServerPlayer caster;
    protected int age;

    protected ArtSequence(ServerLevel level, ServerPlayer caster) {
        this.level = level;
        this.caster = caster;
    }

    /** @return true while the sequence should keep running. */
    final boolean tick() {
        boolean keep = onTick(age);
        age++;
        return keep;
    }

    protected abstract boolean onTick(int age);

    /** Channelled sequences stop the caster from starting another art. */
    public boolean blocksCasting() {
        return false;
    }

    public ServerPlayer caster() {
        return caster;
    }

    protected boolean casterGone() {
        return caster.isRemoved() || !caster.isAlive();
    }
}
