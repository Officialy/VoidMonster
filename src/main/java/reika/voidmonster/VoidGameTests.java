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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import reika.voidmonster.api.VoidMonsterEatLightEvent;
import reika.voidmonster.auxiliary.VoidMonsterDamage;
import reika.voidmonster.auxiliary.VoidMonsterDrops;
import reika.voidmonster.data.VoidTestStructureProvider;
import reika.voidmonster.entity.EntityVoidMonster;
import reika.voidmonster.registry.VoidEntities;

/** Server contracts exercise registered entities, the live reloadable loot registry, and real potion dispatch. */
public final class VoidGameTests {
    public static final DeferredRegister<MapCodec<? extends GameTestInstance>> TYPES = DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, "voidmonster");
    static { TYPES.register("contract", () -> Contract.CODEC); }
    private VoidGameTests() {}
    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String name : List.of("normal_damage", "ghost_splash", "ghost_regeneration", "persistence", "loot", "light_event", "legacy_drop", "optional_rotary", "spawn_singleton")) {
            var data = new TestData<>(environment, VoidTestStructureProvider.ARENA, 40, 0, true, Rotation.NONE);
            event.registerTest(id(name), new Contract(data, name));
        }
    }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("voidmonster", path); }
    private static EntityVoidMonster monster(GameTestHelper helper) {
        var monster = VoidEntities.VOID_MONSTER.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(monster != null, "registered monster factory must create a monster");
        monster.forcePersist = true;
        monster.snapTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(4, 2, 4)));
        return monster;
    }
    private static void normalDamage(GameTestHelper helper) {
        var monster = monster(helper);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setInvulnerableTime(0);
        player.setHealth(20);
        var source = helper.getLevel().damageSources().playerAttack(player);
        helper.assertTrue(monster.getDamageCap(source, 100) == 20, "normal player damage is capped at 20");
        helper.assertTrue(monster.hurtServer(helper.getLevel(), source, 100), "player attack must land");
        helper.assertTrue(monster.getHealth() == 280, "only capped damage reaches monster health");
        helper.assertTrue(player.getHealth() == 5, "excess damage reflects at most 15");
        helper.assertTrue(monster.getDamageCap(source, 10) == 0, "normal damage enters hit cooldown");
        helper.assertTrue(!monster.hurtServer(helper.getLevel(), helper.getLevel().damageSources().fellOutOfWorld(), 100), "void cannot hurt its resident");
        helper.succeed();
    }
    private static void ghostSplash(GameTestHelper helper) {
        var ghost = monster(helper).setGhost();
        helper.getLevel().addFreshEntity(ghost);
        var owner = helper.makeMockServerPlayerInLevel();
        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.HEALING));
        var splash = new ThrownSplashPotion(helper.getLevel(), owner, potion);
        splash.snapTo(ghost.position());
        splash.onHitAsPotion(helper.getLevel(), potion, new BlockHitResult(ghost.position(), Direction.UP, ghost.blockPosition(), false));
        helper.assertTrue(ghost.getHealth() == 250, "real healing splash must expel for 50 instead of healing the ghost");
        helper.assertTrue(ghost.getDamageCap(helper.getLevel().damageSources().playerAttack(owner), 100) == 0, "ghost ignores ordinary weapons");
        ghost.forcePersist = false;
        ghost.discard();
        helper.succeed();
    }
    private static void ghostRegeneration(GameTestHelper helper) {
        var ghost = monster(helper).setGhost();
        helper.assertTrue(!ghost.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1)), "regen is consumed rather than stored");
        helper.assertTrue(ghost.getHealth() == 250, "regen expulsion respects the special 50 damage cap");
        helper.assertTrue(!ghost.hasEffect(MobEffects.REGENERATION), "ghost must not retain regeneration");
        helper.succeed();
    }
    private static void persistence(GameTestHelper helper) {
        var original = monster(helper).setNether().setGhost();
        original.increaseDifficulty(2);
        original.setHealth(175);
        TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        original.saveWithoutId(saved);
        var restored = monster(helper);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved.buildResult()));
        helper.assertTrue(restored.isNetherVoid() && restored.isGhost() && restored.forcePersist, "spawn flags and persistence survive reload");
        helper.assertTrue(restored.getDifficulty() == 2 && restored.getHealth() == 175, "difficulty and damaged health survive reload");
        helper.assertTrue(restored.getMaxHealth() == original.getMaxHealth(), "difficulty health modifier survives reload");
        restored.increaseDifficulty(2);
        helper.assertTrue(restored.getDifficulty() == 4, "a second difficulty boost after reload remains valid");
        helper.succeed();
    }
    private static List<ItemStack> loot(GameTestHelper helper, EntityVoidMonster monster, long seed) {
        var params = new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.THIS_ENTITY, monster)
                .withParameter(LootContextParams.ORIGIN, monster.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, new VoidMonsterDamage(monster)).create(LootContextParamSets.ENTITY);
        return helper.getLevel().getServer().reloadableRegistries().getLootTable(VoidMonsterDrops.LOOT_TABLE).getRandomItems(params, seed);
    }
    private static void loot(GameTestHelper helper) {
        var monster = monster(helper);
        var drops = loot(helper, monster, 25);
        helper.assertTrue(drops.stream().filter(stack -> stack.is(Items.ENCHANTED_BOOK)).count() == 5, "difficulty one yields five enchanted books");
        helper.assertTrue(drops.stream().filter(stack -> stack.is(Items.ENCHANTED_BOOK))
                .allMatch(stack -> stack.has(DataComponents.STORED_ENCHANTMENTS)), "each book carries a real enchantment component");
        Set<net.minecraft.world.item.Item> expected = Set.of(Items.DIAMOND, Items.GHAST_TEAR, Items.GLISTERING_MELON_SLICE,
                Items.EMERALD, Items.ENDER_PEARL, Items.ENDER_EYE, Items.FIRE_CHARGE, Items.NETHER_WART, Items.NETHER_STAR, Items.OBSIDIAN, Items.GUNPOWDER);
        helper.assertTrue(drops.stream().filter(stack -> !stack.is(Items.ENCHANTED_BOOK)).map(ItemStack::getItem).collect(java.util.stream.Collectors.toSet()).equals(expected), "loot includes exactly the eleven authored items");
        helper.assertTrue(loot(helper, monster.setGhost(), 25).isEmpty(), "ghost loot is empty");
        helper.succeed();
    }
    private static void lightEvent(GameTestHelper helper) {
        var monster = monster(helper);
        BlockPos pos = monster.blockPosition().offset(1, 0, 0);
        AtomicInteger events = new AtomicInteger();
        Consumer<VoidMonsterEatLightEvent> cancel = event -> { events.incrementAndGet(); event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            helper.getLevel().setBlockAndUpdate(pos, Blocks.SEA_LANTERN.defaultBlockState());
            monster.aiStep();
            helper.assertTrue(events.get() > 0 && helper.getLevel().getBlockState(pos).is(Blocks.SEA_LANTERN), "canceled non-core light survives");
            helper.getLevel().setBlockAndUpdate(pos, Blocks.GLOWSTONE.defaultBlockState());
            monster.aiStep();
            helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "ordinary glowstone is consumed even if the event is canceled");
        } finally { NeoForge.EVENT_BUS.unregister(cancel); }
        helper.succeed();
    }
    private static void legacyDrop(GameTestHelper helper) {
        var tag = new net.minecraft.nbt.CompoundTag();
        var ench = new net.minecraft.nbt.ListTag();
        var fortune = new net.minecraft.nbt.CompoundTag();
        fortune.putShort("id", (short)35);
        fortune.putShort("lvl", (short)2);
        ench.add(fortune);
        tag.put("ench", ench);
        tag.putString("voidmonster_test_custom", "preserved");
        ItemStack axe = reika.voidmonster.auxiliary.LegacyDropCodec.migrate(helper.getLevel().getServer(), "minecraft:diamond_axe", 7, 1, tag);
        var expected = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
        helper.assertTrue(axe.getDamageValue() == 7 && axe.getEnchantments().getLevel(expected) == 2, "legacy durability and Fortune II become native components");
        helper.assertTrue(axe.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getStringOr("voidmonster_test_custom", "").equals("preserved"), "unknown original NBT remains available");
        ItemStack dye = reika.voidmonster.auxiliary.LegacyDropCodec.migrate(helper.getLevel().getServer(), "minecraft:dye", 15, 1, new net.minecraft.nbt.CompoundTag());
        helper.assertTrue(dye.is(Items.BONE_MEAL), "legacy dye metadata 15 maps to bone meal");
        helper.succeed();
    }
    private static void spawnSingleton(GameTestHelper helper) {
        var level = helper.getLevel();
        var resident = monster(helper).setNether();
        resident.snapTo(resident.getX(), level.getMaxY() + 5, resident.getZ(), 0, 0);
        level.addFreshEntity(resident);
        var player = helper.makeMockServerPlayerInLevel();
        try {
            helper.assertTrue(resident.tickCount == 0, "resident has not ticked or registered itself");
            helper.assertTrue(VoidMonster.getCurrentMonsterList(level).contains(resident), "above-ceiling resident must count before its first tick");
            for (int attempt = 0; attempt < 4; attempt++)
                helper.assertTrue(!reika.voidmonster.world.MonsterGenerator.instance.spawnIfAbsent(level, player), "repeated spawn attempts must keep the existing monster");
            helper.assertTrue(VoidMonster.getCurrentMonsterList(level).stream().filter(e -> e == resident).count() == 1, "resident remains registered exactly once");
        } finally {
            resident.forcePersist = false;
            resident.discard();
        }
        helper.succeed();
    }
    private static void optionalRotary(GameTestHelper helper) {
        var monster = monster(helper);
        boolean rotary = net.neoforged.fml.ModList.get().isLoaded("rotarycraft");
        helper.assertTrue(monster.getClass().getName().equals(rotary ? "reika.voidmonster.compat.RotaryMonster" : EntityVoidMonster.class.getName()), "factory must select the installed compatibility path");
        if (rotary) {
            try {
                var method = monster.getClass().getMethod("jamRadar", net.minecraft.world.level.Level.class, BlockPos.class);
                helper.assertTrue((Boolean)method.invoke(monster, helper.getLevel(), monster.blockPosition()), "Rotary radar jamming remains active");
            } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
        }
        helper.succeed();
    }
    private static final class Contract extends GameTestInstance {
        static final MapCodec<Contract> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                TestData.CODEC.fieldOf("data").forGetter(test -> test.data), Codec.STRING.fieldOf("case").forGetter(test -> test.name)).apply(instance, Contract::new));
        final TestData<Holder<TestEnvironmentDefinition<?>>> data;
        final String name;
        Contract(TestData<Holder<TestEnvironmentDefinition<?>>> data, String name) { super(data); this.data = data; this.name = name; }
        @Override public void run(GameTestHelper helper) {
            switch (name) {
                case "normal_damage" -> normalDamage(helper);
                case "ghost_splash" -> ghostSplash(helper);
                case "ghost_regeneration" -> ghostRegeneration(helper);
                case "persistence" -> persistence(helper);
                case "loot" -> loot(helper);
                case "light_event" -> lightEvent(helper);
                case "legacy_drop" -> legacyDrop(helper);
                case "optional_rotary" -> optionalRotary(helper);
                case "spawn_singleton" -> spawnSingleton(helper);
                default -> throw new IllegalArgumentException("Unknown Void Monster contract " + name);
            }
        }
        @Override public MapCodec<? extends GameTestInstance> codec() { return CODEC; }
        @Override protected MutableComponent typeDescription() { return Component.literal("Void Monster contract " + name); }
    }
}
