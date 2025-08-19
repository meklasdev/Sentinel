package com.wificraft.sentinel.alerts;

public enum SeverityLevel {
    GREEN(1, "🟢"),
    YELLOW(2, "🟡"),
    ORANGE(3, "🟠"),
    RED(4, "🔴");

    private final int level;
    private final String emoji;

    SeverityLevel(int level, String emoji) {
        this.level = level;
        this.emoji = emoji;
    }

    public int getLevel() {
        return level;
    }

    public String getEmoji() {
        return emoji;
    }

    public static SeverityLevel fromLevel(int level) {
        for (SeverityLevel severity : values()) {
            if (severity.level == level) {
                return severity;
            }
        }
        return GREEN;
    }
}
