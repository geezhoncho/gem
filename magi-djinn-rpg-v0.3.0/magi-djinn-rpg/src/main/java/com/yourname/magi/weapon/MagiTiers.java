package com.yourname.magi.weapon;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;

/**
 * Weapon tiers. Repair ingredients are PLACEHOLDERS until the Magi materials (Dungeon Steel, Djinn Essence, ...)
 * exist. Elemental / Djinn / Legendary tiers arrive with their materials.
 */
public final class MagiTiers {
    public static final Tier IRON = Tiers.IRON;

    // ForgeTier(level, uses, speed, damage, enchantability, tag, repair). Verify signature on first build.
    public static final Tier STEEL = new ForgeTier(2, 700, 6.5F, 2.5F, 12,
            BlockTags.NEEDS_IRON_TOOL, () -> Ingredient.of(Items.IRON_INGOT));

    public static final Tier DUNGEON = new ForgeTier(3, 1300, 8.0F, 3.5F, 15,
            BlockTags.NEEDS_DIAMOND_TOOL, () -> Ingredient.of(Items.DIAMOND));

    private MagiTiers() {}
}
