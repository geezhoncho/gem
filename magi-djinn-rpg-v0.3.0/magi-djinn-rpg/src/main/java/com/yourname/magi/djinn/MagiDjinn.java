package com.yourname.magi.djinn;

import com.yourname.magi.MagiMod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Ids of Djinn shipped with the mod (used for creative-tab display only; behaviour is data-driven). */
public final class MagiDjinn {
    public static final ResourceLocation BAAL = MagiMod.id("baal");
    public static final ResourceLocation LERAJE = MagiMod.id("leraje");
    public static final ResourceLocation CERBERUS = MagiMod.id("cerberus");
    public static final ResourceLocation ZAGAN = MagiMod.id("zagan");
    public static final ResourceLocation AMON = MagiMod.id("amon");
    public static final ResourceLocation TALMIR = MagiMod.id("talmir");

    public static final List<ResourceLocation> BUILT_IN = List.of(BAAL, LERAJE, CERBERUS, ZAGAN, AMON, TALMIR);

    /** Translation key: djinn.<namespace>.<path> */
    public static String nameKey(ResourceLocation id) {
        return "djinn." + id.getNamespace() + "." + id.getPath();
    }

    private MagiDjinn() {}
}
