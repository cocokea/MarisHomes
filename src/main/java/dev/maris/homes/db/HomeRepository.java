package dev.maris.homes.db;

import dev.maris.homes.MarisHomesPlugin;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public final class HomeRepository {
    private final StorageManager storage;
    private final ExecutorService executor;

    public HomeRepository(MarisHomesPlugin plugin, StorageManager storage) {
        this.storage = storage;
        int threads = Math.max(1, plugin.getConfig().getInt("async.sql-threads", 3));
        this.executor = Executors.newFixedThreadPool(threads, r -> { Thread t = new Thread(r, "MarisHomes-SQL"); t.setDaemon(true); return t; });
    }

    public CompletableFuture<Void> initSchema() {
        return run(() -> {
            try (Connection c = storage.getConnection(); Statement st = c.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS maris_homes (uuid VARCHAR(36) NOT NULL, home_slot INT NOT NULL, world VARCHAR(128) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, yaw FLOAT NOT NULL, pitch FLOAT NOT NULL, PRIMARY KEY(uuid, home_slot))");
            }
        });
    }

    public CompletableFuture<Map<Integer, HomeData>> getHomes(UUID uuid) {
        return supply(() -> {
            Map<Integer, HomeData> map = new HashMap<>();
            try (Connection c = storage.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM maris_homes WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) map.put(rs.getInt("home_slot"), new HomeData(uuid, rs.getInt("home_slot"), rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch")));
                }
            }
            return map;
        });
    }

    public CompletableFuture<Optional<HomeData>> getHome(UUID uuid, int slot) {
        return supply(() -> {
            try (Connection c = storage.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM maris_homes WHERE uuid=? AND home_slot=?")) {
                ps.setString(1, uuid.toString()); ps.setInt(2, slot);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    return Optional.of(new HomeData(uuid, slot, rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch")));
                }
            }
        });
    }

    public CompletableFuture<Void> setHome(HomeData h) {
        return run(() -> {
            String sql = storage.isMysql()
                ? "INSERT INTO maris_homes(uuid,home_slot,world,x,y,z,yaw,pitch) VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE world=VALUES(world),x=VALUES(x),y=VALUES(y),z=VALUES(z),yaw=VALUES(yaw),pitch=VALUES(pitch)"
                : "INSERT INTO maris_homes(uuid,home_slot,world,x,y,z,yaw,pitch) VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(uuid,home_slot) DO UPDATE SET world=excluded.world,x=excluded.x,y=excluded.y,z=excluded.z,yaw=excluded.yaw,pitch=excluded.pitch";
            try (Connection c = storage.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, h.uuid().toString()); ps.setInt(2, h.slot()); ps.setString(3, h.world()); ps.setDouble(4, h.x()); ps.setDouble(5, h.y()); ps.setDouble(6, h.z()); ps.setFloat(7, h.yaw()); ps.setFloat(8, h.pitch()); ps.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> deleteHome(UUID uuid, int slot) {
        return run(() -> { try (Connection c = storage.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM maris_homes WHERE uuid=? AND home_slot=?")) { ps.setString(1, uuid.toString()); ps.setInt(2, slot); ps.executeUpdate(); } });
    }

    private CompletableFuture<Void> run(SqlRunnable r) { return CompletableFuture.runAsync(() -> { try { r.run(); } catch (Exception e) { throw new CompletionException(e); } }, executor); }
    private <T> CompletableFuture<T> supply(SqlSupplier<T> s) { return CompletableFuture.supplyAsync(() -> { try { return s.get(); } catch (Exception e) { throw new CompletionException(e); } }, executor); }
    interface SqlRunnable { void run() throws Exception; }
    interface SqlSupplier<T> { T get() throws Exception; }
}
