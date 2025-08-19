package com.wificraft.sentinel.modules;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class PlayerStats {
    private final UUID uuid;
    private int inspections;
    private int bans;
    private final List<ChatLogEntry> chatLog = new ArrayList<>();

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
        this.inspections = 0;
        this.bans = 0;
    }

    public void incrementInspections() {
        inspections++;
    }

    public void incrementBans() {
        bans++;
    }

    public void addChatEntry(String message, long timestamp) {
        chatLog.add(new ChatLogEntry(message, timestamp));
    }

    public List<ChatLogEntry> getChatLog() {
        return Collections.unmodifiableList(chatLog);
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getInspections() {
        return inspections;
    }

    public int getBans() {
        return bans;
    }

    public static class ChatLogEntry {
        private final String message;
        private final long timestamp;

        public ChatLogEntry(String message, long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
