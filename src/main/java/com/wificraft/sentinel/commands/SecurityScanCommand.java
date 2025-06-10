package com.wificraft.sentinel.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.wificraft.sentinel.SentinelPlugin;
import com.wificraft.sentinel.modules.security.SecurityModule;
import com.wificraft.sentinel.modules.security.ClientSecurity;
import com.wificraft.sentinel.modules.security.HardwareFingerprint;

public class SecurityScanCommand implements CommandExecutor {
    private final SentinelPlugin plugin;
    private final SecurityModule securityModule;
    private final ClientSecurity clientSecurity;

    public SecurityScanCommand(SentinelPlugin plugin) {
        this.plugin = plugin;
        this.securityModule = plugin.getSecurityModule();
        this.clientSecurity = plugin.getClientSecurity();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cTa komenda może być użyta tylko przez graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sentinel.security.scan")) {
            player.sendMessage("§cNie masz uprawnień do użycia tej komendy!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUżycie: /scan <gracz>");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            player.sendMessage("§cNie znaleziono gracza: " + targetName);
            return true;
        }
        
        try {
            // Pobierz informacje o kliencie
            HardwareFingerprint fingerprint = clientSecurity.getFingerprint(target);
            String fingerprintId = fingerprint != null ? fingerprint.getHardwareId() : "Nieznane";
            String clientBrand = clientSecurity.getClientBrand(target);
            String clientVersion = clientSecurity.getClientVersion(target);
            boolean isUsingVPN = clientSecurity.isUsingVPN(target);
            boolean isUsingProxy = clientSecurity.isUsingProxy(target);
            
            // Wyświetl informacje
            player.sendMessage("§6===== Analiza bezpieczeństwa =====");
            player.sendMessage("§7Gracz: §f" + target.getName());
            player.sendMessage("§7ID sprzętu: §f" + fingerprintId);
            player.sendMessage("§7Klient: §f" + clientBrand + " " + clientVersion);
            
            if (isUsingVPN) {
                player.sendMessage("§c- Wykryto połączenie przez VPN!");
            }
            
            if (isUsingProxy) {
                player.sendMessage("§c- Wykryto połączenie przez proxy!");
            }
            
            if (!isUsingVPN && !isUsingProxy) {
                player.sendMessage("§a- Brak podejrzanych aktywności sieciowych");
            }

            // Check for modded client
            if (clientSecurity.isUsingModdedClient(target)) {
                player.sendMessage("§c- Wykryto zmodyfikowanego klienta!");
            }

            // Show hardware analysis if available
            if (fingerprint != null) {
                player.sendMessage("\n§6=== Analiza sprzętu ===");
                player.sendMessage(fingerprint.toString());
            }
        } catch (Exception e) {
            player.sendMessage("§cWystąpił błąd podczas analizy bezpieczeństwa: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
}
