package com.wificraft.sentinel.modules.security;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;

public class ReportManager implements IReportManager {
    private final Map<String, Report> reports;
    private final JavaPlugin plugin;

    public ReportManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.reports = new HashMap<>();
        loadReports();
    }

    @Override
    public String listReports() {
        if (reports.isEmpty()) {
            return "No reports available";
        }

        StringBuilder sb = new StringBuilder("Available reports:\n");
        for (String id : reports.keySet()) {
            Report report = reports.get(id);
            sb.append(id).append(" - ").append(report.getPlayerName()).append(" - ").append(report.getReason()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String viewReport(String reportId) {
        Report report = reports.get(reportId);
        if (report == null) {
            return "Report not found";
        }

        return String.format("Report ID: %s\n" +
                           "Player: %s\n" +
                           "Reason: %s\n" +
                           "Timestamp: %s\n" +
                           "Evidence: %s",
                           reportId,
                           report.getPlayerName(),
                           report.getReason(),
                           report.getTimestamp(),
                           report.getEvidence());
    }

    private void loadReports() {
        FileConfiguration config = plugin.getConfig();
        for (String id : config.getConfigurationSection("reports").getKeys(false)) {
            String playerName = config.getString("reports." + id + ".player");
            String reason = config.getString("reports." + id + ".reason");
            long timestamp = config.getLong("reports." + id + ".timestamp");
            String evidence = config.getString("reports." + id + ".evidence");
            
            reports.put(id, new Report(playerName, reason, timestamp, evidence));
        }
    }

    private class Report {
        private final String playerName;
        private final String reason;
        private final long timestamp;
        private final String evidence;

        public Report(String playerName, String reason, long timestamp, String evidence) {
            this.playerName = playerName;
            this.reason = reason;
            this.timestamp = timestamp;
            this.evidence = evidence;
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getReason() {
            return reason;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getEvidence() {
            return evidence;
        }
    }
}
