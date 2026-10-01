package com.yourname.magi.djinn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;
import java.util.Optional;

/**
 * Data-driven Djinn description, loaded from data/<ns>/djinn/<name>.json.
 * The Djinn id is the JSON file's resource location (e.g. magi:baal).
 * "abilities" lists the Djinn's arts (cast with the art key); "signature" is its own-key ability.
 */
public record DjinnDefinition(
        String element,
        int color,
        int maxLevel,
        double maxEnergy,
        List<AttributeBonus> attributes,
        List<ResourceLocation> abilities,
        Optional<ResourceLocation> signature
) {
    public static final Codec<DjinnDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("element").forGetter(DjinnDefinition::element),
            Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(DjinnDefinition::color),
            Codec.intRange(1, 100).optionalFieldOf("max_level", 10).forGetter(DjinnDefinition::maxLevel),
            Codec.doubleRange(0.0, 10000.0).optionalFieldOf("max_energy", 100.0).forGetter(DjinnDefinition::maxEnergy),
            AttributeBonus.CODEC.listOf().optionalFieldOf("attributes", List.of()).forGetter(DjinnDefinition::attributes),
            ResourceLocation.CODEC.listOf().optionalFieldOf("abilities", List.of()).forGetter(DjinnDefinition::abilities),
            ResourceLocation.CODEC.optionalFieldOf("signature").forGetter(DjinnDefinition::signature)
    ).apply(i, DjinnDefinition::new));

    /**
     * amount = base + perLevel * (level - 1). The attribute is resolved by id at apply time, so entries for
     * attributes of absent mods (e.g. irons_spellbooks:*) are silently skipped.
     */
    public record AttributeBonus(ResourceLocation attribute, AttributeModifier.Operation operation,
                                 double base, double perLevel) {
        private static final Codec<AttributeModifier.Operation> OPERATION = Codec.STRING.comapFlatMap(s -> switch (s) {
            case "addition" -> DataResult.success(AttributeModifier.Operation.ADDITION);
            case "multiply_base" -> DataResult.success(AttributeModifier.Operation.MULTIPLY_BASE);
            case "multiply_total" -> DataResult.success(AttributeModifier.Operation.MULTIPLY_TOTAL);
            default -> DataResult.error(() -> "Unknown attribute operation: " + s);
        }, op -> switch (op) {
            case ADDITION -> "addition";
            case MULTIPLY_BASE -> "multiply_base";
            case MULTIPLY_TOTAL -> "multiply_total";
        });

        public static final Codec<AttributeBonus> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("attribute").forGetter(AttributeBonus::attribute),
                OPERATION.optionalFieldOf("operation", AttributeModifier.Operation.ADDITION).forGetter(AttributeBonus::operation),
                Codec.DOUBLE.fieldOf("base").forGetter(AttributeBonus::base),
                Codec.DOUBLE.optionalFieldOf("per_level", 0.0).forGetter(AttributeBonus::perLevel)
        ).apply(i, AttributeBonus::new));
    }
}
