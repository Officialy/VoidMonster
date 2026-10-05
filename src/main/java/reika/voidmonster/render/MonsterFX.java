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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector4f;
import reika.voidmonster.entity.EntityVoidMonster;
import reika.dragonapi.instantiable.RayTracer;

/** Per-frame visibility ramp and the original voiddistort radial warp/desaturation equation. */
public final class MonsterFX {
    private static final Identifier EFFECT = VoidRenderPipelines.id("voiddistort");
    private static final Identifier WARPED = VoidRenderPipelines.id("warped");
    private static float screenFactor;
    private static double monsterDistance;
    private static Vec3 focus;
    private static MappableRingBuffer uniform;
    private static final RayTracer sight = RayTracer.getVisualLOS();
    private MonsterFX() {}
    public static boolean clearLOS(EntityVoidMonster monster) { return clearLOS(monster, 0); }
    private static boolean clearLOS(EntityVoidMonster monster, double height) {
        var mc = Minecraft.getInstance();
        if (mc.level != monster.level()) return false;
        Vec3 eye = mc.gameRenderer.mainCamera().position();
        sight.setOrigins(monster.getX(), monster.getY() + height, monster.getZ(), eye.x, eye.y, eye.z);
        return sight.isClearLineOfSight(monster.level());
    }
    public static boolean markRendered(EntityVoidMonster monster, float partialTick) {
        if (Minecraft.getInstance().level.getEntity(monster.getId()) != monster) return false;
        if (!clearLOS(monster) && !clearLOS(monster, 1)) return false;
        screenFactor = Math.min(1, screenFactor + 0.05F);
        // getScreenPos(0,0.5,0) was evaluated inside the preRender shell matrix:
        // inversion + (-1.5-scale/2) + scale*0.5 puts it exactly 1.5 above the entity.
        double death = Math.toRadians(deathRotation(monster, partialTick));
        focus = monster.getPosition(partialTick).add(1.5 * Math.sin(death), 1.5 * Math.cos(death), 0);
        monsterDistance = monster.position().distanceTo(Minecraft.getInstance().gameRenderer.mainCamera().position());
        return true;
    }
    public static float deathRotation(EntityVoidMonster monster, float partialTick) {
        return monster.deathTime > 0 ? 90 * Math.min(1, (float)Math.sqrt(Math.max(0, (monster.deathTime + partialTick - 1) / 20 * 1.6F))) : 0;
    }
    public static float getColorFactor() { return screenFactor; }
    public static float rampFog(float original) {
        return (float)((3 + 5.5 * monsterDistance) * screenFactor + (1 - screenFactor) * original);
    }
    public static void reset() { screenFactor = 0; focus = null; monsterDistance = 0; }
    public static void afterLevel(RenderLevelStageEvent.AfterLevel event) {
        try {
            if (focus != null && screenFactor > 0) render(event);
        } finally {
            screenFactor = Math.max(0, screenFactor - 0.0125F);
            if (screenFactor == 0) focus = null;
        }
    }
    private static void render(RenderLevelStageEvent.AfterLevel event) {
        Minecraft mc = Minecraft.getInstance();
        var camera = event.getLevelRenderState().cameraRenderState;
        Vector4f point = new Vector4f((float)(focus.x - camera.pos.x), (float)(focus.y - camera.pos.y), (float)(focus.z - camera.pos.z), 1);
        point.mul(event.getModelViewMatrix()).mul(camera.projectionMatrix);
        if (point.w <= 0.0001F) return;
        PostChain chain = mc.getShaderManager().getPostChain(EFFECT, Set.of(PostChain.MAIN_TARGET_ID, WARPED));
        if (chain == null) return;
        if (uniform == null) uniform = new MappableRingBuffer(() -> "Void Monster distortion",
                GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_UNIFORM, 16);
        try (var mapping = uniform.currentBuffer().map(false, true)) {
            Std140Builder.intoBuffer(mapping.data()).putVec4(point.x / point.w * 0.5F + 0.5F,
                    point.y / point.w * 0.5F + 0.5F, (float)monsterDistance, screenFactor);
        }
        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        FrameGraphBuilder frame = new FrameGraphBuilder();
        var source = frame.importExternal("main", main);
        var warped = frame.createInternal("voidmonster_warped", new RenderTargetDescriptor(main.width, main.height,
                new RenderTargetDescriptor.TextureProperties(new Vector4f(), GpuFormat.RGBA8_UNORM), null));
        var pass = frame.addPass("voidmonster_distortion");
        pass.reads(source);
        var output = pass.readsAndWrites(warped);
        pass.executes(() -> {
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            try (var draw = encoder.createRenderPass(() -> "Void Monster distortion", output.get().getColorTextureView(), Optional.empty())) {
                draw.setPipeline(RenderSystem.getCompiledPipeline(VoidRenderPipelines.DISTORTION));
                RenderSystem.bindDefaultUniforms(draw);
                draw.setUniform("MonsterFocus", uniform.currentBuffer());
                draw.setUniform("InSampler", source.get().getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                draw.draw(3, 1, 0, 0);
            }
        });
        Map<Identifier, ResourceHandle<RenderTarget>> targets = new HashMap<>();
        targets.put(PostChain.MAIN_TARGET_ID, source);
        targets.put(WARPED, output);
        chain.addToFrame(frame, main.width, main.height, new PostChain.TargetBundle() {
            @Override public void replace(Identifier id, ResourceHandle<RenderTarget> handle) { targets.put(id, handle); }
            @Override public ResourceHandle<RenderTarget> get(Identifier id) { return targets.get(id); }
        });
        frame.execute(GraphicsResourceAllocator.UNPOOLED);
        uniform.rotate();
    }
}
