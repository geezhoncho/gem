package com.yourname.magi.registry;

import com.yourname.magi.armor.MagiArmorMaterial;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** One line per armor set. Phase 7 grows this to 30 sets (and moves numbers to config/data). */
public final class MagiArmorMaterials {
    public static final MagiArmorMaterial DESERT_NOMAD = new MagiArmorMaterial(
            "magi:desert_nomad", 18, new int[]{2, 5, 4, 2}, 12,
            SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER), 0.0F, 0.0F);

    private MagiArmorMaterials() {}
}
