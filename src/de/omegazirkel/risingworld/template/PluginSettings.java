package de.omegazirkel.risingworld.template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsEntry;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsType;
import de.omegazirkel.risingworld.tools.settings.JsonSettingsFile;
import de.omegazirkel.risingworld.tools.settings.SettingsFileEditor;
import net.risingworld.api.World;
import net.risingworld.api.Plugin;

public class PluginSettings {
	private static PluginSettings instance = null;

	private static Plugin plugin;

	private static OZLogger logger() {
		return OZLogger.getInstance(plugin == null ? "MavenTemplate" : plugin.getDescription("name"));
	}

	// Settings
	public boolean enableWelcomeMessage = false;
	private Path settingsFile;
	private java.util.Map<String, String> currentSettings = new LinkedHashMap<>();
	private java.util.Map<String, String> defaultSettings = new LinkedHashMap<>();

	// END Settings

	public static PluginSettings getInstance(Plugin p) {
		plugin = p;
		return getInstance();
	}

	public long longValue(String key, long fallback, long minimum, long maximum) {
		try {
			long value = Long.parseLong(currentSettings.getOrDefault(key,
					defaultSettings.getOrDefault(key, Long.toString(fallback))));
			return Math.max(minimum, Math.min(maximum, value));
		} catch (NumberFormatException ex) {
			return fallback;
		}
	}

	public static PluginSettings getInstance() {

		if (instance == null) {
			instance = new PluginSettings();
		}
		return instance;
	}

	private PluginSettings() {
	}

	public void initSettings() {
		Path pluginPath = Path.of(plugin.getPath() != null ? plugin.getPath() : ".");
		initSettings(pluginPath.resolve("settings." + safeWorldName() + ".json").toString());
	}

	public void initSettings(String filePath) {
		settingsFile = Path.of(filePath);
		Path defaultSettingsFile = settingsFile.resolveSibling("settings.default.json");
		Path legacySettingsFile = settingsFile.resolveSibling("settings.properties");

		try {
			if (JsonSettingsFile.migrateLegacyProperties(legacySettingsFile, settingsFile))
				logger().info("Migrated legacy settings.properties to " + settingsFile.getFileName());
			if (Files.notExists(settingsFile) && Files.exists(defaultSettingsFile))
				JsonSettingsFile.writeFlatAtomically(settingsFile, JsonSettingsFile.loadFlat(defaultSettingsFile));
			java.util.Map<String, String> settings = JsonSettingsFile.loadFlat(settingsFile);
			java.util.Map<String, String> defaults = JsonSettingsFile.loadFlat(defaultSettingsFile);

			// motd settings
			enableWelcomeMessage = settings.getOrDefault("enableWelcomeMessage",
					defaults.getOrDefault("enableWelcomeMessage", "false")).contentEquals("true");

			logger().info(plugin.getName() + " Plugin settings loaded");
			logger().info("Sending welcome message on login is: " + String.valueOf(enableWelcomeMessage));
			currentSettings = settings;
			defaultSettings = defaults;

		} catch (IOException ex) {
			logger().error("IOException on initSettings: " + ex.getMessage());
			ex.printStackTrace();
		} catch (NumberFormatException ex) {
			logger().error("NumberFormatException on initSettings: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	public List<AdminSettingsEntry> adminSettingsEntries() {
		return Arrays.asList(
				AdminSettingsEntry.group("restorer", "tc.setting.restorer.label", "tc.setting.restorer.desc"),
				entry("restorerHoursAtOneDurability", "tc.setting.restorerhoursatonedurability.label", "tc.setting.restorerhoursatonedurability.desc", AdminSettingsType.INTEGER, "6"),
				entry("restorerMaterialCostPercent", "tc.setting.restorermaterialcostpercent.label", "tc.setting.restorermaterialcostpercent.desc", AdminSettingsType.INTEGER, "25"),
				entry("restorerServiceFee", "tc.setting.restorerservicefee.label", "tc.setting.restorerservicefee.desc", AdminSettingsType.INTEGER, "5"),
				AdminSettingsEntry.group("notifications", "tc.setting.notifications.label", "tc.setting.notifications.desc"),
				entry("restorerDiscordChannelId", "tc.setting.restorerdiscordchannelid.label", "tc.setting.restorerdiscordchannelid.desc", AdminSettingsType.STRING, ""),
				entry("augmenterDiscordChannelId", "tc.setting.augmenterdiscordchannelid.label", "tc.setting.augmenterdiscordchannelid.desc", AdminSettingsType.STRING, ""));
	}

	private AdminSettingsEntry entry(String key, String label, String description, AdminSettingsType type) {
		return entry(key, label, description, type, "");
	}

	private AdminSettingsEntry entry(String key, String label, String description, AdminSettingsType type, String fallbackDefault) {
		String defaultValue = defaultSettings.getOrDefault(key, fallbackDefault);
		return new AdminSettingsEntry(
				key,
				label,
				description,
				currentSettings.getOrDefault(key, defaultValue),
				defaultValue,
				type,
				false,
				value -> SettingsFileEditor.writeValue(settingsFile, key, value));
	}

	private static String safeWorldName() {
		String world;
		try {
			world = World.getName();
		} catch (LinkageError ex) {
			world = "default";
		}
		return (world == null || world.isBlank() ? "default" : world).replaceAll("[^A-Za-z0-9._-]", "_");
	}
}
