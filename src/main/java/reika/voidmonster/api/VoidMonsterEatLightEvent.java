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

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.ICancellableEvent;
import reika.dragonapi.instantiable.event.base.WorldPositionEvent;

/** Cancellation cannot protect ordinary torches or glowstone, as in the original API. */
public final class VoidMonsterEatLightEvent extends WorldPositionEvent implements ICancellableEvent {
    public final Block block;
    public final BlockState state;
    public VoidMonsterEatLightEvent(Level level, BlockPos pos, BlockState state) {
        super(level, pos);
        this.state = state;
        block = state.getBlock();
    }
}
