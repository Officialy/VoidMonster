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

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageScaling;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import reika.voidmonster.registry.VoidDamageTypes;

@EventBusSubscriber(modid = "voidmonster")
public final class VoidDataProviders {
    private VoidDataProviders() {}
    @SubscribeEvent public static void client(GatherDataEvent.Client event) {
        event.createProvider(VoidClientData::new);
        event.createProvider(VoidClientData.Language::new);
    }
    @SubscribeEvent public static void server(GatherDataEvent.Server event) {
        event.createProvider(VoidTestStructureProvider::new);
        event.createWorldRegistryObjects(new RegistrySetBuilder().add(Registries.DAMAGE_TYPE, context -> {
            context.register(VoidDamageTypes.VOID_MONSTER, new DamageType("voidmonster", DamageScaling.NEVER, 0));
            context.register(VoidDamageTypes.GHOST, new DamageType("voidmonster.ghost", DamageScaling.NEVER, 0));
        }));
        event.createProvider((output, lookup) -> new DamageTypeTagsProvider(output, event.getWorldLookupProvider(), "voidmonster") {
            @Override protected void addTags(net.minecraft.core.HolderLookup.Provider registries) {
                tag(DamageTypeTags.BYPASSES_ARMOR).add(VoidDamageTypes.VOID_MONSTER);
                tag(DamageTypeTags.BYPASSES_SHIELD).add(VoidDamageTypes.VOID_MONSTER);
                tag(DamageTypeTags.BYPASSES_EFFECTS).add(VoidDamageTypes.VOID_MONSTER);
                tag(DamageTypeTags.BYPASSES_RESISTANCE).add(VoidDamageTypes.VOID_MONSTER);
                tag(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(VoidDamageTypes.VOID_MONSTER);
            }
        });
        event.createReloadableRegistryObjects(new RegistrySetBuilder().add(Registries.LOOT_TABLE, new VoidLootProvider()));
    }
}
