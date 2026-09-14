package de.omegazirkel.risingworld.servicenpc;

import java.util.List;
import java.util.Random;
import net.risingworld.api.definitions.Items.Modifier;

/** The five modifier bands exposed by Rising World's item API. */
public enum ModifierQuality {
    RED(Modifier.Broken, Modifier.Weak),
    WHITE(Modifier.Normal, Modifier.Nice),
    GREEN(Modifier.Improved, Modifier.Powerful),
    TURQUOISE(Modifier.Merciless, Modifier.Unique),
    YELLOW(Modifier.Epic, Modifier.Godly);

    private final Modifier first; private final Modifier last;
    ModifierQuality(Modifier first, Modifier last) { this.first = first; this.last = last; }
    public static ModifierQuality of(String modifier) {
        try { return of(Modifier.valueOf(modifier == null || modifier.isBlank() ? "Normal" : modifier)); }
        catch (IllegalArgumentException ignored) { return WHITE; }
    }
    public static ModifierQuality of(Modifier modifier) {
        if (modifier == Modifier.Normal || modifier.ordinal() >= Modifier.Boring.ordinal() && modifier.ordinal() <= Modifier.Nice.ordinal()) return WHITE;
        if (modifier.ordinal() >= Modifier.Broken.ordinal() && modifier.ordinal() <= Modifier.Weak.ordinal()) return RED;
        if (modifier.ordinal() >= Modifier.Improved.ordinal() && modifier.ordinal() <= Modifier.Powerful.ordinal()) return GREEN;
        if (modifier.ordinal() >= Modifier.Merciless.ordinal() && modifier.ordinal() <= Modifier.Unique.ordinal()) return TURQUOISE;
        if (modifier.ordinal() >= Modifier.Epic.ordinal() && modifier.ordinal() <= Modifier.Godly.ordinal()) return YELLOW;
        return WHITE;
    }
    public ModifierQuality next() { return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null; }
    public Modifier random(Random random) { List<Modifier> values = modifiers(); return values.get(random.nextInt(values.size())); }
    public List<Modifier> modifiers() { return java.util.Arrays.stream(Modifier.values()).filter(value -> of(value) == this).toList(); }
}
