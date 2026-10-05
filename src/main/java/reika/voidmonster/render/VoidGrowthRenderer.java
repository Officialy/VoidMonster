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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import reika.voidmonster.VoidMonster;
import reika.voidmonster.entity.EntityVoidMonster;

/** Growth is accumulated and decayed per frame, as in V33a, rather than per game tick. */
public final class VoidGrowthRenderer {
    public static final VoidGrowthRenderer instance = new VoidGrowthRenderer();
    private static final int MAX_VALUE = 240;
    private static final ContextKey<List<Face>> FACES = new ContextKey<>(VoidRenderPipelines.id("growth_faces"));
    private final Map<BlockPos, Integer> data = new HashMap<>();
    private ClientLevel level;
    private VoidGrowthRenderer() {}
    public float getBlockLevel(BlockPos pos) { return data.getOrDefault(pos, 0) / (float)MAX_VALUE; }
    public java.util.Set<BlockPos> getLocations() { return java.util.Set.copyOf(data.keySet()); }
    public void reset() { level = null; data.clear(); }
    public void extract(ExtractLevelRenderStateEvent event) {
        if (level != event.getLevel()) { reset(); level = event.getLevel(); }
        for (EntityVoidMonster monster : VoidMonster.getCurrentMonsterList(level)) tickMonster(monster);
        List<Face> faces = new ArrayList<>();
        data.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            int amount = entry.getValue();
            if (level.hasChunkAt(pos)) {
                var state = level.getBlockState(pos);
                for (Direction direction : Direction.values())
                    if (Block.shouldRenderFace(level, pos, state, level.getBlockState(pos.relative(direction)), direction))
                        faces.add(new Face(pos, direction, 49 * amount / MAX_VALUE));
            }
            entry.setValue(amount - 1);
            return amount <= 0;
        });
        event.getRenderState().setRenderData(FACES, List.copyOf(faces));
    }
    private void tickMonster(EntityVoidMonster monster) {
        var player = Minecraft.getInstance().player;
        if (player == null || monster.distanceToSqr(player) >= 4096) return;
        int radius = monster.getY() < level.getMinY() + 2 ? 7 : 5;
        BlockPos center = monster.blockPosition();
        double maximum = radius * radius + 1.5;
        for (int y = -radius; y <= radius; y++)
            for (int x = -radius; x <= radius; x++)
                for (int z = -radius; z <= radius; z++) {
                    double distance = x * x + y * y + z * z;
                    if (distance <= maximum) addBlockLevel(center.offset(x, y, z), distance / maximum);
                }
    }
    public void addBlockLevel(BlockPos pos, double edge) {
        if (level == null || level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return;
        var state = level.getBlockState(pos);
        if (state.isAir() || state.getRenderShape() != RenderShape.MODEL || state.is(Blocks.BEDROCK) && pos.getY() < level.getMinY() + 6) return;
        int old = data.getOrDefault(pos, 0);
        int maximum = MAX_VALUE;
        if (edge >= 0.25) maximum = Math.max(old, (int)(maximum * (1 - (edge - 0.25) / 0.75)));
        data.put(pos.immutable(), Math.min(old + 2, maximum));
    }
    public void submit(SubmitCustomGeometryEvent event) {
        List<Face> faces = event.getLevelRenderState().getRenderData(FACES);
        if (faces == null || faces.isEmpty()) return;
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        Vec3 eye = event.getLevelRenderState().cameraRenderState.pos;
        poses.translate(-eye.x, -eye.y, -eye.z);
        event.getSubmitNodeCollector().submitCustomGeometry(poses, VoidRenderPipelines.GROWTH_TYPE,
                (pose, vertices) -> faces.forEach(face -> drawFace(pose, vertices, face)));
        poses.popPose();
    }
    private static void drawFace(PoseStack.Pose pose, VertexConsumer vertices, Face face) {
        float x0 = face.pos().getX() - 0.01F, x1 = x0 + 1.02F;
        float y0 = face.pos().getY() - 0.01F, y1 = y0 + 1.02F;
        float z0 = face.pos().getZ() - 0.01F, z1 = z0 + 1.02F;
        float[][] points = switch (face.direction()) {
            case DOWN -> new float[][]{{x0,y0,z0},{x1,y0,z0},{x1,y0,z1},{x0,y0,z1}};
            case UP -> new float[][]{{x0,y1,z1},{x1,y1,z1},{x1,y1,z0},{x0,y1,z0}};
            case EAST -> new float[][]{{x1,y0,z0},{x1,y1,z0},{x1,y1,z1},{x1,y0,z1}};
            case WEST -> new float[][]{{x0,y0,z1},{x0,y1,z1},{x0,y1,z0},{x0,y0,z0}};
            case SOUTH -> new float[][]{{x1,y0,z1},{x1,y1,z1},{x0,y1,z1},{x0,y0,z1}};
            case NORTH -> new float[][]{{x0,y0,z0},{x0,y1,z0},{x1,y1,z0},{x1,y0,z0}};
        };
        // The original col=frame%10 relied on texture repetition; modulo 5 is the
        // same texel selection without sampling outside the five-column atlas.
        float u = face.frame() % 5 / 5F, v = face.frame() / 5 / 10F;
        for (int i = 0; i < 4; i++) vertices.addVertex(pose, points[i][0], points[i][1], points[i][2])
                .setUv(u + (i == 1 || i == 2 ? 0.2F : 0), v + (i >= 2 ? 0.1F : 0)).setColor(-1);
    }
    private record Face(BlockPos pos, Direction direction, int frame) {}
}
