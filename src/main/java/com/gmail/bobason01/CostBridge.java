package com.gmail.bobason01;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CostBridge {

    public static boolean checkAndTake(Player player, GUIManager plugin, double moneyCost, ItemStack[] itemCosts) {
        if (moneyCost > 0) {
            if (GUIManager.econ == null) {
                player.sendMessage(ChatColor.RED + "Economy is not enabled on this server");
                return false;
            }
            if (!GUIManager.econ.has(player, moneyCost)) {
                player.sendMessage(ChatColor.RED + "You don't have enough money");
                return false;
            }
        }

        if (itemCosts != null && itemCosts.length > 0) {
            if (!hasItems(player.getInventory(), itemCosts)) {
                player.sendMessage(ChatColor.RED + "You don't have the required items");
                return false;
            }
        }

        if (moneyCost > 0 && GUIManager.econ != null) {
            GUIManager.econ.withdrawPlayer(player, moneyCost);
        }

        if (itemCosts != null && itemCosts.length > 0) {
            GUIListener.removeStaticItems(player, itemCosts);
        }

        return true;
    }

    private static boolean hasItems(Inventory inv, ItemStack[] costs) {
        for (ItemStack cost : costs) {
            if (cost == null || cost.getType().isAir()) continue;
            int required = cost.getAmount();
            int found = 0;
            for (ItemStack item : inv.getContents()) {
                if (item == null || item.getType().isAir()) continue;
                if (item.getType() == cost.getType()) {
                    if (cost.hasItemMeta()) {
                        if (!item.hasItemMeta() || !org.bukkit.Bukkit.getItemFactory().equals(item.getItemMeta(), cost.getItemMeta())) {
                            continue;
                        }
                    }
                    found += item.getAmount();
                }
            }
            if (found < required) return false;
        }
        return true;
    }
}