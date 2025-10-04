package com.ryzz3nn.woidzquests.database;

import com.ryzz3nn.woidzquests.WoidZQuests;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@Getter
public class DatabaseManager {
    
    private final WoidZQuests plugin;
    private HikariDataSource dataSource;
    
    public DatabaseManager(WoidZQuests plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        try {
            setupDatabase();
            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }
    
    private void setupDatabase() {
        String databasePath = plugin.getConfigManager().getDatabasePath();
        File databaseFile = new File(databasePath);
        
        // Create directories if they don't exist
        if (!databaseFile.getParentFile().exists()) {
            databaseFile.getParentFile().mkdirs();
        }
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + databasePath);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(plugin.getConfigManager().getInt("database.connection-pool-size", 10));
        config.setConnectionTimeout(plugin.getConfigManager().getInt("database.connection-timeout", 30000));
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        // SQLite specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("cache_size", "10000");
        config.addDataSourceProperty("temp_store", "MEMORY");
        
        dataSource = new HikariDataSource(config);
        
        plugin.getLogger().info("Database connection pool initialized");
    }
    
    private void createTables() throws SQLException {
        try (Connection connection = getConnection()) {
            // Player data table
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid TEXT PRIMARY KEY,
                    username TEXT NOT NULL,
                    quest_points INTEGER DEFAULT 0,
                    daily_rerolls INTEGER DEFAULT 0,
                    weekly_rerolls INTEGER DEFAULT 0,
                    last_daily_reset TEXT,
                    last_weekly_reset TEXT,
                    total_quests_completed INTEGER DEFAULT 0,
                    join_date TEXT NOT NULL,
                    last_seen TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Player quests table
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS player_quests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    quest_id TEXT NOT NULL,
                    quest_type TEXT NOT NULL,
                    quest_data TEXT NOT NULL,
                    progress INTEGER DEFAULT 0,
                    target INTEGER NOT NULL,
                    status TEXT DEFAULT 'ACTIVE',
                    is_tracked BOOLEAN DEFAULT FALSE,
                    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    completed_at TIMESTAMP NULL,
                    expires_at TIMESTAMP NULL,
                    FOREIGN KEY (player_uuid) REFERENCES player_data(uuid) ON DELETE CASCADE
                )
            """);
            
            // Quest definitions table
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS quest_definitions (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    category TEXT NOT NULL,
                    type TEXT NOT NULL,
                    difficulty TEXT NOT NULL,
                    target_min INTEGER NOT NULL,
                    target_max INTEGER NOT NULL,
                    requirements TEXT,
                    rewards TEXT NOT NULL,
                    cooldown INTEGER DEFAULT 0,
                    repeatable BOOLEAN DEFAULT TRUE,
                    job_specific TEXT NULL,
                    world_specific TEXT NULL,
                    enabled BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Player statistics table
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS player_statistics (
                    player_uuid TEXT NOT NULL,
                    stat_type TEXT NOT NULL,
                    stat_value INTEGER DEFAULT 0,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (player_uuid, stat_type),
                    FOREIGN KEY (player_uuid) REFERENCES player_data(uuid) ON DELETE CASCADE
                )
            """);
            
            // Placed blocks tracking (anti-cheese)
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS placed_blocks (
                    world TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    material TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    placed_at INTEGER NOT NULL,
                    PRIMARY KEY (world, x, y, z)
                )
            """);
            
            // Create index for placed blocks cleanup
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_placed_blocks_time ON placed_blocks(placed_at)");
            
            // Global statistics table
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS global_statistics (
                    stat_type TEXT PRIMARY KEY,
                    stat_value INTEGER DEFAULT 0,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Global quests table
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS global_quests (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    type TEXT NOT NULL,
                    target TEXT NOT NULL,
                    target_amount INTEGER NOT NULL,
                    current_progress INTEGER DEFAULT 0,
                    completed BOOLEAN DEFAULT FALSE,
                    display_material TEXT NOT NULL,
                    reward_data TEXT NOT NULL,
                    player_contributions TEXT DEFAULT '{}',
                    player_claimed TEXT DEFAULT '{}',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Daily quests table
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS daily_quests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    quest_id TEXT NOT NULL,
                    quest_name TEXT NOT NULL,
                    quest_description TEXT NOT NULL,
                    quest_type TEXT NOT NULL,
                    display_material TEXT NOT NULL,
                    target TEXT NOT NULL,
                    target_amount INTEGER NOT NULL,
                    current_progress INTEGER DEFAULT 0,
                    completed BOOLEAN DEFAULT FALSE,
                    claimed BOOLEAN DEFAULT FALSE,
                    requirements TEXT DEFAULT '{}',
                    reward_data TEXT NOT NULL,
                    created_date INTEGER NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (player_uuid) REFERENCES player_data(uuid) ON DELETE CASCADE
                )
            """);
            
            // Weekly quests table
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS weekly_quests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    quest_id TEXT NOT NULL,
                    quest_name TEXT NOT NULL,
                    quest_description TEXT NOT NULL,
                    quest_type TEXT NOT NULL,
                    display_material TEXT NOT NULL,
                    target TEXT NOT NULL,
                    target_amount INTEGER NOT NULL,
                    current_progress INTEGER DEFAULT 0,
                    completed BOOLEAN DEFAULT FALSE,
                    claimed BOOLEAN DEFAULT FALSE,
                    reward_data TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (player_uuid) REFERENCES player_data(uuid) ON DELETE CASCADE
                )
            """);
            
            // Create indexes for better performance
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_player_quests_uuid ON player_quests(player_uuid)");
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_player_quests_status ON player_quests(status)");
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_player_quests_type ON player_quests(quest_type)");
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_player_statistics_uuid ON player_statistics(player_uuid)");
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_placed_blocks_player ON placed_blocks(player_uuid)");
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_placed_blocks_material ON placed_blocks(material)");
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_global_quests_type ON global_quests(type)");
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_global_quests_completed ON global_quests(completed)");
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_daily_quests_player ON daily_quests(player_uuid)");
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_daily_quests_date ON daily_quests(created_date)");
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_weekly_quests_player ON weekly_quests(player_uuid)");
            
            plugin.getLogger().info("Database tables created successfully");
        }
    }
    
    private void executeUpdate(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public CompletableFuture<Void> executeAsync(DatabaseOperation operation) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                operation.execute(connection);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database operation failed", e);
            }
        });
    }
    
    public <T> CompletableFuture<T> queryAsync(DatabaseQuery<T> query) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                return query.execute(connection);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database query failed", e);
                return null;
            }
        });
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed");
        }
    }
    
    @FunctionalInterface
    public interface DatabaseOperation {
        void execute(Connection connection) throws SQLException;
    }
    
    @FunctionalInterface
    public interface DatabaseQuery<T> {
        T execute(Connection connection) throws SQLException;
    }
}
