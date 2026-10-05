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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.CommandableUpdateChecker;
import reika.dragonapi.auxiliary.trackers.DonatorController;
import reika.dragonapi.base.DragonAPIMod;
import reika.voidmonster.auxiliary.VoidMonsterDrops;
import reika.voidmonster.entity.EntityVoidMonster;
import reika.voidmonster.registry.VoidEntities;
import reika.voidmonster.registry.VoidLootTypes;
import reika.voidmonster.world.AmbientSoundGenerator;
import reika.voidmonster.world.MonsterGenerator;

@Mod(VoidMonster.MODID)
public final class VoidMonster extends DragonAPIMod {
    public static final String MODID = "voidmonster";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static VoidMonster instance;
    public static final VoidMonsterConfig config = new VoidMonsterConfig();
    private static final Map<Level, Set<Integer>> monsters = new WeakHashMap<>();

    public VoidMonster(IEventBus modBus, ModContainer container) {
        instance = this;
        startTiming(LoadProfiler.LoadPhase.PRELOAD);
        container.registerConfig(ModConfig.Type.COMMON, config.spec, "Reika/VoidMonster.toml");
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::configLoaded);
        modBus.addListener(this::configReloaded);
        VoidEntities.ENTITIES.register(modBus);
        reika.voidmonster.registry.VoidSounds.SOUNDS.register(modBus);
        VoidGameTests.TYPES.register(modBus);
        VoidLootTypes.register(modBus);
        modBus.addListener(VoidEntities::attributes);
        modBus.addListener(reika.voidmonster.network.VoidNetwork::register);
        NeoForge.EVENT_BUS.addListener(MonsterGenerator.instance::tick);
        NeoForge.EVENT_BUS.addListener(AmbientSoundGenerator.instance::tick);
        NeoForge.EVENT_BUS.addListener(MonsterGenerator.instance::unload);
        NeoForge.EVENT_BUS.addListener(VoidMonster::unload);
        NeoForge.EVENT_BUS.addListener(VoidMonsterDrops::loadCustomDrops);
        modBus.addListener(VoidGameTests::register);
        if (FMLEnvironment.getDist() == Dist.CLIENT)
            VoidClient.register(modBus);
        basicSetup();
        finishTiming();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DonatorController.instance.registerMod(this, DonatorController.reikaURL);
            // MYSTCRAFT-PORT: register VoidMystPages, and reject monster LinkEventAllow.
            // THAUMCRAFT-PORT: restore ELDRITCH/VOID/DARKNESS=40, BEAST/DEATH=25,
            // ENTROPY=20, AURA=5, ARMOR/CRYSTAL=10 entity aspects.
            // MFR-PORT: restore Safari Net and AutoSpawner class blacklists.
            // ENDERIO-PORT: restore soul-vessel capture blacklist for this entity type.
            // FORESTRY-PORT: register the complete retained VoidMonsterBee integration.
        });
    }

    private void configLoaded(ModConfigEvent.Loading event) { applyConfig(event); }
    private void configReloaded(ModConfigEvent.Reloading event) { applyConfig(event); }
    private void applyConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() == config.spec)
            MonsterGenerator.instance.loadConfig(config);
    }

    public static boolean allowedIn(Level level) { return MonsterGenerator.instance.isDimensionAllowed(level); }
    public int getMonsterSoundDelay() { return config.getMonsterSoundDelay(); }
    public float getMonsterDifficulty() { return config.getMonsterDifficulty(); }
    public float getFogStrength() { return config.getFogStrength(); }

    public static void registerExistingMonster(EntityVoidMonster monster) {
        synchronized (monsters) {
            monsters.computeIfAbsent(monster.level(), key -> new HashSet<>()).add(monster.getId());
        }
    }

    public static Collection<EntityVoidMonster> getCurrentMonsterList(Level level) {
        ArrayList<EntityVoidMonster> result = new ArrayList<>();
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            // Spawns above the Nether ceiling can remain loaded without ever ticking.
            // The level lookup includes them immediately after insertion and chunk loading;
            // registration in tick() cannot enforce the dimension's spawn limit.
            for (Entity entity : server.getAllEntities()) {
                if (entity instanceof EntityVoidMonster monster && !monster.isRemoved())
                    result.add(monster);
            }
            return result;
        }
        synchronized (monsters) {
            Set<Integer> ids = monsters.get(level);
            if (ids != null) {
                ids.removeIf(id -> {
                    Entity entity = level.getEntity(id);
                    if (entity instanceof EntityVoidMonster monster && !monster.isRemoved()) {
                        result.add(monster);
                        return false;
                    }
                    return true;
                });
            }
        }
        return result;
    }

    private static void unload(LevelEvent.Unload event) {
        synchronized (monsters) { monsters.remove(event.getLevel()); }
    }

    @Override public String getModId() { return MODID; }
    @Override public String getDisplayName() { return "Void Monster"; }
    @Override public String getModAuthorName() { return "Reika"; }
    @Override public URL getDocumentationSite() { return DragonAPI.getReikaForumPage(); }
    @Override public URL getBugSite() { return DragonAPI.getReikaGithubPage(); }
    @Override public String getUpdateCheckURL() { return CommandableUpdateChecker.reikaURL; }
    @Override public Logger getModLogger() { return LOGGER; }
    @Override public File getConfigFolder() { return FMLPaths.CONFIGDIR.get().resolve("Reika").toFile(); }
}
