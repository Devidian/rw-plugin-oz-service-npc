package de.omegazirkel.risingworld.template.runtime;

import de.omegazirkel.risingworld.MavenTemplate;
import de.omegazirkel.risingworld.template.PluginGUI;
import de.omegazirkel.risingworld.template.PluginSettings;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.objects.Player;

/** Event workflow delegate. It is deliberately not a Rising World Listener. */
public final class TemplatePlayerEventHandler {
    private final MavenTemplate plugin;
    private final String pluginName;
    private final PluginSettings settings;
    private final I18n i18n;
    private final PluginGUI gui;
    private final Colors colors = Colors.getInstance();

    TemplatePlayerEventHandler(MavenTemplate plugin, String pluginName, PluginSettings settings, I18n i18n,
            PluginGUI gui) {
        this.plugin = plugin;
        this.pluginName = pluginName;
        this.settings = settings;
        this.i18n = i18n;
        this.gui = gui;
    }

    public void onPlayerCommand(PlayerCommandEvent event) {
        Player player = event.getPlayer();
        String[] commandParts = event.getCommand().split(" ", 2);
        if (!commandParts[0].equals("/" + TemplatePluginRuntime.COMMAND)) {
            return;
        }
        if (commandParts.length < 2) {
            gui.openMainMenu(player);
            return;
        }
        switch (commandParts[1]) {
            case "info", "status" -> PluginInfoStatusProviders.show(player, pluginName);
            case "help" -> player.sendTextMessage(colors.okay + plugin.getName() + ":> " + colors.endTag
                    + i18n.get("tc.cmd.help", player).replace("PH_PLUGIN_CMD", TemplatePluginRuntime.COMMAND));
            case "open" -> gui.openMainMenu(player);
            default -> player.sendTextMessage(i18n.get("tc.err.cmd.unknown")
                    .replace("PH_PLUGIN_CMD", TemplatePluginRuntime.COMMAND));
        }
    }

    public void onPlayerSpawn(PlayerSpawnEvent event) {
        if (!settings.enableWelcomeMessage) {
            return;
        }
        Player player = event.getPlayer();
        player.sendTextMessage(i18n.get("tc.msg.plugin.welcome", player.getSystemLanguage())
                .replace("PH_PLUGIN_NAME", plugin.getDescription("name"))
                .replace("PH_PLUGIN_CMD", TemplatePluginRuntime.COMMAND)
                .replace("PH_PLUGIN_VERSION", plugin.getDescription("version")));
    }
}
