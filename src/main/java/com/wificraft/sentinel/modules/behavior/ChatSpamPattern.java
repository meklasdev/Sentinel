package com.wificraft.sentinel.modules.behavior;

import org.bukkit.entity.Player;

public class ChatSpamPattern extends BehaviorPattern {
    private int maxMessagesPerMinute;
    private double maxCapsRatio;

    public ChatSpamPattern(BehaviorAnalyzer analyzer, int maxMessagesPerMinute, double maxCapsRatio) {
        super(analyzer, "ChatSpam", 2, 1.0);
        this.maxMessagesPerMinute = maxMessagesPerMinute;
        this.maxCapsRatio = maxCapsRatio;
    }

    @Override
    public boolean matches(Player player) {
        int messagesPerMinute = analyzer.getChatMessageRate(player);
        double capsRatio = analyzer.getChatCapsRatio(player);
        
        return messagesPerMinute > maxMessagesPerMinute || capsRatio > maxCapsRatio;
    }

    @Override
    public String getDescription() {
        return "Player is spamming chat or using excessive caps";
    }

    @Override
    public String getAlertMessage() {
        return "Podejrzany spam w czacie: " + getData("player") + 
               "\nWiadomości/min: " + getData("messages") + 
               "\nStosunek caps: " + getData("caps_ratio");
    }
}
