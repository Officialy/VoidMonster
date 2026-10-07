package reika.voidmonster.render;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MonsterScreenProjectionTest {
    private final Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(70), 16F / 9F, .05F, 1024);

    @Test void turningAwayRejectsAFormerlyVisibleMonster() {
        Vector4f focus = new Vector4f(0, 0, -12, 1);
        assertTrue(MonsterScreenProjection.isOnScreen(new Vector4f(focus).mul(projection)));
        assertFalse(MonsterScreenProjection.isOnScreen(new Vector4f(focus)
                .mul(new Matrix4f().rotateY((float)Math.toRadians(75))).mul(projection)));
        assertFalse(MonsterScreenProjection.isOnScreen(new Vector4f(focus)
                .mul(new Matrix4f().rotateY((float)Math.PI)).mul(projection)));
    }

    @Test void movingPastTheMonsterRejectsTheStaleFocus() {
        assertTrue(MonsterScreenProjection.isOnScreen(new Vector4f(0, 0, -12, 1).mul(projection)));
        assertFalse(MonsterScreenProjection.isOnScreen(new Vector4f(-100, 0, -12, 1).mul(projection)));
        assertFalse(MonsterScreenProjection.isOnScreen(new Vector4f(0, -100, -12, 1).mul(projection)));
        assertFalse(MonsterScreenProjection.isOnScreen(new Vector4f(0, 0, 100, 1).mul(projection)));
    }

    @Test void invalidAndNearPlaneFocusCannotReachTheShader() {
        assertFalse(MonsterScreenProjection.isOnScreen(new Vector4f(0, 0, 0, 0)));
        assertFalse(MonsterScreenProjection.isOnScreen(new Vector4f(0, 0, 0, .00001F)));
        assertFalse(MonsterScreenProjection.isOnScreen(new Vector4f(Float.NaN, 0, 0, 1)));
        assertFalse(MonsterScreenProjection.isOnScreen(new Vector4f(0, Float.POSITIVE_INFINITY, 0, 1)));
        assertFalse(MonsterScreenProjection.isOnScreen(new Vector4f(0, 0, Float.NaN, 1)));
        assertFalse(MonsterScreenProjection.isOnScreen(new Vector4f(0, 0, 0, Float.POSITIVE_INFINITY)));
        assertTrue(MonsterScreenProjection.isOnScreen(new Vector4f(1, -1, 0, 1)));
    }
}
