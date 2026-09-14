package de.omegazirkel.risingworld.servicenpc;

/** Persistent custody record. An OPEN job is the sole authority for an item held by a service NPC. */
public record ServiceJob(String id, long npcId, int playerDbId, String playerName, ServiceType type,
        long acceptedAt, long completedAt, String status, String language, String itemName, int itemVariant, int itemDurability,
        short itemStatus, String itemModifier, String targetModifier, int itemColor, long cost, String currency, String correlationId) {
    public boolean open() { return "PREPARING".equals(status) || "OPEN".equals(status); }
}
