package com.wificraft.sentinel.modules.behavior;

import org.bukkit.entity.Player;

public class MovementPattern extends BehaviorPattern {
    private double maxSpeed;
    private double maxDistance;

    public MovementPattern(BehaviorAnalyzer analyzer, double maxSpeed, double maxDistance) {
        super(analyzer, "Movement", 2, 1.0);
        this.maxSpeed = maxSpeed;
        this.maxDistance = maxDistance;
    }

    @Override
    public boolean matches(Player player) {
        double speed = analyzer.getMovementSpeed(player);
        double distance = analyzer.getMovementDistance(player);
        
        return speed > maxSpeed || distance > maxDistance;
    }

    @Override
    public String getDescription() {
        return "Player is moving at unusual speed or distance";
    }

    @Override
    public String getAlertMessage() {
        return "Podejrzane ruchy gracza: " + getData("player") + 
               "\nPrędkość: " + getData("speed") + 
               "\nOdległość: " + getData("distance");
    }
}
