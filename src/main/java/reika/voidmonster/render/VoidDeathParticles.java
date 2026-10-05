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
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import reika.voidmonster.entity.EntityVoidMonster;

/** V33a's 24 black, basic-blended, colliding EntityBlurFX motes, independent of the unported DragonAPI particle. */
public final class VoidDeathParticles {
    private static final List<Mote> particles = new ArrayList<>();
    private static final ContextKey<List<Sprite>> SPRITES = new ContextKey<>(VoidRenderPipelines.id("death_particles"));
    private static ClientLevel level;
    private VoidDeathParticles() {}
    public static void spawn(EntityVoidMonster monster) {
        if (!(monster.level() instanceof ClientLevel client)) return;
        if (level != client) { reset(); level = client; }
        var random = monster.getRandom();
        for (int i = 0; i < 24; i++) {
            double speed = 0.0625 + random.nextDouble() * 0.1875;
            double angle = random.nextDouble() * Math.PI * 2;
            particles.add(new Mote(monster.position(), new Vec3(speed * Math.cos(angle), 0, speed * Math.sin(angle)),
                    1.5F + random.nextFloat() * 3.5F, 40 + random.nextInt(161), random.nextDouble() * Math.PI * 2));
        }
    }
    public static void reset() { particles.clear(); level = null; }
    public static void tick() {
        if (Minecraft.getInstance().level != level) { reset(); return; }
        particles.removeIf(mote -> {
            mote.oldPosition = mote.position;
            if (mote.freeze > 0) mote.freeze--; else mote.age++;
            if (mote.age >= mote.life) return true;
            Vec3 actual = Entity.collideBoundingBox((Entity)null, mote.velocity,
                    new AABB(mote.position.x - 0.1, mote.position.y, mote.position.z - 0.1,
                            mote.position.x + 0.1, mote.position.y + 0.2, mote.position.z + 0.1), level, List.of());
            mote.position = mote.position.add(actual);
            boolean blockedY = actual.y != mote.velocity.y;
            if (mote.colliding && blockedY) {
                double speed = level.getRandom().nextDouble() * 0.0625;
                mote.velocity = new Vec3(speed * Math.sin(mote.collisionAngle), 0, speed * Math.cos(mote.collisionAngle));
                mote.colliding = false;
                mote.freeze = 20;
            } else {
                mote.velocity = new Vec3(actual.x == mote.velocity.x ? mote.velocity.x : 0,
                        blockedY ? 0 : mote.velocity.y, actual.z == mote.velocity.z ? mote.velocity.z : 0);
                if (mote.colliding) mote.velocity = mote.velocity.scale(0.98);
            }
            return false;
        });
    }
    public static void extract(ExtractLevelRenderStateEvent event) {
        List<Sprite> sprites = new ArrayList<>();
        if (event.getLevel() == level) {
            float partial = event.getRenderState().worldPartialTicks;
            for (Mote mote : particles) sprites.add(new Sprite(mote.oldPosition.lerp(mote.position, partial),
                    0.1F * mote.scale * (float)Math.sin(Math.PI * Math.max(1, mote.age) / mote.life)));
        }
        event.getRenderState().setRenderData(SPRITES, List.copyOf(sprites));
    }
    public static void submit(SubmitCustomGeometryEvent event) {
        List<Sprite> sprites = event.getLevelRenderState().getRenderData(SPRITES);
        if (sprites == null) return;
        var camera = event.getLevelRenderState().cameraRenderState;
        for (Sprite sprite : sprites) {
            PoseStack poses = new PoseStack();
            poses.last().set(event.getPoseStack().last());
            poses.translate(sprite.position.subtract(camera.pos));
            poses.rotate(camera.orientation);
            float s = sprite.size;
            event.getSubmitNodeCollector().submitCustomGeometry(poses, VoidRenderPipelines.DEATH_TYPE, (pose, out) -> {
                out.addVertex(pose, -s, s, 0).setUv(0, 1).setColor(0xff000000);
                out.addVertex(pose, s, s, 0).setUv(1, 1).setColor(0xff000000);
                out.addVertex(pose, s, -s, 0).setUv(1, 0).setColor(0xff000000);
                out.addVertex(pose, -s, -s, 0).setUv(0, 0).setColor(0xff000000);
            });
        }
    }
    private record Sprite(Vec3 position, float size) {}
    private static final class Mote {
        Vec3 position, oldPosition, velocity;
        final float scale;
        final int life;
        final double collisionAngle;
        int age, freeze;
        boolean colliding = true;
        Mote(Vec3 position, Vec3 velocity, float scale, int life, double angle) {
            this.position = this.oldPosition = position;
            this.velocity = velocity;
            this.scale = scale;
            this.life = life;
            this.collisionAngle = angle;
        }
    }
}
