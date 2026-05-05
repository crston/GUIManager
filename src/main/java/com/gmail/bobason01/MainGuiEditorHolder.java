package com.gmail.bobason01;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
public class MainGuiEditorHolder implements InventoryHolder {
    private Inventory inventory;
    private final String guiId;
    public MainGuiEditorHolder(String guiId) { this.guiId = guiId; }
    @Override public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public String getGuiId() { return guiId; }
}