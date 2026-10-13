package de.omegazirkel.risingworld.servicenpc;

import java.util.Arrays;

import de.omegazirkel.risingworld.ServiceNpcPlugin;
import de.omegazirkel.risingworld.OZTools;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.BasePluginOverlayWithTabs;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.MessageBoxButtons;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.ui.style.Pivot;

/** Scoped management view for global and faction-owned service NPCs. */
public final class ServiceNpcManagementOverlay extends BasePluginOverlayWithTabs {
    private final ServiceNpcPlugin plugin;
    private final ServiceNpcManager manager;
    private final I18n texts;
    private ServiceType selectedType = ServiceType.RESTORER;

    public ServiceNpcManagementOverlay(ServiceNpcPlugin plugin, ServiceNpcManager manager, I18n texts, Player player) {
        super(player, ignored -> { });
        this.plugin = plugin;
        this.manager = manager;
        this.texts = texts;
        rebuild();
    }

    @Override protected I18n t() { return texts; }
    @Override protected String titleText() { return texts.get("tc.service.manage.title", uiPlayer); }
    @Override protected String descriptionText() { return texts.get("tc.service.manage.description", uiPlayer); }
    @Override protected String legendText() { return ""; }

    @Override protected void setupTabs() {
        setupTabContainer();
        for (ServiceType type : ServiceType.values()) {
            addTab(texts.get("tc.service.ui.identity." + type.key(), uiPlayer), 180, selectedType == type,
                    false, () -> { selectedType = type; rebuild(); });
        }
        TableScrollView table = new TableScrollView(Arrays.asList(
                texts.get("tc.service.manage.name", uiPlayer),
                texts.get("tc.service.manage.position", uiPlayer),
                texts.get("tc.service.manage.scope", uiPlayer),
                texts.get("tc.service.manage.actions", uiPlayer)),
                Arrays.asList(25f, 24f, 15f, 36f));
        table.setScrollBodyHeight(375f);
        manager.endpoints().stream().filter(endpoint -> endpoint.type() == selectedType)
                .filter(endpoint -> manager.canManage(uiPlayer, endpoint))
                .forEach(endpoint -> table.addRow(row(endpoint)));
        body.addChild(table);
    }

    private TableRow row(ServiceNpc endpoint) {
        OZUIElement actions = new OZUIElement();
        actions.setSize(100, 100, true);
        action(actions, "service-action-teleport", 0, () -> confirm(endpoint, "teleport", () -> manager.teleport(uiPlayer, endpoint.npcId())));
        action(actions, "service-action-dissolve", 43, () -> confirm(endpoint, "dissolve", () -> manager.dissolve(uiPlayer, endpoint.npcId())));
        action(actions, "service-action-rename", 86, () -> rename(endpoint));
        action(actions, "service-action-move", 129, () -> confirm(endpoint, "move", () -> manager.move(uiPlayer, endpoint.npcId())));
        String position = String.format(java.util.Locale.ROOT, "%.0f, %.0f, %.0f", endpoint.x(), endpoint.y(), endpoint.z());
        return new TableRow(Arrays.asList(
                cell(endpoint.name(), 25f), cell(position, 24f),
                cell(texts.get(endpoint.factionId() == 0 ? "tc.service.manage.global" : "tc.service.manage.faction_scope", uiPlayer), 15f),
                new TableCell(actions, 36f)));
    }

    private TableCell cell(String value, float width) { return new TableCell(new UILabel(value), width); }

    private void action(OZUIElement parent, String icon, int x, Runnable callback) {
        AdvancedButton button = AdvancedButtonFactory.defaultButton("", event -> callback.run());
        button.setSurfaceIcon(AssetManager.getIcon(uiPlayer, icon));
        button.setPivot(Pivot.UpperLeft);
        button.setPosition(x, 3, false);
        button.setSize(38, 25, false);
        parent.addChild(button);
    }

    private void handoff(Runnable dialog) {
        uiPlayer.removeUIElement(this);
        uiPlayer.closeAllActiveUIWindows();
        OZTools.runAfterModalClose(uiPlayer, dialog);
    }

    private void reopen() {
        if (!uiPlayer.isConnected()) return;
        rebuild();
        uiPlayer.addUIElement(this, UITarget.Modal);
    }

    private void confirm(ServiceNpc endpoint, String action, Runnable mutation) {
        if (!manager.canManage(uiPlayer, endpoint)) return;
        handoff(() -> uiPlayer.showMessageBox(MessageBoxButtons.Yes_No,
                texts.get("tc.service.manage." + action, uiPlayer),
                endpoint.name(), 0, answer -> plugin.enqueue(() -> {
                    if (answer == 0 && manager.canManage(uiPlayer, endpoint)) mutation.run();
                    reopen();
                })));
    }

    private void rename(ServiceNpc endpoint) {
        if (!manager.canManage(uiPlayer, endpoint)) return;
        handoff(() -> uiPlayer.showInputMessageBox(texts.get("tc.service.manage.rename", uiPlayer),
                texts.get("tc.service.manage.name", uiPlayer), endpoint.name(), answer -> plugin.enqueue(() -> {
                    if (answer != null && manager.canManage(uiPlayer, endpoint)) manager.rename(uiPlayer, endpoint.npcId(), answer);
                    reopen();
                })));
    }
}
