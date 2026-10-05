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

public final class VoidMonsterDamage extends DamageSource {
    private final String message;
    public VoidMonsterDamage(EntityVoidMonster monster) {
        super(CustomStringDamageSource.resolve(monster.level(), VoidDamageTypes.VOID_MONSTER), monster);
        StringBuilder text = new StringBuilder();
        while (text.length() < 24) {
            int length = Math.min(24 - text.length(), 4 + monster.getRandom().nextInt(5));
            while (length > 0 && text.length() < 24) {
                int code;
                do { code = 33 + monster.getRandom().nextInt(222); }
                while (code == 127 || code == '%' || code == '\\');
                text.append((char)code);
            }
            text.append(' ');
        }
        text.deleteCharAt(text.length() - 1);
        message = text.toString();
    }
    @Override public Component getLocalizedDeathMessage(LivingEntity victim) {
        return Component.literal(victim.getName().getString() + " " + message);
    }
}
