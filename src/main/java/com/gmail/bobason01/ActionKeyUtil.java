package com.gmail.bobason01;

import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class ActionKeyUtil {

    public enum KeyAction { F, SHIFT_F, Q, SHIFT_Q }

    private ActionKeyUtil() {}

    public static void copyPersistentData(PersistentDataContainer source, PersistentDataContainer target) {
        List<NamespacedKey> allKeys = getAllKeys();
        int size = allKeys.size();
        for (int i = 0; i < size; i++) {
            NamespacedKey key = allKeys.get(i);
            if (source.has(key, PersistentDataType.STRING)) {
                target.set(key, PersistentDataType.STRING, source.get(key, PersistentDataType.STRING));
            } else if (source.has(key, PersistentDataType.DOUBLE)) {
                target.set(key, PersistentDataType.DOUBLE, source.get(key, PersistentDataType.DOUBLE));
            } else if (source.has(key, PersistentDataType.BYTE)) {
                target.set(key, PersistentDataType.BYTE, source.get(key, PersistentDataType.BYTE));
            } else if (source.has(key, PersistentDataType.INTEGER)) {
                target.set(key, PersistentDataType.INTEGER, source.get(key, PersistentDataType.INTEGER));
            }
        }
    }

    public static List<NamespacedKey> getAllKeys() {
        List<NamespacedKey> keys = new ArrayList<>(80);
        keys.add(GUIManager.KEY_PERMISSION_MESSAGE);
        keys.add(GUIManager.KEY_REQUIRE_TARGET);
        keys.add(GUIManager.KEY_CUSTOM_MODEL_DATA);
        keys.add(GUIManager.KEY_ITEM_DAMAGE);
        keys.add(GUIManager.KEY_ITEM_MODEL);

        keys.add(GUIManager.KEY_COMMAND_LEFT); keys.add(GUIManager.KEY_EXECUTOR_LEFT); keys.add(GUIManager.KEY_COOLDOWN_LEFT);
        keys.add(GUIManager.KEY_MONEY_COST_LEFT); keys.add(GUIManager.KEY_COST_LEFT); keys.add(GUIManager.KEY_KEEP_OPEN_LEFT); keys.add(GUIManager.KEY_PERMISSION_LEFT);

        keys.add(GUIManager.KEY_COMMAND_SHIFT_LEFT); keys.add(GUIManager.KEY_EXECUTOR_SHIFT_LEFT); keys.add(GUIManager.KEY_COOLDOWN_SHIFT_LEFT);
        keys.add(GUIManager.KEY_MONEY_COST_SHIFT_LEFT); keys.add(GUIManager.KEY_COST_SHIFT_LEFT); keys.add(GUIManager.KEY_KEEP_OPEN_SHIFT_LEFT); keys.add(GUIManager.KEY_PERMISSION_SHIFT_LEFT);

        keys.add(GUIManager.KEY_COMMAND_RIGHT); keys.add(GUIManager.KEY_EXECUTOR_RIGHT); keys.add(GUIManager.KEY_COOLDOWN_RIGHT);
        keys.add(GUIManager.KEY_MONEY_COST_RIGHT); keys.add(GUIManager.KEY_COST_RIGHT); keys.add(GUIManager.KEY_KEEP_OPEN_RIGHT); keys.add(GUIManager.KEY_PERMISSION_RIGHT);

        keys.add(GUIManager.KEY_COMMAND_SHIFT_RIGHT); keys.add(GUIManager.KEY_EXECUTOR_SHIFT_RIGHT); keys.add(GUIManager.KEY_COOLDOWN_SHIFT_RIGHT);
        keys.add(GUIManager.KEY_MONEY_COST_SHIFT_RIGHT); keys.add(GUIManager.KEY_COST_SHIFT_RIGHT); keys.add(GUIManager.KEY_KEEP_OPEN_SHIFT_RIGHT); keys.add(GUIManager.KEY_PERMISSION_SHIFT_RIGHT);

        keys.add(GUIManager.KEY_COMMAND_F); keys.add(GUIManager.KEY_EXECUTOR_F); keys.add(GUIManager.KEY_COOLDOWN_F);
        keys.add(GUIManager.KEY_MONEY_COST_F); keys.add(GUIManager.KEY_COST_F); keys.add(GUIManager.KEY_PERMISSION_F); keys.add(GUIManager.KEY_KEEP_OPEN_F);

        keys.add(GUIManager.KEY_COMMAND_SHIFT_F); keys.add(GUIManager.KEY_EXECUTOR_SHIFT_F); keys.add(GUIManager.KEY_COOLDOWN_SHIFT_F);
        keys.add(GUIManager.KEY_MONEY_COST_SHIFT_F); keys.add(GUIManager.KEY_COST_SHIFT_F); keys.add(GUIManager.KEY_PERMISSION_SHIFT_F); keys.add(GUIManager.KEY_KEEP_OPEN_SHIFT_F);

        keys.add(GUIManager.KEY_COMMAND_Q); keys.add(GUIManager.KEY_EXECUTOR_Q); keys.add(GUIManager.KEY_COOLDOWN_Q);
        keys.add(GUIManager.KEY_MONEY_COST_Q); keys.add(GUIManager.KEY_COST_Q); keys.add(GUIManager.KEY_PERMISSION_Q); keys.add(GUIManager.KEY_KEEP_OPEN_Q);

        keys.add(GUIManager.KEY_COMMAND_SHIFT_Q); keys.add(GUIManager.KEY_EXECUTOR_SHIFT_Q); keys.add(GUIManager.KEY_COOLDOWN_SHIFT_Q);
        keys.add(GUIManager.KEY_MONEY_COST_SHIFT_Q); keys.add(GUIManager.KEY_COST_SHIFT_Q); keys.add(GUIManager.KEY_PERMISSION_SHIFT_Q); keys.add(GUIManager.KEY_KEEP_OPEN_SHIFT_Q);

        return keys;
    }

    public static NamespacedKey getCommandKey(ClickType t) {
        if (t.isShiftClick()) return t.isLeftClick() ? GUIManager.KEY_COMMAND_SHIFT_LEFT : GUIManager.KEY_COMMAND_SHIFT_RIGHT;
        return t.isLeftClick() ? GUIManager.KEY_COMMAND_LEFT : GUIManager.KEY_COMMAND_RIGHT;
    }

    public static NamespacedKey getCooldownKey(ClickType t) {
        if (t.isShiftClick()) return t.isLeftClick() ? GUIManager.KEY_COOLDOWN_SHIFT_LEFT : GUIManager.KEY_COOLDOWN_SHIFT_RIGHT;
        return t.isLeftClick() ? GUIManager.KEY_COOLDOWN_LEFT : GUIManager.KEY_COOLDOWN_RIGHT;
    }

    public static NamespacedKey getExecutorKey(ClickType t) {
        if (t.isShiftClick()) return t.isLeftClick() ? GUIManager.KEY_EXECUTOR_SHIFT_LEFT : GUIManager.KEY_EXECUTOR_SHIFT_RIGHT;
        return t.isLeftClick() ? GUIManager.KEY_EXECUTOR_LEFT : GUIManager.KEY_EXECUTOR_RIGHT;
    }

    public static NamespacedKey getKeepOpenKey(ClickType t) {
        if (t.isShiftClick()) return t.isLeftClick() ? GUIManager.KEY_KEEP_OPEN_SHIFT_LEFT : GUIManager.KEY_KEEP_OPEN_SHIFT_RIGHT;
        return t.isLeftClick() ? GUIManager.KEY_KEEP_OPEN_LEFT : GUIManager.KEY_KEEP_OPEN_RIGHT;
    }

    public static NamespacedKey getPermissionKey(ClickType t) {
        if (t.isShiftClick()) return t.isLeftClick() ? GUIManager.KEY_PERMISSION_SHIFT_LEFT : GUIManager.KEY_PERMISSION_SHIFT_RIGHT;
        return t.isLeftClick() ? GUIManager.KEY_PERMISSION_LEFT : GUIManager.KEY_PERMISSION_RIGHT;
    }

    public static NamespacedKey getItemCostKey(ClickType t) {
        if (t.isShiftClick()) return t.isLeftClick() ? GUIManager.KEY_COST_SHIFT_LEFT : GUIManager.KEY_COST_SHIFT_RIGHT;
        return t.isLeftClick() ? GUIManager.KEY_COST_LEFT : GUIManager.KEY_COST_RIGHT;
    }

    public static NamespacedKey getMoneyCostKey(ClickType t) {
        if (t.isShiftClick()) return t.isLeftClick() ? GUIManager.KEY_MONEY_COST_SHIFT_LEFT : GUIManager.KEY_MONEY_COST_SHIFT_RIGHT;
        return t.isLeftClick() ? GUIManager.KEY_MONEY_COST_LEFT : GUIManager.KEY_MONEY_COST_RIGHT;
    }

    public static NamespacedKey getCommandKey(KeyAction a) {
        switch (a) {
            case F: return GUIManager.KEY_COMMAND_F;
            case SHIFT_F: return GUIManager.KEY_COMMAND_SHIFT_F;
            case Q: return GUIManager.KEY_COMMAND_Q;
            case SHIFT_Q: return GUIManager.KEY_COMMAND_SHIFT_Q;
            default: return null;
        }
    }

    public static NamespacedKey getCooldownKey(KeyAction a) {
        switch (a) {
            case F: return GUIManager.KEY_COOLDOWN_F;
            case SHIFT_F: return GUIManager.KEY_COOLDOWN_SHIFT_F;
            case Q: return GUIManager.KEY_COOLDOWN_Q;
            case SHIFT_Q: return GUIManager.KEY_COOLDOWN_SHIFT_Q;
            default: return null;
        }
    }

    public static NamespacedKey getExecutorKey(KeyAction a) {
        switch (a) {
            case F: return GUIManager.KEY_EXECUTOR_F;
            case SHIFT_F: return GUIManager.KEY_EXECUTOR_SHIFT_F;
            case Q: return GUIManager.KEY_EXECUTOR_Q;
            case SHIFT_Q: return GUIManager.KEY_EXECUTOR_SHIFT_Q;
            default: return null;
        }
    }

    public static NamespacedKey getPermissionKey(KeyAction a) {
        switch (a) {
            case F: return GUIManager.KEY_PERMISSION_F;
            case SHIFT_F: return GUIManager.KEY_PERMISSION_SHIFT_F;
            case Q: return GUIManager.KEY_PERMISSION_Q;
            case SHIFT_Q: return GUIManager.KEY_PERMISSION_SHIFT_Q;
            default: return null;
        }
    }

    public static NamespacedKey getItemCostKey(KeyAction a) {
        switch (a) {
            case F: return GUIManager.KEY_COST_F;
            case SHIFT_F: return GUIManager.KEY_COST_SHIFT_F;
            case Q: return GUIManager.KEY_COST_Q;
            case SHIFT_Q: return GUIManager.KEY_COST_SHIFT_Q;
            default: return null;
        }
    }

    public static NamespacedKey getMoneyCostKey(KeyAction a) {
        switch (a) {
            case F: return GUIManager.KEY_MONEY_COST_F;
            case SHIFT_F: return GUIManager.KEY_MONEY_COST_SHIFT_F;
            case Q: return GUIManager.KEY_MONEY_COST_Q;
            case SHIFT_Q: return GUIManager.KEY_MONEY_COST_SHIFT_Q;
            default: return null;
        }
    }

    public static NamespacedKey getKeepOpenKey(KeyAction a) {
        switch (a) {
            case F: return GUIManager.KEY_KEEP_OPEN_F;
            case SHIFT_F: return GUIManager.KEY_KEEP_OPEN_SHIFT_F;
            case Q: return GUIManager.KEY_KEEP_OPEN_Q;
            case SHIFT_Q: return GUIManager.KEY_KEEP_OPEN_SHIFT_Q;
            default: return null;
        }
    }

    public static NamespacedKey getKeyFromType(EditSession.EditType type) {
        if (type == null) return null;
        switch (type) {
            case COMMAND_LEFT: return GUIManager.KEY_COMMAND_LEFT;
            case COST_LEFT: return GUIManager.KEY_COST_LEFT;
            case MONEY_COST_LEFT: return GUIManager.KEY_MONEY_COST_LEFT;
            case COOLDOWN_LEFT: return GUIManager.KEY_COOLDOWN_LEFT;
            case EXECUTOR_LEFT: return GUIManager.KEY_EXECUTOR_LEFT;
            case KEEP_OPEN_LEFT: return GUIManager.KEY_KEEP_OPEN_LEFT;
            case PERMISSION_LEFT: return GUIManager.KEY_PERMISSION_LEFT;

            case COMMAND_SHIFT_LEFT: return GUIManager.KEY_COMMAND_SHIFT_LEFT;
            case COST_SHIFT_LEFT: return GUIManager.KEY_COST_SHIFT_LEFT;
            case MONEY_COST_SHIFT_LEFT: return GUIManager.KEY_MONEY_COST_SHIFT_LEFT;
            case COOLDOWN_SHIFT_LEFT: return GUIManager.KEY_COOLDOWN_SHIFT_LEFT;
            case EXECUTOR_SHIFT_LEFT: return GUIManager.KEY_EXECUTOR_SHIFT_LEFT;
            case KEEP_OPEN_SHIFT_LEFT: return GUIManager.KEY_KEEP_OPEN_SHIFT_LEFT;
            case PERMISSION_SHIFT_LEFT: return GUIManager.KEY_PERMISSION_SHIFT_LEFT;

            case COMMAND_RIGHT: return GUIManager.KEY_COMMAND_RIGHT;
            case COST_RIGHT: return GUIManager.KEY_COST_RIGHT;
            case MONEY_COST_RIGHT: return GUIManager.KEY_MONEY_COST_RIGHT;
            case COOLDOWN_RIGHT: return GUIManager.KEY_COOLDOWN_RIGHT;
            case EXECUTOR_RIGHT: return GUIManager.KEY_EXECUTOR_RIGHT;
            case KEEP_OPEN_RIGHT: return GUIManager.KEY_KEEP_OPEN_RIGHT;
            case PERMISSION_RIGHT: return GUIManager.KEY_PERMISSION_RIGHT;

            case COMMAND_SHIFT_RIGHT: return GUIManager.KEY_COMMAND_SHIFT_RIGHT;
            case COST_SHIFT_RIGHT: return GUIManager.KEY_COST_SHIFT_RIGHT;
            case MONEY_COST_SHIFT_RIGHT: return GUIManager.KEY_MONEY_COST_SHIFT_RIGHT;
            case COOLDOWN_SHIFT_RIGHT: return GUIManager.KEY_COOLDOWN_SHIFT_RIGHT;
            case EXECUTOR_SHIFT_RIGHT: return GUIManager.KEY_EXECUTOR_SHIFT_RIGHT;
            case KEEP_OPEN_SHIFT_RIGHT: return GUIManager.KEY_KEEP_OPEN_SHIFT_RIGHT;
            case PERMISSION_SHIFT_RIGHT: return GUIManager.KEY_PERMISSION_SHIFT_RIGHT;

            case COMMAND_F: return GUIManager.KEY_COMMAND_F;
            case COST_F: return GUIManager.KEY_COST_F;
            case MONEY_COST_F: return GUIManager.KEY_MONEY_COST_F;
            case COOLDOWN_F: return GUIManager.KEY_COOLDOWN_F;
            case EXECUTOR_F: return GUIManager.KEY_EXECUTOR_F;
            case PERMISSION_F: return GUIManager.KEY_PERMISSION_F;

            case COMMAND_SHIFT_F: return GUIManager.KEY_COMMAND_SHIFT_F;
            case COST_SHIFT_F: return GUIManager.KEY_COST_SHIFT_F;
            case MONEY_COST_SHIFT_F: return GUIManager.KEY_MONEY_COST_SHIFT_F;
            case COOLDOWN_SHIFT_F: return GUIManager.KEY_COOLDOWN_SHIFT_F;
            case EXECUTOR_SHIFT_F: return GUIManager.KEY_EXECUTOR_SHIFT_F;
            case PERMISSION_SHIFT_F: return GUIManager.KEY_PERMISSION_SHIFT_F;

            case COMMAND_Q: return GUIManager.KEY_COMMAND_Q;
            case COST_Q: return GUIManager.KEY_COST_Q;
            case MONEY_COST_Q: return GUIManager.KEY_MONEY_COST_Q;
            case COOLDOWN_Q: return GUIManager.KEY_COOLDOWN_Q;
            case EXECUTOR_Q: return GUIManager.KEY_EXECUTOR_Q;
            case PERMISSION_Q: return GUIManager.KEY_PERMISSION_Q;

            case COMMAND_SHIFT_Q: return GUIManager.KEY_COMMAND_SHIFT_Q;
            case COST_SHIFT_Q: return GUIManager.KEY_COST_SHIFT_Q;
            case MONEY_COST_SHIFT_Q: return GUIManager.KEY_MONEY_COST_SHIFT_Q;
            case COOLDOWN_SHIFT_Q: return GUIManager.KEY_COOLDOWN_SHIFT_Q;
            case EXECUTOR_SHIFT_Q: return GUIManager.KEY_EXECUTOR_SHIFT_Q;
            case PERMISSION_SHIFT_Q: return GUIManager.KEY_PERMISSION_SHIFT_Q;

            case REQUIRE_TARGET: return GUIManager.KEY_REQUIRE_TARGET;
            case PERMISSION_MESSAGE: return GUIManager.KEY_PERMISSION_MESSAGE;
            case CUSTOM_MODEL_DATA: return GUIManager.KEY_CUSTOM_MODEL_DATA;
            case ITEM_DAMAGE: return GUIManager.KEY_ITEM_DAMAGE;
            case ITEM_MODEL_ID: return GUIManager.KEY_ITEM_MODEL;

            default: return null;
        }
    }
}