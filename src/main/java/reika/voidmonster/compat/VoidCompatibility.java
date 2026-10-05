/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.compat;

import java.lang.reflect.Method;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import reika.dragonapi.ModList;
import reika.dragonapi.interfaces.item.UnbreakableArmor;

/** Optional APIs stay isolated from server class loading until their mod is installed. */
public final class VoidCompatibility {
    private VoidCompatibility() {}

    public static boolean isDeepDark(Level level) {
        String key = level.dimension().identifier().toString();
        return ModList.EXTRAUTILS.isLoaded() && (key.equals("extrautils:deep_dark") || key.equals("extrautils2:deep_dark"));
    }

    public static void addTemporaryWarp(Player player, int amount) {
        // THAUMCRAFT-PORT: the DragonAPI bridge must retain the original temporary-warp call.
        if (ModList.THAUMCRAFT.isLoaded())
            invoke("reika.dragonapi.modinteract.deepinteract.ReikaThaumHelper", "addPlayerTempWarp",
                    new Class<?>[]{Player.class, int.class}, player, amount);
    }

    public static int drainLifePoints(LivingEntity entity, float attack) {
        if (!(entity instanceof Player player) || !ModList.BLOODMAGIC.isLoaded())
            return 0;
        // BLOODMAGIC-PORT: preserve the authored username SoulNetwork contract until its target exists.
        String name = player.getName().getString();
        int current = ((Number)invoke("WayofTime.alchemicalWizardry.api.soulNetwork.SoulNetworkHandler",
                "getCurrentEssence", new Class<?>[]{String.class}, name)).intValue();
        int amount = Math.min((int)Math.ceil(attack), current);
        if (amount > 0)
            invoke("WayofTime.alchemicalWizardry.api.soulNetwork.SoulNetworkHandler", "syphonFromNetwork",
                    new Class<?>[]{String.class, int.class}, name, amount);
        return Math.max(0, amount);
    }

    private static boolean thaumItem(String method, ItemStack stack) {
        return ModList.THAUMCRAFT.isLoaded() && (Boolean)invoke(
                "reika.dragonapi.modinteract.itemhandlers.ThaumItemHelper", method, new Class<?>[]{ItemStack.class}, stack);
    }

    public static float modifyArmorDamage(float factor, ItemStack stack) {
        if (thaumItem("isWarpingToolOrArmor", stack))
            factor -= thaumItem("isVoidMetalArmor", stack) ? 0.225F : 0.125F;
        return factor;
    }

    public static float weaponDamageMultiplier(ItemStack stack) {
        return thaumItem("isVoidMetalTool", stack) ? 2 : thaumItem("isWarpingToolOrArmor", stack) ? 1.5F : 1;
    }

    public static boolean isVoidMetalAmmo(Object ammo) {
        // ROTARY-PORT: ItemVoidMetalRailgunAmmo has not landed; retain its exact nested type contract.
        return ammo != null && ammo.getClass().getName().endsWith("ItemVoidMetalRailgunAmmo$VoidMetalRailGunAmmo");
    }

    public static int damageArmor(LivingEntity entity, EquipmentSlot slot, int amount) {
        if (entity instanceof Player && entity.level() instanceof ServerLevel server && !server.isPvpAllowed())
            return 0;
        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.isEmpty())
            return 0;
        ItemStack previous = stack.copy();
        int result = 0;
        EnergyHandler energy = ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
        if (energy != null) {
            try (Transaction transaction = Transaction.openRoot()) {
                energy.extract((int)Math.min(Integer.MAX_VALUE, 100 + energy.getAmountAsLong() / 5), transaction);
                result += energy.extract((int)Math.min(Integer.MAX_VALUE, (long)amount * 300), transaction);
                transaction.commit();
            }
        } else {
            if (thaumItem("isWarpingToolOrArmor", stack))
                result += thaumItem("isVoidMetalArmor", stack) ? amount * 5 / 4 : amount / 2;
            // IC2/Mekanism/ModularPowersuits energy and gas armor branches remain in the
            // retained DragonAPI source; their modern armor APIs are not available yet.
            if (!(stack.getItem() instanceof UnbreakableArmor armor) || armor.canBeDamaged()) {
                stack.hurtAndBreak(amount, entity, slot);
                entity.playSound(net.minecraft.sounds.SoundEvents.ITEM_BREAK.value(), 0.1F, 0.8F);
                result += amount;
            }
        }
        return ItemStack.matches(previous, entity.getItemBySlot(slot)) ? 0 : result;
    }

    private static Object invoke(String owner, String name, Class<?>[] parameters, Object... arguments) {
        try {
            Method method = Class.forName(owner).getMethod(name, parameters);
            return method.invoke(null, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Installed optional mod lacks the Void Monster bridge " + owner + "." + name, exception);
        }
    }
}
