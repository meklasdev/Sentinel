package com.wificraft.sentinel.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SecurityLogger {
    private static final String LOG_FILE = "sentinel.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public enum LogLevel {
        INFO("ℹ️"),
        WARNING("⚠️"),
        SEVERE("❌"),
        DEBUG("⚙️");

        private final String emoji;

        LogLevel(String emoji) {
            this.emoji = emoji;
        }

        public String getEmoji() {
            return emoji;
        }
    }

    public static void log(String message, LogLevel level) {
        String timestamp = formatter.format(Instant.now());
        String logMessage = String.format("[%s] %s %s\n", timestamp, level.getEmoji(), message);
        
        System.out.print(logMessage);
        writeToFile(logMessage);
    }

    public static void info(String message) {
        log(message, LogLevel.INFO);
    }

    public static void warning(String message) {
        log(message, LogLevel.WARNING);
    }

    public static void severe(String message) {
        log(message, LogLevel.SEVERE);
    }

    public static void debug(String message) {
        log(message, LogLevel.DEBUG);
    }

    private static void writeToFile(String message) {
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            writer.write(message);
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

    public static void logException(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Exception: ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }
        severe(sb.toString());
    }
}
