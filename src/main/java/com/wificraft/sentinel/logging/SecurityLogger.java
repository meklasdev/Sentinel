package com.wificraft.sentinel.logging;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

public class SecurityLogger {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final File logFile;
    private final Level logLevel;
    private static SecurityLogger instance;
    
    public SecurityLogger(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        
        // Get log file path from config
        String logFilePath = config.getString("logging.log-file", "sentinel.log");
        this.logFile = new File(plugin.getDataFolder(), logFilePath);
        
        // Get log level from config
        String levelString = config.getString("logging.level", "INFO").toUpperCase();
        this.logLevel = Level.parse(levelString);
        
        // Create log file if it doesn't exist
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().severe("Failed to create log file: " + e.getMessage());
            }
        }
    }
    
    public static SecurityLogger getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new SecurityLogger(plugin);
        }
        return instance;
    }
    
    public void log(String message) {
        log(Level.INFO, message);
    }
    
    public void log(Level level, String message) {
        if (level.intValue() >= logLevel.intValue()) {
            writeLog(level, message);
        }
    }
    
    public void log(Level level, String message, Throwable throwable) {
        if (level.intValue() >= logLevel.intValue()) {
            writeLog(level, message + "\n" + getStackTrace(throwable));
        }
    }
    
    private void writeLog(Level level, String message) {
        String timestamp = DATE_FORMATTER.format(LocalDateTime.now());
        String logEntry = String.format("[%s] %s: %s%n", timestamp, level.getName(), message);
        
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(logEntry);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to write to log file: " + e.getMessage());
        }
    }
    
    private String getStackTrace(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            builder.append("\tat ").append(element).append("\n");
        }
        return builder.toString();
    }
    
    public void info(String message) {
        log(Level.INFO, message);
    }
    
    public void warning(String message) {
        log(Level.WARNING, message);
    }
    
    public void severe(String message) {
        log(Level.SEVERE, message);
    }
    
    public void debug(String message) {
        log(Level.FINE, message);
    }
}
