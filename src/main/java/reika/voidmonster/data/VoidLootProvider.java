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

import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import reika.voidmonster.auxiliary.VoidMonsterDrops;

public final class VoidLootProvider extends LootTableProvider {
    public VoidLootProvider() {
        super(Set.of(), List.of(new SubProviderEntry(context -> () -> {
            LootTable.Builder table = LootTable.lootTable();
            add(table, Items.DIAMOND, 2, 8, 0.8);
            add(table, Items.GHAST_TEAR, 1, 1, 0.6);
            add(table, Items.GLISTERING_MELON_SLICE, 2, 5, 0);
            add(table, Items.EMERALD, 2, 6, 0.4);
            add(table, Items.ENDER_PEARL, 1, 3, 0.2);
            add(table, Items.ENDER_EYE, 1, 3, 0.25);
            add(table, Items.FIRE_CHARGE, 2, 8, 0);
            add(table, Items.NETHER_WART, 8, 22, 0);
            add(table, Items.NETHER_STAR, 1, 2, 1);
            add(table, Blocks.OBSIDIAN, 6, 16, 0.6);
            add(table, Items.GUNPOWDER, 8, 12, 0.3);
            table.withPool(LootPool.lootPool().add(EnchantedBooksEntry.books()));
            context.accept(VoidMonsterDrops.LOOT_TABLE, table);
        }, LootContextParamSets.ENTITY)));
    }
    private static void add(LootTable.Builder table, ItemLike item, int minimum, int maximum, double difficulty) {
        table.withPool(LootPool.lootPool().add(LootItem.lootTableItem(item)
                .when(() -> new DifficultyCondition(difficulty))
                .apply(SetItemCountFunction.setCount(Holder.direct(new DifficultyCount(minimum, maximum))))));
    }
}
