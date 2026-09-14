package de.omegazirkel.risingworld.servicenpc;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.Random;

import de.omegazirkel.risingworld.ServiceNpcPlugin;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.bridge.MailBridge;
import de.omegazirkel.risingworld.tools.bridge.WalletBridge;
import de.omegazirkel.risingworld.template.PluginSettings;
import net.risingworld.api.objects.Player;

/** Owns the Restorer custody/payment/delivery transaction. */
public final class ServiceJobService {
    private static final String PLUGIN_NAME = "OZ - Service NPC";
    private final ServiceNpcPlugin plugin;
    private final ServiceNpcRepository repository;
    private final I18n texts;
    private final PluginSettings settings;
    private final AugmentationCatalog augmentation;
    private final Random random = new Random();
    private volatile boolean active = true;

    public ServiceJobService(ServiceNpcPlugin plugin, ServiceNpcRepository repository, I18n texts, PluginSettings settings, AugmentationCatalog augmentation) {
        this.plugin = plugin; this.repository = repository; this.texts = texts; this.settings = settings; this.augmentation = augmentation;
    }

    public List<ServiceInventoryTransfer.Candidate> repairCandidates(Player player) {
        return ServiceInventoryTransfer.repairCandidates(player);
    }
    public List<ServiceInventoryTransfer.Candidate> augmentCandidates(Player player) { return ServiceInventoryTransfer.augmentCandidates(player).stream().filter(item -> augmentation.nextTier(item.modifier()) != null).toList(); }
    public String currency() { return new WalletBridge(plugin).defaultCurrencyIdentifier(); }
    public AugmentQuote augmentQuote(ServiceInventoryTransfer.Candidate item) {
        if (item == null) return null;
        AugmentationCatalog.Tier tier = augmentation.nextTier(item.modifier());
        if (tier == null || !tier.enabled()) return null;
        return new AugmentQuote(item, tier, tier.modifiers().get(random.nextInt(tier.modifiers().size())), Math.addExact(System.currentTimeMillis(), tier.minutes() * 60_000L));
    }
    public List<ServiceJob> jobsFor(Player player, ServiceNpc endpoint) {
        if (player == null || endpoint == null) return List.of();
        try { return player.isAdmin() ? repository.jobsForNpc(endpoint.npcId()) : repository.jobsForPlayer(endpoint.npcId(), player.getDbID()); }
        catch (SQLException exception) { ServiceNpcPlugin.logger().error("Cannot load service jobs: " + exception.getMessage()); return List.of(); }
    }

    public RestorerQuote quote(ServiceInventoryTransfer.Candidate item) {
        if (item == null) return null;
        long missing = item.missingDurability();
        long material = Math.max(0L, Math.round(missing * settings.longValue("restorerMaterialCostPercent", 25L, 0L, 10_000L) / 100.0d));
        long cost = Math.addExact(material, settings.longValue("restorerServiceFee", 5L, 0L, Long.MAX_VALUE));
        long configuredHours = settings.longValue("restorerHoursAtOneDurability", 6L, 0L, 10_000L);
        // The configured duration is the full repair time at 1/max durability,
        // not a multiplier per missing durability point.
        double repairShare = Math.min(1d, missing / (double) Math.max(1, item.maxDurability() - 1));
        long durationMillis = Math.round(configuredHours * 3_600_000d * repairShare);
        long completion = Math.addExact(System.currentTimeMillis(), durationMillis);
        return new RestorerQuote(item, cost, completion);
    }

    public boolean submitRestorer(Player player, ServiceNpc endpoint, RestorerQuote quote) {
        if (player == null || endpoint == null || quote == null || endpoint.type() != ServiceType.RESTORER) return false;
        if (!new MailBridge(plugin).canReceiveMail(player.getDbID())) { message(player, "tc.service.job.mail.unavailable"); return false; }
        WalletBridge wallet = new WalletBridge(plugin);
        String currency = wallet.defaultCurrencyIdentifier();
        if (!wallet.isAvailable() || !wallet.hasSystemAccountApi() || currency.isBlank()) { message(player, "tc.service.wallet.unavailable"); return false; }
        String id = UUID.randomUUID().toString();
        String correlation = "service-npc-job-" + id;
        ServiceInventoryTransfer.Candidate item = quote.item();
        ServiceJob job = new ServiceJob(id, endpoint.npcId(), player.getDbID(), player.getName(), ServiceType.RESTORER,
                System.currentTimeMillis(), quote.completedAt(), "PREPARING", player.getLanguage(), item.itemName(), item.variant(), item.durability(),
                item.status(), item.modifier(), "", item.color(), quote.cost(), currency, correlation);
        try {
            repository.saveJob(job);
            if (!wallet.transferPlayerToSystemIdempotent(player.getDbID(), endpoint.accountId(), quote.cost(),
                    "Restorer service", currency, PLUGIN_NAME, correlation).success()) { repository.deleteJob(id); message(player, "tc.service.job.payment.failed"); return false; }
            if (!ServiceInventoryTransfer.remove(player, item)) {
                boolean refunded = wallet.reverseAccountTransferIdempotent(correlation, correlation + "-refund",
                        "Restorer service refund", PLUGIN_NAME).success();
                if (refunded) repository.deleteJob(id); else repository.updateJobStatus(id, "REFUND_PENDING");
                message(player, refunded ? "tc.service.job.item.changed" : "tc.service.job.refund.pending");
                return false;
            }
            repository.updateJobStatus(id, "OPEN");
            message(player, "tc.service.job.accepted", "PH_COST", Long.toString(quote.cost()), "PH_MINUTES",
                    Long.toString(durationMinutes(quote.completedAt())));
            return true;
        } catch (SQLException | ArithmeticException exception) {
            ServiceNpcPlugin.logger().error("Cannot submit restorer job: " + exception.getMessage());
            message(player, "tc.service.job.failed");
            return false;
        }
    }
    public boolean submitAugmenter(Player player, ServiceNpc endpoint, AugmentQuote quote) {
        if (player == null || endpoint == null || quote == null || endpoint.type() != ServiceType.AUGMENTER) return false;
        if (!new MailBridge(plugin).canReceiveMail(player.getDbID())) { message(player, "tc.service.job.mail.unavailable"); return false; }
        if (!ServiceInventoryTransfer.hasMaterials(player, quote.tier().materials())) { message(player, "tc.service.job.materials.missing"); return false; }
        WalletBridge wallet = new WalletBridge(plugin); String currency = wallet.defaultCurrencyIdentifier();
        if (!wallet.isAvailable() || !wallet.hasSystemAccountApi() || currency.isBlank()) { message(player, "tc.service.wallet.unavailable"); return false; }
        String id=UUID.randomUUID().toString(), correlation="service-npc-job-"+id; ServiceInventoryTransfer.Candidate item=quote.item();
        ServiceJob job=new ServiceJob(id,endpoint.npcId(),player.getDbID(),player.getName(),ServiceType.AUGMENTER,System.currentTimeMillis(),quote.completedAt(),"PREPARING",player.getLanguage(),item.itemName(),item.variant(),item.durability(),item.status(),item.modifier(),quote.targetModifier(),item.color(),quote.tier().serviceFee(),currency,correlation);
        try {
            repository.saveJob(job);
            if (!wallet.transferPlayerToSystemIdempotent(player.getDbID(), endpoint.accountId(), quote.tier().serviceFee(), "Augmenter service", currency, PLUGIN_NAME, correlation).success()) { repository.deleteJob(id); message(player,"tc.service.job.payment.failed"); return false; }
            if (!ServiceInventoryTransfer.remove(player,item) || !ServiceInventoryTransfer.removeMaterials(player,quote.tier().materials())) {
                boolean refunded=wallet.reverseAccountTransferIdempotent(correlation,correlation+"-refund","Augmenter service refund",PLUGIN_NAME).success();
                if(refunded) repository.deleteJob(id); else repository.updateJobStatus(id,"REFUND_PENDING"); message(player,refunded?"tc.service.job.item.changed":"tc.service.job.refund.pending"); return false;
            }
            repository.updateJobStatus(id,"OPEN"); message(player,"tc.service.job.accepted","PH_COST",Long.toString(quote.tier().serviceFee()),"PH_MINUTES",Long.toString(durationMinutes(quote.completedAt()))); return true;
        } catch(SQLException|ArithmeticException exception) { ServiceNpcPlugin.logger().error("Cannot submit augmenter job: "+exception.getMessage()); message(player,"tc.service.job.failed"); return false; }
    }

    public void completeDueJobs() {
        if (!active) return;
        try {
            for (ServiceJob job : repository.dueJobs(System.currentTimeMillis())) complete(job);
        } catch (SQLException exception) { ServiceNpcPlugin.logger().error("Cannot process service jobs: " + exception.getMessage()); }
        plugin.executeDelayed(60f, this::completeDueJobs);
    }
    public void stop() { active = false; }
    public boolean completeNow(String jobId) {
        try { var job=repository.job(jobId); if(job.isEmpty() || !job.get().open()) return false; complete(job.get()); return true; }
        catch (SQLException exception) { ServiceNpcPlugin.logger().error("Cannot complete service job: " + exception.getMessage()); return false; }
    }

    private void complete(ServiceJob job) throws SQLException {
        int durability = job.type() == ServiceType.RESTORER ? Math.max(job.itemDurability(), maxDurability(job.itemName())) : job.itemDurability();
        String modifier = job.type() == ServiceType.AUGMENTER ? job.targetModifier() : job.itemModifier();
        String prefix = job.type() == ServiceType.RESTORER ? "restorer" : "augmenter";
        String body = texts.get("tc.service.mail." + prefix + ".body", job.language())
                .replace("PH_ITEM", localizedItemName(job))
                .replace("PH_BEFORE", job.type() == ServiceType.RESTORER ? Integer.toString(job.itemDurability()) : job.itemModifier())
                .replace("PH_AFTER", job.type() == ServiceType.RESTORER ? Integer.toString(durability) : modifier)
                .replace("PH_MAX", Integer.toString(maxDurability(job.itemName())));
        MailBridge.BridgeResult result = new MailBridge(plugin).sendAttachmentMail(new MailBridge.PluginAttachmentMailRequest(
                PLUGIN_NAME, job.playerDbId(), job.playerName(), texts.get("tc.service.mail." + prefix + ".subject", job.language()),
                body, "service-npc-delivery-" + job.id(), List.of(new MailBridge.PluginAttachment(
                        job.itemName(), job.itemVariant(), 1, durability, job.itemStatus(), modifier, job.itemColor()))));
        if (result.success()) repository.updateJobStatus(job.id(), "COMPLETED");
        else ServiceNpcPlugin.logger().warn("Restorer delivery pending for " + job.id() + ": " + result.code());
    }

    private static int maxDurability(String itemName) {
        var definition = net.risingworld.api.definitions.Definitions.getItemDefinition(itemName);
        return definition == null ? 0 : Math.max(0, definition.durability);
    }
    private String localizedItemName(ServiceJob job) {
        var definition = net.risingworld.api.definitions.Definitions.getItemDefinition(job.itemName());
        if (definition == null) return job.itemName();
        String value = definition.getLocalizedName(job.language());
        return value == null || value.isBlank() ? job.itemName() : value;
    }
    public static long durationMinutes(long completedAt) { return Math.max(0L, (completedAt - System.currentTimeMillis() + 59_999L) / 60_000L); }
    private void message(Player player, String key, String... replacements) {
        String value = texts.get(key, player);
        for (int i = 0; i + 1 < replacements.length; i += 2) value = value.replace(replacements[i], replacements[i + 1]);
        player.sendTextMessage(value);
    }
    public record RestorerQuote(ServiceInventoryTransfer.Candidate item, long cost, long completedAt) { }
    public record AugmentQuote(ServiceInventoryTransfer.Candidate item, AugmentationCatalog.Tier tier, String targetModifier, long completedAt) { }
}
