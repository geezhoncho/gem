package com.yourname.magi.magic;

import com.yourname.magi.magic.baal.BararaqInqeradSaiqaArt;
import com.yourname.magi.magic.baal.BararaqSaiqaArt;
import com.yourname.magi.magic.baal.ChainLightningArt;
import com.yourname.magi.magic.baal.LightningDashArt;
import com.yourname.magi.magic.baal.LightningSpearArt;
import com.yourname.magi.magic.baal.ThunderCageArt;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/** All art behaviours by id. Djinn JSON files decide which Djinn owns which arts. */
public final class MagiArts {
    private static final Map<ResourceLocation, DjinnArt> ARTS = new LinkedHashMap<>();

    public static final DjinnArt LIGHTNING_SPEAR = register(new LightningSpearArt());
    public static final DjinnArt CHAIN_LIGHTNING = register(new ChainLightningArt());
    public static final DjinnArt THUNDER_CAGE = register(new ThunderCageArt());
    public static final DjinnArt BARARAQ_SAIQA = register(new BararaqSaiqaArt());
    public static final DjinnArt BARARAQ_INQERAD_SAIQA = register(new BararaqInqeradSaiqaArt());
    public static final DjinnArt LIGHTNING_DASH = register(new LightningDashArt());

    private static DjinnArt register(DjinnArt art) {
        ARTS.put(art.id(), art);
        return art;
    }

    @Nullable
    public static DjinnArt get(ResourceLocation id) {
        return ARTS.get(id);
    }

    /** Forces class loading from the mod constructor. */
    public static void init() {}

    private MagiArts() {}
}
