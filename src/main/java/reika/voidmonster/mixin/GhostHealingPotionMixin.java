/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.voidmonster.entity.EntityVoidMonster;

@Mixin(targets = "net.minecraft.world.effect.HealOrHarmMobEffect")
public abstract class GhostHealingPotionMixin {
    @Inject(method = "applyInstantaneousEffect", at = @At("HEAD"), cancellable = true)
    private void expelGhost(ServerLevel level, Entity source, Entity owner, LivingEntity victim, int amplifier, double factor, CallbackInfo ci) {
        if ((Object)this == MobEffects.INSTANT_HEALTH.value() && source instanceof ThrownSplashPotion
                && victim instanceof EntityVoidMonster monster && monster.isGhost()) {
            monster.doGhostDamage(owner instanceof LivingEntity living ? living : null, amplifier, factor);
            ci.cancel();
        }
    }
}
