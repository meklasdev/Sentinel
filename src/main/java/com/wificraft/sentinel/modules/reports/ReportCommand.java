package com.wificraft.sentinel.modules.reports;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ReportCommand implements CommandExecutor {
    private final ReportManager reportManager;
    private final ReportGUI reportGUI;

    private static final String PREFIX = "<gradient:#FF6B6B:#4ECDC4>[Raporty]</gradient> ";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public ReportCommand(ReportManager reportManager, ReportGUI reportGUI) {
        this.reportManager = reportManager;
        this.reportGUI = reportGUI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Tę komendę może użyć tylko gracz!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "create":
            case "tworzenie":
                return handleCreate(player, subArgs);
            case "view":
            case "zobacz":
                return handleView(player, subArgs);
            case "list":
            case "lista":
                return handleList(player, subArgs);
            case "search":
            case "szukaj":
                return handleSearch(player, subArgs);
            case "resolve":
            case "rozwiaz":
                return handleResolve(player, subArgs);
            case "close":
            case "zamknij":
                return handleClose(player, subArgs);
            default:
                showHelp(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + "<red>Użycie: /report create <gracz> <powód>"));
            return false;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore()) {
            player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + "<red>Nie znaleziono gracza: " + targetName));
            return false;
        }

        Report report = reportManager.createReport(player.getUniqueId(), target.getUniqueId(), reason);
        player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + "<green>Pomyślnie utworzono raport o ID: #" + report.getId().toString().substring(0, 8)));

        // Notify online staff
        Component notification = MINI_MESSAGE.deserialize(PREFIX + "<gold>" + player.getName() + "</gold> zgłosił <gold>" + target.getName() + "</gold>: <white>" + reason);
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("sentinel.reports.view"))
                .forEach(p -> p.sendMessage(notification));

        return true;
    }

    private boolean handleView(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + "<red>Użycie: /report view <id>"));
            return false;
        }

        try {
            final String idArg = args[0];
            Optional<Report> reportOpt = reportManager.getAllReports().stream()
                    .filter(r -> r.getId().toString().startsWith(idArg))
                    .findFirst();

            if (reportOpt.isEmpty()) {
                player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + "<red>Raport o podanym ID nie istnieje!"));
                return false;
            }

            reportGUI.openReport(player, reportOpt.get().getId());
            return true;

        } catch (Exception e) {
            player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + "<red>Wystąpił błąd podczas wyświetlania raportu."));
            return false;
        }
    }

    private boolean handleList(Player player, String[] args) {
        if (!player.hasPermission("sentinel.reports.view")) {
            player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + "<red>Nie masz uprawnień do przeglądania listy raportów!"));
            return false;
        }

        int page = 0;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]) - 1;
                if (page < 0) page = 0;
            } catch (NumberFormatException e) {
                player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + "<red>Nieprawidłowy numer strony!"));
                return false;
            }
        }

        reportGUI.openReportList(player, player.getUniqueId(), page);
        return true;
    }

    private boolean handleSearch(Player player, String[] args) {
        if (args.length < 1) {
            showHelp(player);
            return false;
        }

        String query = String.join(" ", args);
        List<Report> results = reportManager.searchReports(query, null, null, null);

        if (results.isEmpty()) {
            player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + "<yellow>Brak wyników wyszukiwania dla zapytania: '" + query + "'"));
            return true;
        }

        player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + "<green>Wyniki wyszukiwania (" + results.size() + "):"));
        for (Report report : results) {
            player.sendMessage(MINI_MESSAGE.deserialize(
                String.format("<gray>#%s | %s | <white>%s <gray>→ <white>%s <gray>| <white>%s",
                        report.getId().toString().substring(0, 8),
                        report.getStatus().name(),
                        Bukkit.getOfflinePlayer(report.getReporterId()).getName(),
                        Bukkit.getOfflinePlayer(report.getReportedPlayerId()).getName(),
                        report.getReason()
                )));
        }
        return true;
    }
    
    private boolean handleResolve(Player player, String[] args) {
        return updateReportStatus(player, args, Report.ReportStatus.RESOLVED, "resolve");
    }

    private boolean handleClose(Player player, String[] args) {
        return updateReportStatus(player, args, Report.ReportStatus.CLOSED, "close");
    }

    private boolean updateReportStatus(Player player, String[] args, Report.ReportStatus status, String commandName) {
        if (!player.hasPermission("sentinel.reports.manage")) {
            player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + "<red>Nie masz uprawnień do zarządzania raportami."));
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + String.format("<red>Użycie: /report %s <id> <notatka>", commandName)));
            return false;
        }

        try {
            final String idArg = args[0];
            String note = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            Optional<Report> reportOpt = reportManager.getAllReports().stream()
                    .filter(r -> r.getId().toString().startsWith(idArg))
                    .findFirst();

            if (reportOpt.isEmpty()) {
                player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + "<red>Raport o podanym ID nie istnieje!"));
                return false;
            }

            Report report = reportOpt.get();
            report.setStatus(status);
            report.addNote(player.getName() + ": " + note);
            reportManager.addNote(report.getId(), player.getName() + ": " + note);
            if (status == Report.ReportStatus.RESOLVED) {
                reportManager.resolveReport(report.getId(), note);
            } else if (status == Report.ReportStatus.CLOSED) {
                reportManager.closeReport(report.getId(), note);
            }

            player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + String.format("<green>Raport #%s został oznaczony jako %s.", report.getId().toString().substring(0, 8), status.name())));
            return true;

        } catch (Exception e) {
            player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + "<red>Wystąpił błąd podczas aktualizacji raportu."));
            return false;
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + "<yellow>Dostępne komendy:"));
        player.sendMessage(Component.text("/report create <gracz> <powód>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  Zgłoś gracza", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/report view <id>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  Zobacz szczegóły raportu", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/report list [strona]", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  Lista raportów", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/report search <fraza>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  Wyszukaj raporty", NamedTextColor.GRAY));

        if (player.hasPermission("sentinel.reports.manage")) {
            player.sendMessage(Component.text(" ", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Komendy administracyjne:", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("/report resolve <id> <notatka>", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("  Oznacz raport jako rozwiązany", NamedTextColor.GRAY));
            player.sendMessage(Component.text("/report close <id> <notatka>", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("  Zamknij raport bez rozwiązywania", NamedTextColor.GRAY));
        }
    }
}
