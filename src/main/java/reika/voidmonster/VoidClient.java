/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import reika.dragonapi.instantiable.event.client.SkyColorEvent;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;
import reika.voidmonster.entity.EntityVoidMonster;
import reika.voidmonster.network.VoidNetwork;
import reika.voidmonster.registry.VoidEntities;
import reika.voidmonster.render.*;

/** Loaded only on the physical client; common registries and packets contain no eager client initialization. */
public final class VoidClient {
    private VoidClient() {}
    public static void register(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(VoidEntities.VOID_MONSTER.get(), RenderVoidMonster::new));
        VoidRenderPipelines.register(modBus);
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(MonsterFX::afterLevel);
        bus.addListener(VoidGrowthRenderer.instance::extract);
        bus.addListener(VoidGrowthRenderer.instance::submit);
        bus.addListener(VoidDeathParticles::extract);
        bus.addListener(VoidDeathParticles::submit);
        bus.addListener((ClientTickEvent.Post event) -> { if (!Minecraft.getInstance().isPaused()) VoidDeathParticles.tick(); });
        bus.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel().isClientSide()) {
                MonsterFX.reset(); VoidGrowthRenderer.instance.reset(); VoidDeathParticles.reset();
            }
        });
        bus.addListener((ViewportEvent.RenderFog event) -> {
            float distance = VoidFogManager.limitDistance(event.getFarPlaneDistance());
            event.setFarPlaneDistance(distance);
            event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), distance * 0.75F));
        });
        bus.addListener((ViewportEvent.ComputeFogColor event) -> {
            float mix = Math.max(VoidFogManager.getColorFactor(), MonsterFX.getColorFactor());
            int original = ReikaColorAPI.RGBtoHex((int)(event.getRed() * 255), (int)(event.getGreen() * 255), (int)(event.getBlue() * 255));
            int color = ReikaColorAPI.mixColors(0x0b0b0b, original, mix);
            event.setRed(ReikaColorAPI.getRed(color) / 255F);
            event.setGreen(ReikaColorAPI.getGreen(color) / 255F);
            event.setBlue(ReikaColorAPI.getBlue(color) / 255F);
        });
        bus.addListener((SkyColorEvent event) -> event.color = ReikaColorAPI.mixColors(0x101010, event.color,
                Math.max(VoidFogManager.getColorFactor(), MonsterFX.getColorFactor())));
    }
    public static void verifyEntity(EntityVoidMonster monster) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                new VoidNetwork.Verify(monster.level().dimension().identifier(), monster.getId(), monster.getUUID()));
    }
    public static void removeMissing(VoidNetwork.Missing packet) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.dimension().identifier().equals(packet.dimension())) {
            Entity entity = level.getEntity(packet.id());
            if (entity instanceof EntityVoidMonster && entity.getUUID().equals(packet.uuid())) entity.discard();
        }
    }
    public static void spawnDeathParticles(EntityVoidMonster monster) { VoidDeathParticles.spawn(monster); }
    public static void playMonsterSounds(EntityVoidMonster monster) {
        if (monster.isGhost() || !MonsterFX.clearLOS(monster)) return;
        int t3 = monster.tickCount % 64, t2 = t3 % 32, t = t2 % 16;
        if (t == 0 || t == 5 || t == 8 && t2 > 16 && t3 > 32) {
            play(monster, SoundEvents.WITHER_SPAWN, 0.8F, (float)(0.75 + 0.13 * Math.sin(monster.tickCount / 40D)));
            play(monster, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1, 0.5F);
        }
        if (t == 3 || t == 8 || t == 13)
            play(monster, SoundEvents.NOTE_BLOCK_BASS.value(), 0.5F, 0.5F + monster.getRandom().nextFloat() * 0.25F);
    }
    private static void play(EntityVoidMonster monster, SoundEvent sound, float volume, float pitch) {
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(sound.location(), SoundSource.MASTER,
                volume, pitch, monster.getRandom(), false, 0, SoundInstance.Attenuation.LINEAR,
                monster.getX(), monster.getY(), monster.getZ(), false));
    }
}
