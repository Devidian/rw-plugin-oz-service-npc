package de.omegazirkel.risingworld.servicenpc;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import de.omegazirkel.risingworld.ServiceNpcPlugin;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.bridge.WalletBridge;
import net.risingworld.api.World;
import net.risingworld.api.definitions.Clothing.ClothingDefinition;
import net.risingworld.api.definitions.Definitions;
import net.risingworld.api.events.player.PlayerNpcInteractionEvent;
import net.risingworld.api.objects.Npc;
import net.risingworld.api.objects.Player;
import net.risingworld.api.objects.Skin;
import net.risingworld.api.utils.Quaternion;
import net.risingworld.api.utils.Vector3f;

/** NPC lifecycle, account ownership and interaction boundary. Item workflow remains in service-specific handlers. */
public final class ServiceNpcManager {
    private static final String PLUGIN_NAME = "OZ - Service NPC";
    private final ServiceNpcPlugin plugin;
    private final ServiceNpcRepository repository;
    private final NameCatalog names;
    private final OutfitCatalog outfits;
    private final I18n i18n;
    private final ServiceJobService jobs;
    private final Random random = new Random();

    public ServiceNpcManager(ServiceNpcPlugin plugin, ServiceNpcRepository repository, NameCatalog names, OutfitCatalog outfits, I18n i18n,
            ServiceJobService jobs) {
        this.plugin = plugin; this.repository = repository; this.names = names; this.outfits = outfits; this.i18n = i18n; this.jobs = jobs;
    }
    public void rehydrate() {
        try { for (ServiceNpc endpoint : repository.all()) {
            Npc npc = World.getNpc(endpoint.npcId());
            if (npc == null || npc.isDead()) {
                var definition = Definitions.getNpcDefinition("dummy");
                if (definition == null) { ServiceNpcPlugin.logger().warn("Service NPC dummy definition is unavailable."); continue; }
                npc = World.spawnNpc(definition.id, endpoint.male() ? 0 : 1, new Vector3f(endpoint.x(), endpoint.y(), endpoint.z()), new Quaternion(endpoint.rx(), endpoint.ry(), endpoint.rz(), endpoint.rw()));
                if (npc == null) { ServiceNpcPlugin.logger().warn("Could not rehydrate " + endpoint.name()); continue; }
                ServiceNpc replacement = fromNpc(npc, endpoint.type(), endpoint.name(), endpoint.male(), endpoint.accountId(), endpoint.outfit());
                repository.replaceId(endpoint.npcId(), replacement); endpoint = replacement;
            }
            initialize(npc, endpoint); ensureAccount(endpoint);
        }} catch (SQLException ex) { ServiceNpcPlugin.logger().error("Cannot rehydrate service NPCs: " + ex.getMessage()); }
    }
    public void create(Player player, ServiceType type, boolean male) {
        if (player == null || !player.isAdmin()) { message(player, "tc.service.admin.required"); return; }
        var definition = Definitions.getNpcDefinition("dummy");
        if (definition == null) { message(player, "tc.service.create.failed"); return; }
        Npc npc = World.spawnNpc(definition.id, male ? 0 : 1, player.getPosition(), player.getRotation());
        if (npc == null) { message(player, "tc.service.create.failed"); return; }
        String prefix = i18n.get("tc.service.ui.identity." + type.key(), player);
        String name = uniqueName(prefix, male);
        ServiceNpc endpoint = fromNpc(npc, type, name, male, "service-npc:" + UUID.randomUUID(), "");
        try { repository.save(endpoint); initialize(npc, endpoint); if (!ensureAccount(endpoint)) { repository.delete(endpoint.npcId()); npc.delete(); message(player, "tc.service.wallet.unavailable"); return; } message(player, "tc.service.create.success", "PH_NAME", name); }
        catch (SQLException ex) { npc.delete(); ServiceNpcPlugin.logger().error("Cannot create service NPC: " + ex.getMessage()); message(player, "tc.service.create.failed"); }
    }
    public void move(Player player, long id) {
        if (player == null || !player.isAdmin()) { message(player, "tc.service.admin.required"); return; }
        try { ServiceNpc endpoint = repository.find(id).orElse(null); if (endpoint == null) { message(player,"tc.service.admin.notfound"); return; }
            Npc npc = World.getNpc(id); if (npc == null || npc.isDead()) { message(player,"tc.service.admin.notfound"); return; }
            npc.setPosition(player.getPosition()); npc.setRotation(player.getRotation()); repository.save(fromNpc(npc, endpoint.type(), endpoint.name(), endpoint.male(), endpoint.accountId(), endpoint.outfit())); message(player,"tc.service.admin.moved");
        } catch (SQLException ex) { ServiceNpcPlugin.logger().error("Cannot move service NPC: " + ex.getMessage()); }
    }
    public void rename(Player player, long id, String name) {
        if (player == null || !player.isAdmin() || name == null || name.isBlank() || name.length() > 120) { message(player,"tc.service.admin.invalid"); return; }
        try { ServiceNpc old=repository.find(id).orElse(null); if(old==null){message(player,"tc.service.admin.notfound");return;} ServiceNpc updated=new ServiceNpc(old.npcId(),old.type(),name.trim(),old.male(),old.x(),old.y(),old.z(),old.rx(),old.ry(),old.rz(),old.rw(),old.accountId(),old.outfit()); repository.save(updated); Npc npc=World.getNpc(id); if(npc!=null&&!npc.isDead()) npc.setName(updated.name()); new WalletBridge(plugin).updateSystemAccountDisplayName(updated.accountId(),updated.name(),PLUGIN_NAME); message(player,"tc.service.admin.renamed"); } catch(SQLException ex){ServiceNpcPlugin.logger().error("Cannot rename service NPC: "+ex.getMessage());}
    }
    public void nextOutfit(Player player, long id) { if (player == null || !player.isAdmin()) { message(player,"tc.service.admin.required"); return; } try { ServiceNpc old=repository.find(id).orElse(null);if(old==null)return;OutfitCatalog.Outfit next=outfits.next(old.outfit(),old.accountId());if(next==null){message(player,"tc.service.admin.invalid");return;}ServiceNpc updated=new ServiceNpc(old.npcId(),old.type(),old.name(),old.male(),old.x(),old.y(),old.z(),old.rx(),old.ry(),old.rz(),old.rw(),old.accountId(),next.key());repository.save(updated);Npc npc=World.getNpc(id);if(npc!=null)initialize(npc,updated); }catch(SQLException ex){ServiceNpcPlugin.logger().error("Cannot change service NPC outfit: "+ex.getMessage());} }
    public boolean dissolve(Player player, long id) {
        if (player == null || !player.isAdmin()) { message(player,"tc.service.admin.required"); return false; }
        try { ServiceNpc endpoint=repository.find(id).orElse(null); if(endpoint==null){message(player,"tc.service.admin.notfound");return false;} if(repository.hasOpenJobs(id)){message(player,"tc.service.admin.jobs.open");return false;} if(!settleAndArchive(endpoint)){message(player,"tc.service.admin.settlement.failed");return false;} repository.delete(id); Npc npc=World.getNpc(id); if(npc!=null&&!npc.isDead())npc.delete(); message(player,"tc.service.admin.dissolved","PH_NAME",endpoint.name()); return true; } catch(SQLException ex){ServiceNpcPlugin.logger().error("Cannot dissolve service NPC: "+ex.getMessage()); message(player,"tc.service.admin.settlement.failed"); return false;}
    }
    public void interact(PlayerNpcInteractionEvent event) {
        try { ServiceNpc endpoint=repository.find(event.getNpc().getGlobalID()).orElse(null); if(endpoint==null)return; event.setCancelled(true); new ServiceNpcOverlay(plugin,event.getPlayer(),endpoint,this,i18n,jobs).open(); } catch(SQLException ex){ServiceNpcPlugin.logger().error("Cannot open service NPC: "+ex.getMessage());}
    }
    public List<ServiceNpc> endpoints() { try { return repository.all(); } catch(SQLException ex) { ServiceNpcPlugin.logger().error("Cannot list service NPCs: "+ex.getMessage()); return List.of(); } }
    private boolean ensureAccount(ServiceNpc endpoint) { WalletBridge wallet=new WalletBridge(plugin); return wallet.isAvailable() && wallet.hasSystemAccountApi() && wallet.createSystemAccount(endpoint.accountId(),"SERVICE_NPC",endpoint.name(),PLUGIN_NAME).success(); }
    private boolean settleAndArchive(ServiceNpc endpoint) { WalletBridge wallet=new WalletBridge(plugin); if(!wallet.isAvailable()||!wallet.hasSystemAccountApi())return false; var account=wallet.systemAccount(endpoint.accountId()); if(!account.success())return "ACCOUNT_NOT_FOUND".equals(account.errorCode()); String world=wallet.worldSystemAccountId(); if(world.isBlank())return false; for(var balance:wallet.systemAccountBalances(endpoint.accountId())) { if(balance.balance()<0)return false; if(balance.balance()>0&&!wallet.transferSystemToSystemIdempotent(endpoint.accountId(),world,balance.balance(),"Dissolved service NPC",balance.currencyIdentifier(),PLUGIN_NAME,"service-npc-dissolve-"+endpoint.npcId()+"-"+balance.currencyIdentifier()).success())return false; } return wallet.archiveSystemAccount(endpoint.accountId(),PLUGIN_NAME).success(); }
    private ServiceNpc fromNpc(Npc npc, ServiceType type, String name, boolean male, String account, String outfit) { Vector3f p=npc.getPosition(); Quaternion r=npc.getRotation(); return new ServiceNpc(npc.getGlobalID(),type,name,male,p.x,p.y,p.z,r.x,r.y,r.z,r.w,account,outfit); }
    private String uniqueName(String prefix, boolean male) { try { java.util.Set<String> used=repository.all().stream().map(ServiceNpc::name).collect(java.util.stream.Collectors.toSet()); for(int attempt=0;attempt<20;attempt++){String candidate=prefix+" "+names.random(male,random);if(!used.contains(candidate))return candidate;} }catch(SQLException ignored){} return prefix+" "+names.random(male,random)+" "+Integer.toUnsignedString(random.nextInt(),36); }
    private void initialize(Npc npc, ServiceNpc endpoint) {
        plugin.executeDelayed(.1f, () -> configure(npc, endpoint));
        plugin.executeDelayed(.5f, () -> configure(npc, endpoint));
    }
    private void configure(Npc npc, ServiceNpc endpoint) {
        if (npc == null || npc.isDead()) return;
        try {
            npc.setName(endpoint.name());
            npc.setLocked(true);
            npc.setInteractable(true);
            npc.setInvincible(true);
            npc.setStatic(false);
            Skin skin = npc.getSkin();
            if (skin != null) {
                skin.setGender(endpoint.male() ? Skin.Gender.Male : Skin.Gender.Female);
                int variant=Math.floorMod(endpoint.accountId().hashCode(),4);
                skin.setSkinColor(new int[]{0xC68642,0x8D5524,0xE0AC69,0xF1C27D}[variant]);
                skin.setHairColor(new int[]{0x1C120C,0x3B2A1D,0x5C3B24,0x2A1A12}[variant]);
                skin.setEyeColor(new int[]{0x4E7AA8,0x4C7A4C,0x6B4E2E,0x626262}[variant]);
                skin.setHairstyle(endpoint.male() ? new byte[]{58,55,62,48}[variant] : new byte[]{108,105,112,101}[variant]);
                skin.setBeard((byte) (endpoint.male() ? variant % 3 : -1));
            }
            npc.getClothes().removeAll();
            OutfitCatalog.Outfit outfit = outfits.find(endpoint.outfit(), endpoint.accountId());
            if (outfit == null) return;
            for (String garment : outfit.clothing()) {
                ClothingDefinition definition = Definitions.getClothingDefinition(garment);
                if (definition != null) npc.getClothes().add((short) definition.id);
            }
        } catch (Exception exception) {
            ServiceNpcPlugin.logger().error("Cannot initialize Service NPC " + endpoint.npcId() + ": " + exception.getMessage());
        }
    }
    private void message(Player p,String key,String... replacements) { if(p==null)return; String value=i18n.get(key,p); for(int i=0;i+1<replacements.length;i+=2)value=value.replace(replacements[i],replacements[i+1]); if(key.endsWith(".success")||key.endsWith(".renamed")||key.endsWith(".moved")||key.endsWith(".dissolved"))p.showSuccessMessageBox(i18n.get("tc.service.ui.title",p),value);else if(key.contains("failed")||key.contains("settlement"))p.showErrorMessageBox(i18n.get("tc.service.ui.title",p),value);else p.showWarningMessageBox(i18n.get("tc.service.ui.title",p),value); }
}
