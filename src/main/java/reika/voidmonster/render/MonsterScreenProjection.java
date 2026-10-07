package reika.voidmonster.render;

import org.joml.Vector4fc;

/** A world-space focus must project into the player's viewport before it can warp that view. */
final class MonsterScreenProjection {
    private MonsterScreenProjection() {}

    static boolean isOnScreen(Vector4fc point) {
        return Float.isFinite(point.x()) && Float.isFinite(point.y())
                && Float.isFinite(point.z()) && Float.isFinite(point.w())
                && point.w() > 0.0001F
                && Math.abs(point.x()) <= point.w() && Math.abs(point.y()) <= point.w();
    }
}
