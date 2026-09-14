package de.omegazirkel.risingworld.template.runtime;

import java.nio.file.Path;

import de.omegazirkel.risingworld.MavenTemplate;
import de.omegazirkel.risingworld.template.PluginGUI;
import de.omegazirkel.risingworld.template.PluginSettings;
import de.omegazirkel.risingworld.template.ui.TemplatePlayerPluginData;
import de.omegazirkel.risingworld.template.ui.TemplatePlayerPluginSettings;
import de.omegazirkel.risingworld.template.ui.TemplatePluginInfoStatusProvider;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.settings.PlayerPluginAdminSettings;
import de.omegazirkel.risingworld.tools.ui.InventoryOverlayButtons;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettingsOverlay;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import de.omegazirkel.risingworld.tools.ui.PluginShortcutVisibility;
import de.omegazirkel.risingworld.tools.ui.SharedIndicatorProvider;
import de.omegazirkel.risingworld.tools.ui.SharedIndicators;
import net.risingworld.api.objects.Player;

public final class TemplatePluginRuntime {
    public static final String COMMAND = "mt";

    private final MavenTemplate plugin;
    private final String pluginName;
    private final PluginSettings settings;
    private final I18n i18n;
    private final PluginGUI gui;
    private final TemplatePlayerEventHandler events;

    public TemplatePluginRuntime(MavenTemplate plugin) {
        this.plugin = plugin;
        pluginName = plugin.getDescription("name");
        settings = PluginSettings.getInstance(plugin);
        i18n = I18n.getInstance(plugin);
        gui = PluginGUI.getInstance(plugin);
        events = new TemplatePlayerEventHandler(plugin, pluginName, settings, i18n, gui);
    }

    public void enable() {
        settings.initSettings();
        PluginMenuManager.registerPluginMenu(new MenuItem(pluginName, "maven-template", "Template Plugin",
                (Player player) -> gui.openMainMenu(player)));
        PluginShortcutVisibility.register(pluginName, player -> true);
        InventoryOverlayButtons.registerButton(pluginName, "Open", "maven-template",
                event -> gui.openMainMenu(event.getPlayer()));
        SharedIndicators.registerProvider(pluginName, new SharedIndicatorProvider() {
            @Override
            public boolean showIndicator(Player player) {
                return false;
            }

            @Override
            public String getIcon(Player player) {
                return "maven-template";
            }
        });
        String version = plugin.getDescription("version");
        PluginInfoStatusProviders.registerProvider(new TemplatePluginInfoStatusProvider(pluginName, version, COMMAND));
        PlayerPluginSettingsOverlay.registerPlayerPluginSettings(new TemplatePlayerPluginSettings(pluginName, version));
        PlayerPluginSettingsOverlay.registerPlayerPluginData(new TemplatePlayerPluginData(pluginName, version));
        PlayerPluginSettingsOverlay.registerPlayerPluginAdminSettings(new PlayerPluginAdminSettings(pluginName, version,
                settings::adminSettingsEntries, settings::initSettings));
        MavenTemplate.logger().info("✅ " + plugin.getName() + " Plugin is enabled version:" + version);
    }

    public void disable() {
        InventoryOverlayButtons.unregisterButtons(pluginName);
        PluginShortcutVisibility.unregister(pluginName);
        SharedIndicators.unregisterProvider(pluginName);
        PluginInfoStatusProviders.unregisterProvider(pluginName);
    }

    public void reloadSettings(Path settingsPath) {
        settings.initSettings(settingsPath.toString());
    }

    public TemplatePlayerEventHandler events() {
        return events;
    }
}
