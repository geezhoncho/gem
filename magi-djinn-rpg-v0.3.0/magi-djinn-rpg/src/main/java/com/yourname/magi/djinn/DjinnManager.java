package com.yourname.magi.djinn;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.yourname.magi.MagiMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Server-side reload listener holding all Djinn definitions (data/<ns>/djinn/*.json).
 * The map is swapped atomically on reload; readers never see a half-built map.
 * NOTE: definitions are NOT yet synced to clients (needed for the Phase 8 GUI).
 */
public class DjinnManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Map<ResourceLocation, DjinnDefinition> definitions = Map.of();

    public DjinnManager() {
        super(GSON, "djinn");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        ImmutableMap.Builder<ResourceLocation, DjinnDefinition> builder = ImmutableMap.builder();
        objects.forEach((id, json) -> {
            try {
                DjinnDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(err -> MagiMod.LOGGER.error("Invalid Djinn definition {}: {}", id, err))
                        .ifPresent(def -> builder.put(id, def));
            } catch (Exception e) {
                MagiMod.LOGGER.error("Failed to parse Djinn definition {}", id, e);
            }
        });
        definitions = builder.build();
        MagiMod.LOGGER.info("Loaded {} Djinn definitions", definitions.size());
    }

    @Nullable
    public static DjinnDefinition get(ResourceLocation id) {
        return definitions.get(id);
    }

    public static Map<ResourceLocation, DjinnDefinition> entries() {
        return definitions;
    }

    public static Set<ResourceLocation> ids() {
        return definitions.keySet();
    }
}
