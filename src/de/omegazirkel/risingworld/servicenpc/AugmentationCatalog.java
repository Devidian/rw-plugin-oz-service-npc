package de.omegazirkel.risingworld.servicenpc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.omegazirkel.risingworld.ServiceNpcPlugin;
import net.risingworld.api.Plugin;

/** World-local augmentation policy. A source modifier advances only to the next configured level. */
public final class AugmentationCatalog {
    private List<Tier> tiers = List.of();

    public void load(Plugin plugin) {
        try {
            Path path = WorldConfigFiles.resolve(plugin, "augmentation");
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Tier> parsed = parse(root);
            if (parsed.isEmpty()) throw new IllegalStateException("no valid augmentation tiers");
            tiers = parsed;
            ServiceNpcPlugin.logger().info("Loaded Service NPC augmentation policy from " + path.getFileName() + ": " + tiers.size() + " tiers.");
        } catch (Exception exception) {
            tiers = List.of();
            ServiceNpcPlugin.logger().error("Cannot load Service NPC augmentation policy: " + exception.getMessage());
        }
    }

    public Tier nextTier(String modifier) {
        String source = normalize(modifier);
        Optional<Tier> current = tiers.stream().filter(tier -> tier.modifiers().stream().anyMatch(value -> value.equalsIgnoreCase(source))).findFirst();
        if (current.isEmpty()) return null;
        return tiers.stream().filter(tier -> tier.level() > current.get().level() && tier.enabled() && !tier.modifiers().isEmpty())
                .min(Comparator.comparingInt(Tier::level)).orElse(null);
    }

    private static List<Tier> parse(JsonObject root) {
        if (root == null || !root.has("tiers") || !root.get("tiers").isJsonArray()) return List.of();
        List<Tier> result = new ArrayList<>();
        for (JsonElement value : root.getAsJsonArray("tiers")) {
            if (!value.isJsonObject()) continue;
            JsonObject tier = value.getAsJsonObject();
            if (!tier.has("level") || !tier.has("modifiers")) continue;
            int level = tier.get("level").getAsInt();
            List<String> modifiers = strings(tier.get("modifiers"));
            if (modifiers.isEmpty()) continue;
            boolean enabled = !tier.has("enabled") || tier.get("enabled").getAsBoolean();
            long minutes = tier.has("minutes") ? Math.max(0L, tier.get("minutes").getAsLong()) : 0L;
            long fee = tier.has("serviceFee") ? Math.max(0L, tier.get("serviceFee").getAsLong()) : 0L;
            result.add(new Tier(level, enabled, minutes, fee, materials(tier.get("materials")), modifiers));
        }
        result.sort(Comparator.comparingInt(Tier::level));
        for (int index = 1; index < result.size(); index++) if (result.get(index - 1).level() == result.get(index).level()) return List.of();
        return List.copyOf(result);
    }

    private static Map<String, Integer> materials(JsonElement value) {
        if (value == null || !value.isJsonObject()) return Map.of();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (var entry : value.getAsJsonObject().entrySet()) if (entry.getValue().isJsonPrimitive()) {
            int amount = entry.getValue().getAsInt();
            if (amount > 0 && !entry.getKey().isBlank()) result.put(entry.getKey(), amount);
        }
        return Map.copyOf(result);
    }

    private static List<String> strings(JsonElement value) {
        if (value == null || !value.isJsonArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonElement entry : value.getAsJsonArray()) if (entry.isJsonPrimitive() && !entry.getAsString().isBlank()) result.add(entry.getAsString().trim());
        return List.copyOf(result);
    }

    private static String normalize(String modifier) { return modifier == null || modifier.isBlank() ? "Normal" : modifier.trim(); }

    public record Tier(int level, boolean enabled, long minutes, long serviceFee, Map<String, Integer> materials,
                       List<String> modifiers) { }
}
