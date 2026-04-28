package com.gmail.bobason01;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class CostBridge {

    private CostBridge() {}

    public static boolean checkAndTake(Player p, GUIManager plugin, double moneyCost, ItemStack[] parsedCosts) {
        if (parsedCosts != null && parsedCosts.length > 0) {
            if (!bridgeHasItems(p.getInventory(), parsedCosts)) {
                p.sendMessage(plugin.getLanguageManager().getMessage("check_no_item"));
                return false;
            }
        }

        if (GUIManager.econ != null && moneyCost > 0) {
            if (!GUIManager.econ.has(p, moneyCost)) {
                p.sendMessage(plugin.getLanguageManager().getMessage("check_no_money"));
                return false;
            }
        }

        if (parsedCosts != null && parsedCosts.length > 0) {
            try {
                GUIListener.removeStaticItems(p, parsedCosts);
            } catch (Exception ignored) {}
        }

        if (GUIManager.econ != null && moneyCost > 0) {
            GUIManager.econ.withdrawPlayer(p, moneyCost);
        }

        return true;
    }

    private static boolean bridgeHasItems(Inventory inventory, ItemStack[] requiredItems) {
        if (requiredItems == null) return true;
        for (int i = 0; i < requiredItems.length; i++) {
            ItemStack requiredItem = requiredItems[i];
            if (requiredItem == null || requiredItem.getType().isAir()) continue;

            if (!containsAtLeastStrict(inventory, requiredItem, requiredItem.getAmount())) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsAtLeastStrict(Inventory inventory, ItemStack costItem, int amountNeeded) {
        int found = 0;
        ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack invItem = contents[i];
            if (invItem == null || invItem.getType() != costItem.getType()) continue;

            boolean isMatch = true;
            if (costItem.hasItemMeta()) {
                if (!invItem.hasItemMeta()) {
                    isMatch = false;
                } else {
                    ItemMeta costMeta = costItem.getItemMeta();
                    ItemMeta invMeta = invItem.getItemMeta();

                    if (costMeta.hasDisplayName()) {
                        if (!invMeta.hasDisplayName() || !invMeta.getDisplayName().equals(costMeta.getDisplayName())) {
                            isMatch = false;
                        }
                    }

                    if (isMatch && costMeta.hasCustomModelData()) {
                        if (!invMeta.hasCustomModelData() || invMeta.getCustomModelData() != costMeta.getCustomModelData()) {
                            isMatch = false;
                        }
                    }
                }
            }

            if (isMatch) {
                found += invItem.getAmount();
            }
            if (found >= amountNeeded) return true;
        }
        return found >= amountNeeded;
    }
}