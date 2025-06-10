package com.wificraft.sentinel;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.UUID;

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

import com.wificraft.sentinel.modules.ip.IpAnalyzer;
import com.wificraft.sentinel.modules.logging.HistoryLogger;
import com.wificraft.sentinel.modules.config.NotificationConfig;

public class WiFiCraftSentinel extends JavaPlugin {
    // private SecurityModule securityModule;
    // private PerformanceModule performanceModule;
    private AlertManager alertManager;
    private InspectionModule inspectionModule;
    // private LiveMonitor liveMonitor;
    private ModeratorRanking moderatorRanking;
    private ReportManager reportManager;
    private ReportGUI reportGUI;
    private HistoryLogger historyLogger;
    private NotificationConfig notificationConfig;
    private AntiCheatIntegration antiCheat;
    private IpAnalyzer ipAnalyzer;

    @Override
    public void onEnable() {
        // Load configuration first
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        
        // Initialize core components
        initializeSecurity();
        initializeModules();
        
        // Initialize notification config
        this.notificationConfig = new NotificationConfig(getDataFolder());
        
        // Initialize HistoryLogger with log retention days from config.yml
        int logRetentionDays = getConfig().getInt("logging.retention-days", 7); // Default to 7 days if not set
        this.historyLogger = new HistoryLogger(getDataFolder(), this.notificationConfig, logRetentionDays);
        
        // Initialize anti-cheat integration
        this.antiCheat = new AntiCheatIntegration(this);
        antiCheat.registerGrimIntegration(this);

        // Initialize IP analyzer
        this.ipAnalyzer = new IpAnalyzer(this);
        ipAnalyzer.start();

        getLogger().info("WiFiCraft Sentinel włączony!");
    }

    /* public AlertManager getAlertManager() {
        return alertManager;
    } */

    private void initializeModules() {
        // securityModule = new SecurityModule(this);
        // performanceModule = new PerformanceModule(this);

        moderatorRanking = new ModeratorRanking(this);
        inspectionModule = new InspectionModule(this, moderatorRanking);
        // liveMonitor = new LiveMonitor(this);

        // Initialize modules
        // securityModule.initialize();
        // performanceModule.initialize();

        inspectionModule.initialize();
        // liveMonitor.initialize();
        moderatorRanking.initialize();
        
        // Initialize report system
        reportManager = new ReportManager(this);
        reportGUI = new ReportGUI(this, reportManager);
        getServer().getPluginManager().registerEvents(reportGUI, this);
        getCommand("report").setExecutor(new ReportCommand(reportManager, reportGUI));

        // Register commands
        getCommand("sentinel").setExecutor(new SentinelCommand(this));
        getCommand("sprawdz").setExecutor(new SprawdzCommand(this));

        getCommand("rankmod").setExecutor(new RankmodCommand(this));

        getLogger().info("WiFiCraft Sentinel włączony!");
    }

    @Override
    public void onDisable() {
        // Disable modules
        // securityModule.disable();
        if (performanceModule != null) {
            performanceModule.disable();
        }

        if (inspectionModule != null) {
            inspectionModule.disable();
        }
        
        if (liveMonitor != null) {
            liveMonitor.disable();
        }
        
        if (moderatorRanking != null) {
            moderatorRanking.disable();
        }
        
        // Clean up report system
        if (reportGUI != null) {
            reportGUI.cleanup();
        }
        if (reportManager != null) {
            reportManager.saveReports();
        }

        // Clean up anti-cheat integration
        if (antiCheat != null) {
            antiCheat.unregisterGrimIntegration();
        }

        // Clean up IP analyzer
        if (ipAnalyzer != null) {
            ipAnalyzer.shutdown();
        }

        getLogger().info("WiFiCraft Sentinel wyłączone!");
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
            // Initialize security module (it will initialize its own dependencies)
            this.securityModule = new SecurityModule(this);
            this.hardwareFingerprinter = securityModule.getHardwareFingerprinter();
            this.clientSecurity = securityModule.getClientSecurity();
            
            getLogger().info("Zainicjowano moduł bezpieczeństwa");
        } catch (Exception e) {
            getLogger().severe("Błąd podczas inicjalizacji modułu bezpieczeństwa: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public SecurityModule getSecurityModule() {
        return securityModule;
    }
    
    public HardwareFingerprinter getHardwareFingerprinter() {
        return hardwareFingerprinter;
    }
    
    public ClientSecurity getClientSecurity() {
        return clientSecurity;
    }

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
    public AlertManager getAlertManager() {
        // Lazy initialization of AlertManager if not already created
        if (alertManager == null) {
            this.alertManager = new AlertManager(this);
        }
        return alertManager;
    }

    public NotificationConfig getNotificationConfig() {
        return this.notificationConfig;
    }
}
