package de.omegazirkel.risingworld.servicenpc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.omegazirkel.risingworld.ServiceNpcPlugin;
import net.risingworld.api.Plugin;

/** World-local, administrator-editable NPC clothing sets. */
public final class OutfitCatalog {
    private List<Outfit> outfits = List.of();

    public void load(Plugin plugin) {
        try {
            Path path = WorldConfigFiles.resolve(plugin, "outfits");
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Outfit> parsed = parse(root);
            if (parsed.isEmpty()) throw new IllegalStateException("no valid outfits");
            outfits = parsed;
            ServiceNpcPlugin.logger().info("Loaded Service NPC outfits from " + path.getFileName() + ": " + outfits.size() + " sets.");
        } catch (Exception exception) {
            outfits = List.of();
            ServiceNpcPlugin.logger().error("Cannot load Service NPC outfit catalog: " + exception.getMessage());
        }
    }

    public Outfit initial(String seed) { return outfits.isEmpty() ? null : outfits.get(Math.floorMod(seed.hashCode(), outfits.size())); }
    public Outfit find(String key, String seed) {
        if (key != null && !key.isBlank()) for (Outfit outfit : outfits) if (outfit.key().equalsIgnoreCase(key)) return outfit;
        return initial(seed);
    }
    public Outfit next(String key, String seed) {
        Outfit current = find(key, seed);
        if (current == null) return null;
        return outfits.get((outfits.indexOf(current) + 1) % outfits.size());
    }

    private static List<Outfit> parse(JsonObject root) {
        if (root == null || !root.has("outfits") || !root.get("outfits").isJsonArray()) return List.of();
        List<Outfit> result = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("outfits")) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            if (!object.has("key") || !object.has("clothing") || !object.get("clothing").isJsonArray()) continue;
            String key = object.get("key").getAsString().trim();
            List<String> clothing = new ArrayList<>();
            for (JsonElement garment : object.getAsJsonArray("clothing")) if (garment.isJsonPrimitive() && !garment.getAsString().isBlank()) clothing.add(garment.getAsString().trim());
            if (!key.isBlank() && !clothing.isEmpty() && result.stream().noneMatch(outfit -> outfit.key().equalsIgnoreCase(key))) result.add(new Outfit(key, List.copyOf(clothing)));
        }
        return List.copyOf(result);
    }

    public record Outfit(String key, List<String> clothing) { }
}
