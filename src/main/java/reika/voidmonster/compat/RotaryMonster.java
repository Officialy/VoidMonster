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

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import reika.rotarycraft.api.interfaces.RadarJammer;
import reika.rotarycraft.api.interfaces.TargetEntity;
import reika.rotarycraft.api.interfaces.RailGunAmmo.RailGunAmmoType;
import reika.voidmonster.entity.EntityVoidMonster;

/** Only instantiated when RotaryCraft is installed; the base monster loads without its API. */
public final class RotaryMonster extends EntityVoidMonster implements RadarJammer, TargetEntity {
    public RotaryMonster(EntityType<? extends EntityVoidMonster> type, Level level) { super(type, level); }
    public static EntityType.EntityFactory<EntityVoidMonster> factory() { return RotaryMonster::new; }
    @Override public boolean jamRadar(Level level, BlockPos pos) { return true; }
    @Override public boolean onRailgunImpact(BlockEntity source, RailGunAmmoType ammo) {
        if (VoidCompatibility.isVoidMetalAmmo(ammo)) {
            if (!isHealing() && level() instanceof ServerLevel server)
                damageFromVoidMetalAmmo(server);
            return true;
        }
        return false;
    }
    @Override public double getKnockbackMultiplier(BlockEntity source, RailGunAmmoType ammo) {
        return ammo == null ? 0 : (ammo.isExplosive() ? 0.5 : 0.125) * ammo.getMass() / 5000D;
    }
    @Override public void onLaserBeam(BlockEntity source) { igniteForSeconds(5); }
    @Override public void onFreeze(BlockEntity source) { /* Upstream immunity. */ }
    @Override public void flakShot(BlockEntity source) { /* Upstream immunity. */ }
    @Override public boolean shouldTarget(BlockEntity source, UUID owner) { return getY() > level().getMinY() + 2 && !isHealing(); }
}
