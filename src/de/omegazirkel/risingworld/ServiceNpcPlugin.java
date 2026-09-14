package de.omegazirkel.risingworld;

import java.nio.file.Path;

import de.omegazirkel.risingworld.servicenpc.ServiceNpcRuntime;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.Plugin;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerNpcInteractionEvent;

/** Sole Rising World listener; domain work is delegated to {@code servicenpc}. */
public final class ServiceNpcPlugin extends Plugin implements Listener, FileChangeListener {
    private ServiceNpcRuntime runtime;

    public static OZLogger logger() { return OZLogger.getInstance("OZServiceNPC"); }

    @Override public void onEnable() { runtime = new ServiceNpcRuntime(this); runtime.enable(); registerEventListener(this); }
    @Override public void onDisable() { if (runtime != null) runtime.disable(); }
    @Override public void onSettingsChanged(Path path) { if (runtime != null) runtime.reloadSettings(path); }
    @EventMethod public void onPlayerCommand(PlayerCommandEvent event) { runtime.events().command(event); }
    @EventMethod public void onNpcInteraction(PlayerNpcInteractionEvent event) { runtime.events().interact(event); }
}
