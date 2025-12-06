package com.gmail.bobason01;

import org.bukkit.inventory.ItemStack;

public class EditSession {

    private final String guiName;
    private final int slot;
    private ItemStack item;
    private EditType editType;
    private int lorePage = 0;
    private int loreLineEditIndex = -1;

    public EditSession(String guiName, int slot, ItemStack item) {
        this.guiName = guiName;
        this.slot = slot;
        this.item = item;
    }

    public String getGuiName() {
        return guiName;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public EditType getEditType() {
        return editType;
    }

    public void setEditType(EditType editType) {
        this.editType = editType;
    }

    public int getLorePage() {
        return lorePage;
    }

    public void setLorePage(int lorePage) {
        this.lorePage = lorePage;
    }

    public int getLoreLineEditIndex() {
        return loreLineEditIndex;
    }

    public void setLoreLineEditIndex(int loreLineEditIndex) {
        this.loreLineEditIndex = loreLineEditIndex;
    }

    public enum EditType {
        // General Item Settings
        NAME,
        LORE_ADD,
        LORE_EDIT,
        CUSTOM_MODEL_DATA,
        ITEM_DAMAGE,
        ITEM_MODEL_ID,
        SKULL,

        // General Options
        REQUIRE_TARGET,
        PERMISSION_MESSAGE,

        // --- Action: Left Click ---
        COMMAND_LEFT,
        EXECUTOR_LEFT,
        MONEY_COST_LEFT,
        COST_LEFT,
        KEEP_OPEN_LEFT,
        COOLDOWN_LEFT,

        // --- Action: Right Click ---
        COMMAND_RIGHT,
        EXECUTOR_RIGHT,
        MONEY_COST_RIGHT,
        COST_RIGHT,
        KEEP_OPEN_RIGHT,
        COOLDOWN_RIGHT,

        // --- Action: Shift + Left Click ---
        COMMAND_SHIFT_LEFT,
        EXECUTOR_SHIFT_LEFT,
        MONEY_COST_SHIFT_LEFT,
        COST_SHIFT_LEFT,
        KEEP_OPEN_SHIFT_LEFT,
        COOLDOWN_SHIFT_LEFT,

        // --- Action: Shift + Right Click ---
        COMMAND_SHIFT_RIGHT,
        EXECUTOR_SHIFT_RIGHT,
        MONEY_COST_SHIFT_RIGHT,
        COST_SHIFT_RIGHT,
        KEEP_OPEN_SHIFT_RIGHT,
        COOLDOWN_SHIFT_RIGHT,

        // --- Action: F (Swap Hand) ---
        COMMAND_F,
        EXECUTOR_F,
        MONEY_COST_F,
        COST_F,
        PERMISSION_F,
        COOLDOWN_F,

        // --- Action: Shift + F ---
        COMMAND_SHIFT_F,
        EXECUTOR_SHIFT_F,
        MONEY_COST_SHIFT_F,
        COST_SHIFT_F,
        PERMISSION_SHIFT_F,
        COOLDOWN_SHIFT_F,

        // --- Action: Q (Drop) ---
        COMMAND_Q,
        EXECUTOR_Q,
        MONEY_COST_Q,
        COST_Q,
        PERMISSION_Q,
        COOLDOWN_Q,

        // --- Action: Shift + Q ---
        COMMAND_SHIFT_Q,
        EXECUTOR_SHIFT_Q,
        MONEY_COST_SHIFT_Q,
        COST_SHIFT_Q,
        PERMISSION_SHIFT_Q,
        COOLDOWN_SHIFT_Q
    }
}