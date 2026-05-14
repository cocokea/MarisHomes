package dev.maris.homes.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

public final class StorageManager {
    private final JavaPlugin plugin;
    private HikariDataSource dataSource;
    private boolean mysql;

    public StorageManager(JavaPlugin plugin) { this.plugin = plugin; }

    public void init() {
        String type = plugin.getConfig().getString("storage.type", "sqlite").toLowerCase(Locale.ROOT);
        mysql = type.equals("mysql");
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("MarisHomesPool");
        cfg.setMaximumPoolSize(mysql ? plugin.getConfig().getInt("storage.mysql.pool-size", 10) : plugin.getConfig().getInt("storage.sqlite.pool-size", 1));
        if (mysql) {
            String host = plugin.getConfig().getString("storage.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
            String db = plugin.getConfig().getString("storage.mysql.database", "marishomes");
            boolean ssl = plugin.getConfig().getBoolean("storage.mysql.useSSL", false);
            cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=" + ssl + "&characterEncoding=utf8&useUnicode=true&serverTimezone=UTC");
            cfg.setUsername(plugin.getConfig().getString("storage.mysql.username", "root"));
            cfg.setPassword(plugin.getConfig().getString("storage.mysql.password", ""));
            cfg.setConnectionTimeout(plugin.getConfig().getLong("storage.mysql.connection-timeout", 30000L));
            cfg.setIdleTimeout(plugin.getConfig().getLong("storage.mysql.idle-timeout", 600000L));
            cfg.setMaxLifetime(plugin.getConfig().getLong("storage.mysql.max-lifetime", 1800000L));
            cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File file = new File(plugin.getDataFolder(), plugin.getConfig().getString("storage.sqlite.file", "homes.db"));
            cfg.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            cfg.setDriverClassName("org.sqlite.JDBC");
            cfg.setConnectionTestQuery("SELECT 1");
        }
        this.dataSource = new HikariDataSource(cfg);
    }

    public Connection getConnection() throws SQLException { return dataSource.getConnection(); }
    public boolean isMysql() { return mysql; }
    public void close() { if (dataSource != null) dataSource.close(); }
}
