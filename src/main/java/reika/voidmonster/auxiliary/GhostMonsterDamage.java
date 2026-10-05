/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.auxiliary;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import reika.dragonapi.instantiable.CustomStringDamageSource;
import reika.voidmonster.entity.EntityVoidMonster;
import reika.voidmonster.registry.VoidDamageTypes;

public final class GhostMonsterDamage extends DamageSource {
    public GhostMonsterDamage(EntityVoidMonster victim, LivingEntity attacker) {
        super(CustomStringDamageSource.resolve(victim.level(), VoidDamageTypes.GHOST), attacker);
    }
    @Override public Component getLocalizedDeathMessage(LivingEntity victim) {
        return getEntity() != null ? Component.translatable("death.attack.voidmonster.ghost", victim.getName(), getEntity().getName())
                : Component.literal(victim.getName().getString() + " ");
    }
}
