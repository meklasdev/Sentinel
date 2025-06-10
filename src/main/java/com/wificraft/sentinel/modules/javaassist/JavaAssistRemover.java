package com.wificraft.sentinel.modules.javaassist;

import com.wificraft.sentinel.SentinelPlugin;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Simple scanner that detects plugin jars in the plugins directory
 * that are not loaded by the server. It attempts to remove them to
 * prevent potential malicious injections.
 */
public class JavaAssistRemover {
    private final SentinelPlugin plugin;
    private final File pluginsDir;

    public JavaAssistRemover(SentinelPlugin plugin) {
        this.plugin = plugin;
        this.pluginsDir = new File("plugins");
    }

    public void initialize() {
        scanPlugins();
    }

    /**
     * Scan the plugins folder for jars that are not loaded and attempt to delete them.
     */
    public void scanPlugins() {
        File[] jars = pluginsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jars == null) {
            return;
        }
        for (File jar : jars) {
            String pluginName = getPluginName(jar);
            Plugin loaded = plugin.getServer().getPluginManager().getPlugin(pluginName);
            if (loaded == null) {
                plugin.getLogger().warning("Unauthorized plugin detected: " + jar.getName());
                if (jar.delete()) {
                    plugin.getLogger().info("Deleted unauthorized plugin: " + jar.getName());
                } else {
                    plugin.getLogger().warning("Could not delete: " + jar.getName());
                }
            }
        }
    }

    private String getPluginName(File jar) {
        try (JarFile jf = new JarFile(jar)) {
            JarEntry entry = jf.getJarEntry("plugin.yml");
            if (entry != null) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(jf.getInputStream(entry));
                String name = yaml.getString("name");
                if (name != null) {
                    return name;
                }
            }
        } catch (IOException ignored) {
        }
        // Fallback to jar name without extension
        String fileName = jar.getName();
        if (fileName.endsWith(".jar")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }
}
