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
import net.risingworld.api.ui.UITarget;

/** Template-style radial entry point. Creation is restricted to administrators. */
public final class ServiceNpcPluginGUI {
    private final ServiceNpcPlugin plugin;
    private final String pluginName;
    private final ServiceNpcManager manager;
    private final I18n texts;

    public ServiceNpcPluginGUI(ServiceNpcPlugin plugin, ServiceNpcManager manager, I18n texts) {
        this.plugin = plugin;
        pluginName = plugin.getDescription("name");
        this.manager = manager;
        this.texts = texts;
        AssetManager.loadIconFromPlugin(plugin, "oz-service-npc");
        AssetManager.loadIconFromPlugin(plugin, "service-restorer");
        AssetManager.loadIconFromPlugin(plugin, "service-augmenter");
        AssetManager.loadIconFromPlugin(plugin, "service-npc-male");
        AssetManager.loadIconFromPlugin(plugin, "service-npc-female");
        AssetManager.loadIconFromPlugin(plugin, "service-manage");
        AssetManager.loadIconFromPlugin(plugin, "service-action-teleport");
        AssetManager.loadIconFromPlugin(plugin, "service-action-dissolve");
        AssetManager.loadIconFromPlugin(plugin, "service-action-rename");
        AssetManager.loadIconFromPlugin(plugin, "service-action-move");
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
                    selected -> openGenderMenu(selected, ServiceType.RESTORER, false)));
            items.add(new MenuItem(pluginName, "service-augmenter", text("tc.service.menu.augmenter"),
                    selected -> openGenderMenu(selected, ServiceType.AUGMENTER, false)));
        }
        if (manager.canCreateFaction(player)) {
            items.add(new MenuItem(pluginName, "service-restorer", text("tc.service.manage.faction.create.restorer", player),
                    selected -> openGenderMenu(selected, ServiceType.RESTORER, true)));
            items.add(new MenuItem(pluginName, "service-augmenter", text("tc.service.manage.faction.create.augmenter", player),
                    selected -> openGenderMenu(selected, ServiceType.AUGMENTER, true)));
        }
        if (player.isAdmin() || manager.canCreateFaction(player)) {
            items.add(new MenuItem(pluginName, "service-manage", text("tc.service.manage.title", player), selected -> {
                selected.hideRadialMenu(false);
                selected.addUIElement(new ServiceNpcManagementOverlay(plugin, manager, texts, selected), UITarget.Modal);
            }));
        }
        PluginMenuManager.showMenu(player, items);
    }

    private void openGenderMenu(Player player, ServiceType type, boolean factionOwned) {
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem(pluginName, "service-npc-male", text("tc.service.menu.male"),
                selected -> create(selected, type, true, factionOwned)));
        items.add(new MenuItem(pluginName, "service-npc-female", text("tc.service.menu.female"),
                selected -> create(selected, type, false, factionOwned)));
        items.add(MenuItem.backMenu(player, this::openMainMenu));
        PluginMenuManager.showMenu(player, items);
    }

    private void create(Player player, ServiceType type, boolean male, boolean factionOwned) {
        player.hideRadialMenu(true);
        if (factionOwned) manager.createFaction(player, type, male);
        else manager.create(player, type, male);
    }

    private String text(String key) { return texts.get(key, "en"); }
    private String text(String key, Player player) { return texts.get(key, player); }
}
