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
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import reika.voidmonster.entity.EntityVoidMonster;

public record DifficultyCondition(double minimum) implements LootItemCondition {
    public static final MapCodec<DifficultyCondition> CODEC = Codec.DOUBLE.fieldOf("minimum").xmap(DifficultyCondition::new, DifficultyCondition::minimum);
    @Override public MapCodec<DifficultyCondition> codec() { return CODEC; }
    @Override public boolean test(LootContext context) {
        return context.getOptional(LootContextParams.THIS_ENTITY) instanceof EntityVoidMonster monster
                && !monster.isGhost() && monster.getDifficulty() >= minimum;
    }
}
