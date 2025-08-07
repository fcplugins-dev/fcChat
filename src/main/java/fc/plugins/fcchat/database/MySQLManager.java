package fc.plugins.fcchat.database;

import fc.plugins.fcchat.FcChat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MySQLManager {
    private final FcChat plugin;
    private FileConfiguration mysqlConfig;
    private Connection connection;
    private ExecutorService executor;
    private boolean enabled;

    public MySQLManager(FcChat plugin) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(3);
        loadConfig();
        if (enabled) {
            connect();
        }
    }

    private void loadConfig() {
        File mysqlFile = new File(plugin.getDataFolder(), "mysql.yml");
        if (!mysqlFile.exists()) {
            plugin.saveResource("mysql.yml", false);
        }
        mysqlConfig = YamlConfiguration.loadConfiguration(mysqlFile);
        enabled = mysqlConfig.getBoolean("mysql.enabled");
    }

    private void connect() {
        CompletableFuture.runAsync(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    return;
                }

                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    try {
                        Class.forName("com.mysql.jdbc.Driver");
                    } catch (ClassNotFoundException e2) {
                        enabled = false;
                        return;
                    }
                }

                String host = mysqlConfig.getString("mysql.host");
                int port = mysqlConfig.getInt("mysql.port");
                String database = mysqlConfig.getString("mysql.database");
                String username = mysqlConfig.getString("mysql.username");
                String password = mysqlConfig.getString("mysql.password");

                String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                        "?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&connectTimeout=60000&socketTimeout=60000&serverTimezone=UTC&useLegacyDatetimeCode=false";

                connection = DriverManager.getConnection(url, username, password);
                createTables();

            } catch (SQLException e) {
                enabled = false;
            } catch (Exception e) {
                enabled = false;
            }
        }, executor);
    }

    private void createTables() {
        if (!enabled || connection == null) return;

        try {
            String tablePrefix = mysqlConfig.getString("mysql.table-prefix");
            String createTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "messages (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "server_name VARCHAR(50) NOT NULL," +
                    "player_name VARCHAR(50) NOT NULL," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "message TEXT NOT NULL," +
                    "channel_type VARCHAR(20) NOT NULL," +
                    "timestamp BIGINT NOT NULL," +
                    "INDEX idx_timestamp (timestamp)," +
                    "INDEX idx_server (server_name)" +
                    ")";

            try (PreparedStatement stmt = connection.prepareStatement(createTable)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                enabled = false;
            }
        } catch (Exception e) {
            enabled = false;
        }
    }

    public void saveMessage(String playerName, String playerUuid, String message, String channelType) {
        if (!enabled || !isSyncEnabled()) return;

        CompletableFuture.runAsync(() -> {
            String tablePrefix = mysqlConfig.getString("mysql.table-prefix");
            String serverName = mysqlConfig.getString("sync.server-name");

            String query = "INSERT INTO " + tablePrefix + "messages " +
                    "(server_name, player_name, player_uuid, message, channel_type, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, serverName);
                stmt.setString(2, playerName);
                stmt.setString(3, playerUuid);
                stmt.setString(4, message);
                stmt.setString(5, channelType);
                stmt.setLong(6, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException e) {
            }
        }, executor);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSyncEnabled() {
        return enabled && mysqlConfig.getBoolean("sync.enabled");
    }

    public boolean shouldSyncChannel(String channelType) {
        return mysqlConfig.getStringList("sync.sync-channels").contains(channelType);
    }

    public String getServerName() {
        return mysqlConfig.getString("sync.server-name");
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
            }
        }
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void reconnect() {
        disconnect();
        if (enabled) {
            connect();
        }
    }

    public void reloadConfig() {
        loadConfig();
        if (enabled && (connection == null || !isConnectionValid())) {
            connect();
        } else if (!enabled) {
            disconnect();
        }
    }

    private boolean isConnectionValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    public void testConnection() {
        if (enabled) {
            CompletableFuture.runAsync(() -> {
                try {
                    String host = mysqlConfig.getString("mysql.host");
                    int port = mysqlConfig.getInt("mysql.port");
                    String database = mysqlConfig.getString("mysql.database");
                    String username = mysqlConfig.getString("mysql.username");
                    String password = mysqlConfig.getString("mysql.password");

                    String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                            "?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&connectTimeout=10000&socketTimeout=10000&serverTimezone=UTC";

                    Connection testConnection = DriverManager.getConnection(url, username, password);
                    testConnection.close();
                } catch (Exception e) {
                }
            }, executor);
        }
    }

    public void getNewMessages(long lastMessageId, java.util.function.Consumer<java.util.List<SyncMessage>> callback) {
        if (!enabled || !isSyncEnabled()) return;

        CompletableFuture.runAsync(() -> {
            String tablePrefix = mysqlConfig.getString("mysql.table-prefix");
            String currentServer = mysqlConfig.getString("sync.server-name");
            String query = "SELECT id, server_name, player_name, player_uuid, message, channel_type, timestamp " +
                    "FROM " + tablePrefix + "messages " +
                    "WHERE id > ? AND server_name != ? " +
                    "ORDER BY id ASC LIMIT 50";

            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setLong(1, lastMessageId);
                stmt.setString(2, currentServer);

                try (ResultSet rs = stmt.executeQuery()) {
                    java.util.List<SyncMessage> messages = new java.util.ArrayList<>();

                    while (rs.next()) {
                        SyncMessage message = new SyncMessage(
                                rs.getLong("id"),
                                rs.getString("server_name"),
                                rs.getString("player_name"),
                                rs.getString("player_uuid"),
                                rs.getString("message"),
                                rs.getString("channel_type"),
                                rs.getLong("timestamp")
                        );
                        messages.add(message);
                    }

                    if (!messages.isEmpty()) {
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> callback.accept(messages));
                    }
                }
            } catch (SQLException e) {
            }
        }, executor);
    }

    public long getLastMessageId() {
        if (!enabled || connection == null) return 0;

        try {
            String tablePrefix = mysqlConfig.getString("mysql.table-prefix");
            String query = "SELECT MAX(id) as max_id FROM " + tablePrefix + "messages";

            try (PreparedStatement stmt = connection.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long maxId = rs.getLong("max_id");
                    return maxId;
                }
            }
        } catch (SQLException e) {
        }
        return 0;
    }

    public static class SyncMessage {
        public final long id;
        public final String serverName;
        public final String playerName;
        public final String playerUuid;
        public final String message;
        public final String channelType;
        public final long timestamp;

        public SyncMessage(long id, String serverName, String playerName, String playerUuid, String message, String channelType, long timestamp) {
            this.id = id;
            this.serverName = serverName;
            this.playerName = playerName;
            this.playerUuid = playerUuid;
            this.message = message;
            this.channelType = channelType;
            this.timestamp = timestamp;
        }
    }
}