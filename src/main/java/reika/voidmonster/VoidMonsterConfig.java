/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Registry names replace the old world's numeric dimension IDs. */
public final class VoidMonsterConfig {
    public final ModConfigSpec spec;
    private final ModConfigSpec.DoubleValue difficulty;
    private final ModConfigSpec.DoubleValue fogStrength;
    private final ModConfigSpec.IntValue soundDelay;
    private final ModConfigSpec.BooleanValue whitelist;
    private final ModConfigSpec.ConfigValue<List<? extends String>> dimensions;

    public VoidMonsterConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("Control Setup");
        difficulty = builder.comment("Multiplier for health, attacks, healing and loot thresholds.")
                .defineInRange("Void Monster Difficulty Factor", 1D, -Float.MAX_VALUE, Float.MAX_VALUE);
        fogStrength = builder.comment("Zero disables height-dependent void fog.")
                .defineInRange("Void Fog Strength", 1D, -Float.MAX_VALUE, Float.MAX_VALUE);
        soundDelay = builder.defineInRange("Sound Interval in Ticks", 80, 0, 1200);
        whitelist = builder.define("Dimension list is actually whitelist", false);
        dimensions = builder.comment("Use dimension registry names, for example minecraft:the_end.",
                "The original default IDs were 1 and -112. The End is mapped here;",
                "add the former -112 dimension's registry name when that dimension is ported.")
                .defineListAllowEmpty("Banned Dimensions", List.of("minecraft:the_end"),
                        () -> "minecraft:the_end", value -> value instanceof String name && Identifier.tryParse(name) != null);
        builder.pop();
        spec = builder.build();
    }

    public float getMonsterDifficulty() { return difficulty.get().floatValue(); }
    public float getFogStrength() { return fogStrength.get().floatValue(); }
    public int getMonsterSoundDelay() { return soundDelay.get(); }
    public boolean isDimensionWhitelist() { return whitelist.get(); }
    public List<ResourceKey<Level>> getDimensions() {
        return dimensions.get().stream().map(name -> ResourceKey.create(Registries.DIMENSION, Identifier.parse(name))).toList();
    }
}
