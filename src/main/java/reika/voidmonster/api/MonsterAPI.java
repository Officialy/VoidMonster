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

import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import reika.voidmonster.VoidMonster;
import reika.voidmonster.auxiliary.VoidMonsterDrops;
import reika.voidmonster.entity.EntityVoidMonster;

/** Public integration entry points; callers must only load this API when Void Monster is installed. */
public final class MonsterAPI {
    private MonsterAPI() {}

    public static void addDrop(ItemStack stack, int min, int max) { addDrop(stack, min, max, 0); }
    public static void addDrop(ItemStack stack, int min, int max, double requiredDifficulty) {
        VoidMonsterDrops.addDrop(stack, min, max, requiredDifficulty);
    }
    public static void addDrop(Item item, int min, int max) { addDrop(new ItemStack(item), min, max); }
    public static void addDrop(Block block, int min, int max) { addDrop(new ItemStack(block), min, max); }
    public static void addDrop(ItemStack stack) { addDrop(stack, 1, 1); }
    public static void addDrop(Item item) { addDrop(new ItemStack(item)); }
    public static void addDrop(Block block) { addDrop(new ItemStack(block)); }

    public static Mob getNearestMonster(Level level, double x, double y, double z) {
        EntityVoidMonster nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (EntityVoidMonster monster : VoidMonster.getCurrentMonsterList(level)) {
            double candidate = monster.distanceToSqr(x, y, z);
            if (candidate < distance) {
                nearest = monster;
                distance = candidate;
            }
        }
        return nearest;
    }
}
