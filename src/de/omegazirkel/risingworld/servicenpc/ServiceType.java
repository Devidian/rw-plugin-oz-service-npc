package de.omegazirkel.risingworld.servicenpc;

public enum ServiceType {
    RESTORER("restorer"), AUGMENTER("augmenter");
    private final String key;
    ServiceType(String key) { this.key = key; }
    public String key() { return key; }
    public static ServiceType parse(String raw) {
        for (ServiceType value : values()) if (value.key.equalsIgnoreCase(raw)) return value;
        return null;
    }
}
