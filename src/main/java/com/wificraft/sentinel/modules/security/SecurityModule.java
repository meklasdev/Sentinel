package com.wificraft.sentinel.modules.security;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import com.wificraft.sentinel.WiFiCraftSentinel;
import java.util.logging.Level;
import com.wificraft.sentinel.alerts.AlertManager;
import com.wificraft.sentinel.logging.SecurityLogger;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SecurityModule {
    private final WiFiCraftSentinel plugin;
    private SecurityLogger logger;
    private final Set<String> whitelist;
    private final ClientSecurity clientSecurity;
    private final HardwareFingerprinter hardwareFingerprinter;
    private final Map<UUID, Long> lastSecurityScan;
    private final FileConfiguration config;

    public SecurityModule(WiFiCraftSentinel plugin, HardwareFingerprinter hardwareFingerprinter, ClientSecurity clientSecurity) {
        this.plugin = plugin;
        this.hardwareFingerprinter = hardwareFingerprinter;
        this.clientSecurity = clientSecurity;
        this.whitelist = new HashSet<>();
        this.lastSecurityScan = new HashMap<>();
        this.config = plugin.getConfig();
        
        // Initialize logger
        try {
            this.logger = SecurityLogger.getInstance(plugin);
        } catch (Exception e) {
            // Fallback to plugin's logger if SecurityLogger fails
            plugin.getLogger().severe("Failed to initialize SecurityLogger: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the entire module if logger fails
        }
        
        // Load whitelist from config
        loadWhitelist();
    }
    
    /**
     * Initializes the security module and all its components.
     * This method should be called during plugin startup.
     */
    public void initialize() {
        try {
            // No need to reinitialize final fields, they're set in constructor
            
            // Load configuration
            loadWhitelist();
            
            // Initialize webhook if configured
            if (config.getBoolean("security.webhook.enabled", false)) {
                configureWebhook();
            }
            
            // Log successful initialization
            if (logger != null) {
                logger.info("Security module initialized successfully");
            } else {
                plugin.getLogger().info("[Security] Security module initialized");
            }
        } catch (Exception e) {
            String errorMsg = "Failed to initialize security module: " + e.getMessage();
            if (logger != null) {
                logger.severe(errorMsg);
            } else {
                plugin.getLogger().severe("[Security] " + errorMsg);
            }
            e.printStackTrace();
        }
    }
    
    public ClientSecurity getClientSecurity() {
        return clientSecurity;
    }
    
    public HardwareFingerprinter getHardwareFingerprinter() {
        return hardwareFingerprinter;
    }
    
    public long getLastSecurityScan(UUID playerId) {
        return lastSecurityScan.getOrDefault(playerId, 0L);
    }
    
    public void updateLastSecurityScan(UUID playerId) {
        lastSecurityScan.put(playerId, System.currentTimeMillis());
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    private void log(Level level, String message) {
        if (logger != null) {
            logger.log(level, message);
        } else {
            plugin.getLogger().log(java.util.logging.Level.INFO, "[Security] " + message);
        }
    }

    private void loadWhitelist() {
        if (config.contains("security.whitelist")) {
            whitelist.addAll(config.getStringList("security.whitelist"));
        }
    }

    public boolean isWhitelisted(String player) {
        return whitelist.contains(player.toLowerCase());
    }

    public void addWhitelist(String player) {
        whitelist.add(player.toLowerCase());
        saveWhitelist();
    }

    public void removeWhitelist(String player) {
        whitelist.remove(player.toLowerCase());
        saveWhitelist();
    }
    
    public ClientSecurity getClientSecurity() {
        return clientSecurity;
    }

    private void saveWhitelist() {
        FileConfiguration config = plugin.getConfig();
        config.set("security.whitelist", new ArrayList<>(whitelist));
        plugin.saveConfig();
    }

    /* public void analyzePlayerSecurity(String player) {
        // Analyze IP
        ipAnalyzer.analyzeIP(player);
        
        // Analyze behavior
        behaviorAnalyzer.analyzePlayer(player);
    } */

    public void scanPlayer(String player) {
        // TODO: Implement security scanning
        logger.info("Scanning player: " + player);
    }

    public void verifyClient(String player) {
        // TODO: Implement client hash verification
        logger.info("Verifying client for: " + player);
    }

    public void configureWebhook() {
        // TODO: Implement webhook configuration
        logger.info("Configuring security webhook");
    }

    public void monitorChat(String player, String message) {
        // TODO: Implement chat monitoring
        logger.info("Monitoring chat for: " + player);
    }

    public void trackPlayer(String player) {
        // TODO: Implement player tracking
        logger.info("Tracking player: " + player);
    }

    public void fingerprintPlayer(String player) {
        // TODO: Implement fingerprinting
        logger.info("Fingerprinting player: " + player);
    }

    public void createReport(String player, String reason) {
        // TODO: Implement report creation
        logger.info("Creating report for: " + player);
    }

    /* public void createAlert(String title, String description, SeverityLevel severity) {
        alertManager.createAlert(title, description, severity);
    } */

    public void logSecurityEvent(String message) {
        this.logger.log(message);
    }
}
