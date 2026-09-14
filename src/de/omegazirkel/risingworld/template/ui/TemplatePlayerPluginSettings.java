package de.omegazirkel.risingworld.template.ui;

import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;

/* This is an example, rename <Template> if you use this feature */

public class TemplatePlayerPluginSettings extends PlayerPluginSettings {

    public TemplatePlayerPluginSettings(String pluginLabel, String pluginVersion) {
        this.pluginLabel = pluginLabel;
        this.pluginVersion = pluginVersion;
    }

    @Override
    public BasePlayerPluginSettingsPanel createPlayerPluginSettingsUIElement(Player uiPlayer) {
        return new BasePlayerPluginSettingsPanel(uiPlayer, pluginLabel) {
            
            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();
                // TODO: implement actual settings content for MavenTemplate plugin
                UILabel placeholderLabel = new UILabel("MavenTemplate plugin settings will be here.");
                flexWrapper.addChild(placeholderLabel);
            }

        };
    }

}
