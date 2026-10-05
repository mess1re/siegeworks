package me.mss1r.siegeworks.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(Siegeworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConstructionSectionsGameTests {
    private ConstructionSectionsGameTests() {
    }

    /**
     * If an engine's build sections don't match its model's cube count, it renders as finished while still under
     * construction. Checks every engine.
     */
    @GameTest(template = "empty")
    public static void everyEngineBuildsInTheStagesItsModelHas(GameTestHelper helper) {
        List<String> drifted = new ArrayList<>();
        int checked = 0;
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (!id.getNamespace().equals(Siegeworks.MOD_ID)) {
                continue;
            }
            JsonObject sections = read("/data/siegeworks/construction/" + id.getPath() + ".json");
            if (sections == null) {
                continue;
            }
            checked++;
            String model = sections.get("model").getAsString();
            JsonObject geometry = read("/assets/siegeworks/" + model.substring(model.indexOf(':') + 1));
            helper.assertTrue(geometry != null, id + " builds from a model that is missing: " + model);
            Map<String, Integer> expected = new LinkedHashMap<>();
            sections.getAsJsonObject("boneCubes").entrySet()
                    .forEach(entry -> expected.put(entry.getKey(), entry.getValue().getAsInt()));
            Map<String, Integer> loaded = new LinkedHashMap<>();
            for (JsonElement bone : geometry.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                    .getAsJsonArray("bones")) {
                JsonObject object = bone.getAsJsonObject();
                if (object.has("cubes") && object.getAsJsonArray("cubes").size() > 0) {
                    loaded.put(object.get("name").getAsString(), object.getAsJsonArray("cubes").size());
                }
            }
            if (!expected.equals(loaded)) {
                drifted.add(id.getPath());
            }
        }
        helper.assertTrue(checked > 0, "No engine has build sections to check");
        helper.assertTrue(drifted.isEmpty(), "Build sections no longer match the models of " + drifted);
        helper.succeed();
    }

    private static JsonObject read(String path) {
        try (InputStream stream = ConstructionSectionsGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
