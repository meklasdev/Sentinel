package com.wificraft.sentinel.modules.behavior;

import org.bukkit.entity.Player;

public class BlockInteractionPattern extends BehaviorPattern {
    private int maxBlocksPerSecond;

    public BlockInteractionPattern(BehaviorAnalyzer analyzer, int maxBlocksPerSecond) {
        super(analyzer, "BlockInteraction", 3, 1.0);
        this.maxBlocksPerSecond = maxBlocksPerSecond;
    }

    @Override
    public boolean matches(Player player) {
        int blocksPerSecond = analyzer.getBlockInteractionRate(player);
        return blocksPerSecond > maxBlocksPerSecond;
    }

    @Override
    public String getDescription() {
        return "Player is interacting with blocks at unusual rate";
    }

    @Override
    public String getAlertMessage() {
        return "Podejrzane interakcje z blokami: " + getData("player") + 
               "\nLiczba bloków: " + getData("blocks") + 
               "\nCzas: " + getData("time");
    }
}
