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

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PlayerLookAtVoidMonsterEvent extends PlayerEvent {
    public final LivingEntity monster;
    public PlayerLookAtVoidMonsterEvent(Player player, LivingEntity monster) {
        super(player);
        this.monster = monster;
    }
}
