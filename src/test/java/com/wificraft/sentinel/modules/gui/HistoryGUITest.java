package com.wificraft.sentinel.modules.gui;

import com.wificraft.sentinel.modules.config.NotificationConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryGUITest {
    @Mock
    private Player mockPlayer;
    @Mock
    private YamlConfiguration mockInspections;
    @Mock
    private YamlConfiguration mockBanHistory;
    @Mock
    private NotesManager mockNotesManager;
    @Mock
    private PlayerStatsManager mockStatsManager;
    private HistoryGUI historyGUI;
    private UUID testUUID;

    @BeforeEach
    void setUp() {
        testUUID = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(testUUID);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        
        historyGUI = new HistoryGUI(
            mockInspections,
            mockBanHistory,
            mockNotesManager,
            mockStatsManager
        );
    }

    @Test
    void testSearchFilter() {
        SearchFilter filter = new SearchFilter();
        filter.addStringFilter("text", "test");
        filter.addDateFilter("after", "2025-06-01 00:00:00");
        filter.addNumericFilter("min", 300);
        filter.addListFilter("moderator", new String[]{"John", "Doe"});

        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Arrays.asList(
            "§7Data: 2025-06-02 12:00:00",
            "§7Moderator: John",
            "§7Czas: 600 sekund",
            "§7Testowy tekst"
        ));
        item.setItemMeta(meta);

        assertTrue(filter.matches(item));
    }

    @Test
    void testHistoryPagination() {
        when(mockInspections.getList("inspections." + testUUID.toString()))
            .thenReturn(Arrays.asList(
                createTestInspection("2025-06-01 12:00:00"),
                createTestInspection("2025-06-01 13:00:00"),
                createTestInspection("2025-06-01 14:00:00")
            ));

        historyGUI.updateHistoryGUI(mockPlayer, mockPlayer, 0);
        verify(mockPlayer).openInventory(any());
    }

    @Test
    void testDiscordNotifications() {
        when(mockPlayer.getLastChatMessage()).thenReturn("discord on");
        historyGUI.handleFilterCommand(mockPlayer);
        verify(mockPlayer).sendMessage("§aPowiadomienia Discord włączone!");

        when(mockPlayer.getLastChatMessage()).thenReturn("discord off");
        historyGUI.handleFilterCommand(mockPlayer);
        verify(mockPlayer).sendMessage("§cPowiadomienia Discord wyłączone!");
    }

    @Test
    void testInspectionRecording() {
        historyGUI.recordInspection(mockPlayer, mockPlayer);
        verify(mockPlayer).sendMessage(anyString());
    }

    @Test
    void testLoginRecording() {
        historyGUI.recordLogin(mockPlayer);
        verify(mockPlayer).sendMessage(anyString());
    }

    private Map<String, Object> createTestInspection(String timestamp) {
        Map<String, Object> inspection = new HashMap<>();
        inspection.put("timestamp", timestamp);
        inspection.put("moderator", "TestMod");
        inspection.put("result", "Clean");
        inspection.put("duration", 300L);
        return inspection;
    }

    @Test
    void testFilterCommands() {
        // Test text filter
        when(mockPlayer.getLastChatMessage()).thenReturn("text test");
        historyGUI.handleFilterCommand(mockPlayer);
        verify(mockPlayer).sendMessage(anyString());

        // Test date filter
        when(mockPlayer.getLastChatMessage()).thenReturn("date after 2025-06-01 00:00:00");
        historyGUI.handleFilterCommand(mockPlayer);
        verify(mockPlayer).sendMessage(anyString());

        // Test time filter
        when(mockPlayer.getLastChatMessage()).thenReturn("time min 300");
        historyGUI.handleFilterCommand(mockPlayer);
        verify(mockPlayer).sendMessage(anyString());

        // Test list filter
        when(mockPlayer.getLastChatMessage()).thenReturn("list moderator John,Doe");
        historyGUI.handleFilterCommand(mockPlayer);
        verify(mockPlayer).sendMessage(anyString());

        // Test clear filter
        when(mockPlayer.getLastChatMessage()).thenReturn("clear");
        historyGUI.handleFilterCommand(mockPlayer);
        verify(mockPlayer).sendMessage("§aFiltry zostały wyczyszczone!");
    }

    @Test
    void testExportFunctionality() {
        when(mockPlayer.getLastChatMessage()).thenReturn("export txt");
        historyGUI.handleExportCommand(mockPlayer);
        verify(mockPlayer).sendMessage(anyString());

        when(mockPlayer.getLastChatMessage()).thenReturn("export csv");
        historyGUI.handleExportCommand(mockPlayer);
        verify(mockPlayer).sendMessage(anyString());

        when(mockPlayer.getLastChatMessage()).thenReturn("export json");
        historyGUI.handleExportCommand(mockPlayer);
        verify(mockPlayer).sendMessage(anyString());
    }

    @Test
    void testNotificationCommands() {
        // Test threshold command
        when(mockPlayer.getLastChatMessage()).thenReturn("threshold hourly 10");
        historyGUI.handleNotificationCommand(mockPlayer);
        verify(mockPlayer).sendMessage(anyString());

        // Test pattern command
        when(mockPlayer.getLastChatMessage()).thenReturn("pattern add hacking");
        historyGUI.handleNotificationCommand(mockPlayer);
        verify(mockPlayer).sendMessage(anyString());

        // Test reset command
        when(mockPlayer.getLastChatMessage()).thenReturn("reset stats");
        historyGUI.handleNotificationCommand(mockPlayer);
        verify(mockPlayer).sendMessage(anyString());

        // Test status command
        when(mockPlayer.getLastChatMessage()).thenReturn("status");
        historyGUI.handleNotificationCommand(mockPlayer);
        verify(mockPlayer).sendMessage(anyString());
    }

    @Test
    void testInvalidCommands() {
        when(mockPlayer.getLastChatMessage()).thenReturn("invalid command");
        historyGUI.handleFilterCommand(mockPlayer);
        verify(mockPlayer).sendMessage("§cNieznana komenda!");

        historyGUI.handleExportCommand(mockPlayer);
        verify(mockPlayer).sendMessage("§cNieznana komenda!");

        historyGUI.handleNotificationCommand(mockPlayer);
        verify(mockPlayer).sendMessage("§cNieznana komenda!");
    }
}
