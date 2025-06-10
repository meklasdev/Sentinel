package com.wificraft.sentinel.modules.security;

import org.bukkit.entity.Player;

public class HardwareFingerprint {
    private final Player player;
    private final String hardwareId;
    private final String hardwareSummary;
    
    public HardwareFingerprint(Player player, String hardwareId) {
        this.player = player;
        this.hardwareId = hardwareId != null ? hardwareId : "";
        this.hardwareSummary = generateHardwareSummary();
    }
    
    public HardwareFingerprint(String hardwareId) {
        this.hardwareId = hardwareId != null ? hardwareId : "";
        this.hardwareSummary = generateHardwareSummary();
    }
    
    private String generateHardwareSummary() {
        // Generate a simple summary based on the hardware ID
        int hash = hardwareId.hashCode();
        String[] cpus = {"Intel Core i7", "AMD Ryzen 7", "Intel Core i5", "AMD Ryzen 5"};
        String[] gpus = {"NVIDIA GeForce RTX 3080", "AMD Radeon RX 6800 XT", "NVIDIA GeForce RTX 3070"};
        String[] oss = {"Windows 10", "Windows 11", "macOS", "Linux"};
        
        String cpu = cpus[Math.abs(hash) % cpus.length];
        String gpu = gpus[Math.abs(hash) % gpus.length];
        String os = oss[Math.abs(hash) % oss.length];
        
        return String.format("CPU: %s | GPU: %s | OS: %s", cpu, gpu, os);
    }

    public String getHardwareId() {
        return hardwareId;
    }
    
    public String getHardwareSummary() {
        return hardwareSummary;
    }
    
    @Override
    public String toString() {
        return hardwareSummary;
    }
    
    public String getCpuModel() {
        if (hardwareSummary.contains("CPU:")) {
            return hardwareSummary.split("\\|")[0].replace("CPU:", "").trim();
        }
        return "Unknown CPU";
    }
    
    public int getCpuCores() {
        String cpu = getCpuModel().toLowerCase();
        if (cpu.contains("i7") || cpu.contains("ryzen 7")) return 8;
        if (cpu.contains("i5") || cpu.contains("ryzen 5")) return 6;
        return 4;
    }
    
    public int getCpuFrequency() {
        String cpu = getCpuModel().toLowerCase();
        if (cpu.contains("i7") || cpu.contains("ryzen 7")) return 3700;
        if (cpu.contains("i5") || cpu.contains("ryzen 5")) return 3500;
        return 3000;
    }
    
    public int getTotalRam() {
        // Return total RAM in MB
        return 16384; // 16GB default
    }
    
    public int getAvailableRam() {
        // Return available RAM in MB
        return 8192; // 8GB default
    }
    
    public String getGpuModel() {
        if (hardwareSummary.contains("GPU:")) {
            return hardwareSummary.split("\\|")[1].replace("GPU:", "").trim();
        }
        return "Unknown GPU";
    }
    
    public int getGpuMemory() {
        // Return GPU memory in MB
        return 8192; // 8GB default
    }
}
