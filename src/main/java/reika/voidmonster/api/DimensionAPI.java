/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import reika.voidmonster.world.AmbientSoundGenerator;
import reika.voidmonster.world.MonsterGenerator;

public final class DimensionAPI {
    private DimensionAPI() {}
    public static void blacklistDimensionForSounds(ResourceKey<Level> key) {
        AmbientSoundGenerator.instance.blacklistDimension(key);
    }
    public static void blacklistBiomeForSounds(ResourceKey<Biome> key) {
        AmbientSoundGenerator.instance.blacklistBiome(key);
    }
    public static void setDimensionRuleForSpawning(ResourceKey<Level> key, boolean allow) {
        MonsterGenerator.instance.setDimensionRuleAPI(key, allow);
    }
}
