package com.wificraft.sentinel.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.wificraft.sentinel.SentinelPlugin;
import com.wificraft.sentinel.gui.GUIManager;

import com.wificraft.sentinel.SentinelPlugin;
import com.wificraft.sentinel.gui.GUIManager;

public class OpenGUICmd implements CommandExecutor {
    private final SentinelPlugin plugin;
    private final GUIManager guiManager;

    public OpenGUICmd(SentinelPlugin plugin) {
        this.plugin = plugin;
        this.guiManager = new GUIManager(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda może być użyta tylko przez graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sentinel.gui.open")) {
            player.sendMessage("§cNie masz uprawnień do użycia tej komendy!");
            return true;
        }

        if (args.length == 0) {
            // Open default GUI (security dashboard)
            guiManager.openGUI(player, "security_dashboard");
            return true;
        }

        String guiType = args[0].toLowerCase();
        
        switch (guiType) {
            case "player":
            case "playerinfo":
                if (args.length < 2) {
                    player.sendMessage("§cUżycie: /gui player <gracz>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage("§cGracz nie jest online!");
                    return true;
                }
                guiManager.openGUI(player, "player_info");
                return true;

            case "security":
            case "dashboard":
                guiManager.openGUI(player, "security_dashboard");
                return true;

            default:
                player.sendMessage("§cNieznany typ GUI: " + guiType);
                player.sendMessage("§7Dostępne typy: player, security");
                return true;
        }
    }
}
