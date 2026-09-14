package de.omegazirkel.risingworld.servicenpc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.risingworld.api.Plugin;
import net.risingworld.api.World;

/** Keeps editable service policy isolated per Rising World world. */
final class WorldConfigFiles {
    private WorldConfigFiles() { }

    static Path resolve(Plugin plugin, String stem) throws IOException {
        Path pluginPath = Path.of(plugin.getPath());
        Path worldFile = pluginPath.resolve(stem + "." + worldName() + ".json");
        if (Files.notExists(worldFile)) Files.copy(pluginPath.resolve(stem + ".default.json"), worldFile);
        return worldFile;
    }

    private static String worldName() {
        String name;
        try { name = World.getName(); }
        catch (LinkageError ignored) { name = "default"; }
        if (name == null || name.isBlank()) name = "default";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
