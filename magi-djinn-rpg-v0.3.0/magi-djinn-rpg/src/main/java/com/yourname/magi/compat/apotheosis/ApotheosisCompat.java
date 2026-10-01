package com.yourname.magi.compat.apotheosis;

import com.yourname.magi.compat.CompatManager;
import com.yourname.magi.compat.CompatModule;

/** Placeholder: Phase 7 - affix/loot integration. Only instantiated by CompatManager when the mod is present. */
public class ApotheosisCompat implements CompatModule {
    @Override
    public String modId() {
        return CompatManager.APOTHEOSIS;
    }
}
