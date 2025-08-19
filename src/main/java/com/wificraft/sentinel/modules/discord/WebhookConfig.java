package com.wificraft.sentinel.modules.discord;

import java.awt.Color;

public class WebhookConfig {
    private final String webhookUrl;
    private final int severityLevel;
    private final boolean enabled;
    private final Color color;

    public WebhookConfig(String webhookUrl, int severityLevel, boolean enabled, Color color) {
        this.webhookUrl = webhookUrl;
        this.severityLevel = severityLevel;
        this.enabled = enabled;
        this.color = color;
    }

    public String getUrl() {
        return webhookUrl;
    }

    public int getSeverityLevel() {
        return severityLevel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Color getColor() {
        return color;
    }
}
