package com.yourname.magi.compat.epicfight;

import com.yourname.magi.compat.CompatManager;
import com.yourname.magi.compat.CompatModule;

/** Placeholder: Phase 2 - weapon categories, styles, mob patches. Only instantiated by CompatManager when the mod is present. */
public class EpicFightCompat implements CompatModule {
    @Override
    public String modId() {
        return CompatManager.EPIC_FIGHT;
    }
}
