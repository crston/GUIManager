package com.gmail.bobason01;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;

public final class GUIListenerFastHook {

    private GUIListenerFastHook() {}

    public static boolean checkAndTakeCostsFast(GUIListener origin,
                                                Player player,
                                                PersistentDataContainer pdc,
                                                NamespacedKey moneyCostKey,
                                                NamespacedKey itemCostKey,
                                                double money,
                                                String itemCostBase64) {
        return CostBridge.checkAndTake(player, origin, pdc, moneyCostKey, itemCostKey, money, itemCostBase64);
    }
}
