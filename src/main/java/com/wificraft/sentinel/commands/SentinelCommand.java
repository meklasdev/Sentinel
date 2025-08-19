package com.wificraft.sentinel.commands;

import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.modules.InspectionModule;
import com.wificraft.sentinel.modules.LiveMonitor;
import com.wificraft.sentinel.modules.ModeratorRanking;
import com.wificraft.sentinel.modules.PerformanceModule;
import com.wificraft.sentinel.modules.logging.HistoryLogger;
import com.wificraft.sentinel.modules.config.NotificationConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SentinelCommand implements CommandExecutor {
    private final WiFiCraftSentinel plugin;
    private final PerformanceModule performanceModule;
    private final InspectionModule inspectionModule;
    private final LiveMonitor liveMonitor;
    private final ModeratorRanking moderatorRanking;
    private final HistoryLogger historyLogger;
    private final NotificationConfig notificationConfig;

    public SentinelCommand(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
        this.performanceModule = plugin.getPerformanceModule();
        this.inspectionModule = plugin.getInspectionModule();
        this.liveMonitor = plugin.getLiveMonitor();
        this.moderatorRanking = plugin.getModeratorRanking();
        this.historyLogger = plugin.getHistoryLogger();
        this.notificationConfig = plugin.getNotificationConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eDostępne komendy:");
            sender.sendMessage("§8• §eoptimize - optymalizacja systemu");
            sender.sendMessage("§8• §estatus - status systemu");
            sender.sendMessage("§8• §einspect - zarządzanie inspekcjami");
            sender.sendMessage("§8• §ehistory - historia gracza");
            sender.sendMessage("§8• §enotifications - zarządzanie powiadomieniami");
            sender.sendMessage("§8• §emod-rank - ranking moderatorów");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "optimize":
                return handleOptimizeCommand(sender);
            case "status":
                return handleStatusCommand(sender);
            case "inspect":
                return handleInspectCommand(sender, args);
            case "history":
                return handleHistoryCommand(sender, args);
            case "notifications":
                return handleNotificationsCommand(sender, args);
            case "mod-rank":
                return handleModeratorRankCommand(sender);
            default:
                sender.sendMessage("§cNieznana podkomenda!");
                return false;
        }
    }

    private boolean handleOptimizeCommand(CommandSender sender) {
        if (!sender.hasPermission("sentinel.optimize")) {
            sender.sendMessage("§cNie masz uprawnień do użycia tej komendy!");
            return false;
        }

        if (performanceModule != null) performanceModule.optimizeServer();
        if (historyLogger != null) historyLogger.cleanupOldLogs();
        if (inspectionModule != null) Bukkit.getScheduler().runTaskAsynchronously(plugin, inspectionModule::cleanupOldInspections);
        if (moderatorRanking != null) Bukkit.getScheduler().runTaskAsynchronously(plugin, moderatorRanking::refreshCache);
        if (liveMonitor != null) liveMonitor.refreshMonitoredPlayers();
        
        sender.sendMessage("§aOptymalizacja systemu zakończona pomyślnie!");
        return true;
    }

    private boolean handleStatusCommand(CommandSender sender) {
        if (!sender.hasPermission("sentinel.status")) {
            sender.sendMessage("§cNie masz uprawnień do użycia tej komendy!");
            return false;
        }

        sender.sendMessage("§eStatus systemu Sentinel:");
        sender.sendMessage(String.format("§8• §eWersja: §7%s", plugin.getDescription().getVersion()));
        sender.sendMessage(String.format("§8• §eUżycie RAM: §7%s", formatRamUsage()));
        sender.sendMessage(String.format("§8• §eUżycie CPU: §7%s", formatCpuUsage()));
        if (performanceModule != null) sender.sendMessage(String.format("§8• §eTPS: §7%.2f", performanceModule.getTps()));
        if (liveMonitor != null) sender.sendMessage(String.format("§8• §eAktywnych monitorów: §7%d", liveMonitor.getActiveMonitorCount()));
        if (inspectionModule != null) sender.sendMessage(String.format("§8• §eAktywnych inspekcji: §7%d", inspectionModule.getActiveInspectionCount()));
        if (moderatorRanking != null) sender.sendMessage(String.format("§8• §eModeratorów w rankingu: §7%d", moderatorRanking.getRankedModeratorCount()));
        
        return true;
    }

    private boolean handleInspectCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sentinel.inspect")) {
            sender.sendMessage("§cNie masz uprawnień do użycia tej komendy!");
            return false;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§eDostępne komendy inspekcji:");
            sender.sendMessage("§8• §estart <gracz> - rozpocznij inspekcję");
            sender.sendMessage("§8• §enotes <gracz> <notatka> - dodaj notatkę");
            sender.sendMessage("§8• §eend <gracz> [powód] - zakończ inspekcję");
            return true;
        }
        
        switch (args[1].toLowerCase()) {
            case "start": return handleInspectStart(sender, args);
            case "notes": return handleInspectNotes(sender, args);
            case "end": return handleInspectEnd(sender, args);
            default:
                sender.sendMessage("§cNieznana podkomenda inspekcji!");
                return false;
        }
    }

    private boolean handleHistoryCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sentinel.history")) {
            sender.sendMessage("§cNie masz uprawnień do użycia tej komendy!");
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage("§eDostępne komendy historii:");
            sender.sendMessage("§8• §eview <gracz> - wyświetl historię gracza");
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "view":
                return handleHistoryView(sender, args);
            default:
                sender.sendMessage("§cNieznana podkomenda historii! Użyj /sentinel history");
                return false;
        }
    }

    private boolean handleNotificationsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda jest dostępna tylko dla graczy!");
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage("§eDostępne komendy powiadomień:");
            sender.sendMessage("§8• §elist - wyświetl swoje powiadomienia");
            sender.sendMessage("§8• §eview <id> - wyświetl szczegóły powiadomienia");
            sender.sendMessage("§8• §eclear - wyczyść swoje powiadomienia");
            sender.sendMessage("§8• §esettings <sound/chat> <on/off> - zarządzaj ustawieniami");
            sender.sendMessage("§8• §ethreshold <typ> <wartość> - ustaw progi (admin)");
            sender.sendMessage("§8• §epattern <add/remove> <wzorzec> - zarządzaj wzorcami (admin)");
            sender.sendMessage("§8• §estatus - wyświetl status konfiguracji (admin)");
            return true;
        }

        Player player = (Player) sender;

        switch (args[1].toLowerCase()) {
            case "list": return handleNotificationList(player);
            case "view": return handleNotificationView(player, args);
            case "clear": return handleNotificationClear(player);
            case "settings": return handleNotificationSettings(player, args);
            case "threshold": return handleNotificationThreshold(sender, args);
            case "pattern":
                if (!sender.hasPermission("sentinel.notifications.admin")) {
                    sender.sendMessage("§cNie masz uprawnień do zarządzania wzorcami!");
                    return false;
                }
                return handleNotificationPattern(sender, args);
            case "status":
                if (!sender.hasPermission("sentinel.notifications.admin")) {
                    sender.sendMessage("§cNie masz uprawnień do wyświetlania statusu!");
                    return false;
                }
                return handleNotificationStatus(sender);
            default:
                sender.sendMessage("§cNieznana podkomenda powiadomień!");
                return false;
        }
    }
    
    private boolean handleNotificationList(Player player) {
        List<Map<String, Object>> notifications = notificationConfig.getNotifications(player.getUniqueId());
        if (notifications.isEmpty()) {
            player.sendMessage("§eNie masz żadnych nowych powiadomień.");
            return true;
        }
        player.sendMessage("§eTwoje powiadomienia:");
        notifications.forEach(n -> player.sendMessage(String.format("§8• §eID: %d - §7%s", n.get("id"), n.get("message"))));
        return true;
    }
    
    private boolean handleNotificationView(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUżycie: /sentinel notifications view <id>");
            return false;
        }
        try {
            int notificationId = Integer.parseInt(args[2]);
            Map<String, Object> notification = notificationConfig.getNotification(player.getUniqueId(), notificationId);
            if (notification == null) {
                player.sendMessage("§cNie znaleziono powiadomienia o podanym ID!");
                return false;
            }
            player.sendMessage("§eSzczegóły powiadomienia ID: " + notificationId);
            player.sendMessage(String.format("§8• §eWiadomość: §7%s", notification.get("message")));
            player.sendMessage(String.format("§8• §eData: §7%s", new Date((long) notification.get("timestamp"))));
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage("§cNieprawidłowy numer powiadomienia!");
            return false;
        }
    }
    
    private boolean handleNotificationClear(Player player) {
        notificationConfig.clearNotifications(player.getUniqueId());
        player.sendMessage("§aWyczyszczono wszystkie powiadomienia.");
        return true;
    }
    
    private boolean handleNotificationSettings(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§eUżycie: /sentinel notifications settings <sound|chat> <on|off>");
            return false;
        }
        String setting = args[2].toLowerCase();
        boolean value = args[3].equalsIgnoreCase("on");
        switch (setting) {
            case "sound":
                notificationConfig.setSoundEnabled(player.getUniqueId(), value);
                player.sendMessage("§aDźwięk powiadomień " + (value ? "§awłączony" : "§cwyłączony"));
                break;
            case "chat":
                notificationConfig.setChatEnabled(player.getUniqueId(), value);
                player.sendMessage("§aPowiadomienia na czacie " + (value ? "§awłączone" : "§cwyłączone"));
                break;
            default:
                player.sendMessage("§cNieznane ustawienie!");
                return false;
        }
        return true;
    }

    private boolean handleModeratorRankCommand(CommandSender sender) {
        if (!sender.hasPermission("sentinel.mod-rank")) {
            sender.sendMessage("§cNie masz uprawnień do użycia tej komendy!");
            return false;
        }
        if (moderatorRanking != null) {
            moderatorRanking.displayRanking(sender);
        } else {
            sender.sendMessage("§cModuł rankingu moderatorów nie jest dostępny.");
        }
        return true;
    }

    private boolean handleHistoryView(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTę komendę może użyć tylko gracz!");
            return false;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUżycie: /sentinel history view <gracz>");
            return false;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage("§cGracz " + args[2] + " nie jest online!");
            return false;
        }
        historyLogger.viewHistory((Player) sender, target);
        return true;
    }

    private boolean handleNotificationThreshold(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUżycie: /sentinel notifications threshold <typ> <wartość>");
            return false;
        }
        if (!sender.hasPermission("sentinel.notifications.admin")) {
            sender.sendMessage("§cNie masz uprawnień do zmiany progów!");
            return false;
        }
        String type = args[2];
        int value;
        try {
            value = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cWartość musi być liczbą!");
            return false;
        }
        notificationConfig.setThreshold(type, value);
        sender.sendMessage("§aPróg '" + type + "' został zaktualizowany na " + value + "!");
        return true;
    }

    private boolean handleNotificationPattern(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUżycie: /sentinel notifications pattern <add/remove> <wzorzec>");
            return false;
        }
        String action = args[2].toLowerCase();
        String pattern = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        if (action.equals("add")) {
            notificationConfig.addPattern(pattern);
            sender.sendMessage("§aWzorzec został dodany!");
        } else if (action.equals("remove")) {
            notificationConfig.removePattern(pattern);
            sender.sendMessage("§aWzorzec został usunięty!");
        } else {
            sender.sendMessage("§cNieznana akcja!");
            return false;
        }
        return true;
    }

    private boolean handleNotificationStatus(CommandSender sender) {
        sender.sendMessage("§eStatus powiadomień:");
        sender.sendMessage("§8• §eProgi:");
        notificationConfig.getThresholds().forEach((type, value) -> 
            sender.sendMessage(String.format("§8  • §7%s: §f%s", type, value.toString()))
        );

        sender.sendMessage("§8• §eWzorce:");
        List<String> patterns = notificationConfig.getPatterns();
        if (patterns.isEmpty()) {
            sender.sendMessage("§8  • §7Brak zdefiniowanych wzorców");
        } else {
            patterns.forEach(pattern -> 
                sender.sendMessage(String.format("§8  • §7\"%s\"", pattern))
            );
        }
        return true;
    }

    private boolean handleInspectStart(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUżycie: /sentinel inspect start <gracz>");
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda jest dostępna tylko dla graczy!");
            return false;
        }

        Player inspector = (Player) sender;
        Player target = Bukkit.getPlayer(args[2]);

        if (target == null) {
            sender.sendMessage("§cNie znaleziono gracza " + args[2]);
            return false;
        }

        if (inspectionModule != null) {
            inspectionModule.startInspection(inspector, target);
            sender.sendMessage("§aRozpoczęto inspekcję gracza " + target.getName());
        } else {
            sender.sendMessage("§cModuł inspekcji nie jest dostępny");
        }
        return true;
    }
    
    private boolean handleInspectNotes(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUżycie: /sentinel inspect notes <gracz> <notatka>");
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda jest dostępna tylko dla graczy!");
            return false;
        }

        Player inspector = (Player) sender;
        Player target = Bukkit.getPlayer(args[2]);

        if (target == null) {
            sender.sendMessage("§cNie znaleziono gracza " + args[2]);
            return false;
        }

        String note = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        
        if (inspectionModule != null) {
            inspectionModule.addInspectionNote(inspector, target, note);
            sender.sendMessage("§aDodano notatkę do inspekcji gracza " + target.getName());
        } else {
            sender.sendMessage("§cModuł inspekcji nie jest dostępny");
        }
        
        return true;
    }

    private boolean handleInspectEnd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUżycie: /sentinel inspect end <gracz> [powód]");
            return false;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage("§cGracz nie jest online!");
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTę komendę może użyć tylko gracz!");
            return false;
        }
        
        String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "Brak podanego powodu";
        inspectionModule.endInspection((Player) sender, target, reason);
        sender.sendMessage("§aZakończono inspekcję gracza " + target.getName());
        return true;
    }

    private String formatRamUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            long allocatedMemory = runtime.totalMemory() / (1024 * 1024);
            long freeMemory = runtime.freeMemory() / (1024 * 1024);
            return String.format("%d/%dMB (Wolne: %dMB)", allocatedMemory, maxMemory, freeMemory);
        } catch (Exception e) {
            return "Błąd odczytu użycia pamięci: " + e.getMessage();
        }
    }

    private String formatCpuUsage() {
        // Placeholder for CPU usage calculation
        // In a real implementation, you would use an external library or JMX to get CPU usage
        return "N/A";
    }
}
