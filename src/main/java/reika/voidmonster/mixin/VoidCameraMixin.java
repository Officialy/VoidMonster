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

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.voidmonster.render.VoidFogManager;

/** 26.3 moved the far clip plane into Camera's projection setup. */
@Mixin(Camera.class)
public abstract class VoidCameraMixin {
    @Shadow private float depthFar;
    @Inject(method = "update", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Camera;depthFar:F",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void voidmonster$farPlane(DeltaTracker deltaTracker, CallbackInfo callback) {
        depthFar = VoidFogManager.limitDistance(depthFar);
    }
}
