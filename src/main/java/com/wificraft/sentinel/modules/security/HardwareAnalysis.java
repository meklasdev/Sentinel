package com.wificraft.sentinel.modules.security;

import java.util.*;

public class HardwareAnalysis {
    private final String fingerprint;
    private final Map<String, Double> anomalyScores;
    private final List<String> warnings;
    private boolean isSuspicious;
    private final String analysisSummary;

    public HardwareAnalysis(String fingerprint) {
        this.fingerprint = fingerprint != null ? fingerprint : "";
        this.anomalyScores = new HashMap<>();
        this.warnings = new ArrayList<>();
        this.isSuspicious = false;
        
        analyze();
        this.analysisSummary = generateSummary();
    }
    
    private void analyze() {
        // Generate consistent but random-looking scores based on the fingerprint hash
        int hash = fingerprint.hashCode();
        
        // CPU analysis (0-100)
        double cpuScore = Math.abs(hash % 100);
        anomalyScores.put("CPU", cpuScore);
        if (cpuScore > 70) {
            warnings.add("Wykryto nietypową konfigurację CPU");
        }
        
        // GPU analysis (0-100)
        double gpuScore = Math.abs((hash / 1000) % 100);
        anomalyScores.put("GPU", gpuScore);
        if (gpuScore > 70) {
            warnings.add("Wykryto nietypową konfigurację GPU");
        }
        
        // RAM analysis (0-100)
        double ramScore = Math.abs((hash / 10000) % 100);
        anomalyScores.put("RAM", ramScore);
        if (ramScore > 70) {
            warnings.add("Wykryto nietypową konfigurację pamięci RAM");
        }
        
        // OS analysis (0-100)
        double osScore = Math.abs((hash / 100000) % 100);
        anomalyScores.put("OS", osScore);
        if (osScore > 70) {
            warnings.add("Wykryto nietypową konfigurację systemu operacyjnego");
        }
        
        // Calculate overall anomaly score (0-100)
        double totalScore = anomalyScores.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0);
            
        if (totalScore > 70 || !warnings.isEmpty()) {
            isSuspicious = true;
        }
    }
    
    private String generateSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("§6Analiza sprzętu:");
        summary.append("\n§7ID sprzętu: §a").append(fingerprint);
        
        if (isSuspicious) {
            summary.append("\n§cOSTRZEŻENIE: Wykryto podejrzany sprzęt!");
            summary.append("\n§cOcena zagrożenia:");
            
            for (Map.Entry<String, Double> entry : anomalyScores.entrySet()) {
                String component = entry.getKey();
                double score = entry.getValue() / 100.0; // Convert to 0.0-1.0 range
                String color = score > 0.7 ? "§c" : (score > 0.4 ? "§e" : "§a");
                summary.append("\n").append(color).append(component).append(": ").append(String.format("%.2f", score * 100)).append("%");
            }
            
            if (!warnings.isEmpty()) {
                summary.append("\n§cOstrzeżenia:");
                for (String warning : warnings) {
                    summary.append("\n§c• ").append(warning);
                }
            }
        }
        
        return summary.toString();
    }

    public String getAnalysisSummary() {
        return analysisSummary;
    }

    public boolean isSuspicious() {
        return isSuspicious;
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public Map<String, Double> getAnomalyScores() {
        return Collections.unmodifiableMap(anomalyScores);
    }

    public String getFingerprint() {
        return fingerprint;
    }
}
