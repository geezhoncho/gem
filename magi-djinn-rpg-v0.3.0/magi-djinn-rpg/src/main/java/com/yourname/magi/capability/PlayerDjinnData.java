package com.yourname.magi.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Per-player Djinn state. Server is authoritative; the client holds a read-only mirror
 * (see ClientDjinnState). Nothing here ticks: energy regenerates lazily from timestamps and
 * cooldowns are absolute game-time deadlines.
 */
public class PlayerDjinnData implements INBTSerializable<CompoundTag> {

    public static final class Progress {
        public int level = 1;
        public int xp = 0;
    }

    private final Map<ResourceLocation, Progress> owned = new LinkedHashMap<>();
    private final Map<ResourceLocation, Long> cooldownEnds = new HashMap<>();
    @Nullable
    private ResourceLocation equipped;
    private double energy;
    private long energyStamp;
    /** Health at logout, restored after login once Djinn max-health bonuses are re-applied. */
    private float storedHealth = -1.0F;
    /** Not saved: anti-spam guard for client equip requests. */
    private long lastEquipRequest = -1000L;

    // ---- ownership / equipment ----

    public boolean owns(ResourceLocation id) {
        return owned.containsKey(id);
    }

    /** @return true if newly unlocked, false if already owned (duplicate-progress guard). */
    public boolean unlock(ResourceLocation id) {
        if (owned.containsKey(id)) return false;
        owned.put(id, new Progress());
        return true;
    }

    public boolean equip(ResourceLocation id) {
        if (!owned.containsKey(id)) return false;
        equipped = id;
        return true;
    }

    public void unequip() {
        equipped = null;
    }

    /** @return true if the Djinn was owned and has been removed (unequipped if it was active). */
    public boolean revoke(ResourceLocation id) {
        if (owned.remove(id) == null) return false;
        if (id.equals(equipped)) equipped = null;
        return true;
    }

    public boolean setLevel(ResourceLocation id, int level, int maxLevel) {
        Progress p = owned.get(id);
        if (p == null) return false;
        p.level = Math.max(1, Math.min(level, maxLevel));
        p.xp = 0;
        return true;
    }

    public void storeHealth(float health) {
        storedHealth = health;
    }

    /** Returns the stored logout health (or -1) and clears it. */
    public float takeStoredHealth() {
        float h = storedHealth;
        storedHealth = -1.0F;
        return h;
    }

    public boolean tryEquipRequest(long now, long minIntervalTicks) {
        if (now - lastEquipRequest < minIntervalTicks) return false;
        lastEquipRequest = now;
        return true;
    }

    @Nullable
    public ResourceLocation getEquipped() {
        return equipped;
    }

    public Set<ResourceLocation> ownedIds() {
        return Collections.unmodifiableSet(owned.keySet());
    }

    // ---- progression ----

    public int level(ResourceLocation id) {
        Progress p = owned.get(id);
        return p == null ? 0 : p.level;
    }

    public int xp(ResourceLocation id) {
        Progress p = owned.get(id);
        return p == null ? 0 : p.xp;
    }

    public static int xpForNext(int level) {
        return 100 * level;
    }

    /** @return true if at least one level was gained. */
    public boolean addXp(ResourceLocation id, int amount, int maxLevel) {
        Progress p = owned.get(id);
        if (p == null || amount <= 0 || p.level >= maxLevel) return false;
        p.xp += amount;
        boolean leveled = false;
        while (p.level < maxLevel && p.xp >= xpForNext(p.level)) {
            p.xp -= xpForNext(p.level);
            p.level++;
            leveled = true;
        }
        if (p.level >= maxLevel) p.xp = 0;
        return leveled;
    }

    // ---- energy (lazy regeneration, zero ticking) ----

    public double currentEnergy(long now, double regenPerTick, double max) {
        double e = energy + Math.max(0L, now - energyStamp) * regenPerTick;
        return Math.min(max, e);
    }

    public boolean trySpendEnergy(long now, double regenPerTick, double max, double cost) {
        double e = currentEnergy(now, regenPerTick, max);
        if (e < cost) return false;
        energy = e - cost;
        energyStamp = now;
        return true;
    }

    // ---- cooldowns ----

    public void startCooldown(ResourceLocation abilityId, long endGameTime) {
        cooldownEnds.put(abilityId, endGameTime);
    }

    public long cooldownRemaining(ResourceLocation abilityId, long now) {
        Long end = cooldownEnds.get(abilityId);
        return end == null ? 0L : Math.max(0L, end - now);
    }

    // ---- persistence ----

    public void copyFrom(PlayerDjinnData other) {
        deserializeNBT(other.serializeNBT());
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        owned.forEach((id, p) -> {
            CompoundTag t = new CompoundTag();
            t.putString("Id", id.toString());
            t.putInt("Level", p.level);
            t.putInt("Xp", p.xp);
            list.add(t);
        });
        tag.put("Owned", list);
        if (equipped != null) tag.putString("Equipped", equipped.toString());
        tag.putDouble("Energy", energy);
        tag.putLong("EnergyStamp", energyStamp);
        CompoundTag cds = new CompoundTag();
        cooldownEnds.forEach((id, end) -> cds.putLong(id.toString(), end));
        tag.put("Cooldowns", cds);
        if (storedHealth > 0.0F) tag.putFloat("StoredHealth", storedHealth);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        owned.clear();
        cooldownEnds.clear();
        equipped = null;

        ListTag list = tag.getList("Owned", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(t.getString("Id"));
            if (id == null) continue;
            Progress p = new Progress();
            p.level = Math.max(1, t.getInt("Level"));
            p.xp = Math.max(0, t.getInt("Xp"));
            owned.put(id, p);
        }
        if (tag.contains("Equipped", Tag.TAG_STRING)) {
            ResourceLocation eq = ResourceLocation.tryParse(tag.getString("Equipped"));
            if (eq != null && owned.containsKey(eq)) equipped = eq;
        }
        energy = tag.getDouble("Energy");
        energyStamp = tag.getLong("EnergyStamp");
        storedHealth = tag.contains("StoredHealth", Tag.TAG_FLOAT) ? tag.getFloat("StoredHealth") : -1.0F;
        CompoundTag cds = tag.getCompound("Cooldowns");
        for (String key : cds.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) cooldownEnds.put(id, cds.getLong(key));
        }
    }
}
