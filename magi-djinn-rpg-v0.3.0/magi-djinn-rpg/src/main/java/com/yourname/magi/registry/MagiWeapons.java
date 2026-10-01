package com.yourname.magi.registry;

import com.yourname.magi.weapon.MagiAxeItem;
import com.yourname.magi.weapon.MagiBowItem;
import com.yourname.magi.weapon.MagiSwordItem;
import com.yourname.magi.weapon.MagiTiers;
import com.yourname.magi.weapon.WeaponType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Registers tier x archetype (iron/steel/dungeon x scimitar/saber/dagger/spear/axe/bow = 18 items). */
public final class MagiWeapons {
    public record Entry(String tierName, WeaponType type, RegistryObject<Item> item) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();

    static {
        register("iron", MagiTiers.IRON, 0.0);
        register("steel", MagiTiers.STEEL, 0.5);
        register("dungeon", MagiTiers.DUNGEON, 1.0);
    }

    private static void register(String tierName, Tier tier, double bowBonus) {
        for (WeaponType type : WeaponType.values()) {
            RegistryObject<Item> obj = MagiItems.ITEMS.register(tierName + "_" + type.id(),
                    () -> create(tier, type, bowBonus));
            ENTRIES.add(new Entry(tierName, type, obj));
        }
    }

    private static Item create(Tier tier, WeaponType type, double bowBonus) {
        Item.Properties props = new Item.Properties();
        return switch (type) {
            case SCIMITAR, SABER, DAGGER, SPEAR ->
                    new MagiSwordItem(tier, type.damage(), type.speed(), props, type.reach());
            case AXE -> new MagiAxeItem(tier, (float) type.damage(), type.speed(), props, type.reach());
            case BOW -> new MagiBowItem(props.durability(tier.getUses()), bowBonus);
        };
    }

    /** Forces class loading (and therefore registration) from the mod constructor. */
    public static void init() {}

    public static List<Entry> entries() {
        return List.copyOf(ENTRIES);
    }

    public static List<RegistryObject<Item>> bows() {
        return ENTRIES.stream().filter(e -> e.type() == WeaponType.BOW).map(Entry::item).toList();
    }

    private MagiWeapons() {}
}
