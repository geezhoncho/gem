package com.yourname.magi.magic;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** A Djinn technique. Which Djinn knows which arts is data (the Djinn JSON); behaviour is code. */
public abstract class DjinnArt {
    private final ResourceLocation id;

    protected DjinnArt(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation id() { return id; }
    public String nameKey() { return "art." + id.getNamespace() + "." + id.getPath(); }
    public String descriptionKey() { return nameKey() + ".desc"; }

    /** Iron's school whose spell power boosts this art (e.g. "lightning"). */
    public abstract String school();
    public abstract int cooldownTicks();
    /** Paid from Iron's mana (or Djinn Energy when Iron's is absent). */
    public float manaCost() { return 0.0F; }
    /** Paid from Djinn Energy. */
    public double energyCost() { return 0.0; }
    public boolean requiresSword() { return false; }
    public int minDjinnLevel() { return 1; }
    /** Extreme Magic: needs a full Djinn Energy bar and drains it. */
    public boolean requiresFullEnergy() { return false; }

    /** Fire the art. Return a timeline for multi-tick arts, or null if it resolved instantly. */
    @Nullable
    public abstract ArtSequence cast(ArtContext ctx);
}
