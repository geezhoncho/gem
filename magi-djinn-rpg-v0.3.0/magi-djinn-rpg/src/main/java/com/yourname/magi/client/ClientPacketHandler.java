package com.yourname.magi.client;

import com.yourname.magi.djinn.DjinnDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Only ever invoked through DistExecutor on the physical client. */
public final class ClientPacketHandler {

    public static void handleSync(CompoundTag tag) {
        ClientDjinnState.DATA.deserializeNBT(tag);
        ClientDjinnState.regenPerTick = tag.getDouble("RegenPerTick");
        ClientDjinnState.maxEnergy = tag.getDouble("MaxEnergy");
    }

    public static void handleDefinitions(CompoundTag tag) {
        Map<ResourceLocation, DjinnDefinition> defs = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null) continue;
            DjinnDefinition.CODEC.parse(NbtOps.INSTANCE, tag.get(key)).result().ifPresent(def -> defs.put(id, def));
        }
        ClientDjinnDefinitions.set(defs);
    }

    private ClientPacketHandler() {}
}
