package com.gmail.bobason01;

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

        fill(plugin, pdc, meta, GuiItemMeta.LEFT,
                GUIManager.KEY_COMMAND_LEFT, GUIManager.KEY_PERMISSION_LEFT, GUIManager.KEY_COST_LEFT,
                GUIManager.KEY_MONEY_COST_LEFT, GUIManager.KEY_COOLDOWN_LEFT, GUIManager.KEY_EXECUTOR_LEFT,
                GUIManager.KEY_KEEP_OPEN_LEFT);

        fill(plugin, pdc, meta, GuiItemMeta.SHIFT_LEFT,
                GUIManager.KEY_COMMAND_SHIFT_LEFT, GUIManager.KEY_PERMISSION_SHIFT_LEFT, GUIManager.KEY_COST_SHIFT_LEFT,
                GUIManager.KEY_MONEY_COST_SHIFT_LEFT, GUIManager.KEY_COOLDOWN_SHIFT_LEFT, GUIManager.KEY_EXECUTOR_SHIFT_LEFT,
                GUIManager.KEY_KEEP_OPEN_SHIFT_LEFT);

        fill(plugin, pdc, meta, GuiItemMeta.RIGHT,
                GUIManager.KEY_COMMAND_RIGHT, GUIManager.KEY_PERMISSION_RIGHT, GUIManager.KEY_COST_RIGHT,
                GUIManager.KEY_MONEY_COST_RIGHT, GUIManager.KEY_COOLDOWN_RIGHT, GUIManager.KEY_EXECUTOR_RIGHT,
                GUIManager.KEY_KEEP_OPEN_RIGHT);

        fill(plugin, pdc, meta, GuiItemMeta.SHIFT_RIGHT,
                GUIManager.KEY_COMMAND_SHIFT_RIGHT, GUIManager.KEY_PERMISSION_SHIFT_RIGHT, GUIManager.KEY_COST_SHIFT_RIGHT,
                GUIManager.KEY_MONEY_COST_SHIFT_RIGHT, GUIManager.KEY_COOLDOWN_SHIFT_RIGHT, GUIManager.KEY_EXECUTOR_SHIFT_RIGHT,
                GUIManager.KEY_KEEP_OPEN_SHIFT_RIGHT);

        fill(plugin, pdc, meta, GuiItemMeta.F,
                GUIManager.KEY_COMMAND_F, GUIManager.KEY_PERMISSION_F, GUIManager.KEY_COST_F,
                GUIManager.KEY_MONEY_COST_F, GUIManager.KEY_COOLDOWN_F, GUIManager.KEY_EXECUTOR_F,
                null);

        fill(plugin, pdc, meta, GuiItemMeta.SHIFT_F,
                GUIManager.KEY_COMMAND_SHIFT_F, GUIManager.KEY_PERMISSION_SHIFT_F, GUIManager.KEY_COST_SHIFT_F,
                GUIManager.KEY_MONEY_COST_SHIFT_F, GUIManager.KEY_COOLDOWN_SHIFT_F, GUIManager.KEY_EXECUTOR_SHIFT_F,
                null);

        fill(plugin, pdc, meta, GuiItemMeta.Q,
                GUIManager.KEY_COMMAND_Q, GUIManager.KEY_PERMISSION_Q, GUIManager.KEY_COST_Q,
                GUIManager.KEY_MONEY_COST_Q, GUIManager.KEY_COOLDOWN_Q, GUIManager.KEY_EXECUTOR_Q,
                null);

        fill(plugin, pdc, meta, GuiItemMeta.SHIFT_Q,
                GUIManager.KEY_COMMAND_SHIFT_Q, GUIManager.KEY_PERMISSION_SHIFT_Q, GUIManager.KEY_COST_SHIFT_Q,
                GUIManager.KEY_MONEY_COST_SHIFT_Q, GUIManager.KEY_COOLDOWN_SHIFT_Q, GUIManager.KEY_EXECUTOR_SHIFT_Q,
                null);

        if (pdc.getOrDefault(GUIManager.KEY_REQUIRE_TARGET, PersistentDataType.BYTE, (byte) 0) == 1) {
            for (String k : new String[]{GuiItemMeta.LEFT, GuiItemMeta.SHIFT_LEFT, GuiItemMeta.RIGHT, GuiItemMeta.SHIFT_RIGHT, GuiItemMeta.F, GuiItemMeta.SHIFT_F, GuiItemMeta.Q, GuiItemMeta.SHIFT_Q}) {
                GuiItemMeta.Variant v = meta.getOrCreate(k);
                v.requireTarget = true;
            }
        }
        return meta;
    }

    private static void fill(GUIManager plugin,
                             PersistentDataContainer pdc,
                             GuiItemMeta meta,
                             String key,
                             NamespacedKey cmd, NamespacedKey perm, NamespacedKey itemCost,
                             NamespacedKey moneyCost, NamespacedKey cooldown, NamespacedKey exec,
                             NamespacedKey keepOpen) {
        GuiItemMeta.Variant v = meta.getOrCreate(key);
        v.command = pdc.get(cmd, PersistentDataType.STRING);
        v.permission = pdc.get(perm, PersistentDataType.STRING);
        v.itemCostBase64 = pdc.get(itemCost, PersistentDataType.STRING);
        v.moneyCost = pdc.getOrDefault(moneyCost, PersistentDataType.DOUBLE, 0.0);
        v.cooldownSeconds = pdc.getOrDefault(cooldown, PersistentDataType.DOUBLE, 0.0);
        String ex = pdc.get(exec, PersistentDataType.STRING);
        v.executor = ex != null ? safeExec(ex) : GUIManager.ExecutorType.PLAYER;
        if (keepOpen != null) v.keepOpen = pdc.getOrDefault(keepOpen, PersistentDataType.BYTE, (byte) 0) == 1;
    }

    private static GUIManager.ExecutorType safeExec(String s) {
        try { return GUIManager.ExecutorType.valueOf(s); }
        catch (IllegalArgumentException e) { return GUIManager.ExecutorType.PLAYER; }
    }
}
