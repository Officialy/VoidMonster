/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The original aura remains available; V33a deliberately disabled its periodic playback. */
public final class VoidSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, "voidmonster");
    public static final DeferredHolder<SoundEvent, SoundEvent> AURA = SOUNDS.register("aura",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("voidmonster", "aura")));
    private VoidSounds() {}
}
