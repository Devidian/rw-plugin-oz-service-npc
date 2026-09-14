package de.omegazirkel.risingworld.servicenpc;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import de.omegazirkel.risingworld.ServiceNpcPlugin;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.db.SQLiteConnectionFactory;
import de.omegazirkel.risingworld.tools.settings.PlayerPluginAdminSettings;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettingsOverlay;
import de.omegazirkel.risingworld.template.PluginSettings;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;

/** Composes template-provided runtime facilities with the Service NPC domain. */
public final class ServiceNpcRuntime {
    private final ServiceNpcPlugin plugin; private Connection db; private NameCatalog names; private OutfitCatalog outfits; private AugmentationCatalog augmentation; private ServiceNpcManager manager; private ServiceNpcEventHandler events; private ServiceJobService jobs; private PluginSettings settings;
    public ServiceNpcRuntime(ServiceNpcPlugin plugin){this.plugin=plugin;}
    public void enable(){ I18n i18n=I18n.getInstance(plugin); settings=PluginSettings.getInstance(plugin);settings.initSettings();names=new NameCatalog();names.load(plugin);outfits=new OutfitCatalog();outfits.load(plugin);augmentation=new AugmentationCatalog();augmentation.load(plugin);db=SQLiteConnectionFactory.open(plugin);try{ServiceNpcRepository repository=new ServiceNpcRepository(db);repository.initialize();jobs=new ServiceJobService(plugin,repository,i18n,settings,augmentation);manager=new ServiceNpcManager(plugin,repository,names,outfits,i18n,jobs);ServiceNpcPluginGUI gui=new ServiceNpcPluginGUI(plugin,manager,i18n);events=new ServiceNpcEventHandler(plugin,manager,gui,i18n);String name=plugin.getDescription("name");PlayerPluginSettingsOverlay.registerPlayerPluginAdminSettings(new PlayerPluginAdminSettings(name,plugin.getDescription("version"),settings::adminSettingsEntries,settings::initSettings));plugin.executeDelayed(2f,manager::rehydrate);plugin.executeDelayed(5f,jobs::completeDueJobs);ServiceNpcPlugin.logger().info("OZ Service NPC enabled.");}catch(SQLException ex){throw new IllegalStateException("Cannot initialize Service NPC persistence",ex);} }
    public void disable(){if(jobs!=null)jobs.stop();try{if(db!=null)db.close();}catch(SQLException ex){ServiceNpcPlugin.logger().error("Cannot close database: "+ex.getMessage());}}
    public void reloadSettings(Path ignored){ settings.initSettings(); names.load(plugin); outfits.load(plugin); augmentation.load(plugin); }
    public ServiceNpcEventHandler events(){return events;}
}
