package reika.voidmonster.render;

import java.lang.reflect.Method;
import net.neoforged.fml.ModList;

/** Query Iris's public API without making Iris a required client dependency. */
final class VoidShaderCompatibility {
    private record Api(Object instance, Method shadow) {}
    private static Api api;
    private static boolean resolved;

    private VoidShaderCompatibility() {}

    static boolean shadowPass() {
        if (!resolved) {
            if (ModList.get().isLoaded("iris")) {
                try {
                    Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                    api = new Api(type.getMethod("getInstance").invoke(null), type.getMethod("isRenderingShadowPass"));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Loaded Iris does not expose its supported rendering API", e);
                }
            }
            resolved = true;
        }
        if (api == null) return false;
        try {
            return (boolean)api.shadow().invoke(api.instance());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot query the Iris render pass", e);
        }
    }
}
