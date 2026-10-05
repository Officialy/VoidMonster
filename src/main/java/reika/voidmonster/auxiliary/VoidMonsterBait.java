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

public interface VoidMonsterBait {
    boolean isActive();
    double maxRangeSquared();
    void attack(double damage);
}
