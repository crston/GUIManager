package com.gmail.bobason01;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class CostBridge {

    private CostBridge() {}

    // GUIListener bridge -> GUIManager plugin 으로 변경
    public static boolean checkAndTake(Player p,
                                       GUIManager plugin,
                                       PersistentDataContainer pdc,
                                       NamespacedKey moneyCostKey,
                                       NamespacedKey itemCostKey,
                                       double moneyOverride,
                                       String itemCostBase64) {

        // 1. 아이템 비용 확인
        if (itemCostBase64 != null && !itemCostBase64.isEmpty()) {
            try {
                ItemStack[] costs = ItemSerialization.itemStackArrayFromBase64(itemCostBase64);
                if (!bridgeHasItems(p.getInventory(), costs)) {
                    p.sendMessage(plugin.getLanguageManager().getMessage("check.no_item"));
                    return false;
                }
            } catch (Exception e) {
                p.sendMessage(plugin.getLanguageManager().getMessage("check.error_item"));
                return false;
            }
        } else if (pdc != null && itemCostKey != null && pdc.has(itemCostKey, PersistentDataType.STRING)) {
            String serialized = pdc.get(itemCostKey, PersistentDataType.STRING);
            if (serialized != null && !serialized.isEmpty()) {
                try {
                    ItemStack[] costs = ItemSerialization.itemStackArrayFromBase64(serialized);
                    if (!bridgeHasItems(p.getInventory(), costs)) {
                        p.sendMessage(plugin.getLanguageManager().getMessage("check.no_item"));
                        return false;
                    }
                } catch (Exception e) {
                    p.sendMessage(plugin.getLanguageManager().getMessage("check.error_item"));
                    return false;
                }
            }
        }

        // 2. 돈 비용 확인
        double money = moneyOverride;
        if (money <= 0 && pdc != null && moneyCostKey != null) {
            money = pdc.getOrDefault(moneyCostKey, PersistentDataType.DOUBLE, 0.0);
        }

        if (GUIManager.econ != null && money > 0 && !GUIManager.econ.has(p, money)) {
            p.sendMessage(plugin.getLanguageManager().getMessage("check.no_money"));
            return false;
        }

        // 3. 아이템 차감 (수정된 부분: plugin 인자 전달)
        if (itemCostBase64 != null && !itemCostBase64.isEmpty()) {
            try {
                GUIListener.removeStaticItems(p, ItemSerialization.itemStackArrayFromBase64(itemCostBase64), plugin);
            } catch (Exception ignored) {}
        } else if (pdc != null && itemCostKey != null && pdc.has(itemCostKey, PersistentDataType.STRING)) {
            String serialized = pdc.get(itemCostKey, PersistentDataType.STRING);
            if (serialized != null && !serialized.isEmpty()) {
                try {
                    GUIListener.removeStaticItems(p, ItemSerialization.itemStackArrayFromBase64(serialized), plugin);
                } catch (Exception ignored) {}
            }
        }

        // 4. 돈 차감
        if (GUIManager.econ != null && money > 0) {
            GUIManager.econ.withdrawPlayer(p, money);
        }
        return true;
    }

    // 이름과 커스텀 모델 데이터까지 확인하는 엄격한 검사 로직 (GUIListener와 통일)
    private static boolean bridgeHasItems(Inventory inventory, ItemStack[] requiredItems) {
        if (requiredItems == null) return true;
        for (ItemStack requiredItem : requiredItems) {
            if (requiredItem == null || requiredItem.getType().isAir()) continue;

            if (!containsAtLeastStrict(inventory, requiredItem, requiredItem.getAmount())) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsAtLeastStrict(Inventory inventory, ItemStack costItem, int amountNeeded) {
        int found = 0;
        for (ItemStack invItem : inventory.getContents()) {
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
                    if (costMeta.hasCustomModelData()) {
                        if (!invMeta.hasCustomModelData() || invMeta.getCustomModelData() != costMeta.getCustomModelData()) {
                            isMatch = false;
                        }
                    }
                }
            }

            if (isMatch) {
                found += invItem.getAmount();
            }
        }
        return found >= amountNeeded;
    }
}