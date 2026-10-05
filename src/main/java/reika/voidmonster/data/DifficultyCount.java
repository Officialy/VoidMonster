/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import reika.voidmonster.entity.EntityVoidMonster;

/** The original ItemDrop formula scales the random range, leaving the minimum fixed. */
public record DifficultyCount(int minimum, int maximum) implements ContextIntProvider {
    public static final MapCodec<DifficultyCount> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("min").forGetter(DifficultyCount::minimum),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("max").forGetter(DifficultyCount::maximum)).apply(i, DifficultyCount::new));
    public DifficultyCount {
        if (maximum < minimum)
            throw new IllegalArgumentException("Drop maximum is below minimum");
    }
    @Override public MapCodec<DifficultyCount> codec() { return CODEC; }
    @Override public void validate(net.minecraft.world.level.storage.loot.ValidationContext context) {
        if (minimum < 0 || maximum < minimum || maximum == Integer.MAX_VALUE)
            context.reportProblem(() -> "Invalid Void Monster drop count range");
    }
    @Override public int getIntUnsafe(LootContext context) {
        float difficulty = context.getOptional(LootContextParams.THIS_ENTITY) instanceof EntityVoidMonster monster ? monster.getDifficulty() : 1;
        return minimum + (int)(difficulty * context.getRandom().nextInt(1 + maximum - minimum));
    }
}
