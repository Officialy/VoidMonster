/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.api;

import net.minecraft.world.entity.Entity;

@FunctionalInterface
public interface VoidMonsterHook {
    void tick(Entity entity);
}
