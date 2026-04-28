package com.gmail.bobason01;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class CostBridge {

    private CostBridge() {}

    public static boolean checkAndTake(Player p, GUIManager plugin, double moneyCost, ItemStack[] parsedCosts) {
        // 1. 아이템 비용 보유 여부 선확인
        if (parsedCosts != null && parsedCosts.length > 0) {
            if (!bridgeHasItems(p.getInventory(), parsedCosts)) {
                p.sendMessage(plugin.getLanguageManager().getMessage("check.no_item"));
                return false;
            }
        }

        // 2. 돈 비용 보유 여부 선확인 (Vault)
        if (GUIManager.econ != null && moneyCost > 0) {
            if (!GUIManager.econ.has(p, moneyCost)) {
                p.sendMessage(plugin.getLanguageManager().getMessage("check.no_money"));
                return false;
            }
        }

        // 3. 실제 비용 차감 (아이템)
        if (parsedCosts != null && parsedCosts.length > 0) {
            try {
                // 이 메서드는 내부적으로 GUIListener.removeStaticItems 를 호출하여
                // 수량이 0이 된 슬롯을 null 처리하도록 패치된 로직을 사용합니다.
                GUIListener.removeStaticItems(p, parsedCosts, plugin);
            } catch (Exception ignored) {}
        }

        // 4. 실제 비용 차감 (돈)
        if (GUIManager.econ != null && moneyCost > 0) {
            GUIManager.econ.withdrawPlayer(p, moneyCost);
        }

        return true;
    }

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
        ItemStack[] contents = inventory.getContents();

        for (ItemStack invItem : contents) {
            if (invItem == null || invItem.getType() != costItem.getType()) continue;

            boolean isMatch = true;
            if (costItem.hasItemMeta()) {
                if (!invItem.hasItemMeta()) {
                    isMatch = false;
                } else {
                    ItemMeta costMeta = costItem.getItemMeta();
                    ItemMeta invMeta = invItem.getItemMeta();

                    // 표시 이름 비교
                    if (costMeta.hasDisplayName()) {
                        if (!invMeta.hasDisplayName() || !invMeta.getDisplayName().equals(costMeta.getDisplayName())) {
                            isMatch = false;
                        }
                    }

                    // 커스텀 모델 데이터 비교 (필수 성능 요소)
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
        }
        return found >= amountNeeded;
    }
}