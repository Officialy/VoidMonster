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

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import reika.voidmonster.entity.EntityVoidMonster;

public final class VoidEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, "voidmonster");
    public static final DeferredHolder<EntityType<?>, EntityType<EntityVoidMonster>> VOID_MONSTER = ENTITIES.register("void_monster",
            () -> EntityType.Builder.of(factory(), MobCategory.MONSTER)
                    .sized(3, 3).fireImmune().clientTrackingRange(16).updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("voidmonster", "void_monster"))));

    private VoidEntities() {}

    @SuppressWarnings("unchecked")
    private static EntityType.EntityFactory<EntityVoidMonster> factory() {
        if (!net.neoforged.fml.ModList.get().isLoaded("rotarycraft")) return EntityVoidMonster::new;
        // A direct reference to the subclass makes the JVM resolve its optional
        // interfaces while verifying this registry class, before the branch runs.
        try {
            return (EntityType.EntityFactory<EntityVoidMonster>)Class.forName("reika.voidmonster.compat.RotaryMonster")
                    .getMethod("factory").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot load Void Monster's installed RotaryCraft adapter", exception);
        }
    }

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(VOID_MONSTER.get(), EntityVoidMonster.createAttributes().build());
    }
}
