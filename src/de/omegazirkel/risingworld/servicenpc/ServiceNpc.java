package de.omegazirkel.risingworld.servicenpc;

public record ServiceNpc(long npcId, ServiceType type, String name, boolean male, float x, float y, float z,
        float rx, float ry, float rz, float rw, String accountId, String outfit, int factionId) {
    public ServiceNpc(long npcId, ServiceType type, String name, boolean male, float x, float y, float z,
            float rx, float ry, float rz, float rw, String accountId, String outfit) {
        this(npcId, type, name, male, x, y, z, rx, ry, rz, rw, accountId, outfit, 0);
    }
}
