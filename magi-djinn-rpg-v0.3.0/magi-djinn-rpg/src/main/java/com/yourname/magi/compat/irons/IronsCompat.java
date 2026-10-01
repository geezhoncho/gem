package com.yourname.magi.compat.irons;

import com.yourname.magi.compat.CompatManager;
import com.yourname.magi.compat.CompatModule;
import com.yourname.magi.magic.ManaProvider;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Iron's Spells integration: shared mana now; Iron's spell registration (scrolls/spellbooks) next round. */
public class IronsCompat implements CompatModule {
    @Override
    public String modId() {
        return CompatManager.IRONS_SPELLBOOKS;
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        ManaProvider.Registry.set(IronsMana.create());
    }
}
