package de.omegazirkel.risingworld.servicenpc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.omegazirkel.risingworld.ServiceNpcPlugin;
import net.risingworld.api.Plugin;

/** Reads the world-local, administrator-editable name pool. */
public final class NameCatalog {
    private List<String> male = List.of("Alden");
    private List<String> female = List.of("Elara");
    private int maleCursor;
    private int femaleCursor;

    public void load(Plugin plugin) {
        try {
            Path path = WorldConfigFiles.resolve(plugin, "names");
            JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            List<String> loadedMale = names(json, "male");
            List<String> loadedFemale = names(json, "female");
            if (loadedMale.size() < 2 || loadedFemale.size() < 2) throw new IllegalStateException("both pools need at least two names");
            male = loadedMale;
            female = loadedFemale;
            maleCursor = ThreadLocalRandom.current().nextInt(male.size());
            femaleCursor = ThreadLocalRandom.current().nextInt(female.size());
            ServiceNpcPlugin.logger().info("Loaded Service NPC names from " + path.getFileName() + ": male=" + male.size() + ", female=" + female.size());
        } catch (Exception exception) {
            ServiceNpcPlugin.logger().error("Cannot load Service NPC name catalog: " + exception.getMessage());
        }
    }

    /** A shuffled starting point and full cycle avoid duplicates before the pool is exhausted. */
    public String random(boolean isMale, Random random) {
        List<String> values = isMale ? male : female;
        if (values.isEmpty()) return isMale ? "Alden" : "Elara";
        int index = isMale ? maleCursor++ : femaleCursor++;
        return values.get(Math.floorMod(index, values.size()));
    }

    private static List<String> names(JsonObject json, String key) {
        if (json == null || !json.has(key) || !json.get(key).isJsonArray()) return List.of();
        JsonArray values = json.getAsJsonArray(key);
        List<String> result = new ArrayList<>();
        for (var value : values) if (value.isJsonPrimitive() && !value.getAsString().isBlank()) result.add(value.getAsString().trim());
        return List.copyOf(result);
    }
}
