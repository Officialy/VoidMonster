/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.auxiliary;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import reika.dragonapi.instantiable.io.CustomRecipeList;
import reika.dragonapi.instantiable.io.LuaBlock;
import reika.voidmonster.VoidMonster;
import reika.voidmonster.entity.EntityVoidMonster;

/** Authored drops come from the generated loot table; this list contains API/config additions only. */
public final class VoidMonsterDrops {
    public static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath("voidmonster", "entities/void_monster"));
    private static final List<Drop> apiDrops = new CopyOnWriteArrayList<>();
    private static final List<Drop> configuredDrops = new CopyOnWriteArrayList<>();
    private VoidMonsterDrops() {}

    public static void loadCustomDrops(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        configuredDrops.clear();
        DropList list = new DropList();
        if (list.load()) {
            for (LuaBlock block : list.getEntries()) {
                try {
                    ItemStack stack;
                    if (block.containsKey("item_stack_file")) {
                        var folder = VoidMonster.instance.getConfigFolder().toPath().resolve("Void Monster_CustomDrops").toAbsolutePath().normalize();
                        var file = folder.resolve(block.getString("item_stack_file")).normalize();
                        if (!file.startsWith(folder)) throw new IllegalArgumentException("Drop JSON must be inside the custom-drop folder");
                        stack = ItemStack.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()),
                                JsonParser.parseString(java.nio.file.Files.readString(file))).getOrThrow();
                    } else {
                        stack = LegacyDropCodec.parse(server, block.getString("item"), block.getChild("item_nbt"));
                    }
                    add(configuredDrops, stack, Math.max(1, block.getInt("min")), Math.max(1, block.getInt("max")),
                            block.getDouble("required_difficulty"));
                    VoidMonster.LOGGER.info("Loaded custom monster drop '{}'", block.getString("type"));
                } catch (RuntimeException | java.io.IOException exception) {
                    VoidMonster.LOGGER.error("Could not load custom monster drop '{}'", block.getString("type"), exception);
                }
            }
        } else {
            list.createFolders();
            LuaBlock first = list.createExample("customDrop1");
            first.putData("type", "customDrop1");
            CustomRecipeList.writeItem(first, new ItemStack(Items.BONE_MEAL));
            first.putData("item", "minecraft:bone_meal");
            first.putData("min", 3);
            first.putData("max", 14);
            LuaBlock second = list.createExample("customDrop2");
            second.putData("type", "customDrop2");
            second.putData("item", "minecraft:redstone");
            second.putData("min", 1);
            second.putData("max", 6);
            second.putData("required_difficulty", 0.4);
            LuaBlock third = list.createExample("customDrop3");
            third.putData("type", "customDrop3");
            ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
            axe.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 2);
            third.putData("item", "minecraft:diamond_axe");
            // LuaBlock's legacy parser interprets every brace as a block delimiter;
            // keep full native component JSON in a companion file instead.
            third.putData("item_stack_file", "example_fortune_axe.json");
            var json = VoidMonster.instance.getConfigFolder().toPath().resolve("Void Monster_CustomDrops/example_fortune_axe.json");
            try {
                if (!java.nio.file.Files.exists(json)) java.nio.file.Files.writeString(json,
                        ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()), axe).getOrThrow().toString());
            } catch (java.io.IOException exception) { throw new IllegalStateException("Cannot write the original custom-drop example", exception); }
            third.putData("required_difficulty", 1.5);
            list.createExampleFile();
        }
    }

    public static void addDrop(ItemStack stack, int minimum, int maximum, double difficulty) {
        add(apiDrops, stack, minimum, maximum, difficulty);
    }
    public static void addDrop(net.minecraft.world.item.Item item, double difficulty) { addDrop(new ItemStack(item), difficulty); }
    public static void addDrop(net.minecraft.world.level.block.Block block, double difficulty) { addDrop(new ItemStack(block), difficulty); }
    public static void addDrop(ItemStack stack, double difficulty) { addDrop(stack, 1, 1, difficulty); }
    public static void addDrop(net.minecraft.world.item.Item item, int min, int max, double difficulty) { addDrop(new ItemStack(item), min, max, difficulty); }
    public static void addDrop(net.minecraft.world.level.block.Block block, int min, int max, double difficulty) { addDrop(new ItemStack(block), min, max, difficulty); }

    private static void add(List<Drop> drops, ItemStack stack, int minimum, int maximum, double difficulty) {
        if (stack == null || stack.isEmpty() || minimum < 0 || maximum < minimum || maximum == Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid Void Monster drop");
        // Original ItemDrop identity ignores the count range and difficulty threshold.
        if (drops.stream().noneMatch(drop -> ItemStack.isSameItemSameComponents(drop.stack(), stack))) {
            drops.add(new Drop(stack.copy(), minimum, maximum, difficulty));
            VoidMonster.LOGGER.info("Adding monster drop {}", stack.getHoverName().getString());
        }
    }

    public static void doDrops(EntityVoidMonster monster) {
        if (!(monster.level() instanceof ServerLevel server) || monster.isGhost())
            return;
        for (List<Drop> drops : List.of(apiDrops, configuredDrops)) {
            for (Drop drop : drops) {
                if (monster.getDifficulty() < drop.difficulty())
                    continue;
                int count = drop.minimum() + (int)(monster.getDifficulty() * monster.getRandom().nextInt(1 + drop.maximum() - drop.minimum()));
                if (count <= 0)
                    continue;
                ItemStack stack = drop.stack().copyWithCount(count);
                ItemEntity item = monster.spawnAtLocation(server, stack);
                if (item != null)
                    item.setPermanentlyInvulnerable(true);
            }
        }
    }

    private record Drop(ItemStack stack, int minimum, int maximum, double difficulty) {}
    private static final class DropList extends CustomRecipeList {
        private DropList() { super(VoidMonster.instance, ""); }
        @Override protected String getFolderName() { return "CustomDrops"; }
        @Override protected String getExtension() { return ".drops"; }
    }
}
