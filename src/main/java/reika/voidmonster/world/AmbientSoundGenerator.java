/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.world;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class AmbientSoundGenerator {
    public static final AmbientSoundGenerator instance = new AmbientSoundGenerator();
    private final RandomSource random = RandomSource.create();
    private final Set<ResourceKey<Level>> bannedDimensions = new HashSet<>();
    private final Set<ResourceKey<Biome>> bannedBiomes = new HashSet<>();

    private AmbientSoundGenerator() {}

    public void tick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        ServerLevel level = player.level();
        if (!canSpawnSounds(level) || random.nextInt(400) != 0)
            return;
        double height = player.getY() - level.getMinY();
        if (height >= 45)
            return;
        BlockPos pos = player.blockPosition().above();
        if (level.canSeeSky(pos) || level.getMaxLocalRawBrightness(pos) > 7
                || level.getBiome(pos).unwrapKey().filter(bannedBiomes::contains).isPresent())
            return;
        float volume = 2 * (45 - (float)height) / 45;
        int count = random.nextInt(4) == 0 ? 1 + random.nextInt(4) : 1;
        for (int i = 0; i < count; i++)
            player.connection.send(new ClientboundSoundPacket(SoundEvents.AMBIENT_CAVE, SoundSource.AMBIENT,
                    player.getX(), player.getY(), player.getZ(), volume / count, 0, random.nextLong()));
    }

    public boolean canSpawnSounds(ServerLevel level) {
        return !MonsterGenerator.isFlat(level) && (MonsterGenerator.isHardcodedAllowed(level.dimension())
                || !bannedDimensions.contains(level.dimension()));
    }

    public void blacklistDimension(ResourceKey<Level> key) { bannedDimensions.add(key); }
    public void blacklistBiome(ResourceKey<Biome> key) { bannedBiomes.add(key); }
}
