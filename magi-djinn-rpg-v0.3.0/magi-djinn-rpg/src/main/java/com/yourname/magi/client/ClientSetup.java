package com.yourname.magi.client;

import com.yourname.magi.MagiMod;
import com.yourname.magi.djinn.MagiDjinn;
import com.yourname.magi.item.DjinnCoreItem;
import com.yourname.magi.registry.MagiItems;
import com.yourname.magi.registry.MagiWeapons;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only item model predicates. */
@Mod.EventBusSubscriber(modid = MagiMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Bow draw animation (vanilla only registers these for Items.BOW)
            MagiWeapons.bows().forEach(obj -> {
                Item bow = obj.get();
                ItemProperties.register(bow, new ResourceLocation("pull"), (stack, level, entity, seed) -> {
                    if (entity == null || entity.getUseItem() != stack) return 0.0F;
                    return (float) (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0F;
                });
                ItemProperties.register(bow, new ResourceLocation("pulling"), (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            });
            // Djinn Core: picks the per-Djinn 3D model (0 = generic core, 1..6 = built-in Djinn)
            ItemProperties.register(MagiItems.DJINN_CORE.get(), MagiMod.id("djinn"), (stack, level, entity, seed) -> {
                ResourceLocation id = DjinnCoreItem.getDjinn(stack);
                return id == null ? 0.0F : MagiDjinn.BUILT_IN.indexOf(id) + 1.0F;
            });
        });
    }

    private ClientSetup() {}
}
