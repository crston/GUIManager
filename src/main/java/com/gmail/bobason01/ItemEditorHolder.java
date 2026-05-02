package com.gmail.bobason01;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ItemEditorHolder implements InventoryHolder {
    private Inventory inventory;

    @Override
    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
}