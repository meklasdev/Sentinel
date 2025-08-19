package com.wificraft.sentinel.modules.reports.evidence;

import com.wificraft.sentinel.modules.reports.Evidence;
import java.util.UUID;

public class ChatEvidence extends Evidence {
    private final String message;
    private final String channel;
    private final long timestamp;

    public ChatEvidence(UUID reporterId, String message, String channel, String notes) {
        super(reporterId, notes, Evidence.EvidenceType.CHAT);
        this.message = message;
        this.channel = channel;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessage() {
        return message;
    }

    public String getChannel() {
        return channel;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
