package com.wificraft.sentinel.modules.reports;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

// Wsparcie dla nowszych wersji Paper (1.19+)
class ModernChatInputListener implements Listener {
    private final JavaPlugin plugin;
    private final Consumer<String> callback;
    private final Player player;
    private boolean registered = true;
    
    public ModernChatInputListener(JavaPlugin plugin, Player player, Consumer<String> callback) {
        this.plugin = plugin;
        this.player = player;
        this.callback = callback;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Automatically unregister after 5 minutes to prevent memory leaks
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (registered) {
                unregister();
                player.sendMessage(Component.text("Czas na wprowadzenie notatki minął.", NamedTextColor.RED));
            }
        }, 20 * 60 * 5); // 5 minutes
    }
    
    @SuppressWarnings("deprecation")
    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        
        event.setCancelled(true);
        String message = event.getMessage();
        
        handleMessage(message);
    }
    
    private void handleMessage(String message) {
        if (!registered) return;
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!registered) return;
            
            if (message.equalsIgnoreCase("anuluj")) {
                player.sendMessage(Component.text("Anulowano.", NamedTextColor.RED));
                callback.accept(null);
            } else {
                callback.accept(message);
            }
            
            unregister();
        });
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
            unregister();
        }
    }
    
    private void unregister() {
        if (!registered) return;
        registered = false;
        
        try {
            // Unregister from all events
            HandlerList.unregisterAll(this);
        } catch (Exception e) {
            // Fallback to manual unregistration
            try {
                // Try to unregister from common events
                PlayerQuitEvent.getHandlerList().unregister(this);
                PlayerQuitEvent.getHandlerList().unregister(this);
                
                // Try to unregister from AsyncPlayerChatEvent using reflection
                try {
                    Class<?> eventClass = Class.forName("org.bukkit.event.player.AsyncPlayerChatEvent");
                    Object handlerList = eventClass.getMethod("getHandlerList").invoke(null);
                    handlerList.getClass().getMethod("unregister", Listener.class).invoke(handlerList, this);
                } catch (ClassNotFoundException ignored) {
                    // Event class not found, probably using newer version
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to unregister chat listener: " + ex.getMessage());
            }
        }
    }
}

// Główna klasa z zachowaniem wstecznej kompatybilności

public class ChatInputListener implements Listener {
    private final JavaPlugin plugin;
    private final boolean useModernChat;
    private final LegacyChatListener legacyListener;
    
    public ChatInputListener(JavaPlugin plugin) {
        this.plugin = plugin;
        // Sprawdź, czy używamy nowoczesnego systemu czatu (1.19+)
        this.useModernChat = checkModernChat();
        if (this.useModernChat) {
            this.legacyListener = null;
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        } else {
            this.legacyListener = new LegacyChatListener(plugin);
        }
    }
    
    private boolean checkModernChat() {
        boolean modernChat = false;
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            modernChat = true;
        } catch (ClassNotFoundException e) {
            // Używamy starszej wersji
        }
        return modernChat;
    }
    
    /**
     * Register a callback for when a player enters text in chat
     * @param player The player to listen for
     * @param onInput Callback that receives the input text (null if cancelled)
     */
    public void awaitChatInput(Player player, Consumer<String> onInput) {
        UUID playerId = player.getUniqueId();
        
        if (useModernChat) {
            // Use ModernChatInputListener for modern versions
            new ModernChatInputListener(plugin, player, onInput);
            player.sendMessage(Component.text("Wprowadź notatkę lub wpisz 'anuluj' aby anulować:", NamedTextColor.YELLOW));
        } else if (legacyListener != null) {
            legacyListener.addPendingInput(playerId, onInput);
            player.sendMessage(Component.text("Wprowadź notatkę lub wpisz 'anuluj' aby anulować:", NamedTextColor.YELLOW));
        } else {
            onInput.accept(null); // Fallback if initialization failed
        }
    }
    
    /**
     * Cancel any pending input for a player
     */
    public void cancelInput(Player player) {
        if (useModernChat) {
            // ModernChatInputListener handles its own cleanup
            // No action needed here as it's self-contained
        } else if (legacyListener != null) {
            legacyListener.cancelInput(player);
        }
    }
    
    /**
     * Clean up all listeners and resources
     * Call this when the plugin is disabled
     */
    public void cleanup() {
        if (legacyListener != null) {
            legacyListener.unregister();
        } else if (useModernChat) {
            // Unregister the main listener for modern chat
            try {
                HandlerList.unregisterAll(this);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to unregister chat listener: " + e.getMessage());
            }
        }
    }
    
    // Klasa wewnętrzna do obsługi starszych wersji (pre-1.19)
    private static class LegacyChatListener implements Listener {
        private final JavaPlugin plugin;
        private final Map<UUID, Consumer<String>> pendingInputs = new HashMap<>();
        private boolean registered = true;
        

        
        public LegacyChatListener(JavaPlugin plugin) {
            this.plugin = plugin;
            if (plugin != null) {
                plugin.getServer().getPluginManager().registerEvents(this, plugin);
                
                // Automatically unregister after 5 minutes to prevent memory leaks
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (registered) {
                        unregister();
                        for (UUID playerId : pendingInputs.keySet()) {
                            Player player = plugin.getServer().getPlayer(playerId);
                            if (player != null) {
                                player.sendMessage(Component.text("Czas na wprowadzenie notatki minął.", NamedTextColor.RED));
                            }
                        }
                        pendingInputs.clear();
                    }
                }, 20 * 60 * 5); // 5 minutes
            }
        }
        
        public void addPendingInput(UUID playerId, Consumer<String> callback) {
            if (!registered) {
                throw new IllegalStateException("Listener is already unregistered");
            }
            pendingInputs.put(playerId, callback);
        }
        
        @SuppressWarnings("deprecation")
        @EventHandler(ignoreCancelled = true)
        public void onPlayerChat(AsyncPlayerChatEvent event) {
            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();
            
            if (!pendingInputs.containsKey(playerId)) {
                return;
            }
            
            event.setCancelled(true);
            String message = event.getMessage();
            
            handleMessage(playerId, message);
        }
        
        private void handleMessage(UUID playerId, String message) {
            if (!registered) return;
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!registered) return;
                
                Consumer<String> callback = pendingInputs.remove(playerId);
                if (callback == null) return;
                
                if (message.equalsIgnoreCase("anuluj")) {
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null) {
                        player.sendMessage(Component.text("Anulowano.", NamedTextColor.RED));
                    }
                    callback.accept(null);
                } else {
                    callback.accept(message);
                }
                
                if (pendingInputs.isEmpty()) {
                    unregister();
                }
            });
        }
        
        /**
         * Cancel any pending input for a player
         * @param player The player whose input should be cancelled
         */
        public void cancelInput(Player player) {
            if (pendingInputs.remove(player.getUniqueId()) != null && pendingInputs.isEmpty()) {
                unregister();
            }
        }
        
        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            cancelInput(event.getPlayer());
        }
        
        private void unregister() {
            if (!registered) return;
            registered = false;
            
            try {
                // Unregister from all events
                HandlerList.unregisterAll(this);
            } catch (Exception e) {
                // Fallback to manual unregistration
                try {
                    // Try to unregister from common events
                    PlayerQuitEvent.getHandlerList().unregister(this);
                    
                    // Try to unregister from AsyncPlayerChatEvent using reflection
                    try {
                        Class<?> eventClass = Class.forName("org.bukkit.event.player.AsyncPlayerChatEvent");
                        Object handlerList = eventClass.getMethod("getHandlerList").invoke(null);
                        handlerList.getClass().getMethod("unregister", Listener.class).invoke(handlerList, this);
                    } catch (ClassNotFoundException ignored) {
                        // Event class not found, probably using newer version
                    }
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to unregister legacy chat listener: " + ex.getMessage());
                }
            }
        }
    }
}
