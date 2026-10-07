package reika.voidmonster.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import reika.voidmonster.render.MonsterFX;

/** 26.3 submits the HUD and screens together, then renders them through GuiRenderer. */
@Mixin(GameRenderer.class)
public abstract class VoidGuiDistortionMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void voidmonster$beginFrame(CallbackInfo ci) {
        MonsterFX.beginFrame();
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;endFrame()V", shift = At.Shift.AFTER))
    private void voidmonster$warpGui(CallbackInfo ci) {
        MonsterFX.afterGui();
    }
}
