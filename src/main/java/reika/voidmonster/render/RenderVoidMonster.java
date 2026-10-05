/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import reika.voidmonster.entity.EntityVoidMonster;

public final class RenderVoidMonster extends EntityRenderer<EntityVoidMonster, RenderVoidMonster.State> {
    private static final Identifier CRYSTAL = Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
    private static final Identifier PORTAL = Identifier.withDefaultNamespace("textures/entity/end_portal/end_portal.png");
    private static final Identifier ARMOR = Identifier.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");
    private final ModelVoidMonster model;
    public RenderVoidMonster(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.5F;
        model = new ModelVoidMonster(context.bakeLayer(ModelLayers.END_CRYSTAL));
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(EntityVoidMonster entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.rotation = entity.innerRotation + partialTick;
        state.healing = entity.isHealing();
        state.walkSpeed = Math.min(1, entity.walkAnimation.speed(partialTick));
        state.deathRotation = MonsterFX.deathRotation(entity, partialTick);
        state.flare = MonsterFX.markRendered(entity, partialTick);
    }
    @Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();
        poses.rotate(Axis.YP.rotationDegrees(180));
        if (state.deathRotation > 0) poses.rotate(Axis.ZP.rotationDegrees(state.deathRotation));
        // The preRender callback ran after inversion but BEFORE the model-origin
        // translation. Preserve that order for the shell, flare and shader focus.
        poses.scale(-1, -1, 1);
        ModelVoidMonster.State crystal = new ModelVoidMonster.State();
        crystal.rotation = state.rotation * 3;
        crystal.showsBottom = false;
        poses.pushPose();
        float scale = (float)(2 + Math.sin(Math.toRadians(state.rotation * 4)));
        poses.translate(0, -1.5 - scale / 2, 0);
        poses.scale(scale, scale, scale);
        poses.pushPose();
        poses.scale(2, 2, 2);
        poses.translate(0, -0.5, 0);
        collector.submitModel(model, crystal, poses, RenderTypes.entityCutout(PORTAL),
                state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        if (state.healing) {
            poses.scale(1.05F, 1.05F, 1.05F);
            float uv = state.ageInTicks * 0.01F;
            collector.submitModel(model, crystal, poses, RenderTypes.energySwirl(ARMOR, uv, uv),
                    0xf000f0, OverlayTexture.NO_OVERLAY, 0xff808080, null, state.outlineColor);
        }
        poses.popPose();
        if (state.flare) {
            PoseStack flare = new PoseStack();
            flare.last().set(poses.last());
            flare.translate(0, 0.8, 0);
            double dx = state.x - camera.pos.x, dy = state.y - camera.pos.y, dz = state.z - camera.pos.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            flare.rotate(Axis.YP.rotationDegrees((float)(-180 - Math.toDegrees(Math.atan2(dx, dz)))));
            flare.rotate(Axis.XP.rotationDegrees((float)Math.toDegrees(Math.asin(distance > 0 ? Math.clamp(dy / distance, -1, 1) : 0))));
            flare.translate(-0.125, 0, 0);
            int frame = Math.floorMod((int)state.ageInTicks, 32);
            float u = frame % 8 / 8F, v = frame / 8 / 4F;
            collector.submitCustomGeometry(flare, VoidRenderPipelines.FLARE_TYPE, (pose, out) -> {
                float s = 2.125F;
                out.addVertex(pose, -s, s, 0).setUv(u, v + 0.25F).setColor(-1);
                out.addVertex(pose, s, s, 0).setUv(u + 0.125F, v + 0.25F).setColor(-1);
                out.addVertex(pose, s, -s, 0).setUv(u + 0.125F, v).setColor(-1);
                out.addVertex(pose, -s, -s, 0).setUv(u, v).setColor(-1);
            });
        }
        poses.popPose();
        ModelVoidMonster.State core = new ModelVoidMonster.State();
        // V33a passed RenderLiving's limb amount and age into the ordinary
        // crystal render as rotation/bob. The visible shell above uses rot*3/0.
        core.rotation = state.walkSpeed;
        core.bob = state.ageInTicks;
        core.showsBottom = false;
        poses.translate(0, -1.5078125, 0);
        poses.scale(2, 2, 2);
        poses.translate(0, -0.5, 0);
        collector.submitModel(model, core, poses, CRYSTAL, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poses.popPose();
        super.submit(state, poses, collector, camera);
    }
    public static final class State extends EndCrystalRenderState {
        float rotation;
        boolean healing;
        boolean flare;
        float walkSpeed;
        float deathRotation;
    }
}
