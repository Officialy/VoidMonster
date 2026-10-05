/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import reika.voidmonster.data.DifficultyCondition;
import reika.voidmonster.data.DifficultyCount;
import reika.voidmonster.data.EnchantedBooksEntry;

public final class VoidLootTypes {
    private static final DeferredRegister<MapCodec<? extends LootItemCondition>> CONDITIONS = DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, "voidmonster");
    private static final DeferredRegister<MapCodec<? extends ContextIntProvider>> COUNTS = DeferredRegister.create(Registries.CONTEXT_INT_PROVIDER_TYPE, "voidmonster");
    private static final DeferredRegister<MapCodec<? extends LootPoolEntryContainer>> ENTRIES = DeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE, "voidmonster");
    static {
        CONDITIONS.register("difficulty", () -> DifficultyCondition.CODEC);
        COUNTS.register("difficulty_count", () -> DifficultyCount.CODEC);
        ENTRIES.register("enchanted_books", () -> EnchantedBooksEntry.CODEC);
    }
    private VoidLootTypes() {}
    public static void register(IEventBus bus) { CONDITIONS.register(bus); COUNTS.register(bus); ENTRIES.register(bus); }
}
