package de.omegazirkel.risingworld.servicenpc;

import de.omegazirkel.risingworld.ServiceNpcPlugin;
import de.omegazirkel.risingworld.tools.I18n;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerNpcInteractionEvent;
import net.risingworld.api.objects.Player;

/** Command and event dispatch only; never a Rising World Listener. */
public final class ServiceNpcEventHandler {
    private final ServiceNpcManager manager; private final ServiceNpcPluginGUI gui; private final I18n i18n; private final ServiceNpcDebugService debug;
    ServiceNpcEventHandler(ServiceNpcPlugin plugin, ServiceNpcManager manager, ServiceNpcPluginGUI gui, I18n i18n) { this.manager=manager; this.gui=gui; this.i18n=i18n; this.debug=new ServiceNpcDebugService(i18n); }
    public void interact(PlayerNpcInteractionEvent event) { if (event.getNpc() != null) manager.interact(event); }
    public void command(PlayerCommandEvent event) {
        String[] parts=event.getCommand().trim().split("\\s+",5); if(parts.length==0||!"/osn".equalsIgnoreCase(parts[0]))return;
        Player p=event.getPlayer(); if(parts.length==1||"open".equalsIgnoreCase(parts[1])) { gui.openMainMenu(p); return; }
        if("create".equalsIgnoreCase(parts[1])&&parts.length>=4){ServiceType type=ServiceType.parse(parts[2]);if(type==null){p.sendTextMessage(i18n.get("tc.service.command.help",p));return;}manager.create(p,type,"male".equalsIgnoreCase(parts[3]));return;}
        if("move".equalsIgnoreCase(parts[1])&&parts.length>=3){manager.move(p,number(parts[2]));return;}
        if("dissolve".equalsIgnoreCase(parts[1])&&parts.length>=3){manager.dissolve(p,number(parts[2]));return;}
        if("rename".equalsIgnoreCase(parts[1])&&parts.length>=4){manager.rename(p,number(parts[2]),parts[3]);return;}
        if("debug".equalsIgnoreCase(parts[1])&&parts.length>=4&&("modifier".equalsIgnoreCase(parts[2])||"modifiers".equalsIgnoreCase(parts[2]))){debug.grantModifiers(p,parts[3],"modifier".equalsIgnoreCase(parts[2])&&parts.length>=5?parts[4]:null);return;}
        p.sendTextMessage(i18n.get("tc.service.command.help",p));
    }
    private long number(String value){try{return Long.parseLong(value);}catch(NumberFormatException ex){return -1L;}}
}
