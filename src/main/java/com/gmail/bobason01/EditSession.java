package com.gmail.bobason01;

import org.bukkit.inventory.ItemStack;

public class EditSession {
    private final String guiName;
    private final int slot;
    private final ItemStack item;
    private EditType editType;
    private int loreLineEditIndex = -1;
    private int lorePage = 0;

    public EditSession(String guiName, int slot, ItemStack item) {
        this.guiName = guiName;
        this.slot = slot;
        this.item = item;
    }

    public String getGuiName() { return guiName; }
    public int getSlot() { return slot; }
    public ItemStack getItem() { return item; }
    public EditType getEditType() { return editType; }
    public int getLoreLineEditIndex() { return loreLineEditIndex; }
    public int getLorePage() { return lorePage; }

    public void setEditType(EditType editType) { this.editType = editType; }
    public void setLoreLineEditIndex(int index) { this.loreLineEditIndex = index; }
    public void setLorePage(int page) { this.lorePage = page; }

    public enum EditType {
        NAME, LORE_ADD, LORE_EDIT, CUSTOM_MODEL_DATA, ITEM_DAMAGE, ITEM_MODEL_ID,
        PERMISSION_MESSAGE, REQUIRE_TARGET,

        COMMAND_LEFT, PERMISSION_LEFT, COST_LEFT, MONEY_COST_LEFT,
        COMMAND_SHIFT_LEFT, PERMISSION_SHIFT_LEFT, COST_SHIFT_LEFT, MONEY_COST_SHIFT_LEFT,

        COMMAND_RIGHT, PERMISSION_RIGHT, COST_RIGHT, MONEY_COST_RIGHT,
        COMMAND_SHIFT_RIGHT, PERMISSION_SHIFT_RIGHT, COST_SHIFT_RIGHT, MONEY_COST_SHIFT_RIGHT,

        COMMAND_F, PERMISSION_F, COST_F, MONEY_COST_F,
        COMMAND_SHIFT_F, PERMISSION_SHIFT_F, COST_SHIFT_F, MONEY_COST_SHIFT_F,

        COMMAND_Q, PERMISSION_Q, COST_Q, MONEY_COST_Q,
        COMMAND_SHIFT_Q, PERMISSION_SHIFT_Q, COST_SHIFT_Q, MONEY_COST_SHIFT_Q
    }
}