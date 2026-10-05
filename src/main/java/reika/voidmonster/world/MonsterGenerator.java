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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.auxiliary.trackers.SpecialDayTracker;
import reika.voidmonster.VoidMonster;
import reika.voidmonster.VoidMonsterConfig;
import reika.voidmonster.entity.EntityVoidMonster;
import reika.voidmonster.registry.VoidEntities;

public final class MonsterGenerator {
    public static final MonsterGenerator instance = new MonsterGenerator();
    private final RandomSource random = RandomSource.create();
    private final DimensionRules<ResourceKey<Level>> dimensions = new DimensionRules<>();
    private final MonsterCooldowns<ServerLevel> cooldowns = new MonsterCooldowns<>();
    private boolean cachedHalloween;
    private long lastCalendarCheck = Long.MIN_VALUE;

    private MonsterGenerator() {}

    public void tick(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level && canSpawnIn(level)) {
            ServerPlayer player = getRandomPlayer(level);
            if (player != null)
                spawnIfAbsent(level, player);
        }
    }

    public boolean spawnIfAbsent(ServerLevel level, ServerPlayer player) {
        if (!VoidMonster.getCurrentMonsterList(level).isEmpty())
            return false;
        EntityVoidMonster monster = VoidEntities.VOID_MONSTER.get().create(level, EntitySpawnReason.EVENT);
        if (monster == null)
            throw new IllegalStateException("Void Monster entity factory returned null");
        if (level.environmentAttributes().getDimensionValue(EnvironmentAttributes.WATER_EVAPORATES))
            monster.setNether();
        monster.snapTo(player.getX(), monster.isNetherVoid() ? level.getMaxY() + 5 : level.getMinY() - 10,
                player.getZ(), 0, 0);
        if (isHalloween())
            monster.setGhost();
        return level.addFreshEntity(monster);
    }

    public boolean isHalloween() {
        if (!FMLEnvironment.isProduction() || !DragonOptions.APRIL.getState())
            return false;
        long now = System.currentTimeMillis();
        if (lastCalendarCheck == Long.MIN_VALUE || now - lastCalendarCheck > 60000) {
            cachedHalloween = SpecialDayTracker.instance.isHalloween();
            lastCalendarCheck = now;
        }
        return cachedHalloween && random.nextInt(4) == 0;
    }

    private ServerPlayer getRandomPlayer(ServerLevel level) {
        List<ServerPlayer> eligible = new ArrayList<>(level.players());
        eligible.removeIf(p -> p instanceof FakePlayer || p.tickCount < 5);
        return eligible.isEmpty() ? null : eligible.get(random.nextInt(eligible.size()));
    }

    public boolean canSpawnIn(ServerLevel level) {
        if (level.players().isEmpty() || level.getGameTime() % 8 != 0)
            return false;
        if (!level.dimension().equals(Level.NETHER) && isFlat(level))
            return false;
        if (cooldowns.active(level, level.getGameTime()) || !VoidMonster.getCurrentMonsterList(level).isEmpty())
            return false;
        // MYSTCRAFT-PORT: retain the authored No Void Monster page veto before normal
        // dimension admission when a modern Mystcraft age/page API becomes available.
        return isHardcodedAllowed(level.dimension()) || isDimensionAllowed(level);
    }

    public boolean isDimensionAllowed(Level level) {
        return dimensions.allows(level.dimension());
    }

    public void addCooldown(LivingEntity entity, int delay) {
        if (entity.level() instanceof ServerLevel level)
            cooldowns.add(level, level.getGameTime(), delay);
    }

    public void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level)
            cooldowns.clear(level);
    }

    public void setDimensions(Collection<ResourceKey<Level>> keys) {
        dimensions.addDimensions(keys);
    }

    public void setDimensionRuleAPI(ResourceKey<Level> key, boolean allow) {
        dimensions.override(key, allow);
    }

    public void loadConfig(VoidMonsterConfig config) {
        dimensions.configure(config.getDimensions(), config.isDimensionWhitelist());
    }

    public static boolean isFlat(ServerLevel level) {
        return level.getChunkSource().getGenerator() instanceof FlatLevelSource;
    }

    public static boolean isHardcodedAllowed(ResourceKey<Level> key) {
        return key.equals(Level.OVERWORLD) || key.equals(Level.NETHER)
                || key.identifier().toString().equals("extrautils2:deep_dark")
                || key.identifier().toString().equals("extrautils:deep_dark");
    }
}
