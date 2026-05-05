package com.gmail.bobason01;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import java.util.List;
public class ItemCostRewardHolder implements InventoryHolder {
    private Inventory inventory;
    private final EditSession session;
    private final NamespacedKey targetKey;
    private final String type;
    private int page;
    private final List<ItemStack> allItems;
    public ItemCostRewardHolder(EditSession session, NamespacedKey targetKey, String type, int page, List<ItemStack> allItems) {
        this.session = session;
        this.targetKey = targetKey;
        this.type = type;
        this.page = page;
        this.allItems = allItems;
    }
    @Override public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public EditSession getSession() { return session; }
    public NamespacedKey getTargetKey() { return targetKey; }
    public String getType() { return type; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public List<ItemStack> getAllItems() { return allItems; }
}