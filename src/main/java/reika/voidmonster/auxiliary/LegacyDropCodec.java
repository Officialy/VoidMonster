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

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;
import reika.dragonapi.instantiable.io.CustomRecipeList;
import reika.dragonapi.instantiable.io.LuaBlock;
import reika.dragonapi.libraries.ReikaNBTHelper;

import java.util.regex.Pattern;

/** Vanilla's data fixer carries original metadata/enchantments/display NBT into native 26.3 components. */
public final class LegacyDropCodec {
    private static final Pattern ITEM = Pattern.compile("([a-z0-9_.-]+:[a-z0-9_./-]+)(?::(\\d+))?(?:\\*(\\d+))?");
    private LegacyDropCodec() {}
    public static ItemStack parse(MinecraftServer server, String item, LuaBlock nbt) {
        var match = ITEM.matcher(item);
        if (item.startsWith("delegate:")) return CustomRecipeList.parseItemString(item, nbt, false);
        if (!match.matches()) return CustomRecipeList.parseItemString(item, nbt, false);
        int metadata = match.group(2) == null ? 0 : Integer.parseInt(match.group(2));
        int count = match.group(3) == null ? 1 : Integer.parseInt(match.group(3));
        if (metadata > Short.MAX_VALUE || count < 1 || count > 64) throw new IllegalArgumentException("Invalid legacy stack " + item);
        return migrate(server, match.group(1), metadata, count, nbt == null ? new CompoundTag() : ReikaNBTHelper.constructNBT(nbt));
    }
    public static ItemStack migrate(MinecraftServer server, String id, int metadata, int count, CompoundTag tag) {
        CompoundTag stack = new CompoundTag();
        stack.putString("id", id);
        stack.putByte("Count", (byte)count);
        stack.putShort("Damage", (short)metadata);
        stack.put("tag", tag.copy());
        // 99 is the pre-data-version schema, which accepts 1.7's string IDs and
        // numeric enchantments. The full fix chain also renames vanilla items.
        var modern = server.getFixerUpper().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, stack),
                99, SharedConstants.getCurrentVersion().dataVersion().version()).getValue();
        ItemStack result = ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, server.registryAccess()), modern).getOrThrow();
        if (result.isEmpty()) throw new IllegalArgumentException("Unknown migrated drop " + id);
        return result;
    }
}
