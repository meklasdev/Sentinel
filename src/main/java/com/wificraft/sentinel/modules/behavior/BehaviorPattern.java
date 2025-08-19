package com.wificraft.sentinel.modules.behavior;

import org.bukkit.entity.Player;
import org.bukkit.Location;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public abstract class BehaviorPattern {
    protected final BehaviorAnalyzer analyzer;
    protected final String name;
    protected final int severity;
    protected final double threshold;
    
    protected Map<String, Object> patternData;

    public BehaviorPattern(BehaviorAnalyzer analyzer, String name, int severity, double threshold) {
        this.analyzer = analyzer;
        this.name = name;
        this.severity = severity;
        this.threshold = threshold;
        this.patternData = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public int getSeverity() {
        return severity;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setData(String key, Object value) {
        patternData.put(key, value);
    }

    public Object getData(String key) {
        return patternData.get(key);
    }

    public abstract boolean matches(Player player);
    public abstract String getDescription();
    public abstract String getAlertMessage();
}


