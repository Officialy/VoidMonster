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

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.SingleEntryContainerBase;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import reika.voidmonster.entity.EntityVoidMonster;

public final class EnchantedBooksEntry extends SingleEntryContainerBase {
    public static final MapCodec<EnchantedBooksEntry> CODEC = RecordCodecBuilder.mapCodec(i -> uniformFields(i).apply(i, EnchantedBooksEntry::new));
    private EnchantedBooksEntry(int weight, int quality, Optional<Holder<LootItemCondition>> condition, Optional<Holder<LootItemFunction>> modifier) {
        super(weight, quality, condition, modifier);
    }
    @Override public MapCodec<EnchantedBooksEntry> codec() { return CODEC; }
    @Override protected void createItemStack(Consumer<ItemStack> output, LootContext context) {
        if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof EntityVoidMonster monster) || monster.isGhost())
            return;
        List<Holder.Reference<Enchantment>> enchantments = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .listElements().filter(holder -> holder.key().identifier().getNamespace().equals("minecraft")).toList();
        if (enchantments.isEmpty())
            throw new IllegalStateException("No vanilla enchantments in the world registry");
        int count = 1 + 2 * (int)Math.ceil(Math.max(1, 1 + monster.getDifficulty()));
        for (int i = 0; i < count; i++) {
            Holder<Enchantment> enchantment = enchantments.get(context.getRandom().nextInt(enchantments.size()));
            int maximum = Math.max(1, (int)(enchantment.value().getMaxLevel() * Math.min(1, monster.getDifficulty())));
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            book.enchant(enchantment, 1 + context.getRandom().nextInt(maximum));
            output.accept(book);
        }
    }
    public static net.minecraft.world.level.storage.loot.entries.UniformContainerBase.Builder<?> books() {
        return simpleBuilder(EnchantedBooksEntry::new);
    }
}
