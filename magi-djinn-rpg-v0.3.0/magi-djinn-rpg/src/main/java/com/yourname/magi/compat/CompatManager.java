package com.yourname.magi.compat;

import com.yourname.magi.MagiMod;
import com.yourname.magi.compat.apotheosis.ApotheosisCompat;
import com.yourname.magi.compat.epicfight.EpicFightCompat;
import com.yourname.magi.compat.irons.IronsCompat;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Loads optional integrations defensively: a module is created only if its mod is present, and any failure
 * (including LinkageError from an incompatible mod version) disables that module instead of crashing launch.
 * RULE: nothing outside compat.<mod> may reference that package's classes.
 */
public final class CompatManager {
    public static final String IRONS_SPELLBOOKS = "irons_spellbooks";
    public static final String EPIC_FIGHT = "epicfight";
    public static final String APOTHEOSIS = "apotheosis";

    private static final List<CompatModule> ACTIVE = new ArrayList<>();

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static boolean isActive(String modId) {
        return ACTIVE.stream().anyMatch(m -> m.modId().equals(modId));
    }

    public static void init(IEventBus modBus) {
        load(IRONS_SPELLBOOKS, IronsCompat::new, modBus);
        load(EPIC_FIGHT, EpicFightCompat::new, modBus);
        load(APOTHEOSIS, ApotheosisCompat::new, modBus);
        modBus.addListener((FMLCommonSetupEvent e) -> ACTIVE.forEach(m -> {
            try {
                m.onCommonSetup(e);
            } catch (Throwable t) {
                MagiMod.LOGGER.error("Compat module {} failed during common setup", m.modId(), t);
            }
        }));
    }

    private static void load(String modId, Supplier<CompatModule> factory, IEventBus modBus) {
        if (!isLoaded(modId)) {
            MagiMod.LOGGER.info("Optional mod '{}' not present; integration skipped", modId);
            return;
        }
        try {
            CompatModule module = factory.get();
            module.onLoad(modBus);
            ACTIVE.add(module);
            MagiMod.LOGGER.info("Integration enabled: {}", modId);
        } catch (Throwable t) {
            MagiMod.LOGGER.error("Integration with '{}' failed to load and was disabled", modId, t);
        }
    }

    private CompatManager() {}
}
