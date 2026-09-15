package de.omegazirkel.risingworld.servicenpc;

import java.util.ArrayList;
import java.util.List;

import de.omegazirkel.risingworld.ServiceNpcPlugin;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import net.risingworld.api.objects.Player;

/** Template-style radial entry point. Creation is restricted to administrators. */
public final class ServiceNpcPluginGUI {
    private final String pluginName;
    private final ServiceNpcManager manager;
    private final I18n texts;

    public ServiceNpcPluginGUI(ServiceNpcPlugin plugin, ServiceNpcManager manager, I18n texts) {
        pluginName = plugin.getDescription("name");
        this.manager = manager;
        this.texts = texts;
        AssetManager.loadIconFromPlugin(plugin, "oz-service-npc");
        AssetManager.loadIconFromPlugin(plugin, "service-restorer");
        AssetManager.loadIconFromPlugin(plugin, "service-augmenter");
        AssetManager.loadIconFromPlugin(plugin, "service-npc-male");
        AssetManager.loadIconFromPlugin(plugin, "service-npc-female");
        PluginMenuManager.registerPluginMenu(new MenuItem(pluginName, "oz-service-npc", text("tc.service.menu.root"),
                this::openMainMenu));
    }

    public void openMainMenu(Player player) {
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem(pluginName, "info-status", text("tc.service.menu.info.status"), selected -> {
            selected.hideRadialMenu(true);
            PluginInfoStatusProviders.show(selected, pluginName);
        }));
        if (player.isAdmin()) {
            items.add(new MenuItem(pluginName, "service-restorer", text("tc.service.menu.restorer"),
                    selected -> openGenderMenu(selected, ServiceType.RESTORER)));
            items.add(new MenuItem(pluginName, "service-augmenter", text("tc.service.menu.augmenter"),
                    selected -> openGenderMenu(selected, ServiceType.AUGMENTER)));
        }
        PluginMenuManager.showMenu(player, items);
    }

    private void openGenderMenu(Player player, ServiceType type) {
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem(pluginName, "service-npc-male", text("tc.service.menu.male"),
                selected -> create(selected, type, true)));
        items.add(new MenuItem(pluginName, "service-npc-female", text("tc.service.menu.female"),
                selected -> create(selected, type, false)));
        items.add(MenuItem.backMenu(player, this::openMainMenu));
        PluginMenuManager.showMenu(player, items);
    }

    private void create(Player player, ServiceType type, boolean male) {
        player.hideRadialMenu(true);
        manager.create(player, type, male);
    }

    private String text(String key) { return texts.get(key, "en"); }
    private String text(String key, Player player) { return texts.get(key, player); }
}
