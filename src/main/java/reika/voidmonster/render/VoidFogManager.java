/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.render;

import net.minecraft.client.Minecraft;
import reika.voidmonster.VoidMonster;

/** Original height curves, measured from the dimension's current minimum build height. */
public final class VoidFogManager {
    private VoidFogManager() {}
    private static double effectiveHeight() {
        var player = Minecraft.getInstance().player;
        var level = Minecraft.getInstance().level;
        double strength = VoidMonster.config.getFogStrength();
        if (player == null || level == null || player.noPhysics || level.getLevelData().voidDarknessOnsetRange() == 1
                || !VoidMonster.allowedIn(level) || strength <= 0)
            return Double.POSITIVE_INFINITY;
        double y = (player.getY() - level.getMinY()) / (strength < 1 ? Math.sqrt(strength) : strength);
        return y < 24 ? y : Double.POSITIVE_INFINITY;
    }
    public static float getFogDistance() {
        double y = effectiveHeight();
        double strength = VoidMonster.config.getFogStrength();
        double minimum = strength <= 1 ? 12 : Math.min(24, 12 / Math.sqrt(strength));
        return Double.isFinite(y) ? curve(y, new double[]{0, 4, 16, 24}, new double[]{minimum, 18, 24, 512}) : Float.MAX_VALUE;
    }
    public static float getColorFactor() {
        double y = effectiveHeight();
        return Double.isFinite(y) ? curve(y, new double[]{0, 4, 16, 20, 24}, new double[]{1, 1, 0.25, 0.5, 0}) : 0;
    }
    static float curve(double x, double[] points, double[] values) {
        if (x <= points[0]) return (float)values[0];
        for (int i = 1; i < points.length; i++)
            if (x <= points[i]) return (float)(values[i - 1] + (values[i] - values[i - 1]) * (x - points[i - 1]) / (points[i] - points[i - 1]));
        return (float)values[values.length - 1];
    }
    public static float limitDistance(float original) {
        return Math.min(original, Math.min(MonsterFX.rampFog(original), getFogDistance()));
    }
}
