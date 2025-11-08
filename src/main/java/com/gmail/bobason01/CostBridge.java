package com.gmail.bobason01;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class CostBridge {

    private CostBridge() {}

    public static boolean checkAndTake(Player p,
                                       GUIListener bridge,
                                       PersistentDataContainer pdc,
                                       NamespacedKey moneyCostKey,
                                       NamespacedKey itemCostKey,
                                       double moneyOverride,
                                       String itemCostBase64) {
        if (itemCostBase64 != null && !itemCostBase64.isEmpty()) {
            try {
                ItemStack[] costs = ItemSerialization.itemStackArrayFromBase64(itemCostBase64);
                if (!bridgeHasItems(p, costs)) {
                    p.sendMessage(ChatColor.RED + "You don't have the required items.");
                    return false;
                }
            } catch (Exception e) {
                p.sendMessage(ChatColor.RED + "Error processing required items.");
                return false;
            }
        } else if (pdc != null && itemCostKey != null && pdc.has(itemCostKey, PersistentDataType.STRING)) {
            String serialized = pdc.get(itemCostKey, PersistentDataType.STRING);
            if (serialized != null && !serialized.isEmpty()) {
                try {
                    ItemStack[] costs = ItemSerialization.itemStackArrayFromBase64(serialized);
                    if (!bridgeHasItems(p, costs)) {
                        p.sendMessage(ChatColor.RED + "You don't have the required items.");
                        return false;
                    }
                } catch (Exception e) {
                    p.sendMessage(ChatColor.RED + "Error processing required items.");
                    return false;
                }
            }
        }

        double money = moneyOverride;
        if (money <= 0 && pdc != null && moneyCostKey != null) {
            money = pdc.getOrDefault(moneyCostKey, PersistentDataType.DOUBLE, 0.0);
        }

        if (GUIManager.econ != null && money > 0 && !GUIManager.econ.has(p, money)) {
            p.sendMessage(ChatColor.RED + "You don't have enough money.");
            return false;
        }

        if (itemCostBase64 != null && !itemCostBase64.isEmpty()) {
            try {
                GUIListener.removeStaticItems(p, ItemSerialization.itemStackArrayFromBase64(itemCostBase64));
            } catch (Exception ignored) {}
        } else if (pdc != null && itemCostKey != null && pdc.has(itemCostKey, PersistentDataType.STRING)) {
            String serialized = pdc.get(itemCostKey, PersistentDataType.STRING);
            if (serialized != null && !serialized.isEmpty()) {
                try {
                    GUIListener.removeStaticItems(p, ItemSerialization.itemStackArrayFromBase64(serialized));
                } catch (Exception ignored) {}
            }
        }

        if (GUIManager.econ != null && money > 0) {
            GUIManager.econ.withdrawPlayer(p, money);
        }
        return true;
    }

    private static boolean bridgeHasItems(Player p, ItemStack[] req) {
        if (req == null) return true;
        for (ItemStack r : req) {
            if (r == null) continue;
            if (!p.getInventory().containsAtLeast(r, r.getAmount())) return false;
        }
        return true;
    }
}
