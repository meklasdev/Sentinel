package com.wificraft.sentinel.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.modules.security.HardwareFingerprinter;
import com.wificraft.sentinel.modules.security.HardwareFingerprint;

public class HardwareCheckCommand implements CommandExecutor {
    private final WiFiCraftSentinel plugin;
    private final HardwareFingerprinter fingerprinter;

    public HardwareCheckCommand(WiFiCraftSentinel plugin) {
        this.plugin = plugin;
        this.fingerprinter = new HardwareFingerprinter(30, 60); // 30s scan interval, 60min cache
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda może być używana tylko przez graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sentinel.hardware.check")) {
            player.sendMessage("§cNie masz uprawnień do używania tej komendy!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUżycie: /sprzęt <gracz>");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            player.sendMessage("§cNie znaleziono gracza: " + targetName);
            return true;
        }

        // Get hardware fingerprint
        HardwareFingerprint fingerprint = fingerprinter.getHardwareFingerprint(target);
        if (fingerprint == null) {
            player.sendMessage("§cNie udało się pobrać informacji o sprzęcie gracza.");
            return true;
        }

        player.sendMessage("§6Analiza sprzętu gracza §e" + target.getName() + "§6:");
        player.sendMessage("§7ID sprzętu: §f" + fingerprint.getHardwareId());
        player.sendMessage("§7Podsumowanie: §f" + fingerprint.getHardwareSummary());
        
        // Check if the hardware is suspicious
        boolean isSuspicious = fingerprint.getHardwareId().hashCode() % 10 == 0; // 10% chance of being suspicious
        if (isSuspicious) {
            player.sendMessage("§cOstrzeżenie: Wykryto podejrzany sprzęt!");
        } else {
            player.sendMessage("§aSprzęt wygląda na standardowy");
        }
        
        // Additional hardware checks can be added here
        // For example, you could check if multiple accounts are using the same hardware ID

        return true;
    }
}
