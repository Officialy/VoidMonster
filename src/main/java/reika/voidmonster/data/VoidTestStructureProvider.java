/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import net.minecraft.SharedConstants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import reika.voidmonster.VoidMonster;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Generates the empty (stone-floored) structure template that the in-world game tests run on.
 * Game tests reference an arena via {@code TestData.structure}; rather than hand-author binary
 * NBT, we emit a flat {@code size×1 floor + air} template here so {@code gradlew runServerData}
 * keeps it in sync. See {@link reika.voidmonster.VoidGameTests}.
 */
public class VoidTestStructureProvider implements DataProvider {

    public static final Identifier ARENA = Identifier.fromNamespaceAndPath(VoidMonster.MODID, "test_arena");
    private static final int SIZE_XZ = 9;
    private static final int SIZE_Y = 9;

    private final PackOutput output;

    public VoidTestStructureProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        // StructureTemplate NBT: a `SIZE_XZ × SIZE_Y × SIZE_XZ` box with a stone floor at y=0 and
        // air above. Game tests place this, clearing the volume, then build their own setup on top.
        CompoundTag root = new CompoundTag();
        root.putInt("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());

        ListTag size = new ListTag();
        size.add(IntTag.valueOf(SIZE_XZ));
        size.add(IntTag.valueOf(SIZE_Y));
        size.add(IntTag.valueOf(SIZE_XZ));
        root.put("size", size);

        root.put("entities", new ListTag());

        ListTag palette = new ListTag();
        CompoundTag stone = new CompoundTag();
        stone.putString("id", "minecraft:stone"); // 26.3 block state NBT key (was "Name")
        palette.add(stone);
        root.put("palette", palette);

        ListTag blocks = new ListTag();
        for (int x = 0; x < SIZE_XZ; x++) {
            for (int z = 0; z < SIZE_XZ; z++) {
                CompoundTag b = new CompoundTag();
                ListTag pos = new ListTag();
                pos.add(IntTag.valueOf(x));
                pos.add(IntTag.valueOf(0));
                pos.add(IntTag.valueOf(z));
                b.put("pos", pos);
                b.putInt("state", 0);
                blocks.add(b);
            }
        }
        root.put("blocks", blocks);

        Path path = output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(ARENA.getNamespace()).resolve("structure").resolve(ARENA.getPath() + ".nbt");

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            HashingOutputStream hashing = new HashingOutputStream(Hashing.sha1(), bytes);
            NbtIo.writeCompressed(root, hashing);
            cache.writeIfNeeded(path, bytes.toByteArray(), hashing.hash());
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String getName() {
        return "Void Monster Test Arena Structure";
    }
}
