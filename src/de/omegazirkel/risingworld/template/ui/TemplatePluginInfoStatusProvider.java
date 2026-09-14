package de.omegazirkel.risingworld.template.ui;

import de.omegazirkel.risingworld.template.PluginSettings;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProvider;
import net.risingworld.api.objects.Player;

public class TemplatePluginInfoStatusProvider implements PluginInfoStatusProvider {
    private final String pluginName;
    private final String version;
    private final String command;

    public TemplatePluginInfoStatusProvider(String pluginName, String version, String command) {
        this.pluginName = pluginName;
        this.version = version;
        this.command = command;
    }

    @Override
    public String getPluginName() {
        return pluginName;
    }

    @Override
    public String getInfo(Player player) {
        return t().get("tc.template.info.panel.info", player)
                .replace("PH_PLUGIN_NAME", pluginName)
                .replace("PH_PLUGIN_VERSION", version)
                .replace("PH_PLUGIN_CMD", command);
    }

    @Override
    public String getStatus(Player player) {
        PluginSettings settings = PluginSettings.getInstance();
        return t().get("tc.template.info.panel.status", player)
                .replace("PH_PLUGIN_NAME", pluginName)
                .replace("PH_WELCOME_MESSAGE", String.valueOf(settings.enableWelcomeMessage));
    }

    private I18n t() {
        return I18n.getInstance(pluginName);
    }
}
