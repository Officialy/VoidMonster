/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.data;

import com.google.gson.JsonParser;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/** Client resource definitions are generated alongside the original, byte-for-byte atlas/audio assets. */
public final class VoidClientData implements DataProvider {
    private final Path output;
    public VoidClientData(PackOutput pack) { output = pack.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("voidmonster"); }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(
                save(cache, "sounds.json", """
                    {"aura":{"sounds":[{"name":"voidmonster:aura3","type":"file"}]}}
                    """),
                save(cache, "post_effect/voiddistort.json", """
                    {"targets":{},"passes":[{
                      "vertex_shader":"minecraft:core/screenquad","fragment_shader":"minecraft:post/blit",
                      "inputs":[{"sampler_name":"In","target":"voidmonster:warped"}],
                      "uniforms":{"BlitConfig":[{"name":"ColorModulate","type":"vec4","value":[1.0,1.0,1.0,1.0]}]},
                      "output":"minecraft:main"
                    }]}
                    """));
    }
    private CompletableFuture<?> save(CachedOutput cache, String file, String json) {
        return DataProvider.saveStable(cache, JsonParser.parseString(json), output.resolve(file));
    }
    @Override public String getName() { return "Void Monster sounds and distortion chain"; }
    public static final class Language extends LanguageProvider {
        public Language(PackOutput pack) { super(pack, "voidmonster", "en_us"); }
        @Override protected void addTranslations() {
            add("entity.voidmonster.void_monster", "Void Monster");
            add("death.attack.voidmonster.ghost", "%1$s was expelled by %2$s");
            add("death.attack.voidmonster.ghost.player", "%1$s was expelled by %2$s");
        }
    }
}
