package de.omegazirkel.risingworld.servicenpc;

import java.sql.*;
import java.util.*;

/** SQLite persistence is deliberately limited to plugin-owned endpoint metadata. */
public final class ServiceNpcRepository {
    private final Connection db;
    public ServiceNpcRepository(Connection db) { this.db = db; }
    public void initialize() throws SQLException {
        try (Statement s = db.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS service_npcs (npc_id INTEGER PRIMARY KEY, type TEXT NOT NULL, name TEXT NOT NULL, male INTEGER NOT NULL, x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, rx REAL NOT NULL, ry REAL NOT NULL, rz REAL NOT NULL, rw REAL NOT NULL, account_id TEXT NOT NULL, outfit TEXT NOT NULL DEFAULT '')");
            ensureColumn(s, "service_npcs", "outfit", "TEXT NOT NULL DEFAULT ''");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS service_jobs (id TEXT PRIMARY KEY, npc_id INTEGER NOT NULL, player_db_id INTEGER NOT NULL, player_name TEXT NOT NULL, type TEXT NOT NULL, accepted_at INTEGER NOT NULL, completed_at INTEGER NOT NULL DEFAULT 0, status TEXT NOT NULL, language TEXT NOT NULL DEFAULT 'en', details TEXT NOT NULL DEFAULT '', item_name TEXT NOT NULL DEFAULT '', item_variant INTEGER NOT NULL DEFAULT 0, item_durability INTEGER NOT NULL DEFAULT 0, item_status INTEGER NOT NULL DEFAULT 0, item_modifier TEXT NOT NULL DEFAULT '', target_modifier TEXT NOT NULL DEFAULT '', item_color INTEGER NOT NULL DEFAULT 0, cost INTEGER NOT NULL DEFAULT 0, currency TEXT NOT NULL DEFAULT '', correlation_id TEXT NOT NULL DEFAULT '')");
            ensureColumn(s, "service_jobs", "language", "TEXT NOT NULL DEFAULT 'en'");
            ensureColumn(s, "service_jobs", "item_name", "TEXT NOT NULL DEFAULT ''");
            ensureColumn(s, "service_jobs", "item_variant", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(s, "service_jobs", "item_durability", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(s, "service_jobs", "item_status", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(s, "service_jobs", "item_modifier", "TEXT NOT NULL DEFAULT ''");
            ensureColumn(s, "service_jobs", "target_modifier", "TEXT NOT NULL DEFAULT ''");
            ensureColumn(s, "service_jobs", "item_color", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(s, "service_jobs", "cost", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(s, "service_jobs", "currency", "TEXT NOT NULL DEFAULT ''");
            ensureColumn(s, "service_jobs", "correlation_id", "TEXT NOT NULL DEFAULT ''");
        }
    }
    public List<ServiceNpc> all() throws SQLException { try (Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM service_npcs")) { List<ServiceNpc> v=new ArrayList<>(); while(r.next()) v.add(read(r)); return v; } }
    public Optional<ServiceNpc> find(long id) throws SQLException { try (PreparedStatement s=db.prepareStatement("SELECT * FROM service_npcs WHERE npc_id=?")) { s.setLong(1,id); try(ResultSet r=s.executeQuery()) { return r.next()?Optional.of(read(r)):Optional.empty(); } } }
    public void save(ServiceNpc v) throws SQLException { try (PreparedStatement s=db.prepareStatement("INSERT INTO service_npcs VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(npc_id) DO UPDATE SET type=excluded.type,name=excluded.name,male=excluded.male,x=excluded.x,y=excluded.y,z=excluded.z,rx=excluded.rx,ry=excluded.ry,rz=excluded.rz,rw=excluded.rw,account_id=excluded.account_id,outfit=excluded.outfit")) { write(s,v); s.executeUpdate(); } }
    public void replaceId(long oldId, ServiceNpc replacement) throws SQLException { try(PreparedStatement s=db.prepareStatement("DELETE FROM service_npcs WHERE npc_id=?")){s.setLong(1,oldId);s.executeUpdate();} save(replacement); }
    public void delete(long id) throws SQLException { try(PreparedStatement s=db.prepareStatement("DELETE FROM service_npcs WHERE npc_id=?")){s.setLong(1,id);s.executeUpdate();} }
    public boolean hasOpenJobs(long id) throws SQLException { try(PreparedStatement s=db.prepareStatement("SELECT 1 FROM service_jobs WHERE npc_id=? AND status NOT IN ('COMPLETED','CANCELLED') LIMIT 1")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){return r.next();}} }
    public void saveJob(ServiceJob job) throws SQLException { try (PreparedStatement s=db.prepareStatement("INSERT INTO service_jobs(id,npc_id,player_db_id,player_name,type,accepted_at,completed_at,status,language,item_name,item_variant,item_durability,item_status,item_modifier,target_modifier,item_color,cost,currency,correlation_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) { s.setString(1,job.id());s.setLong(2,job.npcId());s.setInt(3,job.playerDbId());s.setString(4,job.playerName());s.setString(5,job.type().name());s.setLong(6,job.acceptedAt());s.setLong(7,job.completedAt());s.setString(8,job.status());s.setString(9,job.language());s.setString(10,job.itemName());s.setInt(11,job.itemVariant());s.setInt(12,job.itemDurability());s.setShort(13,job.itemStatus());s.setString(14,job.itemModifier());s.setString(15,job.targetModifier());s.setInt(16,job.itemColor());s.setLong(17,job.cost());s.setString(18,job.currency());s.setString(19,job.correlationId());s.executeUpdate(); } }
    public void updateJobStatus(String id,String status) throws SQLException { try(PreparedStatement s=db.prepareStatement("UPDATE service_jobs SET status=? WHERE id=?")){s.setString(1,status);s.setString(2,id);s.executeUpdate();} }
    public void deleteJob(String id) throws SQLException { try(PreparedStatement s=db.prepareStatement("DELETE FROM service_jobs WHERE id=?")){s.setString(1,id);s.executeUpdate();} }
    public List<ServiceJob> dueJobs(long now) throws SQLException { try(PreparedStatement s=db.prepareStatement("SELECT * FROM service_jobs WHERE status='OPEN' AND completed_at<=? ORDER BY completed_at,id")){s.setLong(1,now);try(ResultSet r=s.executeQuery()){List<ServiceJob> jobs=new ArrayList<>();while(r.next())jobs.add(readJob(r));return jobs;}} }
    public List<ServiceJob> jobsForPlayer(long npcId,int playerDbId) throws SQLException { try(PreparedStatement s=db.prepareStatement("SELECT * FROM service_jobs WHERE npc_id=? AND player_db_id=? ORDER BY CASE WHEN status IN ('PREPARING','OPEN') THEN 0 ELSE 1 END, CASE WHEN status IN ('PREPARING','OPEN') THEN completed_at END ASC, accepted_at DESC LIMIT 30")){s.setLong(1,npcId);s.setInt(2,playerDbId);return readJobs(s);} }
    public List<ServiceJob> jobsForNpc(long npcId) throws SQLException { try(PreparedStatement s=db.prepareStatement("SELECT * FROM service_jobs WHERE npc_id=? ORDER BY CASE WHEN status IN ('PREPARING','OPEN') THEN 0 ELSE 1 END, CASE WHEN status IN ('PREPARING','OPEN') THEN completed_at END ASC, accepted_at DESC LIMIT 30")){s.setLong(1,npcId);return readJobs(s);} }
    public Optional<ServiceJob> job(String id) throws SQLException { try(PreparedStatement s=db.prepareStatement("SELECT * FROM service_jobs WHERE id=?")){s.setString(1,id);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(readJob(r)):Optional.empty();}} }
    private static List<ServiceJob> readJobs(PreparedStatement s) throws SQLException { try(ResultSet r=s.executeQuery()){List<ServiceJob> jobs=new ArrayList<>();while(r.next())jobs.add(readJob(r));return jobs;} }
    private static void ensureColumn(Statement statement,String table,String column,String definition) throws SQLException { try { statement.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition); } catch (SQLException ignored) { } }
    private static ServiceJob readJob(ResultSet r) throws SQLException { return new ServiceJob(r.getString("id"),r.getLong("npc_id"),r.getInt("player_db_id"),r.getString("player_name"),ServiceType.valueOf(r.getString("type")),r.getLong("accepted_at"),r.getLong("completed_at"),r.getString("status"),r.getString("language"),r.getString("item_name"),r.getInt("item_variant"),r.getInt("item_durability"),r.getShort("item_status"),r.getString("item_modifier"),r.getString("target_modifier"),r.getInt("item_color"),r.getLong("cost"),r.getString("currency"),r.getString("correlation_id")); }
    private static ServiceNpc read(ResultSet r) throws SQLException { return new ServiceNpc(r.getLong("npc_id"),ServiceType.valueOf(r.getString("type")),r.getString("name"),r.getInt("male")!=0,r.getFloat("x"),r.getFloat("y"),r.getFloat("z"),r.getFloat("rx"),r.getFloat("ry"),r.getFloat("rz"),r.getFloat("rw"),r.getString("account_id"),r.getString("outfit")); }
    private static void write(PreparedStatement s, ServiceNpc v) throws SQLException { s.setLong(1,v.npcId());s.setString(2,v.type().name());s.setString(3,v.name());s.setInt(4,v.male()?1:0);s.setFloat(5,v.x());s.setFloat(6,v.y());s.setFloat(7,v.z());s.setFloat(8,v.rx());s.setFloat(9,v.ry());s.setFloat(10,v.rz());s.setFloat(11,v.rw());s.setString(12,v.accountId());s.setString(13,v.outfit()); }
}
