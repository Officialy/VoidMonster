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

import java.util.HashMap;
import java.util.Map;

/** World-local absolute game-time deadlines; extending a cooldown never shortens it. */
public final class MonsterCooldowns<K> {
    private final Map<K, Long> deadlines = new HashMap<>();

    public boolean active(K world, long gameTime) {
        Long deadline = deadlines.get(world);
        if (deadline == null)
            return false;
        if (gameTime < deadline)
            return true;
        deadlines.remove(world);
        return false;
    }

    public void add(K world, long gameTime, int ticks) {
        if (ticks < 0)
            throw new IllegalArgumentException("Negative monster cooldown: " + ticks);
        deadlines.merge(world, Math.addExact(gameTime, ticks), Math::max);
    }

    public void clear(K world) {
        deadlines.remove(world);
    }
}
