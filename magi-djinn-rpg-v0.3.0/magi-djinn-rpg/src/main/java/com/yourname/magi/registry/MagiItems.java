package com.yourname.magi.registry;

import com.yourname.magi.MagiMod;
import com.yourname.magi.armor.ArmorBonus;
import com.yourname.magi.armor.MagiArmorItem;
import com.yourname.magi.item.DjinnCoreItem;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public final class MagiItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MagiMod.MODID);

    private static final List<ArmorSet> ARMOR_SETS = new ArrayList<>();

    public record ArmorSet(String name, RegistryObject<Item> helmet, RegistryObject<Item> chestplate,
                           RegistryObject<Item> leggings, RegistryObject<Item> boots) {
        public List<RegistryObject<Item>> all() {
            return List.of(helmet, chestplate, leggings, boots);
        }
    }

    public static final RegistryObject<Item> DJINN_CORE = ITEMS.register("djinn_core",
            () -> new DjinnCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    /** Not obtainable: only used to render Baal's Extreme Magic blade. */
    public static final RegistryObject<Item> SPECTRAL_GREATSWORD = ITEMS.register("spectral_greatsword",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final ArmorSet DESERT_NOMAD = armorSet("desert_nomad", MagiArmorMaterials.DESERT_NOMAD, List.of(
            ArmorBonus.of(() -> Attributes.MOVEMENT_SPEED, 0.0125, AttributeModifier.Operation.MULTIPLY_BASE),
            // Iron's attribute id is UNVERIFIED (see docs/DESIGN.md s.23); silently skipped if absent.
            ArmorBonus.byId("irons_spellbooks:max_mana", 10.0, AttributeModifier.Operation.ADDITION)));

    private static ArmorSet armorSet(String name, ArmorMaterial material, List<ArmorBonus> bonuses) {
        ArmorSet set = new ArmorSet(name,
                piece(name, material, ArmorItem.Type.HELMET, bonuses),
                piece(name, material, ArmorItem.Type.CHESTPLATE, bonuses),
                piece(name, material, ArmorItem.Type.LEGGINGS, bonuses),
                piece(name, material, ArmorItem.Type.BOOTS, bonuses));
        ARMOR_SETS.add(set);
        return set;
    }

    private static RegistryObject<Item> piece(String name, ArmorMaterial material, ArmorItem.Type type, List<ArmorBonus> bonuses) {
        return ITEMS.register(name + "_" + type.getName(),
                () -> new MagiArmorItem(material, type, new Item.Properties(), bonuses));
    }

    public static List<ArmorSet> armorSets() {
        return List.copyOf(ARMOR_SETS);
    }

    private MagiItems() {}
}
