package de.omegazirkel.risingworld.servicenpc;

import de.omegazirkel.risingworld.OZTools;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.InventoryOverlayPanel;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import de.omegazirkel.risingworld.tools.ui.PluginShortcutVisibility;
import net.risingworld.api.objects.Player;

/** Player preference for the Service NPC entry in the shared shortcut bar. */
public final class ServiceNpcPlayerPluginSettings extends PlayerPluginSettings {
    private final I18n i18n;

    public ServiceNpcPlayerPluginSettings(String name, String version, I18n i18n) {
        pluginLabel = name;
        pluginVersion = version;
        this.i18n = i18n;
    }

    public boolean shortcutVisible(Player player) {
        return player == null || OZTools.playerSettings() == null
                || OZTools.playerSettings().getBoolean(player.getDbID(),
                        PluginShortcutVisibility.playerSettingKey(pluginLabel)).orElse(true);
    }

    @Override
    public BasePlayerPluginSettingsPanel createPlayerPluginSettingsUIElement(Player player) {
        return new BasePlayerPluginSettingsPanel(player, pluginLabel) {
            @Override protected void redrawContent() {
                flexWrapper.removeAllChilds();
                OZUIElement setting = defaultSettingsContainer();
                setting.addChild(defaultSettingsLabel(i18n.get("tc.shortcut.visible", player)));
                setting.addChild(switchButtons(player, shortcutVisible(player), event -> {
                    if (OZTools.playerSettings() != null) {
                        OZTools.playerSettings().setBoolean(player.getDbID(),
                                PluginShortcutVisibility.playerSettingKey(pluginLabel), !shortcutVisible(player));
                    }
                    InventoryOverlayPanel.refreshAllVisible();
                    redrawContent();
                }));
                flexWrapper.addChild(setting);
            }
        };
    }
}
