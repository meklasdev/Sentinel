package com.wificraft.sentinel;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;

import com.wificraft.sentinel.modules.InspectionModule;
import com.wificraft.sentinel.modules.ModeratorRanking;
import com.wificraft.sentinel.commands.SentinelCommand;
import com.wificraft.sentinel.commands.SprawdzCommand;
import com.wificraft.sentinel.commands.RankmodCommand;
import com.wificraft.sentinel.modules.reports.ReportManager;
import com.wificraft.sentinel.modules.reports.ReportGUI;
import com.wificraft.sentinel.modules.reports.ReportCommand;
import com.wificraft.sentinel.alerts.AlertManager;
import com.wificraft.sentinel.modules.anticheat.AntiCheatIntegration;
import com.wificraft.sentinel.modules.anticheat.grim.GrimIntegration;
// JavaAssistRemover temporarily disabled
import com.wificraft.sentinel.modules.ip.IpAnalyzer;
import com.wificraft.sentinel.modules.logging.HistoryLogger;
import com.wificraft.sentinel.modules.config.NotificationConfig;
import com.wificraft.sentinel.modules.PerformanceModule;
import com.wificraft.sentinel.modules.LiveMonitor;
import com.wificraft.sentinel.modules.security.SecurityModule;
import com.wificraft.sentinel.modules.security.HardwareFingerprinter;
import com.wificraft.sentinel.modules.security.ClientSecurity;

public class WiFiCraftSentinel extends JavaPlugin {
    private SecurityModule securityModule;
    private PerformanceModule performanceModule;
    private AlertManager alertManager;
    private InspectionModule inspectionModule;
    private LiveMonitor liveMonitor;
    private ModeratorRanking moderatorRanking;
    private ReportManager reportManager;
    private ReportGUI reportGUI;
    private HistoryLogger historyLogger;
    private NotificationConfig notificationConfig;
    private AntiCheatIntegration antiCheat;
    private IpAnalyzer ipAnalyzer;
    // JavaAssistRemover temporarily disabled

    @Override
    public void onEnable() {
        try {
            // Load configuration first
            saveDefaultConfig();
            getConfig().options().copyDefaults(true);
            saveConfig();
            
            // Initialize core components
            initializeSecurity();
            
            // Initialize other modules
            this.alertManager = new AlertManager();
            this.inspectionModule = new InspectionModule(this);
            this.moderatorRanking = new ModeratorRanking(this);
            this.reportManager = new ReportManager(this);
            this.reportGUI = new ReportGUI(this);
            this.historyLogger = new HistoryLogger(this);
            this.notificationConfig = new NotificationConfig(this);
            this.antiCheat = new GrimIntegration(this);
            this.ipAnalyzer = new IpAnalyzer(this);
            // JavaAssistRemover initialization disabled
            
            // Register commands
            getCommand("sprawdz").setExecutor(new SprawdzCommand(this));
            getCommand("rankmod").setExecutor(new RankmodCommand(this));
            getCommand("sentinel").setExecutor(new SentinelCommand(this));
            getCommand("report").setExecutor(new ReportCommand(this));
            
            // Register events
            getServer().getPluginManager().registerEvents(reportGUI, this);
            
            // Initialize performance module if enabled in config
            if (getConfig().getBoolean("performance.enabled", true)) {
                this.performanceModule = new PerformanceModule(this);
                performanceModule.enable();
            }
            
            // Initialize live monitoring if enabled
            if (getConfig().getBoolean("monitoring.enabled", true)) {
                this.liveMonitor = new LiveMonitor(this);
                liveMonitor.initialize();
            }
            
            // Initialize AlertManager if not already initialized
            if (this.alertManager == null) {
                this.alertManager = new AlertManager(this);
                getLogger().info("AlertManager initialized successfully");
            }
            
            getLogger().info("WiFiCraft Sentinel has been enabled!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error enabling WiFiCraft Sentinel", e);
            getServer().getPluginManager().disablePlugin(this);
        }
        initializeModules();
        
        // Initialize notification config
        this.notificationConfig = new NotificationConfig(getDataFolder());
        
        // Initialize HistoryLogger with log retention days from config.yml
        int logRetentionDays = getConfig().getInt("logging.retention-days", 7); // Default to 7 days if not set
        this.historyLogger = new HistoryLogger(getDataFolder(), this.notificationConfig, logRetentionDays);
        
        // Initialize anti-cheat integration
        this.antiCheat = new AntiCheatIntegration(this);
        
        // Register GrimAC if available
        if (getServer().getPluginManager().isPluginEnabled("GrimAC")) {
            antiCheat.registerGrimIntegration();
            getLogger().info("GrimAC integration enabled");
        } else {
            getLogger().warning("GrimAC not found, some anti-cheat features will be disabled");
        }

        // Initialize IP analyzer
        this.ipAnalyzer = new IpAnalyzer(this);
        ipAnalyzer.start();

        getLogger().info("WiFiCraft Sentinel włączony!");
    }

    /* public AlertManager getAlertManager() {
        return alertManager;
    } */

    private void initializeModules() {
        try {
            // Initialize AlertManager first
            this.alertManager = new AlertManager();
            
            // Initialize other modules
            this.moderatorRanking = new ModeratorRanking(this);
            this.inspectionModule = new InspectionModule(this, moderatorRanking);
            
            // Initialize modules with null checks
            if (inspectionModule != null) {
                // Initialize any necessary data for inspection module
                getLogger().info("InspectionModule initialized");
            }
            
            if (moderatorRanking != null) {
                // Initialize any necessary data for moderator ranking
                getLogger().info("ModeratorRanking initialized");
            }
            
            // Initialize report system
            this.reportManager = new ReportManager(this);
            this.reportGUI = new ReportGUI(this, reportManager);
            getServer().getPluginManager().registerEvents(reportGUI, this);
            
            // Register commands with null checks
            if (getCommand("report") != null) {
                getCommand("report").setExecutor(new ReportCommand(reportManager, reportGUI));
            }
            
            if (getCommand("sentinel") != null) {
                getCommand("sentinel").setExecutor(new SentinelCommand(this));
            }
            
            if (getCommand("sprawdz") != null) {
                getCommand("sprawdz").setExecutor(new SprawdzCommand(this));
            }
            
            if (getCommand("rankmod") != null) {
                getCommand("rankmod").setExecutor(new RankmodCommand(this));
            }
            
            getLogger().info("WiFiCraft Sentinel włączony!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Błąd podczas inicjalizacji modułów", e);
        }
    }

    @Override
    public void onDisable() {
        try {
            // Clean up AlertManager resources
            if (alertManager != null) {
                try {
                    alertManager.cleanup();
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Błąd podczas czyszczenia AlertManager", e);
                }
                alertManager = null;
            }
            
            // Save data first
            if (reportManager != null) {
                try {
                    reportManager.saveReports();
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Błąd podczas zapisywania raportów", e);
                }
            }
            
            // Clean up performance module
            if (performanceModule != null) {
                try {
                    performanceModule.disable();
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Błąd podczas wyłączania modułu wydajności", e);
                }
                performanceModule = null;
            }
            
            // Clean up live monitor
            if (liveMonitor != null) {
                try {
                    liveMonitor.disable();
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Błąd podczas wyłączania monitora na żywo", e);
                }
                liveMonitor = null;
            }
            
            // Clean up security module
            if (securityModule != null) {
                // No shutdown method available, just log a message
                getLogger().info("Security module shutdown not implemented");
                securityModule = null;
            }
            
            // Clean up other resources
            alertManager = null;
            inspectionModule = null;
            moderatorRanking = null;
            reportManager = null;
            reportGUI = null;
            historyLogger = null;
            notificationConfig = null;
            antiCheat = null;
            ipAnalyzer = null;
            
            getLogger().info("WiFiCraft Sentinel has been disabled!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin shutdown", e);
        }
    }

    // Getters for modules
    /* public SecurityModule getSecurityModule() {
        return securityModule;
    } */

    /* public PerformanceModule getPerformanceModule() {
        return performanceModule;
    } */

    public InspectionModule getInspectionModule() {
        return inspectionModule;
    }
    
    public LiveMonitor getLiveMonitor() {
        return liveMonitor;
    }
    
    public PerformanceModule getPerformanceModule() {
        return null; // TODO: Implement this method properly
    }

    /* public LiveMonitor getLiveMonitor() {
        return liveMonitor;
    } */

    public ModeratorRanking getModeratorRanking() {
        return moderatorRanking;
    }
    
    public ReportManager getReportManager() {
        return reportManager;
    }
    
    public ReportGUI getReportGUI() {
        return reportGUI;
    }

    public HistoryLogger getHistoryLogger() {
        return this.historyLogger;
    }
    
    private SecurityModule securityModule;
    private HardwareFingerprinter hardwareFingerprinter;
    private ClientSecurity clientSecurity;
    
    private void initializeSecurity() {
        try {
            // Initialize hardware fingerprinter
            HardwareFingerprinter hardwareFingerprinter = new HardwareFingerprinter();
            
            // Initialize client security with hardware fingerprinter
            ClientSecurity clientSecurity = new ClientSecurity(hardwareFingerprinter);
            
            // Initialize security module
            this.securityModule = new SecurityModule(this, hardwareFingerprinter, clientSecurity);
            
            // Initialize security module
            securityModule.initialize();
            
            getLogger().info("Security module initialized successfully");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize security module", e);
            throw new RuntimeException("Failed to initialize security module", e);
        }
    }
    
    public SecurityModule getSecurityModule() {
        return securityModule;
    }
    
    public HardwareFingerprinter getHardwareFingerprinter() {
        return securityModule != null ? securityModule.getHardwareFingerprinter() : null;
    }
    
    public ClientSecurity getClientSecurity() {
        return securityModule != null ? securityModule.getClientSecurity() : null;
    }
    
    public LiveMonitor getLiveMonitor() {
        return liveMonitor;
    }
    
    public PerformanceModule getPerformanceModule() {
        return performanceModule;
    }
    
    // Removed duplicate getHardwareFingerprinter method
    // getClientSecurity() is already implemented above

    public AntiCheatIntegration getAntiCheatIntegration() {
        return antiCheat;
    }

    public IpAnalyzer getIpAnalyzer() {
        return ipAnalyzer;
    }
    
    /**
     * Gets the AlertManager instance
     * @return The AlertManager instance
     */
    /**
     * Gets the AlertManager instance
     * @return The AlertManager instance
     */
    public AlertManager getAlertManager() {
        if (alertManager == null) {
            this.alertManager = new AlertManager(this);
            getLogger().info("Lazy initialization of AlertManager");
        }
        return alertManager;
    }

    public NotificationConfig getNotificationConfig() {
        return this.notificationConfig;
    }
}
