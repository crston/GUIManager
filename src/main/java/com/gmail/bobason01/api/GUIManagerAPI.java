package com.gmail.bobason01.api;

import com.gmail.bobason01.GUI;
import com.gmail.bobason01.GUIManager;
import com.gmail.bobason01.event.GUIOpenEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class GUIManagerAPI {

    private GUIManagerAPI() {}

    public static GUIManager getPlugin() {
        return GUIManager.getInstance();
    }

    public static void openGUI(Player player, String guiId) {
        if (player == null || guiId == null) return;

        GUIOpenEvent event = new GUIOpenEvent(player, guiId);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        Inventory inv = getPlugin().getPlayerSpecificInventory(player, guiId);
        if (inv != null) {
            player.openInventory(inv);
        }
    }

    public static void createGUI(String id, int rows, String title) {
        if (id == null || title == null || rows <= 0) return;
        getPlugin().createGui(id, rows, title);
    }

    public static GUI getGUI(String id) {
        if (id == null) return null;
        return getPlugin().getGui(id);
    }

    public static Map<String, GUI> getAllGUIs() {
        return getPlugin().getGuis();
    }

    public static void setGUIItem(String guiId, int slot, ItemStack item) {
        GUI gui = getGUI(guiId);
        if (gui != null) {
            if (item == null || item.getType().isAir()) {
                gui.getItems().remove(slot);
            } else {
                gui.setItem(slot, item.clone());
            }
            getPlugin().saveGui(guiId);
        }
    }

    public static long getRemainingCooldown(Player player, String actionId) {
        if (player == null || actionId == null) return 0;
        return getPlugin().getRemainingCooldownMillis(player, actionId);
    }

    public static void setCooldown(Player player, String actionId, double seconds) {
        if (player == null || actionId == null || seconds <= 0) return;
        getPlugin().setCooldown(player, actionId, seconds);
    }

    public static boolean hasCooldown(Player player, String actionId) {
        return getRemainingCooldown(player, actionId) > 0;
    }

    public static void reload() {
        getPlugin().loadGuis();
        getPlugin().getLanguageManager().reload();
    }
}