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

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.*;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import reika.dragonapi.extras.shader.IrisCompat;

/** V33a's alpha-covered, unlit quads, with depth writes disabled. */
public final class VoidRenderPipelines {
    public static final RenderPipeline FLARE = sprite("flare", CompareOp.ALWAYS_PASS);
    public static final RenderPipeline GROWTH = sprite("growth", CompareOp.GREATER_THAN_OR_EQUAL);
    public static final RenderPipeline DISTORTION = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(id("pipeline/distortion")).withVertexShader("core/screenquad")
            .withFragmentShader(id("post/voiddistort"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("InSampler", UniformType.COMBINED_IMAGE_SAMPLER)
                    .withUniform("MonsterFocus", UniformType.UNIFORM_BUFFER).build())
            .withColorTargetState(ColorTargetState.DEFAULT).build();
    public static final RenderType FLARE_TYPE = type("voidmonster_flare", FLARE, id("textures/effect/flare.png"));
    public static final RenderType GROWTH_TYPE = type("voidmonster_growth", GROWTH, id("textures/effect/vines3d.png"));
    public static final RenderType DEATH_TYPE = type("voidmonster_death", GROWTH,
            id("textures/effect/fade_basic.png"));
    private VoidRenderPipelines() {}
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("voidmonster", path); }
    private static RenderPipeline sprite(String name, CompareOp depth) {
        return RenderPipeline.builder().withLocation(id("pipeline/" + name))
                .withBindGroupLayout(BindGroupLayouts.GLOBALS).withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                .withVertexShader("core/position_tex_color").withFragmentShader("core/position_tex_color")
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withDepthStencilState(new DepthStencilState(depth, false)).withCull(false).build();
    }
    private static RenderType type(String name, RenderPipeline pipeline, Identifier texture) {
        return RenderType.create(name, RenderSetup.builder(pipeline).withTexture("Sampler0", texture)
                .sortOnUpload().createRenderSetup());
    }
    public static void register(IEventBus bus) {
        bus.addListener((RegisterRenderPipelinesEvent event) -> {
            event.registerPipeline(FLARE);
            event.registerPipeline(GROWTH);
            event.registerPipeline(DISTORTION);
            // Both sprites are alpha-textured, alpha-blended world quads: gbuffers_textured, which packs that override
            // its blend set to alpha blending too. They stay out of the shadow map. DISTORTION runs after Iris composites.
            IrisCompat.assignWithoutShadow(FLARE, "TEXTURED");
            IrisCompat.assignWithoutShadow(GROWTH, "TEXTURED");
        });
    }
}
