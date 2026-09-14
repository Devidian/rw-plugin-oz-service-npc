package de.omegazirkel.risingworld.servicenpc;

import de.omegazirkel.risingworld.tools.I18n;
import net.risingworld.api.definitions.Definitions;
import net.risingworld.api.definitions.Items.ItemDefinition;
import net.risingworld.api.definitions.Items.Modifier;
import net.risingworld.api.objects.Item;
import net.risingworld.api.objects.Player;

/** Admin-only visual comparison aid; it never participates in service transactions. */
final class ServiceNpcDebugService {
    private final I18n texts;
    ServiceNpcDebugService(I18n texts) { this.texts = texts; }

    void grantModifiers(Player player, String itemName, String requestedModifier) {
        if (player == null || !player.isAdmin()) { message(player, "tc.service.admin.required"); return; }
        ItemDefinition definition = Definitions.getItemDefinition(itemName);
        if (definition == null) { message(player, "tc.service.debug.item.unknown", "PH_ITEM", itemName); return; }
        Modifier[] modifiers = requestedModifier == null || requestedModifier.isBlank() ? Modifier.values() : new Modifier[] { modifier(requestedModifier) };
        if (modifiers.length == 1 && modifiers[0] == null) { message(player, "tc.service.debug.modifier.unknown", "PH_MODIFIER", requestedModifier); return; }
        int granted = 0;
        for (Modifier modifier : modifiers) {
            Item item = player.getInventory().addItem(definition.id, 0, 1);
            if (item == null || !item.isValid()) break;
            item.setModifier(modifier);
            if (definition.durability > 0) item.setDurability(definition.durability);
            granted++;
        }
        player.getInventory().syncWithClient();
        message(player, "tc.service.debug.granted", "PH_COUNT", Integer.toString(granted), "PH_ITEM", itemName);
    }

    private static Modifier modifier(String value) {
        for (Modifier modifier : Modifier.values()) if (modifier.name().equalsIgnoreCase(value)) return modifier;
        return null;
    }
    private void message(Player player, String key, String... replacements) {
        if (player == null) return;
        String value = texts.get(key, player);
        for (int i = 0; i + 1 < replacements.length; i += 2) value = value.replace(replacements[i], replacements[i + 1]);
        player.sendTextMessage(value);
    }
}
