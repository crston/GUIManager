package com.gmail.bobason01;

import org.bukkit.inventory.ItemStack;

public class EditSession {
    private String guiName;
    private int slot;
    private ItemStack item;
    private EditType editType;
    private int lorePage = 0;
    private int loreLineEditIndex = -1;

    public enum EditType {
        NAME, MATERIAL, ITEM_FLAGS, ITEM_MODEL_ID, REQUIRE_TARGET, PERMISSION_MESSAGE, SKULL,
        CUSTOM_MODEL_DATA,
        LORE_ADD, LORE_EDIT,
        COMMAND_LEFT, COMMAND_RIGHT, COMMAND_SHIFT_LEFT, COMMAND_SHIFT_RIGHT,
        COMMAND_F, COMMAND_Q, COMMAND_SHIFT_F, COMMAND_SHIFT_Q,
        GUI_TITLE, GUI_PERMISSION, GUI_TARGETS,
        COST_LEFT, COST_RIGHT, COST_SHIFT_LEFT, COST_SHIFT_RIGHT,
        COST_F, COST_Q, COST_SHIFT_F, COST_SHIFT_Q,
        REWARD_LEFT, REWARD_RIGHT, REWARD_SHIFT_LEFT, REWARD_SHIFT_RIGHT,
        REWARD_F, REWARD_Q, REWARD_SHIFT_F, REWARD_SHIFT_Q,
        MONEY_COST_LEFT, MONEY_COST_RIGHT, MONEY_COST_SHIFT_LEFT, MONEY_COST_SHIFT_RIGHT,
        MONEY_COST_F, MONEY_COST_Q, MONEY_COST_SHIFT_F, MONEY_COST_SHIFT_Q,
        COOLDOWN_LEFT, COOLDOWN_RIGHT, COOLDOWN_SHIFT_LEFT, COOLDOWN_SHIFT_RIGHT,
        COOLDOWN_F, COOLDOWN_Q, COOLDOWN_SHIFT_F, COOLDOWN_SHIFT_Q,
        PERMISSION_LEFT, PERMISSION_RIGHT, PERMISSION_SHIFT_LEFT, PERMISSION_SHIFT_RIGHT,
        PERMISSION_F, PERMISSION_Q, PERMISSION_SHIFT_F, PERMISSION_SHIFT_Q,
        EXECUTOR_LEFT, EXECUTOR_RIGHT, EXECUTOR_SHIFT_LEFT, EXECUTOR_SHIFT_RIGHT,
        EXECUTOR_F, EXECUTOR_Q, EXECUTOR_SHIFT_F, EXECUTOR_SHIFT_Q,
        KEEP_OPEN_LEFT, KEEP_OPEN_RIGHT, KEEP_OPEN_SHIFT_LEFT, KEEP_OPEN_SHIFT_RIGHT,
        KEEP_OPEN_F, KEEP_OPEN_Q, KEEP_OPEN_SHIFT_F, KEEP_OPEN_SHIFT_Q
    }

    public EditSession(String guiName, int slot, ItemStack item) {
        this.guiName = guiName;
        this.slot = slot;
        this.item = item;
    }

    public String getGuiName() { return guiName; }
    public int getSlot() { return slot; }
    public ItemStack getItem() { return item; }
    public void setItem(ItemStack item) { this.item = item; }
    public EditType getEditType() { return editType; }
    public void setEditType(EditType editType) { this.editType = editType; }
    public int getLorePage() { return lorePage; }
    public void setLorePage(int lorePage) { this.lorePage = lorePage; }
    public int getLoreLineEditIndex() { return loreLineEditIndex; }
    public void setLoreLineEditIndex(int loreLineEditIndex) { this.loreLineEditIndex = loreLineEditIndex; }
}