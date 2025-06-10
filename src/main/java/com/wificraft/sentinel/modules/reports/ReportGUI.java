package com.wificraft.sentinel.modules.reports;

import com.wificraft.sentinel.WiFiCraftSentinel;
import com.wificraft.sentinel.modules.reports.evidence.ChatEvidence;
import com.wificraft.sentinel.modules.reports.evidence.LocationEvidence;
import com.wificraft.sentinel.modules.reports.evidence.ScreenshotEvidence;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class ReportGUI implements Listener {
    private final JavaPlugin plugin;
    private final ReportManager reportManager;
    private final ChatInputListener chatInputListener;
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, Integer> currentPage = new HashMap<>();
    private final Map<UUID, UUID> viewingReport = new HashMap<>();
    private final Map<UUID, List<Report>> viewingReports = new HashMap<>();
    private final Map<UUID, UUID> viewingPlayerReports = new HashMap<>();
    private static final int PAGE_SIZE = 45; // 5 rzędów po 9 minus jeden rząd na nawigację
    
    public ReportGUI(JavaPlugin plugin, ReportManager reportManager) {
        this.plugin = plugin;
        this.reportManager = reportManager;
        this.chatInputListener = new ChatInputListener(plugin);
    }
    
    /**
     * Clean up resources when the plugin is disabled
     */
    public void cleanup() {
        // Clean up any pending chat inputs
        chatInputListener.cleanup();
        
        // Clear all maps to prevent memory leaks
        playerPages.clear();
        currentPage.clear();
        viewingReport.clear();
        viewingReports.clear();
        viewingPlayerReports.clear();
        
        // Clean up any players with open UIs
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() == null) {
                player.closeInventory();
            }
        }
    }
    
    // Otwiera listę raportów dla gracza
    /**
     * Displays search results in the GUI
     * @param player The player viewing the results
     * @param results List of reports matching the search
     * @param page Page number (0-based)
     */
    /**
     * Displays search results in the GUI
     * @param player The player viewing the results
     * @param results List of reports matching the search
     * @param page Page number (0-based)
     */
    public void displaySearchResults(Player player, List<Report> results, int page) {
        int totalPages = Math.max(1, (int) Math.ceil((double) results.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        
        // Store the results for pagination
        viewingReports.put(player.getUniqueId(), results);
        currentPage.put(player.getUniqueId(), page);
        
        // Create inventory with title showing page info
        Component title = Component.text("Wyniki wyszukiwania - Strona " + (page + 1) + "/" + totalPages)
                .color(NamedTextColor.GOLD);
        Inventory gui = Bukkit.createInventory(null, 54, title);
        
        // Add report items for current page
        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, results.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Report report = results.get(i);
            gui.addItem(createReportItem(report));
        }
        
        // Add navigation buttons
        if (page > 0) {
            gui.setItem(45, createNavigationItem(Material.ARROW, "§ePoprzednia strona"));
        }
        
        if (page < totalPages - 1) {
            gui.setItem(53, createNavigationItem(Material.ARROW, "§eNastępna strona"));
        }
        
        // Add back button
        gui.setItem(49, createNavigationItem(Material.BARRIER, "§cPowrót do raportów"));
        
        // Open the GUI
        player.openInventory(gui);
    }
    

    
    public void openReportList(Player player, UUID targetPlayerId, int page) {
        List<Report> reports = reportManager.getPlayerReports(targetPlayerId);
        int totalPages = Math.max(1, (int) Math.ceil((double) reports.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        
        String playerName = getPlayerName(targetPlayerId);
        // Build the inventory title with proper formatting
        Component title = Component.text()
            .append(Component.text("Raporty: ", NamedTextColor.BLUE))
            .append(Component.text(playerName, NamedTextColor.YELLOW))
            .append(Component.text(" ", NamedTextColor.GRAY))
            .append(Component.text("(Strona ", NamedTextColor.GRAY))
            .append(Component.text((page + 1), NamedTextColor.WHITE))
            .append(Component.text("/", NamedTextColor.GRAY))
            .append(Component.text(totalPages, NamedTextColor.WHITE))
            .append(Component.text(")", NamedTextColor.GRAY))
            .build();
            
        // Create the inventory with the title
        Inventory inv = Bukkit.createInventory(
            player, 
            54, // Size
            title // Title component
        );
        
        // Add reports to the inventory
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, reports.size());
        
        for (int i = start; i < end; i++) {
            Report report = reports.get(i);
            inv.addItem(createReportItem(report));
        }
        
        // Add navigation items
        if (page > 0) {
            inv.setItem(45, createNavigationItem(Material.ARROW, "§ePoprzednia strona", 
                "§7Kliknij, aby przejść do strony " + page));
        }
        
        if (page < totalPages - 1) {
            inv.setItem(53, createNavigationItem(Material.ARROW, "§eNastępna strona", 
                "§7Kliknij, aby przejść do strony " + (page + 2)));
        }
        
        // Add back button
        inv.setItem(49, createNavigationItem(Material.BARRIER, "§cPowrót", 
            "§7Wróć do wyboru gracza"));
        
        // Add create new report button
        inv.setItem(4, createNavigationItem(Material.WRITABLE_BOOK, "§aUtwórz nowy raport", 
            "§7Kliknij, aby utworzyć nowy raport",
            "§7dla " + playerName));
        
        player.openInventory(inv);
        viewingReports.put(player.getUniqueId(), reports);
        currentPage.put(player.getUniqueId(), page);
    }
    
    // Open a specific report
    public void openReport(Player player, UUID reportId) {
        reportManager.getReport(reportId).ifPresent(report -> {
            // Create a proper title for the report view
            Component reportTitle = Component.text()
                .append(Component.text("Raport #", NamedTextColor.BLUE))
                .append(Component.text(reportId.toString().substring(0, 8), NamedTextColor.YELLOW))
                .build();
                
            // Create the inventory with the title
            Inventory inv = Bukkit.createInventory(
                player, // Owner
                54, // Size
                reportTitle // Title component
            );
            
            // Add report information
            inv.setItem(4, createReportInfoItem(report));
            
            // Add evidence items
            List<Evidence> evidence = report.getEvidence();
            for (int i = 0; i < Math.min(evidence.size(), 45); i++) {
                inv.setItem(i < 36 ? i : i + 9, createEvidenceItem(evidence.get(i)));
            }
            
            // Add notes display
            List<String> notes = report.getNotes();
            if (!notes.isEmpty()) {
                // Display up to 5 most recent notes
                int notesToShow = Math.min(5, notes.size());
                for (int i = 0; i < notesToShow; i++) {
                    String note = notes.get(notes.size() - 1 - i); // Show newest first
                    inv.addItem(createNoteItem(note));
                }
                
                if (notes.size() > 5) {
                    player.sendMessage(Component.text("Pokażono 5 najnowszych notatek z " + notes.size() + ".", 
                        NamedTextColor.YELLOW));
                }
            }
            
            // Add action buttons
            if (report.getStatus() == Report.ReportStatus.OPEN) {
                // Only show claim button if report is not claimed or claimed by this moderator
                if (report.getAssignedModeratorId() == null || report.getAssignedModeratorId().equals(player.getUniqueId().toString())) {
                    inv.setItem(45, createActionItem(Material.GOLDEN_AXE, "§6Zażądaj zgłoszenia", 
                        "§7Kliknij, aby zająć się tym zgłoszeniem"));
                    
                    // Add teleport button if there's location evidence
                    boolean hasLocation = report.getEvidence().stream()
                        .anyMatch(e -> e.getType() == Evidence.EvidenceType.LOCATION);
                        
                    if (hasLocation) {
                        inv.setItem(46, createActionItem(Material.ENDER_PEARL, "§bTeleportuj się", 
                            "§7Kliknij, aby się przeteleportować",
                            "§7do lokalizacji zgłoszenia"));
                    }
                    
                    // Add resolve and reject buttons
                    inv.setItem(47, createActionItem(Material.EMERALD, "§aRozwiąż", 
                        "§7Kliknij, aby oznaczyć jako rozwiązane"));
                    inv.setItem(48, createActionItem(Material.BARRIER, "§cOdrzuć", 
                        "§7Kliknij, aby odrzucić zgłoszenie"));
                    
                    // Add notes section
                    List<String> notesList = report.getNotes();
                    if (notesList != null && !notesList.isEmpty()) {
                        // Display up to 5 most recent notes
                        int notesToShow = Math.min(5, notesList.size());
                        for (int i = 0; i < notesToShow; i++) {
                            String note = notesList.get(notesList.size() - 1 - i); // Show newest first
                            inv.setItem(45 + i, createNoteItem(note));
                        }
                    }
                    
                    // Add note button - show count of existing notes
                    List<Component> noteLore = new ArrayList<>();
                    noteLore.add(Component.text("Kliknij, aby dodać notatkę do tego raportu", NamedTextColor.GRAY));
                    if (notesList != null && !notesList.isEmpty()) {
                        noteLore.add(Component.text("Liczba istniejących notatek: " + notesList.size(), NamedTextColor.GRAY));
                    }
                    
                    ItemStack noteItem = createActionItem(Material.BOOK, "§eDodaj notatkę", 
                        noteLore.stream().map(Component::toString).toArray(String[]::new));
                    inv.setItem(49, noteItem);
                }
            }
            
            // Add back to reports button
            inv.setItem(50, createNavigationItem(Material.ARROW, "§ePowrót do raportów", 
                "§7Wróć do listy raportów"));
            
            player.openInventory(inv);
            viewingReport.put(player.getUniqueId(), reportId);
        });
    }
    
    /**
     * Creates an item representing a report in the GUI
     * @param report The report to create an item for
     * @return ItemStack representing the report
     */
    private ItemStack createReportItem(Report report) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        String reporterName = getPlayerName(report.getReporterId());
        String reportedName = getPlayerName(report.getReportedPlayerId());
        Component status = getStatusDisplay(report.getStatus());
        
        // Set display name with report ID
        meta.displayName(Component.text("Raport #" + report.getId().toString().substring(0, 8))
            .color(NamedTextColor.GOLD));
        
        // Set lore with report details
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Reporter: ", NamedTextColor.GRAY)
            .append(Component.text(reporterName != null ? reporterName : "Unknown", NamedTextColor.WHITE)));
            
        lore.add(Component.text("Zgłoszony: ", NamedTextColor.GRAY)
            .append(Component.text(reportedName != null ? reportedName : "Unknown", NamedTextColor.WHITE)));
            
        lore.add(Component.text("Status: ", NamedTextColor.GRAY)
            .append(status));
            
        lore.add(Component.text("Powód: ", NamedTextColor.GRAY)
            .append(Component.text(report.getReason(), NamedTextColor.WHITE)));
            
        lore.add(Component.text("Utworzono: ", NamedTextColor.GRAY)
            .append(Component.text(report.getCreatedAt().toString(), NamedTextColor.WHITE)));
            
        lore.add(Component.text("Kliknij, aby zobaczyć szczegóły", NamedTextColor.YELLOW));
        
        if (report.getAssignedModeratorId() != null) {
            String moderatorName = getPlayerName(UUID.fromString(report.getAssignedModeratorId()));
            lore.add(Component.text("Przypisano do: ", NamedTextColor.GRAY)
                .append(Component.text(moderatorName, NamedTextColor.WHITE)));
        }
        
        if (report.getResolvedAt() != null) {
            lore.add(Component.text("Rozwiązano: ", NamedTextColor.GRAY)
                .append(Component.text(report.getResolvedAt().toString(), NamedTextColor.WHITE)));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("Kliknij, aby zobaczyć szczegóły", NamedTextColor.YELLOW));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    // Create a report info item
    private ItemStack createReportInfoItem(Report report) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        String reporterName = getPlayerName(report.getReporterId());
        String reportedName = getPlayerName(report.getReportedPlayerId());
        Component status = getStatusDisplay(report.getStatus());
        
        meta.displayName(Component.text("Raport #" + report.getId().toString().substring(0, 8))
            .color(NamedTextColor.GOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Reporter: ", NamedTextColor.GRAY)
            .append(Component.text(reporterName, NamedTextColor.WHITE)));
            
        lore.add(Component.text("Zgłoszony: ", NamedTextColor.GRAY)
            .append(Component.text(reportedName, NamedTextColor.WHITE)));
            
        lore.add(Component.text("Status: ", NamedTextColor.GRAY)
            .append(status));
            
        lore.add(Component.text("Utworzono: ", NamedTextColor.GRAY)
            .append(Component.text(report.getCreatedAt().toString(), NamedTextColor.WHITE)));
        
        if (report.getAssignedModeratorId() != null) {
            String moderatorName = getPlayerName(UUID.fromString(report.getAssignedModeratorId()));
            lore.add(Component.text("Przypisano do: ", NamedTextColor.GRAY)
                .append(Component.text(moderatorName, NamedTextColor.WHITE)));
        }
        
        if (report.getResolvedAt() != null) {
            lore.add(Component.text("Rozwiązano: ", NamedTextColor.GRAY)
                .append(Component.text(report.getResolvedAt().toString(), NamedTextColor.WHITE)));
                
            if (report.getResolutionNotes() != null && !report.getResolutionNotes().isEmpty()) {
                lore.add(Component.text("Rozwiązanie: ", NamedTextColor.GRAY)
                    .append(Component.text(report.getResolutionNotes(), NamedTextColor.WHITE)));
            }
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("Powód:", NamedTextColor.GRAY));
        lore.add(Component.text(report.getReason(), NamedTextColor.WHITE));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    // Create a navigation item
    private ItemStack createNavigationItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Use Adventure API for display name
        meta.displayName(Component.text(name));
        
        if (lore != null && lore.length > 0) {
            List<Component> loreComponents = Arrays.stream(lore)
                .map(line -> Component.text(line))
                .collect(Collectors.toList());
            meta.lore(loreComponents);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Creates a note item for display in the GUI
     * @param note The note text to display
     * @return An ItemStack representing the note
     */
    private ItemStack createNoteItem(String note) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        // Extract timestamp if present
        String displayNote = note;
        String timestamp = "";
        if (note.startsWith("[") && note.contains("] ")) {
            int endBracket = note.indexOf("] ") + 2;
            timestamp = note.substring(1, note.indexOf("]"));
            if (endBracket < note.length()) {
                displayNote = note.substring(endBracket);
            }
        }
        
        // Truncate if too long for display
        if (displayNote.length() > 30) {
            displayNote = displayNote.substring(0, 27) + "...";
        }
        
        // Set display name
        meta.displayName(Component.text("Notatka", NamedTextColor.YELLOW));
        
        // Build lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("\"" + displayNote + "\"", NamedTextColor.WHITE));
        
        // Add timestamp if available
        if (!timestamp.isEmpty()) {
            lore.add(Component.text("Data: " + timestamp, NamedTextColor.GRAY));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createActionItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Use Adventure API to set display name and lore
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
        
        if (lore != null && lore.length > 0) {
            List<Component> loreComponents = Arrays.stream(lore)
                .map(line -> LegacyComponentSerializer.legacySection().deserialize(line))
                .collect(Collectors.toList());
            meta.lore(loreComponents);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createEvidenceItem(Evidence evidence) {
        Material material = Material.PAPER;
        Component displayName = Component.text("Nieznany dowód").color(NamedTextColor.YELLOW);
        List<Component> lore = new ArrayList<>();
        
        if (evidence instanceof ChatEvidence) {
            ChatEvidence ce = (ChatEvidence) evidence;
            material = Material.PAPER;
            displayName = Component.text("Wiadomość").color(NamedTextColor.BLUE);
            
            lore.add(Component.empty());
            lore.add(Component.text("Wiadomość:", NamedTextColor.GRAY));
            lore.add(Component.text(ce.getMessage(), NamedTextColor.WHITE));
            
        } else if (evidence instanceof LocationEvidence) {
            LocationEvidence le = (LocationEvidence) evidence;
            material = Material.MAP;
            displayName = Component.text("Lokalizacja").color(NamedTextColor.GREEN);
            
            lore.add(Component.empty());
            lore.add(Component.text("Świat: ", NamedTextColor.GRAY)
                .append(Component.text(le.getLocation().getWorld().getName(), NamedTextColor.WHITE)));
            lore.add(Component.text("X: ", NamedTextColor.GRAY)
                .append(Component.text(le.getLocation().getBlockX(), NamedTextColor.WHITE)));
            lore.add(Component.text("Y: ", NamedTextColor.GRAY)
                .append(Component.text(le.getLocation().getBlockY(), NamedTextColor.WHITE)));
            lore.add(Component.text("Z: ", NamedTextColor.GRAY)
                .append(Component.text(le.getLocation().getBlockZ(), NamedTextColor.WHITE)));
                
        } else if (evidence instanceof ScreenshotEvidence) {
            ScreenshotEvidence se = (ScreenshotEvidence) evidence;
            material = Material.PAINTING;
            displayName = Component.text("Zrzut ekranu").color(NamedTextColor.LIGHT_PURPLE);
            
            lore.add(Component.empty());
            lore.add(Component.text("Plik: ", NamedTextColor.GRAY)
                .append(Component.text(se.getFileName(), NamedTextColor.WHITE)));
        }
        
        // Add timestamp to all evidence types
        lore.add(Component.empty());
        lore.add(Component.text("Dodano: ", NamedTextColor.GRAY)
            .append(Component.text(new Date().toString(), NamedTextColor.WHITE)));
        
        // Add notes if available
        if (evidence.getNotes() != null && !evidence.getNotes().isEmpty()) {
            lore.add(Component.text("Notatki: ", NamedTextColor.GRAY)
                .append(Component.text(evidence.getNotes(), NamedTextColor.WHITE)));
        }
        
        // Create and return the item
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(displayName);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    // Helper method to get player name from UUID
    private String getPlayerName(UUID uuid) {
        if (uuid == null) return "Nieznany";
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : "Gracz_" + uuid.toString().substring(0, 8);
    }
    
    // Helper method to get status display with color
    private Component getStatusDisplay(Report.ReportStatus status) {
        return switch (status) {
            case OPEN -> Component.text("Otwarty", NamedTextColor.GREEN);
            case IN_PROGRESS -> Component.text("W toku", NamedTextColor.YELLOW);
            case RESOLVED -> Component.text("Zamknięty", NamedTextColor.GRAY);
            case REJECTED -> Component.text("Odrzucony", NamedTextColor.RED);
            default -> Component.text("Nieznany", NamedTextColor.WHITE);
        };
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        
        // Handle page navigation
        if (viewingReports.containsKey(playerId)) {
            event.setCancelled(true);
            
            String displayName = LegacyComponentSerializer.legacySection().serialize(clickedItem.getItemMeta().displayName());
            int current = currentPage.getOrDefault(playerId, 0);
            
            if (displayName.contains("Następna")) {
                openReportList(player, viewingPlayerReports.get(playerId), current + 1);
            } else if (displayName.contains("Poprzednia")) {
                openReportList(player, viewingPlayerReports.get(playerId), current - 1);
            } else if (displayName.contains("Powrót")) {
                // Handle back button
                // You might want to implement a back stack or parent view
            }
        }
        
        // Handle report view clicks
        if (viewingReport.containsKey(playerId)) {
            event.setCancelled(true);
            
            if (clickedItem.getType() == Material.ARROW) {
                // Back to reports list
                openReportList(player, viewingPlayerReports.get(playerId), 0);
            } else if (clickedItem.getType() == Material.GOLDEN_AXE) {
                // Claim report
                UUID reportId = viewingReport.get(playerId);
                reportManager.claimReport(reportId, playerId);
                player.sendMessage(Component.text("Zgłoszenie zostało przypisane do Ciebie.", NamedTextColor.GREEN));
                openReport(player, reportId); // Refresh the view
            } else if (clickedItem.getType() == Material.ENDER_PEARL) {
                // Teleport to location
                handleTeleportation(player);
            } else if (clickedItem.getType() == Material.EMERALD) {
                // Resolve report
                UUID reportId = viewingReport.get(playerId);
                reportManager.resolveReport(reportId, "Rozwiązane przez " + player.getName());
                player.sendMessage(Component.text("Zgłoszenie zostało oznaczone jako rozwiązane.", NamedTextColor.GREEN));
                openReport(player, reportId); // Refresh the view
            } else if (clickedItem.getType() == Material.BARRIER) {
                // Reject report
                UUID reportId = viewingReport.get(playerId);
                reportManager.rejectReport(reportId, "Odrzucone przez " + player.getName());
                player.sendMessage(Component.text("Zgłoszenie zostało odrzucone.", NamedTextColor.YELLOW));
                openReport(player, reportId); // Refresh the view
            } else if (clickedItem.getType() == Material.BOOK) {
                // Add note
                player.closeInventory();
                
                UUID currentPlayerId = player.getUniqueId();
                UUID reportId = viewingReport.get(currentPlayerId);
                
                // Use the chat input listener to get the note text
                chatInputListener.awaitChatInput(player, note -> {
                    if (note != null && !note.trim().isEmpty()) {
                        handleAddNote(player, note);
                    } else if (note == null) {
                        player.sendMessage(Component.text("Anulowano dodawanie notatki.", NamedTextColor.RED));
                        openReport(player, reportId);
                    }
                });
            }
        }
    }
    
    private void handleTeleportation(Player player) {
        UUID playerId = player.getUniqueId();
        if (!viewingReport.containsKey(playerId)) {
            player.sendMessage(Component.text("Nie znaleziono aktywnego raportu.", NamedTextColor.RED));
            return;
        }
        
        UUID reportId = viewingReport.get(playerId);
        Optional<Report> reportOpt = reportManager.getReport(reportId);
        
        if (reportOpt.isEmpty()) {
            player.sendMessage(Component.text("Nie znaleziono raportu do teleportacji.", NamedTextColor.RED));
            return;
        }
        
        Report report = reportOpt.get();
        
        // Find first location evidence
        Optional<LocationEvidence> locationEvidence = report.getEvidence().stream()
            .filter(LocationEvidence.class::isInstance)
            .map(LocationEvidence.class::cast)
            .findFirst();
            
        if (locationEvidence.isEmpty()) {
            player.sendMessage(Component.text("Ten raport nie zawiera informacji o lokalizacji.", NamedTextColor.RED));
            return;
        }
        
        org.bukkit.Location location = locationEvidence.get().getLocation();
        if (location.getWorld() == null) {
            player.sendMessage(Component.text("Świat nie jest już dostępny.", NamedTextColor.RED));
            return;
        }
        
        player.teleport(location);
        player.sendMessage(Component.text("Przeteleportowano do lokalizacji z raportu.", NamedTextColor.GREEN));
    }
    
    /**
     * Handles adding a note to the currently viewed report
     */
    /**
     * Handles adding a note to the currently viewed report
     * @param player The player adding the note
     * @param note The note content to add
     */
    public void handleAddNote(Player player, String note) {
        if (note == null || note.trim().isEmpty()) {
            player.sendMessage(Component.text("Notatka nie może być pusta!", NamedTextColor.RED));
            return;
        }
        
        UUID playerId = player.getUniqueId();
        if (!viewingReport.containsKey(playerId)) {
            player.sendMessage(Component.text("Nie masz otwartego żadnego raportu.", NamedTextColor.RED));
            return;
        }
        
        UUID reportId = viewingReport.get(playerId);
        boolean success = reportManager.addNote(reportId, player.getName() + ": " + note);
        
        if (success) {
            player.sendMessage(Component.text("Dodano notatkę do raportu.", NamedTextColor.GREEN));
            openReport(player, reportId); // Refresh the view
        } else {
            player.sendMessage(Component.text("Nie udało się dodać notatki. Raport mógł zostać usunięty.", NamedTextColor.RED));
        }
    }
    
    /**
     * Handles closing the GUI and cleaning up
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Clean up when inventory is closed
        viewingReport.remove(playerId);
        viewingReports.remove(playerId);
        currentPage.remove(playerId);
        viewingPlayerReports.remove(playerId);
    }
    
    /**
     * Opens the main reports list for a player
     */
    public void openMainReportsList(Player player, int page) {
        // This would show all reports, not just for one player
        // Implementation depends on your requirements
        player.sendMessage(Component.text("Funkcja listy wszystkich raportów nie jest jeszcze zaimplementowana.", NamedTextColor.YELLOW));
    }
}
