package com.yourname.magi.client;

import com.yourname.magi.djinn.DjinnDefinition;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** Client copy of the server's Djinn definitions (empty on a dedicated server; never used for authority). */
public final class ClientDjinnDefinitions {
    private static volatile Map<ResourceLocation, DjinnDefinition> definitions = Map.of();

    public static void set(Map<ResourceLocation, DjinnDefinition> defs) {
        definitions = Map.copyOf(defs);
    }

    @Nullable
    public static DjinnDefinition get(ResourceLocation id) {
        return definitions.get(id);
    }

    private ClientDjinnDefinitions() {}
}
