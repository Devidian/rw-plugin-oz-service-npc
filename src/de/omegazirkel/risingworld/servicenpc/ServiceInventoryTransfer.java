package de.omegazirkel.risingworld.servicenpc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.risingworld.api.definitions.Definitions;
import net.risingworld.api.definitions.Items.ItemDefinition;
import net.risingworld.api.definitions.Items.Category;
import net.risingworld.api.definitions.Items.Modifier;
import net.risingworld.api.objects.Inventory;
import net.risingworld.api.objects.Inventory.SlotType;
import net.risingworld.api.objects.Item;
import net.risingworld.api.objects.Player;

/** Narrow custody boundary for one repairable inventory item. */
public final class ServiceInventoryTransfer {
    private ServiceInventoryTransfer() { }

    public static List<Candidate> repairCandidates(Player player) {
        if (player == null || player.getInventory() == null || player.getInventory().getAllItems() == null) return List.of();
        List<Candidate> values = new ArrayList<>();
        for (Item item : player.getInventory().getAllItems()) {
            if (item == null || !item.isValid() || item.getStack() < 1) continue;
            int maxDurability = maxDurability(item);
            if (maxDurability <= 0 || item.getDurability() >= maxDurability) continue;
            String name = itemName(item);
            if (name.isBlank()) continue;
            values.add(new Candidate(name, displayName(item, player.getLanguage()), item.getVariant(), item.getDurability(),
                    maxDurability, item.getStatus(), modifierName(item), color(item)));
        }
        values.sort(Comparator.comparing(Candidate::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(Candidate::durability));
        return List.copyOf(values);
    }
    public static List<Candidate> augmentCandidates(Player player) {
        if (player == null || player.getInventory() == null || player.getInventory().getAllItems() == null) return List.of();
        List<Candidate> values = new ArrayList<>();
        for (Item item : player.getInventory().getAllItems()) {
            if (item == null || item.getStack() < 1 || itemName(item).isBlank() || !isWeapon(item)) continue;
            values.add(new Candidate(itemName(item), displayName(item, player.getLanguage()), item.getVariant(), item.getDurability(), maxDurability(item), item.getStatus(), modifierName(item), color(item)));
        }
        values.sort(Comparator.comparing(Candidate::displayName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(values);
    }
    public static boolean hasMaterials(Player player, java.util.Map<String,Integer> required) {
        if (player == null || player.getInventory() == null) return false;
        java.util.Map<String,Integer> found = new java.util.HashMap<>();
        for (Item item : player.getInventory().getAllItems()) if (item != null && item.isValid()) found.merge(itemName(item).toLowerCase(), item.getStack(), Integer::sum);
        for (var entry : required.entrySet()) if (found.getOrDefault(entry.getKey().toLowerCase(), 0) < entry.getValue()) return false;
        return true;
    }
    public static boolean removeMaterials(Player player, java.util.Map<String,Integer> required) {
        if (!hasMaterials(player, required)) return false;
        Inventory inventory = player.getInventory();
        for (var entry : required.entrySet()) {
            int left=entry.getValue();
            for (SlotType type : SlotType.values()) for (int slot=0; slot<inventory.getSlotCount(type) && left>0; slot++) {
                Item item=inventory.getItem(slot,type); if(item==null || !item.isValid() || !itemName(item).equalsIgnoreCase(entry.getKey())) continue;
                int take=Math.min(left,item.getStack()); if(!inventory.removeItem(slot,type,take)) return false; left-=take;
            }
        }
        inventory.syncWithClient(); return true;
    }

    public static boolean remove(Player player, Candidate candidate) {
        if (player == null || candidate == null || player.getInventory() == null) return false;
        Inventory inventory = player.getInventory();
        for (SlotType type : SlotType.values()) {
            for (int slot = 0; slot < inventory.getSlotCount(type); slot++) {
                Item item = inventory.getItem(slot, type);
                if (!matches(item, candidate)) continue;
                if (!inventory.removeItem(slot, type, 1)) return false;
                inventory.syncWithClient();
                return true;
            }
        }
        return false;
    }

    private static boolean matches(Item item, Candidate candidate) {
        return item != null && item.isValid() && item.getStack() > 0 && item.getVariant() == candidate.variant()
                && itemName(item).equalsIgnoreCase(candidate.itemName()) && item.getDurability() == candidate.durability()
                && item.getStatus() == candidate.status() && modifierName(item).equals(candidate.modifier())
                && color(item) == candidate.color();
    }

    private static int maxDurability(Item item) {
        ItemDefinition definition = item.getDefinition();
        if (definition == null || definition.durability <= 0) definition = Definitions.getItemDefinition(item.getTypeID());
        return definition == null ? 0 : Math.max(0, definition.durability);
    }
    private static boolean isWeapon(Item item) {
        ItemDefinition definition = item.getDefinition();
        if (definition == null) definition = Definitions.getItemDefinition(item.getTypeID());
        return definition != null && definition.category == Category.Weapon;
    }

    private static String itemName(Item item) {
        ItemDefinition definition = item.getDefinition();
        if (definition == null || definition.name == null || definition.name.isBlank()) definition = Definitions.getItemDefinition(item.getTypeID());
        return definition == null || definition.name == null ? "" : definition.name.trim();
    }

    private static String displayName(Item item, String language) {
        String localized = item.getLocalizedName(language);
        return localized == null || localized.isBlank() ? itemName(item) : localized.trim();
    }

    private static String modifierName(Item item) { return item.getModifier() == null ? "" : item.getModifier().name(); }
    private static int color(Item item) { return item instanceof Item.ConstructionItem construction ? construction.getColor() : 0; }

    public record Candidate(String itemName, String displayName, int variant, int durability, int maxDurability,
            short status, String modifier, int color) {
        public Candidate { modifier = modifier == null ? "" : modifier; }
        public int missingDurability() { return Math.max(0, maxDurability - durability); }
        public int repairedDurability() { return maxDurability; }
    }
}
