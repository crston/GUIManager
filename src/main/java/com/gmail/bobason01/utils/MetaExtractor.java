package com.gmail.bobason01.utils;

import com.gmail.bobason01.GUIManager;
import com.gmail.bobason01.GuiItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class MetaExtractor {

    private MetaExtractor() {}

    public static GuiItemMeta extract(GUIManager plugin, ItemStack item) {
        GuiItemMeta meta = new GuiItemMeta();
        if (item == null || !item.hasItemMeta()) return meta;
        ItemMeta im = item.getItemMeta();
        if (im == null) return meta;
        PersistentDataContainer pdc = im.getPersistentDataContainer();

        meta.setMaterialSnapshot(item.getType());

        fill(pdc, meta, GuiItemMeta.LEFT,
                GUIManager.KEY_COMMAND_LEFT, GUIManager.KEY_PERMISSION_LEFT, GUIManager.KEY_COST_LEFT, GUIManager.KEY_REWARD_LEFT,
                GUIManager.KEY_MONEY_COST_LEFT, GUIManager.KEY_COOLDOWN_LEFT, GUIManager.KEY_EXECUTOR_LEFT,
                GUIManager.KEY_KEEP_OPEN_LEFT);

        fill(pdc, meta, GuiItemMeta.SHIFT_LEFT,
                GUIManager.KEY_COMMAND_SHIFT_LEFT, GUIManager.KEY_PERMISSION_SHIFT_LEFT, GUIManager.KEY_COST_SHIFT_LEFT, GUIManager.KEY_REWARD_SHIFT_LEFT,
                GUIManager.KEY_MONEY_COST_SHIFT_LEFT, GUIManager.KEY_COOLDOWN_SHIFT_LEFT, GUIManager.KEY_EXECUTOR_SHIFT_LEFT,
                GUIManager.KEY_KEEP_OPEN_SHIFT_LEFT);

        fill(pdc, meta, GuiItemMeta.RIGHT,
                GUIManager.KEY_COMMAND_RIGHT, GUIManager.KEY_PERMISSION_RIGHT, GUIManager.KEY_COST_RIGHT, GUIManager.KEY_REWARD_RIGHT,
                GUIManager.KEY_MONEY_COST_RIGHT, GUIManager.KEY_COOLDOWN_RIGHT, GUIManager.KEY_EXECUTOR_RIGHT,
                GUIManager.KEY_KEEP_OPEN_RIGHT);

        fill(pdc, meta, GuiItemMeta.SHIFT_RIGHT,
                GUIManager.KEY_COMMAND_SHIFT_RIGHT, GUIManager.KEY_PERMISSION_SHIFT_RIGHT, GUIManager.KEY_COST_SHIFT_RIGHT, GUIManager.KEY_REWARD_SHIFT_RIGHT,
                GUIManager.KEY_MONEY_COST_SHIFT_RIGHT, GUIManager.KEY_COOLDOWN_SHIFT_RIGHT, GUIManager.KEY_EXECUTOR_SHIFT_RIGHT,
                GUIManager.KEY_KEEP_OPEN_SHIFT_RIGHT);

        fill(pdc, meta, GuiItemMeta.F,
                GUIManager.KEY_COMMAND_F, GUIManager.KEY_PERMISSION_F, GUIManager.KEY_COST_F, GUIManager.KEY_REWARD_F,
                GUIManager.KEY_MONEY_COST_F, GUIManager.KEY_COOLDOWN_F, GUIManager.KEY_EXECUTOR_F, null);

        fill(pdc, meta, GuiItemMeta.SHIFT_F,
                GUIManager.KEY_COMMAND_SHIFT_F, GUIManager.KEY_PERMISSION_SHIFT_F, GUIManager.KEY_COST_SHIFT_F, GUIManager.KEY_REWARD_SHIFT_F,
                GUIManager.KEY_MONEY_COST_SHIFT_F, GUIManager.KEY_COOLDOWN_SHIFT_F, GUIManager.KEY_EXECUTOR_SHIFT_F, null);

        fill(pdc, meta, GuiItemMeta.Q,
                GUIManager.KEY_COMMAND_Q, GUIManager.KEY_PERMISSION_Q, GUIManager.KEY_COST_Q, GUIManager.KEY_REWARD_Q,
                GUIManager.KEY_MONEY_COST_Q, GUIManager.KEY_COOLDOWN_Q, GUIManager.KEY_EXECUTOR_Q, null);

        fill(pdc, meta, GuiItemMeta.SHIFT_Q,
                GUIManager.KEY_COMMAND_SHIFT_Q, GUIManager.KEY_PERMISSION_SHIFT_Q, GUIManager.KEY_COST_SHIFT_Q, GUIManager.KEY_REWARD_SHIFT_Q,
                GUIManager.KEY_MONEY_COST_SHIFT_Q, GUIManager.KEY_COOLDOWN_SHIFT_Q, GUIManager.KEY_EXECUTOR_SHIFT_Q, null);

        if (getLegacyOrInt(pdc, GUIManager.KEY_REQUIRE_TARGET) == 1) {
            String[] keys = {GuiItemMeta.LEFT, GuiItemMeta.SHIFT_LEFT, GuiItemMeta.RIGHT, GuiItemMeta.SHIFT_RIGHT, GuiItemMeta.F, GuiItemMeta.SHIFT_F, GuiItemMeta.Q, GuiItemMeta.SHIFT_Q};
            for (int i = 0; i < keys.length; i++) {
                meta.getOrCreate(keys[i]).requireTarget = true;
            }
        }
        return meta;
    }

    private static void fill(PersistentDataContainer pdc,
                             GuiItemMeta meta,
                             String key,
                             NamespacedKey cmd, NamespacedKey perm, NamespacedKey itemCost, NamespacedKey itemReward,
                             NamespacedKey moneyCost, NamespacedKey cooldown, NamespacedKey exec,
                             NamespacedKey keepOpen) {
        GuiItemMeta.Variant v = meta.getOrCreate(key);
        v.command = pdc.get(cmd, PersistentDataType.STRING);
        v.permission = pdc.get(perm, PersistentDataType.STRING);
        v.moneyCost = pdc.getOrDefault(moneyCost, PersistentDataType.DOUBLE, 0.0);
        v.cooldownSeconds = pdc.getOrDefault(cooldown, PersistentDataType.DOUBLE, 0.0);

        String costBase64 = pdc.get(itemCost, PersistentDataType.STRING);
        v.itemCostBase64 = costBase64;
        if (costBase64 != null && !costBase64.isEmpty()) {
            try { v.parsedItemCosts = ItemSerialization.itemStackArrayFromBase64(costBase64); }
            catch (Exception e) { v.parsedItemCosts = new ItemStack[0]; }
        } else { v.parsedItemCosts = new ItemStack[0]; }

        String rewardBase64 = pdc.get(itemReward, PersistentDataType.STRING);
        v.itemRewardBase64 = rewardBase64;
        if (rewardBase64 != null && !rewardBase64.isEmpty()) {
            try { v.parsedItemRewards = ItemSerialization.itemStackArrayFromBase64(rewardBase64); }
            catch (Exception e) { v.parsedItemRewards = new ItemStack[0]; }
        } else { v.parsedItemRewards = new ItemStack[0]; }

        String ex = pdc.get(exec, PersistentDataType.STRING);
        v.executor = ex != null ? safeExec(ex) : GUIManager.ExecutorType.PLAYER;

        if (keepOpen != null) {
            v.keepOpen = getLegacyOrInt(pdc, keepOpen) == 1;
        }
    }

    private static int getLegacyOrInt(PersistentDataContainer pdc, NamespacedKey key) {
        if (pdc.has(key, PersistentDataType.INTEGER)) {
            return pdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
        } else if (pdc.has(key, PersistentDataType.BYTE)) {
            return pdc.getOrDefault(key, PersistentDataType.BYTE, (byte) 0);
        }
        return 0;
    }

    private static GUIManager.ExecutorType safeExec(String s) {
        try { return GUIManager.ExecutorType.valueOf(s); }
        catch (IllegalArgumentException e) { return GUIManager.ExecutorType.PLAYER; }
    }
}