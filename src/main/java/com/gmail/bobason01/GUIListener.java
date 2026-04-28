package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class GUIListener implements Listener {

    private final GUIManager plugin;
    private final ActionExecutor actionExecutor;

    public GUIListener(GUIManager plugin) {
        this.plugin = plugin;
        this.actionExecutor = new ActionExecutor(plugin, this);
    }

    public static void removeStaticItems(Player player, ItemStack[] items) {
        if (items == null || items.length == 0) return;
        Inventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < items.length; i++) {
            ItemStack cost = items[i];
            if (cost == null || cost.getType().isAir()) continue;
            int remaining = cost.getAmount();
            for (int j = 0; j < contents.length; j++) {
                ItemStack invItem = contents[j];
                if (invItem == null || invItem.getType() != cost.getType()) continue;
                if (cost.hasItemMeta()) {
                    if (!invItem.hasItemMeta() || !Bukkit.getItemFactory().equals(invItem.getItemMeta(), cost.getItemMeta())) continue;
                }
                int invAmt = invItem.getAmount();
                if (invAmt > remaining) {
                    invItem.setAmount(invAmt - remaining);
                    remaining = 0;
                    break;
                } else {
                    remaining -= invAmt;
                    inv.setItem(j, null);
                }
                if (remaining <= 0) break;
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.cleanupPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (plugin.isSettingCost(player) && title.startsWith(ItemEditor.COST_TITLE_PREFIX)) {
            event.setCancelled(false);
            return;
        }

        if (title.startsWith(ItemEditor.TITLE_PREFIX)) {
            handleItemEditorClick(event, player, title);
            return;
        }

        if (plugin.isInEditMode(player) && isGuiEditor(player, title)) {
            handleGuiEditorClick(event, player);
        } else {
            handleNormalGuiClick(event, player);
        }
    }

    private void handleItemEditorClick(InventoryClickEvent event, Player player, String title) {
        Inventory editorInv = event.getView().getTopInventory();
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(editorInv)) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && !clicked.getType().isAir()) updateAndRefreshEditor(player, title, editorInv, clicked);
            }
            return;
        }

        event.setCancelled(true);
        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();
        if (slot == 4) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                updateAndRefreshEditor(player, title, editorInv, cursor);
                event.setCursor(null);
            }
            return;
        }

        if (clickedItem == null || clickedItem.getType().isAir()) return;

        int slotIdx = title.lastIndexOf("Slot ");
        String gName = title.substring(ItemEditor.TITLE_PREFIX.length(), slotIdx).trim();
        int iSlot = Integer.parseInt(title.substring(slotIdx + 5).trim());
        ItemStack currentIcon = editorInv.getItem(4);
        if (currentIcon == null) return;

        EditSession session = new EditSession(gName, iSlot, currentIcon.clone());

        if (slot == 13 || slot == 22 || slot == 31 || slot == 40) {
            NamespacedKey key;
            if (slot == 13) key = event.isLeftClick() ? GUIManager.KEY_KEEP_OPEN_LEFT : GUIManager.KEY_KEEP_OPEN_SHIFT_LEFT;
            else if (slot == 22) key = event.isLeftClick() ? GUIManager.KEY_KEEP_OPEN_RIGHT : GUIManager.KEY_KEEP_OPEN_SHIFT_RIGHT;
            else if (slot == 31) key = event.isLeftClick() ? GUIManager.KEY_KEEP_OPEN_F : GUIManager.KEY_KEEP_OPEN_SHIFT_F;
            else key = event.isLeftClick() ? GUIManager.KEY_KEEP_OPEN_Q : GUIManager.KEY_KEEP_OPEN_SHIFT_Q;

            toggleByte(session.getItem(), key, player, session);
            return;
        }

        if (slot == 8) {
            GUI gui = plugin.getGui(session.getGuiName());
            if (gui != null) {
                gui.setItem(session.getSlot(), currentIcon);
                plugin.saveGui(session.getGuiName());
                plugin.setEditMode(player, session.getGuiName());
                player.openInventory(gui.getInventory());
            }
            return;
        }

        if (slot == 53) {
            session.setLorePage(event.isShiftClick() ? Math.max(0, session.getLorePage() - 1) : session.getLorePage() + 1);
            ItemEditor.open(player, session);
            return;
        }

        if (slot >= 45 && slot <= 52) {
            handleLoreClick(event, player, session);
            return;
        }

        EditSession.EditType type = getEditTypeFromSlot(slot);
        if (type == null) return;

        if (event.isRightClick() && !event.isShiftClick()) {
            handleValueReset(player, session, type);
            return;
        }

        session.setEditType(type);
        String typeName = type.name();
        if (typeName.indexOf("COST") != -1 && typeName.indexOf("MONEY") == -1) {
            openItemCostEditor(player, session);
        } else if (type == EditSession.EditType.REQUIRE_TARGET) {
            toggleByte(session.getItem(), ActionKeyUtil.getKeyFromType(type), player, session);
        } else if (clickedItem.getType() == Material.COMMAND_BLOCK && event.isShiftClick()) {
            cycleExecutor(player, session, slot);
        } else {
            plugin.startChatSession(player, session);
            player.closeInventory();
        }
    }

    private void handleValueReset(Player player, EditSession session, EditSession.EditType type) {
        ItemMeta meta = session.getItem().getItemMeta();
        if (meta == null) return;
        NamespacedKey key = ActionKeyUtil.getKeyFromType(type);
        if (key != null) meta.getPersistentDataContainer().remove(key);
        if (type == EditSession.EditType.CUSTOM_MODEL_DATA) meta.setCustomModelData(null);
        else if (type == EditSession.EditType.ITEM_DAMAGE && meta instanceof Damageable) ((Damageable) meta).setDamage(0);
        else if (type == EditSession.EditType.ITEM_MODEL_ID) {
            try { meta.setItemModel(null); } catch (Throwable ignored) {}
        }
        session.getItem().setItemMeta(meta);
        ItemEditor.open(player, session);
    }

    private void updateAndRefreshEditor(Player player, String title, Inventory editorInv, ItemStack sourceItem) {
        ItemStack currentIcon = editorInv.getItem(4);
        if (currentIcon == null) return;
        ItemStack newIcon = sourceItem.clone();
        newIcon.setAmount(1);
        ItemMeta curM = currentIcon.getItemMeta();
        ItemMeta srcM = newIcon.getItemMeta();
        if (curM != null && srcM != null) {
            if (curM.hasDisplayName()) srcM.setDisplayName(curM.getDisplayName());
            if (curM.hasLore()) srcM.setLore(curM.getLore());
            ActionKeyUtil.copyPersistentData(curM.getPersistentDataContainer(), srcM.getPersistentDataContainer());
        }
        newIcon.setItemMeta(srcM);
        int slotIdx = title.lastIndexOf("Slot ");
        String gName = title.substring(ItemEditor.TITLE_PREFIX.length(), slotIdx).trim();
        int iSlot = Integer.parseInt(title.substring(slotIdx + 5).trim());
        ItemEditor.open(player, new EditSession(gName, iSlot, newIcon));
    }

    private void handleGuiEditorClick(InventoryClickEvent event, Player player) {
        Inventory top = event.getView().getTopInventory();
        event.setCancelled(true);
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            ItemStack cursor = player.getItemOnCursor();
            GUI gui = plugin.getGui(plugin.getEditingGuiName(player));
            if (gui != null) {
                if (cursor != null && !cursor.getType().isAir()) {
                    ItemStack ni = cursor.clone(); ni.setAmount(1);
                    gui.setItem(event.getSlot(), ni);
                    top.setItem(event.getSlot(), ni);
                } else {
                    gui.getItems().remove(event.getSlot());
                    top.setItem(event.getSlot(), null);
                }
                plugin.saveGui(gui.getId());
            }
        } else if (event.isRightClick()) {
            ItemStack item = event.getCurrentItem();
            if (item != null && !item.getType().isAir()) {
                ItemEditor.open(player, new EditSession(plugin.getEditingGuiName(player), event.getSlot(), item.clone()));
            }
        }
    }

    private void handleNormalGuiClick(InventoryClickEvent event, Player player) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof GUIHolder) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType().isAir()) return;
            actionExecutor.execute(player, ((GUIHolder) top.getHolder()).getGuiId(), event.getSlot(), item, event.getClick());
        }
    }

    private void handleLoreClick(InventoryClickEvent event, Player player, EditSession session) {
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        if (item.getType() == Material.WRITABLE_BOOK) {
            session.setEditType(EditSession.EditType.LORE_ADD);
            plugin.startChatSession(player, session);
            player.closeInventory();
        } else if (item.getType() == Material.PAPER) {
            session.setLoreLineEditIndex((session.getLorePage() * 7) + (event.getSlot() - 46));
            if (event.isLeftClick()) {
                session.setEditType(EditSession.EditType.LORE_EDIT);
                plugin.startChatSession(player, session);
                player.closeInventory();
            } else if (event.isRightClick()) {
                handleLoreRemove(player, session);
            }
        }
    }

    private void handleLoreRemove(Player player, EditSession session) {
        ItemMeta meta = session.getItem().getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = new ArrayList<>(meta.getLore());
            int idx = session.getLoreLineEditIndex();
            if (idx >= 0 && idx < lore.size()) {
                lore.remove(idx);
                meta.setLore(lore.isEmpty() ? null : lore);
                session.getItem().setItemMeta(meta);
            }
        }
        ItemEditor.open(player, session);
    }

    private void openItemCostEditor(Player player, EditSession session) {
        plugin.startCostSession(player, session);
        player.openInventory(Bukkit.createInventory(null, 54, ItemEditor.COST_TITLE_PREFIX + session.getEditType().name()));
    }

    private void toggleByte(ItemStack item, NamespacedKey key, Player player, EditSession session) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        byte v = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.BYTE, (byte) 0);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) (v == 0 ? 1 : 0));
        item.setItemMeta(meta);
        ItemEditor.open(player, session);
    }

    private void cycleExecutor(Player player, EditSession session, int slot) {
        EditSession.EditType base = getEditTypeFromSlot(slot);
        if (base == null) return;
        NamespacedKey key = ActionKeyUtil.getKeyFromType(EditSession.EditType.valueOf(base.name().replace("COMMAND", "EXECUTOR")));
        ItemMeta meta = session.getItem().getItemMeta();
        if (meta == null) return;
        String cur = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.STRING, "PLAYER");
        String next = cur.equals("PLAYER") ? "CONSOLE" : (cur.equals("CONSOLE") ? "OP" : "PLAYER");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, next);
        session.getItem().setItemMeta(meta);
        ItemEditor.open(player, session);
    }

    private boolean isGuiEditor(Player p, String title) {
        String id = plugin.getEditingGuiName(p);
        return id != null && plugin.getGui(id).getTitle().equals(title);
    }

    private EditSession.EditType getEditTypeFromSlot(int slot) {
        switch (slot) {
            case 0: return EditSession.EditType.NAME;
            case 1: return EditSession.EditType.CUSTOM_MODEL_DATA;
            case 2: return EditSession.EditType.ITEM_DAMAGE;
            case 3: return EditSession.EditType.ITEM_MODEL_ID;
            case 5: return EditSession.EditType.REQUIRE_TARGET;
            case 6: return EditSession.EditType.PERMISSION_MESSAGE;
            case 7: return EditSession.EditType.SKULL;
            case 9: return EditSession.EditType.COMMAND_LEFT;
            case 10: return EditSession.EditType.MONEY_COST_LEFT;
            case 11: return EditSession.EditType.COST_LEFT;
            case 12: return EditSession.EditType.PERMISSION_LEFT;
            case 14: return EditSession.EditType.COMMAND_SHIFT_LEFT;
            case 15: return EditSession.EditType.MONEY_COST_SHIFT_LEFT;
            case 16: return EditSession.EditType.COST_SHIFT_LEFT;
            case 17: return EditSession.EditType.PERMISSION_SHIFT_LEFT;
            case 18: return EditSession.EditType.COMMAND_RIGHT;
            case 19: return EditSession.EditType.MONEY_COST_RIGHT;
            case 20: return EditSession.EditType.COST_RIGHT;
            case 21: return EditSession.EditType.PERMISSION_RIGHT;
            case 23: return EditSession.EditType.COMMAND_SHIFT_RIGHT;
            case 24: return EditSession.EditType.MONEY_COST_SHIFT_RIGHT;
            case 25: return EditSession.EditType.COST_SHIFT_RIGHT;
            case 26: return EditSession.EditType.PERMISSION_SHIFT_RIGHT;
            case 27: return EditSession.EditType.COMMAND_F;
            case 28: return EditSession.EditType.MONEY_COST_F;
            case 29: return EditSession.EditType.COST_F;
            case 30: return EditSession.EditType.PERMISSION_F;
            case 32: return EditSession.EditType.COMMAND_SHIFT_F;
            case 33: return EditSession.EditType.MONEY_COST_SHIFT_F;
            case 34: return EditSession.EditType.COST_SHIFT_F;
            case 35: return EditSession.EditType.PERMISSION_SHIFT_F;
            case 36: return EditSession.EditType.COMMAND_Q;
            case 37: return EditSession.EditType.MONEY_COST_Q;
            case 38: return EditSession.EditType.COST_Q;
            case 39: return EditSession.EditType.PERMISSION_Q;
            case 41: return EditSession.EditType.COMMAND_SHIFT_Q;
            case 42: return EditSession.EditType.MONEY_COST_SHIFT_Q;
            case 43: return EditSession.EditType.COST_SHIFT_Q;
            case 44: return EditSession.EditType.PERMISSION_SHIFT_Q;
            default: return null;
        }
    }
}