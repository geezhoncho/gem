package com.yourname.magi.armor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public class MagiArmorMaterial implements ArmorMaterial {
    // helmet, chestplate, leggings, boots (same base values as vanilla)
    private static final int[] BASE_DURABILITY = {11, 16, 15, 13};

    private final String name;
    private final int durabilityMultiplier;
    private final int[] defense; // helmet, chestplate, leggings, boots
    private final int enchantability;
    private final SoundEvent equipSound;
    private final Supplier<Ingredient> repair;
    private final float toughness;
    private final float knockbackResistance;

    /** @param name must be namespaced ("magi:desert_nomad"); it selects textures/models/armor/<path>_layer_N.png */
    public MagiArmorMaterial(String name, int durabilityMultiplier, int[] defense, int enchantability,
                             SoundEvent equipSound, Supplier<Ingredient> repair, float toughness, float knockbackResistance) {
        if (defense.length != 4) throw new IllegalArgumentException("defense must be {helmet, chest, legs, boots}");
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = defense;
        this.enchantability = enchantability;
        this.equipSound = equipSound;
        this.repair = repair;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
    }

    private static int index(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 0;
            case CHESTPLATE -> 1;
            case LEGGINGS -> 2;
            case BOOTS -> 3;
        };
    }

    @Override public int getDurabilityForType(ArmorItem.Type type) { return BASE_DURABILITY[index(type)] * durabilityMultiplier; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return defense[index(type)]; }
    @Override public int getEnchantmentValue() { return enchantability; }
    @Override public SoundEvent getEquipSound() { return equipSound; }
    @Override public Ingredient getRepairIngredient() { return repair.get(); }
    @Override public String getName() { return name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockbackResistance; }
}
