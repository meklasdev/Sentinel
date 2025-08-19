package com.wificraft.sentinel.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.entity.Player;

public class PlayerBehaviorEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final BehaviorType behaviorType;
    private final double score;
    private final String reason;

    public PlayerBehaviorEvent(Player player, BehaviorType behaviorType, double score, String reason) {
        this.player = player;
        this.behaviorType = behaviorType;
        this.score = score;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public BehaviorType getBehaviorType() {
        return behaviorType;
    }

    public double getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }

    public enum BehaviorType {
        SUSPICIOUS_MOVEMENT,
        CHAT_SPAM,
        BLOCK_SPAM,
        BOT_BEHAVIOR,
        WALL_CLIMB,
        SPEED_HACK,
        FLY_HACK,
        REACH_HACK,
        CUSTOM
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
