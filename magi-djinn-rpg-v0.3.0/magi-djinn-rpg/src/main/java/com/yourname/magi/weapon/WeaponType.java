package com.yourname.magi.weapon;

/**
 * Weapon archetypes. Numbers are Minecraft-side stats (vanilla attribute system):
 * damage = bonus added on top of the tier's own damage; speed = attack-speed modifier (4.0 + speed = swings/sec);
 * reach = extra entity reach (vanilla mode). Epic Fight uses its OWN numbers from
 * data/magi/capabilities/weapons/*.json; keep the two in the same spirit.
 */
public enum WeaponType {
    SCIMITAR("scimitar", 2, -2.2F, 0.25),   // fast-ish curved blade, slight reach
    SABER("saber", 2, -2.0F, 0.0),          // balanced, quickest of the full-size blades
    DAGGER("dagger", 0, -1.3F, -0.5),       // very fast, short reach
    SPEAR("spear", 2, -2.6F, 1.0),          // slow, long reach
    AXE("axe", 5, -3.1F, 0.0),              // heavy, slow, high damage
    BOW("bow", 0, 0.0F, 0.0);               // ranged; tier adds arrow damage

    private final String id;
    private final int damage;
    private final float speed;
    private final double reach;

    WeaponType(String id, int damage, float speed, double reach) {
        this.id = id;
        this.damage = damage;
        this.speed = speed;
        this.reach = reach;
    }

    public String id() { return id; }
    public int damage() { return damage; }
    public float speed() { return speed; }
    public double reach() { return reach; }
}
