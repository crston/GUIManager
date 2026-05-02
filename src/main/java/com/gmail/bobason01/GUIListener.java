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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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
        for (ItemStack cost : items) {
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory topInv = event.getView().getTopInventory();

        if (topInv.getHolder() instanceof ItemEditorHolder) {
            handleItemEditorClick(event, player);
            return;
        }

        if (topInv.getHolder() instanceof GUIHolder) {
            if (plugin.isInEditMode(player)) {
                handleGuiEditorClick(event, player);
            } else {
                event.setCancelled(true);
                handleNormalGuiClick(event, player);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory topInv = event.getView().getTopInventory();

        if (topInv.getHolder() instanceof GUIHolder) {
            if (plugin.isInEditMode(player)) {
                saveGuiLayoutAfterEdit(player);
            } else {
                event.setCancelled(true);
            }
        } else if (topInv.getHolder() instanceof ItemEditorHolder) {
            boolean buttonDragged = false;
            boolean iconDragged = false;
            for (int slot : event.getRawSlots()) {
                if (slot < topInv.getSize()) {
                    if (isButtonSlot(slot, topInv)) {
                        buttonDragged = true;
                        break;
                    }
                    if (slot == 4) {
                        iconDragged = true;
                    }
                }
            }

            if (buttonDragged) {
                event.setCancelled(true);
                return;
            }

            if (iconDragged) {
                Bukkit.getScheduler().runTask(plugin, () -> refreshEditorIcon(player, topInv));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Inventory topInv = event.getInventory();

        if (topInv.getHolder() instanceof GUIHolder && plugin.isInEditMode(player)) {
            String guiId = plugin.getEditingGuiName(player);
            GUI gui = plugin.getGui(guiId);
            if (gui != null) {
                gui.getItems().clear();
                for (int i = 0; i < topInv.getSize(); i++) {
                    ItemStack item = topInv.getItem(i);
                    if (item != null && !item.getType().isAir()) {
                        gui.setItem(i, item.clone());
                    }
                }
                plugin.saveGui(guiId);
            }
        }
    }

    private void handleItemEditorClick(InventoryClickEvent event, Player player) {
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;
        int slot = event.getSlot();
        Inventory topInv = event.getView().getTopInventory();

        if (clickedInv.equals(topInv)) {
            if (isButtonSlot(slot, topInv)) {
                event.setCancelled(true);
                processEditorButtons(event, player, slot, topInv);
            } else if (slot == 4) {
                Bukkit.getScheduler().runTask(plugin, () -> refreshEditorIcon(player, topInv));
            }
        } else {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && !clickedItem.getType().isAir()) {
                    updateEditorIcon(player, topInv, clickedItem);
                }
            }
        }
    }

    private boolean isButtonSlot(int slot, Inventory inv) {
        if (slot == 4) return false;
        ItemStack item = inv.getItem(slot);
        if (item == null || item.getType().isAir()) return false;
        return (slot >= 0 && slot <= 8) || (slot >= 9 && slot <= 44) || (slot >= 45 && slot <= 53);
    }

    private void processEditorButtons(InventoryClickEvent event, Player player, int slot, Inventory inv) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        String title = event.getView().getTitle();
        int slotIdx = title.lastIndexOf("Slot ");
        if (slotIdx == -1) return;
        String gName = title.substring(ItemEditor.TITLE_PREFIX.length(), slotIdx).trim();
        int iSlot = Integer.parseInt(title.substring(slotIdx + 5).trim());
        ItemStack currentIcon = inv.getItem(4);
        if (currentIcon == null) return;

        EditSession session = new EditSession(gName, iSlot, currentIcon.clone());

        if (slot == 8) {
            GUI gui = plugin.getGui(gName);
            if (gui != null) {
                gui.setItem(iSlot, currentIcon.clone());
                plugin.saveGui(gName);
                plugin.setEditMode(player, gName);
                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(plugin.getEditInventory(gName)));
            }
            return;
        }

        if (slot >= 45 && slot <= 53) {
            if (slot == 53) {
                session.setLorePage(event.isShiftClick() ? Math.max(0, session.getLorePage() - 1) : session.getLorePage() + 1);
                ItemEditor.updateUI(inv, session);
            } else {
                handleLoreClick(event, player, session, inv);
            }
            return;
        }

        if (slot == 13 || slot == 22 || slot == 31 || slot == 40) {
            NamespacedKey key;
            ClickType click = event.getClick();
            boolean isLeft = click == ClickType.LEFT || click == ClickType.SHIFT_LEFT;

            if (slot == 13) key = isLeft ? GUIManager.KEY_KEEP_OPEN_LEFT : GUIManager.KEY_KEEP_OPEN_SHIFT_LEFT;
            else if (slot == 22) key = isLeft ? GUIManager.KEY_KEEP_OPEN_RIGHT : GUIManager.KEY_KEEP_OPEN_SHIFT_RIGHT;
            else if (slot == 31) key = isLeft ? GUIManager.KEY_KEEP_OPEN_F : GUIManager.KEY_KEEP_OPEN_SHIFT_F;
            else key = isLeft ? GUIManager.KEY_KEEP_OPEN_Q : GUIManager.KEY_KEEP_OPEN_SHIFT_Q;

            toggleByte(session.getItem(), key);
            ItemEditor.updateUI(inv, session);
            return;
        }

        EditSession.EditType type = getEditTypeFromSlot(slot);
        if (type != null) {
            if (type.name().startsWith("COMMAND_")) {
                if (event.isShiftClick() && event.isLeftClick()) {
                    cycleExecutor(player, session, slot, inv);
                } else if (event.isShiftClick() && event.isRightClick()) {
                    removeLastCommand(player, session, slot, inv);
                } else if (event.isRightClick()) {
                    EditSession.EditType cdType = EditSession.EditType.valueOf(type.name().replace("COMMAND", "COOLDOWN"));
                    session.setEditType(cdType);
                    plugin.startChatSession(player, session);
                    player.closeInventory();
                } else {
                    session.setEditType(type);
                    plugin.startChatSession(player, session);
                    player.closeInventory();
                }
            } else {
                if (event.isRightClick() && !event.isShiftClick()) {
                    resetValue(session.getItem(), type);
                    ItemEditor.updateUI(inv, session);
                } else {
                    session.setEditType(type);
                    plugin.startChatSession(player, session);
                    player.closeInventory();
                }
            }
        }
    }

    private void cycleExecutor(Player player, EditSession session, int slot, Inventory inv) {
        EditSession.EditType baseType = getEditTypeFromSlot(slot);
        if (baseType == null) return;

        String execName = baseType.name().replace("COMMAND", "EXECUTOR");
        EditSession.EditType execType;
        try {
            execType = EditSession.EditType.valueOf(execName);
        } catch (IllegalArgumentException e) {
            return;
        }

        NamespacedKey key = ActionKeyUtil.getKeyFromType(execType);
        ItemMeta meta = session.getItem().getItemMeta();
        if (meta == null || key == null) return;

        String cur = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.STRING, "PLAYER");
        String next = cur.equals("PLAYER") ? "CONSOLE" : (cur.equals("CONSOLE") ? "OP" : "PLAYER");

        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, next);
        session.getItem().setItemMeta(meta);

        ItemEditor.updateUI(inv, session);
    }

    private void removeLastCommand(Player player, EditSession session, int slot, Inventory inv) {
        EditSession.EditType type = getEditTypeFromSlot(slot);
        if (type == null) return;
        NamespacedKey key = ActionKeyUtil.getKeyFromType(type);
        ItemMeta meta = session.getItem().getItemMeta();
        if (meta == null || key == null) return;

        String cmds = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.STRING, "None");
        if (cmds.equals("None") || cmds.isEmpty()) return;

        int lastIndex = cmds.lastIndexOf(";;");
        if (lastIndex == -1) {
            meta.getPersistentDataContainer().remove(key);
        } else {
            String newCmds = cmds.substring(0, lastIndex);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, newCmds);
        }

        session.getItem().setItemMeta(meta);
        ItemEditor.updateUI(inv, session);
    }

    private void handleLoreClick(InventoryClickEvent event, Player player, EditSession session, Inventory inv) {
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
                handleLoreRemove(session, inv);
            }
        }
    }

    private void handleLoreRemove(EditSession session, Inventory inv) {
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
        ItemEditor.updateUI(inv, session);
    }

    private void refreshEditorIcon(Player player, Inventory topInv) {
        String title = player.getOpenInventory().getTitle();
        int slotIdx = title.lastIndexOf("Slot ");
        if (slotIdx == -1) return;
        String gName = title.substring(ItemEditor.TITLE_PREFIX.length(), slotIdx).trim();
        int iSlot = Integer.parseInt(title.substring(slotIdx + 5).trim());

        ItemStack newIcon = topInv.getItem(4);
        ItemStack icon;

        if (newIcon != null && !newIcon.getType().isAir()) {
            icon = newIcon.clone();
            icon.setAmount(1);
        } else {
            icon = new ItemStack(Material.STONE);
        }

        EditSession session = new EditSession(gName, iSlot, icon);
        ItemEditor.updateUI(topInv, session);
    }

    private void updateEditorIcon(Player player, Inventory topInv, ItemStack newItem) {
        String title = player.getOpenInventory().getTitle();
        int slotIdx = title.lastIndexOf("Slot ");
        if (slotIdx == -1) return;
        String gName = title.substring(ItemEditor.TITLE_PREFIX.length(), slotIdx).trim();
        int iSlot = Integer.parseInt(title.substring(slotIdx + 5).trim());

        ItemStack icon = newItem.clone();
        icon.setAmount(1);

        EditSession session = new EditSession(gName, iSlot, icon);
        ItemEditor.updateUI(topInv, session);
    }

    private void toggleByte(ItemStack item, NamespacedKey key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        byte v = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.BYTE, (byte) 0);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) (v == 0 ? 1 : 0));
        item.setItemMeta(meta);
    }

    private void resetValue(ItemStack item, EditSession.EditType type) {
        if (type.name().equals("MATERIAL")) {
            item.setType(Material.STONE);
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        NamespacedKey key = ActionKeyUtil.getKeyFromType(type);
        if (key != null) meta.getPersistentDataContainer().remove(key);

        if (type == EditSession.EditType.ITEM_MODEL_ID) {
            meta.setCustomModelData(null);
            meta.getPersistentDataContainer().remove(GUIManager.KEY_ITEM_MODEL);
            try { meta.setItemModel(null); } catch (Throwable ignored) {}
        } else if (type == EditSession.EditType.ITEM_DAMAGE && meta instanceof Damageable) {
            ((Damageable) meta).setDamage(0);
        }
        item.setItemMeta(meta);
    }

    private void handleGuiEditorClick(InventoryClickEvent event, Player player) {
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;
        Inventory topInv = event.getView().getTopInventory();

        if (event.getClick() == ClickType.RIGHT && clickedInv.equals(topInv)) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && !clicked.getType().isAir()) {
                event.setCancelled(true);
                String guiId = plugin.getEditingGuiName(player);
                ItemEditor.open(player, new EditSession(guiId, event.getSlot(), clicked.clone()));
                return;
            }
        }

        saveGuiLayoutAfterEdit(player);
    }

    private void saveGuiLayoutAfterEdit(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Inventory topInv = player.getOpenInventory().getTopInventory();
            if (topInv.getHolder() instanceof GUIHolder) {
                String guiId = plugin.getEditingGuiName(player);
                GUI gui = plugin.getGui(guiId);
                if (gui != null) {
                    gui.getItems().clear();
                    for (int i = 0; i < topInv.getSize(); i++) {
                        ItemStack item = topInv.getItem(i);
                        if (item != null && !item.getType().isAir()) {
                            gui.setItem(i, item.clone());
                        }
                    }
                    plugin.saveGui(guiId);
                }
            }
        });
    }

    private void handleNormalGuiClick(InventoryClickEvent event, Player player) {
        String guiId = ((GUIHolder) event.getView().getTopInventory().getHolder()).getGuiId();
        actionExecutor.execute(player, guiId, event.getSlot(), event.getCurrentItem(), event.getClick());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.cleanupPlayer(event.getPlayer().getUniqueId());
    }

    private EditSession.EditType getEditTypeFromSlot(int slot) {
        switch (slot) {
            case 0: return EditSession.EditType.NAME;
            case 1: return EditSession.EditType.MATERIAL;
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