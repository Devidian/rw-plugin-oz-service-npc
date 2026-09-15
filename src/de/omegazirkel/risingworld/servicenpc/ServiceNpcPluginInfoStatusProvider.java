package de.omegazirkel.risingworld.servicenpc;

import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProvider;
import net.risingworld.api.objects.Player;

/** Player-facing content for the shared OZ Tools Info / Status panel. */
final class ServiceNpcPluginInfoStatusProvider implements PluginInfoStatusProvider {
    private final String pluginName;
    private final String version;
    private final I18n texts;

    ServiceNpcPluginInfoStatusProvider(String pluginName, String version, I18n texts) {
        this.pluginName = pluginName;
        this.version = version;
        this.texts = texts;
    }

    @Override public String getPluginName() { return pluginName; }

    @Override public String getInfo(Player player) {
        return texts.get("tc.service.info.panel.info", player)
                .replace("PH_PLUGIN_NAME", pluginName)
                .replace("PH_PLUGIN_VERSION", version);
    }

    @Override public String getStatus(Player player) {
        return texts.get("tc.service.info.panel.status", player);
    }
}
