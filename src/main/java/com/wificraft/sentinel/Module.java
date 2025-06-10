package com.wificraft.sentinel;

import org.bukkit.plugin.java.JavaPlugin;

public abstract class Module {
    protected final WiFiCraftSentinel plugin;

    public Module(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
    }

    public abstract void initialize();
    public abstract void disable();
}
