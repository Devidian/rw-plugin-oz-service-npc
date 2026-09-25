package de.omegazirkel.risingworld.servicenpc;

import de.omegazirkel.risingworld.ServiceNpcPlugin;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BasePluginOverlayWithTabs;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import java.util.Arrays;
import java.util.Locale;
import net.risingworld.api.Server;
import net.risingworld.api.definitions.Items.Modifier;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UITextField;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.ui.UIScrollView;
import net.risingworld.api.ui.UIScrollView.ScrollViewMode;
import net.risingworld.api.ui.style.Align;
import net.risingworld.api.ui.style.DisplayStyle;
import net.risingworld.api.ui.style.FlexDirection;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.ui.style.Unit;
import net.risingworld.api.ui.style.Wrap;
import net.risingworld.api.ui.style.ScaleMode;
import net.risingworld.api.assets.TextureAsset;

/** Shared overlay shell. Service actions are intentionally kept outside the entry listener. */
public final class ServiceNpcOverlay extends BasePluginOverlayWithTabs {
    private enum ServiceTab { SERVICE, SETTINGS, JOB_LOG }
    private final ServiceNpc endpoint;
    private final I18n texts;
    private final ServiceJobService jobs;
    private final ServiceNpcManager manager;
    private ServiceTab tab = ServiceTab.SERVICE;

    public ServiceNpcOverlay(ServiceNpcPlugin plugin, Player player, ServiceNpc endpoint, ServiceNpcManager manager, I18n texts, ServiceJobService jobs) {
        super(player, ignored -> player.deleteAttribute("oz.service-npc.overlay"));
        this.endpoint = endpoint; this.manager = manager; this.texts = texts; this.jobs = jobs;
        titleLabelKey = "tc.service.ui.title"; descLabelKey = "tc.service.ui.subtitle"; legendLabelKey = "tc.service.ui.legend";
        rebuild();
    }
    public void open() { uiPlayer.setAttribute("oz.service-npc.overlay", this); uiPlayer.addUIElement(this, UITarget.Modal); }
    @Override protected I18n t() { return texts; }
    @Override protected String titleText() { return endpoint.name(); }
    @Override protected String descriptionText() { return t().get("tc.service.ui.identity." + endpoint.type().key(), uiPlayer); }
    @Override protected void setupTabs() {
        setupTabContainer();
        addTab(t().get("tc.service.ui.tab.service", uiPlayer), 180, tab == ServiceTab.SERVICE, () -> select(ServiceTab.SERVICE));
        if (uiPlayer.isAdmin()) {
            addTab(t().get("tc.service.ui.tab.settings", uiPlayer), 180, tab == ServiceTab.SETTINGS, true, () -> select(ServiceTab.SETTINGS));
        }
        addTab(t().get("tc.service.ui.tab.jobs", uiPlayer), 180, tab == ServiceTab.JOB_LOG, uiPlayer.isAdmin(), () -> select(ServiceTab.JOB_LOG));
        body.removeAllChilds();
        if (tab == ServiceTab.SERVICE && endpoint.type() == ServiceType.RESTORER) { setupRestorer(); return; }
        if (tab == ServiceTab.SERVICE && endpoint.type() == ServiceType.AUGMENTER) { setupAugmenter(); return; }
        if (tab == ServiceTab.JOB_LOG) { setupJobLog(); return; }
        if (tab == ServiceTab.SETTINGS) { setupSettings(); return; }
        String key = tab == ServiceTab.SERVICE ? "tc.service.ui.service." + endpoint.type().key() : tab == ServiceTab.SETTINGS ? "tc.service.ui.settings.help" : "tc.service.ui.jobs.empty";
        UILabel text = new UILabel(t().get(key, uiPlayer)); text.setPivot(Pivot.UpperLeft); text.setPosition(22, 20, false); text.setSize(760, 300, false); text.setFont(Font.Default); text.setFontSize(16); text.setTextWrap(true); body.addChild(text);
    }
    private void setupSettings() {
        UILabel help=new UILabel(t().get("tc.service.ui.settings.help",uiPlayer));help.setPivot(Pivot.UpperLeft);help.setPosition(18,16,false);help.setSize(740,34,false);help.setFontSize(14);help.setTextWrap(true);body.addChild(help);
        UITextField name=new UITextField(endpoint.name());name.setPivot(Pivot.UpperLeft);name.setPosition(18,64,false);name.setSize(400,30,false);name.setMaxCharacters(120);body.addChild(name);
        AdvancedButton rename=AdvancedButtonFactory.defaultButton(t().get("tc.service.ui.settings.rename",uiPlayer),ignored->name.getCurrentText(uiPlayer,value->{manager.rename(uiPlayer,endpoint.npcId(),value);rebuild();}));rename.setPivot(Pivot.UpperLeft);rename.setPosition(432,64,false);rename.setSize(140,30,false);body.addChild(rename);
        AdvancedButton move=AdvancedButtonFactory.defaultButton(t().get("tc.service.ui.settings.move",uiPlayer),ignored->{manager.move(uiPlayer,endpoint.npcId());rebuild();});move.setPivot(Pivot.UpperLeft);move.setPosition(18,112,false);move.setSize(180,32,false);body.addChild(move);
        AdvancedButton outfit=AdvancedButtonFactory.defaultButton(t().get("tc.service.ui.settings.outfit",uiPlayer),ignored->{manager.nextOutfit(uiPlayer,endpoint.npcId());rebuild();});outfit.setPivot(Pivot.UpperLeft);outfit.setPosition(212,112,false);outfit.setSize(180,32,false);body.addChild(outfit);
        AdvancedButton dissolve=AdvancedButtonFactory.danger(t().get("tc.service.ui.settings.dissolve",uiPlayer),ignored->{if(manager.dissolve(uiPlayer,endpoint.npcId()))close();});dissolve.setPivot(Pivot.UpperLeft);dissolve.setPosition(18,170,false);dissolve.setSize(180,32,false);body.addChild(dissolve);
    }
    private void setupAugmenter() {
        java.util.List<ServiceInventoryTransfer.Candidate> values = jobs.augmentCandidates(uiPlayer);
        setupServiceCards(values, true);
    }
    private static String materials(java.util.Map<String,Integer> materials) { return materials.entrySet().stream().map(e->e.getValue()+" "+e.getKey()).collect(java.util.stream.Collectors.joining(", ")); }
    private TextureAsset itemIcon(ServiceInventoryTransfer.Candidate item) { var definition=net.risingworld.api.definitions.Definitions.getItemDefinition(item.itemName()); return definition == null ? AssetManager.getIcon(uiPlayer,"placeholder") : definition.getIcon(item.variant()); }
    private static OZUIElement cardIcon(TextureAsset asset, float x, float y, int size) { OZUIElement icon=new OZUIElement();icon.setPivot(Pivot.MiddleCenter);icon.setPosition(x,y,true);icon.setSize(size,size,false);icon.setBackgroundColor(0,0,0,0);if(asset!=null){icon.style.backgroundImage.set(asset);icon.style.backgroundImageScaleMode.set(ScaleMode.ScaleToFit);}return icon; }
    private void setupRestorer() {
        java.util.List<ServiceInventoryTransfer.Candidate> values = jobs.repairCandidates(uiPlayer);
        setupServiceCards(values, false);
    }
    private void setupServiceCards(java.util.List<ServiceInventoryTransfer.Candidate> values, boolean augmenter) {
        UILabel intro=new UILabel(t().get("tc.service.ui.service."+(augmenter?"augmenter":"restorer"),uiPlayer));intro.setPivot(Pivot.UpperLeft);intro.setPosition(22,16,false);intro.setSize(760,44,false);intro.setFontSize(13);intro.setTextWrap(true);body.addChild(intro);
        if(values.isEmpty()){UILabel empty=new UILabel(t().get(augmenter?"tc.service.job.no.augmentable":"tc.service.job.no.damaged",uiPlayer));empty.setPivot(Pivot.UpperLeft);empty.setPosition(22,80,false);empty.setSize(720,30,false);empty.setFontSize(15);body.addChild(empty);return;}
        UIScrollView scroll=new UIScrollView(ScrollViewMode.Vertical);scroll.setPivot(Pivot.UpperLeft);scroll.setPosition(0,62,false);scroll.style.width.set(100,Unit.Percent);scroll.style.height.set(370,Unit.Pixel);body.addChild(scroll);
        OZUIElement wrapper=new OZUIElement();wrapper.setPivot(Pivot.UpperLeft);wrapper.style.width.set(100,Unit.Percent);wrapper.style.height.set(100,Unit.Percent);wrapper.style.display.set(DisplayStyle.Flex);wrapper.style.flexDirection.set(FlexDirection.Row);wrapper.style.flexWrap.set(Wrap.Wrap);wrapper.style.alignContent.set(Align.FlexStart);
        for(ServiceInventoryTransfer.Candidate item:values){OZUIElement card=new OZUIElement();card.setPivot(Pivot.UpperLeft);card.style.width.set(31,Unit.Percent);card.style.height.set(92,Unit.Pixel);card.style.marginLeft.set(5);card.style.marginRight.set(5);card.style.marginTop.set(5);card.style.marginBottom.set(5);card.setBackgroundColor(.10f,.09f,.08f,.92f);card.setHoverBackgroundColor(0x2A2419DD);card.setBorder(1);card.setBorderColor(.95f,.75f,.25f,.42f);
            UILabel name=new UILabel(item.displayName());name.setPivot(Pivot.UpperLeft);name.setPosition(10,8,false);name.setSize(43,28,true);name.setFontSize(14);name.setTextWrap(true);card.addChild(name);card.addChild(cardIcon(itemIcon(item),54,42,72));
            String detail; AdvancedButton choose;
            if(augmenter){ServiceJobService.AugmentQuote quote=jobs.augmentQuote(item);if(quote==null)continue;detail=localizedModifier(item.modifier())+" → "+localizedModifier(quote.targetModifier())+"\n"+quote.tier().serviceFee()+" "+jobs.currency()+" · "+quote.tier().minutes()+" min";choose=AdvancedButtonFactory.defaultButton(t().get("tc.service.job.accept",uiPlayer),ignored->confirmAugmenter(quote));}
            else {ServiceJobService.RestorerQuote quote=jobs.quote(item);detail=item.durability()+"/"+item.maxDurability()+" · "+quote.cost()+" "+jobs.currency()+" · "+ServiceJobService.durationMinutes(quote.completedAt())+" min";choose=AdvancedButtonFactory.defaultButton(t().get("tc.service.job.accept",uiPlayer),ignored->confirmRestorer(quote));}
            UILabel info=new UILabel(detail);info.setPivot(Pivot.UpperLeft);info.setPosition(10,40,false);info.setSize(43,42,true);info.setFontSize(12);info.setTextWrap(true);card.addChild(info);choose.setPivot(Pivot.UpperRight);choose.setPosition(98,65,true);choose.setSize(126,24,false);card.addChild(choose);wrapper.addChild(card);}
        scroll.addChild(wrapper);
    }
    private void confirmRestorer(ServiceJobService.RestorerQuote quote) {
        OZUIElement dialog = new OZUIElement(); dialog.setPivot(Pivot.MiddleCenter); dialog.setPosition(50, 50, true); dialog.setSize(460, 205, false); dialog.setBackgroundColor(0,0,0,.94f); dialog.setBorder(1); dialog.setBorderColor(.85f,.65f,.2f,.8f); addChild(dialog);
        UILabel text = new UILabel(t().get("tc.service.job.confirm", uiPlayer).replace("PH_ITEM", quote.item().displayName()).replace("PH_COST", quote.cost()+" "+jobs.currency()).replace("PH_MINUTES", Long.toString(ServiceJobService.durationMinutes(quote.completedAt())))); text.setPivot(Pivot.UpperLeft); text.setPosition(20,22,false); text.setSize(420,96,false); text.setFontSize(15); text.setTextWrap(true); dialog.addChild(text);
        AdvancedButton cancel=AdvancedButtonFactory.cancel(t().get("tc.service.job.cancel",uiPlayer), ignored -> removeChild(dialog));cancel.setPivot(Pivot.UpperLeft);cancel.setPosition(20,155,false);cancel.setSize(150,32,false);dialog.addChild(cancel);
        AdvancedButton accept=AdvancedButtonFactory.ok(t().get("tc.service.job.accept",uiPlayer), ignored -> { removeChild(dialog); if(jobs.submitRestorer(uiPlayer,endpoint,quote)) rebuild(); });accept.setPivot(Pivot.UpperRight);accept.setPosition(440,155,false);accept.setSize(150,32,false);dialog.addChild(accept);
    }
    private void confirmAugmenter(ServiceJobService.AugmentQuote quote) {
        OZUIElement dialog=new OZUIElement();dialog.setPivot(Pivot.MiddleCenter);dialog.setPosition(50,50,true);dialog.setSize(460,205,false);dialog.setBackgroundColor(0,0,0,.94f);dialog.setBorder(1);dialog.setBorderColor(.85f,.65f,.2f,.8f);addChild(dialog);
        String value=t().get("tc.service.job.confirm",uiPlayer).replace("PH_ITEM",quote.item().displayName()+" ("+localizedModifier(quote.item().modifier())+" → "+localizedModifier(quote.targetModifier())+")").replace("PH_COST",quote.tier().serviceFee()+" "+jobs.currency()).replace("PH_MINUTES",Long.toString(quote.tier().minutes()))+"\n"+materials(quote.tier().materials());
        UILabel text=new UILabel(value);text.setPivot(Pivot.UpperLeft);text.setPosition(20,22,false);text.setSize(420,112,false);text.setFontSize(15);text.setTextWrap(true);dialog.addChild(text);
        AdvancedButton cancel=AdvancedButtonFactory.cancel(t().get("tc.service.job.cancel",uiPlayer),ignored->removeChild(dialog));cancel.setPivot(Pivot.UpperLeft);cancel.setPosition(20,155,false);cancel.setSize(150,32,false);dialog.addChild(cancel);
        AdvancedButton accept=AdvancedButtonFactory.ok(t().get("tc.service.job.accept",uiPlayer),ignored->{removeChild(dialog);if(jobs.submitAugmenter(uiPlayer,endpoint,quote))rebuild();});accept.setPivot(Pivot.UpperRight);accept.setPosition(440,155,false);accept.setSize(150,32,false);dialog.addChild(accept);
    }
    private void setupJobLog() {
        java.util.List<ServiceJob> values = jobs.jobsFor(uiPlayer, endpoint);
        if (values.isEmpty()) { UILabel empty = new UILabel(t().get("tc.service.ui.jobs.empty", uiPlayer)); empty.setPivot(Pivot.UpperLeft); empty.setPosition(22,20,false); empty.setSize(740,32,false); empty.setFontSize(15); body.addChild(empty); return; }
        TableScrollView table=new TableScrollView(uiPlayer.isAdmin()?Arrays.asList("Item","Preis","Status","Zeit","Aktion"):Arrays.asList("Item","Preis","Status","Zeit"),uiPlayer.isAdmin()?Arrays.asList(30f,16f,18f,18f,18f):Arrays.asList(34f,20f,22f,24f));table.setPosition(0,16,false);table.style.width.set(100,Unit.Percent);table.setScrollBodyHeight(245);body.addChild(table);
        for (int index = 0; index < values.size(); index++) {
            ServiceJob job = values.get(index);
            String status = t().get("tc.service.job.status." + job.status().toLowerCase(), uiPlayer);
            String time = job.open() ? t().get("tc.service.job.remaining", uiPlayer).replace("PH_MINUTES", Long.toString(ServiceJobService.durationMinutes(job.completedAt()))) : t().get("tc.service.job.completed", uiPlayer);
            java.util.List<TableCell> cells=new java.util.ArrayList<>(Arrays.asList(cell(localizedItemName(job),uiPlayer.isAdmin()?30:34),cell(job.cost()+" "+job.currency(),uiPlayer.isAdmin()?16:20),cell(status,uiPlayer.isAdmin()?18:22),cell(time,uiPlayer.isAdmin()?18:24)));
            if(uiPlayer.isAdmin()) { if(job.open()){ AdvancedButton finish=AdvancedButtonFactory.ok(t().get("tc.service.job.finish",uiPlayer),ignored->{if(jobs.completeNow(job.id()))rebuild();});finish.setSize(132,26,false);cells.add(new TableCell(finish,18)); } else cells.add(cell("",18)); }
            table.addRow(new TableRow(cells));
        }
    }
    private static TableCell cell(String value,float width){UILabel label=new UILabel(value);label.setFont(Font.Default);label.setFontSize(12);label.setTextAlign(TextAnchor.MiddleLeft);return new TableCell(label,width);}
    private String localizedItemName(ServiceJob job){var definition=net.risingworld.api.definitions.Definitions.getItemDefinition(job.itemName());if(definition==null)return job.itemName();String value=definition.getLocalizedName(uiPlayer.getLanguage());return value==null||value.isBlank()?job.itemName():value;}
    private String localizedModifier(String name) {
        if (name == null || name.isBlank()) return "";
        if (Modifier.Normal.name().equals(name)) return name;
        String localized = Server.getLocalizedString(uiPlayer.getLanguage(), "item.modifier." + name.toLowerCase(Locale.ROOT));
        return localized == null || localized.isBlank() ? name : localized;
    }
    private void select(ServiceTab value) { tab = value; rebuild(); }
}
