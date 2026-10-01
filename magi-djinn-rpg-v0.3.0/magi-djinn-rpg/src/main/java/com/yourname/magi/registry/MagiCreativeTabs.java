package com.yourname.magi.registry;

import com.yourname.magi.MagiMod;
import com.yourname.magi.djinn.MagiDjinn;
import com.yourname.magi.item.DjinnCoreItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class MagiCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagiMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.magi.main"))
            .icon(() -> new ItemStack(MagiItems.DJINN_CORE.get()))
            .displayItems((params, output) -> {
                MagiDjinn.BUILT_IN.forEach(id -> output.accept(DjinnCoreItem.forDjinn(MagiItems.DJINN_CORE.get(), id)));
                MagiItems.armorSets().forEach(set -> set.all().forEach(o -> output.accept(o.get())));
                MagiWeapons.entries().forEach(e -> output.accept(e.item().get()));
            })
            .build());

    private MagiCreativeTabs() {}
}
