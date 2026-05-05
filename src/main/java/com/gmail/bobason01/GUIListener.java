package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
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

        if (topInv.getHolder() instanceof MainGuiEditorHolder) {
            event.setCancelled(true);
            Inventory clickedInv = event.getClickedInventory();
            if (clickedInv != null && clickedInv.equals(topInv)) {
                MainGuiEditorHolder holder = (MainGuiEditorHolder) topInv.getHolder();
                String guiId = holder.getGuiId();
                GUI gui = plugin.getGui(guiId);
                if (gui == null) return;

                int slot = event.getSlot();
                if (slot == 11) {
                    EditSession session = new EditSession(guiId, -1, null);
                    session.setEditType(EditSession.EditType.GUI_TITLE);
                    plugin.startChatSession(player, session);
                    player.closeInventory();
                } else if (slot == 13) {
                    int rows = gui.getSize() / 9;
                    if (event.isLeftClick() && rows < 6) rows++;
                    else if (event.isRightClick() && rows > 1) rows--;
                    gui.setSize(rows * 9);
                    plugin.saveGui(guiId);
                    MainGuiEditor.open(player, gui);
                } else if (slot == 15) {
                    EditSession session = new EditSession(guiId, -1, null);
                    session.setEditType(EditSession.EditType.GUI_PERMISSION);
                    plugin.startChatSession(player, session);
                    player.closeInventory();
                } else if (slot == 29) {
                    EditSession session = new EditSession(guiId, -1, null);
                    session.setEditType(EditSession.EditType.GUI_TARGETS);
                    plugin.startChatSession(player, session);
                    player.closeInventory();
                } else if (slot == 33) {
                    plugin.setEditMode(player, guiId);
                    player.openInventory(plugin.getEditInventory(guiId));
                }
            }
            return;
        }

        if (topInv.getHolder() instanceof ItemCostRewardHolder) {
            Inventory clickedInv = event.getClickedInventory();
            ItemCostRewardHolder holder = (ItemCostRewardHolder) topInv.getHolder();

            if (clickedInv != null && clickedInv.equals(topInv)) {
                int slot = event.getSlot();
                if (slot >= 36) {
                    event.setCancelled(true);
                    if (slot == 48 && holder.getPage() > 0) {
                        syncToAllItems(topInv, holder.getPage(), holder.getAllItems());
                        holder.setPage(holder.getPage() - 1);
                        refreshVirtualGUI(topInv, holder);
                    } else if (slot == 49) {
                        Bukkit.getScheduler().runTask(plugin, player::closeInventory);
                    } else if (slot == 50) {
                        syncToAllItems(topInv, holder.getPage(), holder.getAllItems());
                        holder.setPage(holder.getPage() + 1);
                        refreshVirtualGUI(topInv, holder);
                    }
                }
            } else if (event.isShiftClick()) {
                int firstEmpty = -1;
                for (int i = 0; i < 36; i++) {
                    if (topInv.getItem(i) == null || topInv.getItem(i).getType().isAir()) {
                        firstEmpty = i;
                        break;
                    }
                }
                if (firstEmpty == -1) {
                    event.setCancelled(true);
                }
            }
            return;
        }

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

        if (topInv.getHolder() instanceof MainGuiEditorHolder) {
            event.setCancelled(true);
            return;
        }

        if (topInv.getHolder() instanceof ItemCostRewardHolder) {
            for (int slot : event.getRawSlots()) {
                if (slot >= 36 && slot < topInv.getSize()) {
                    event.setCancelled(true);
                    break;
                }
            }
            return;
        }

        if (topInv.getHolder() instanceof GUIHolder) {
            if (plugin.isInEditMode(player)) {
                saveGuiLayoutAfterEdit(player);
            } else {
                event.setCancelled(true);
            }
        } else if (topInv.getHolder() instanceof ItemEditorHolder) {
            boolean buttonOrIconDragged = false;
            for (int slot : event.getRawSlots()) {
                if (slot < topInv.getSize() && (isButtonSlot(slot, topInv) || slot == 4)) {
                    buttonOrIconDragged = true;
                    break;
                }
            }
            if (buttonOrIconDragged) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Inventory topInv = event.getInventory();

        if (topInv.getHolder() instanceof ItemCostRewardHolder) {
            ItemCostRewardHolder holder = (ItemCostRewardHolder) topInv.getHolder();
            EditSession session = holder.getSession();
            NamespacedKey key = holder.getTargetKey();

            syncToAllItems(topInv, holder.getPage(), holder.getAllItems());

            List<ItemStack> finalItems = new ArrayList<>();
            for (ItemStack item : holder.getAllItems()) {
                if (item != null && !item.getType().isAir()) {
                    finalItems.add(item);
                }
            }

            ItemMeta meta = session.getItem().getItemMeta();
            if (meta != null) {
                if (finalItems.isEmpty()) {
                    meta.getPersistentDataContainer().remove(key);
                } else {
                    try {
                        String base64 = ItemSerialization.itemStackArrayToBase64(finalItems.toArray(new ItemStack[0]));
                        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, base64);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to save items");
                    }
                }
                session.getItem().setItemMeta(meta);
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                ItemEditor.open(player, session);
            });
            return;
        }

        if (topInv.getHolder() instanceof GUIHolder && plugin.isInEditMode(player)) {
            String guiId = plugin.getEditingGuiName(player);
            GUI gui = plugin.getGui(guiId);
            if (gui != null) {
                // 인벤토리 크기만큼만 순회하여 보이지 않는 슬롯의 데이터 유실 방지
                for (int i = 0; i < topInv.getSize(); i++) {
                    ItemStack item = topInv.getItem(i);
                    if (item == null || item.getType().isAir()) {
                        gui.getItems().remove(i);
                    } else {
                        gui.setItem(i, item.clone());
                    }
                }
                plugin.saveGui(guiId);
            }
            plugin.removeEditMode(player);
        }
    }

    private void syncToAllItems(Inventory inv, int page, List<ItemStack> allItems) {
        int start = page * 36;
        for(int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            int targetIdx = start + i;
            while(allItems.size() <= targetIdx) allItems.add(null);
            allItems.set(targetIdx, item == null || item.getType().isAir() ? null : item.clone());
        }
        while(!allItems.isEmpty() && allItems.get(allItems.size()-1) == null) {
            allItems.remove(allItems.size()-1);
        }
    }

    private void refreshVirtualGUI(Inventory inv, ItemCostRewardHolder holder) {
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, null);
        }
        int start = holder.getPage() * 36;
        List<ItemStack> allItems = holder.getAllItems();
        for (int i = 0; i < 36; i++) {
            if (start + i < allItems.size()) {
                inv.setItem(i, allItems.get(start + i));
            }
        }

        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta pMeta = prev.getItemMeta();
        pMeta.setDisplayName("§ePrevious Page");
        prev.setItemMeta(pMeta);
        inv.setItem(48, holder.getPage() > 0 ? prev : null);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nMeta = next.getItemMeta();
        nMeta.setDisplayName("§eNext Page");
        next.setItemMeta(nMeta);
        inv.setItem(50, next);
    }

    private void openVirtualGUI(Player player, EditSession session, NamespacedKey targetKey, String type) {
        player.closeInventory();

        List<ItemStack> allItems = new ArrayList<>();
        ItemMeta sessionMeta = session.getItem().getItemMeta();
        if (sessionMeta != null && sessionMeta.getPersistentDataContainer().has(targetKey, PersistentDataType.STRING)) {
            String base64 = sessionMeta.getPersistentDataContainer().get(targetKey, PersistentDataType.STRING);
            try {
                ItemStack[] items = ItemSerialization.itemStackArrayFromBase64(base64);
                allItems.addAll(Arrays.asList(items));
            } catch (Exception ignored) {}
        }

        ItemCostRewardHolder holder = new ItemCostRewardHolder(session, targetKey, type, 0, allItems);
        Inventory inv = Bukkit.createInventory(holder, 54, "§8Set " + type + " Items");
        holder.setInventory(inv);

        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.setDisplayName(" ");
            pane.setItemMeta(paneMeta);
        }
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, pane);
        }

        ItemStack backBtn = new ItemStack(Material.OAK_DOOR);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cBack");
            backBtn.setItemMeta(backMeta);
        }
        inv.setItem(49, backBtn);

        refreshVirtualGUI(inv, holder);
        player.openInventory(inv);
    }

    private String getGuiNameFromTitle(String title) {
        String plain = ChatColor.stripColor(title);
        int idx = plain.lastIndexOf(" Slot ");
        if (idx != -1 && plain.startsWith("[Item Editor] ")) {
            return plain.substring(14, idx).trim();
        }
        return null;
    }

    private int getSlotFromTitle(String title) {
        String plain = ChatColor.stripColor(title);
        int idx = plain.lastIndexOf(" Slot ");
        if (idx != -1) {
            try {
                return Integer.parseInt(plain.substring(idx + 6).trim());
            } catch (Exception ignored) {}
        }
        return -1;
    }

    private void handleItemEditorClick(InventoryClickEvent event, Player player) {
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        int slot = event.getSlot();
        Inventory topInv = event.getView().getTopInventory();

        if (clickedInv.equals(topInv)) {
            if (isButtonSlot(slot, topInv)) {
                event.setCancelled(true);
                processEditorButtons(event, player, slot, topInv);
            } else if (slot == 4) {
                event.setCancelled(true);
                ItemStack cursor = event.getCursor();
                if (cursor != null && !cursor.getType().isAir()) {
                    absorbIconItem(player, topInv, cursor);
                }
            }
        } else {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && !clickedItem.getType().isAir()) {
                    absorbIconItem(player, topInv, clickedItem);
                }
            }
        }
    }

    private void absorbIconItem(Player player, Inventory topInv, ItemStack source) {
        ItemStack currentIcon = topInv.getItem(4);
        if (currentIcon == null || source == null || source.getType().isAir()) return;

        currentIcon.setType(source.getType());
        ItemMeta curMeta = currentIcon.getItemMeta();
        ItemMeta sourceMeta = source.getItemMeta();

        if (curMeta != null && sourceMeta != null) {
            if (sourceMeta.hasCustomModelData()) {
                curMeta.setCustomModelData(sourceMeta.getCustomModelData());
                curMeta.getPersistentDataContainer().set(GUIManager.KEY_CUSTOM_MODEL_DATA, PersistentDataType.INTEGER, sourceMeta.getCustomModelData());
            } else {
                curMeta.setCustomModelData(null);
                curMeta.getPersistentDataContainer().remove(GUIManager.KEY_CUSTOM_MODEL_DATA);
            }
            currentIcon.setItemMeta(curMeta);
        }

        topInv.setItem(4, currentIcon);

        String title = player.getOpenInventory().getTitle();
        String gName = getGuiNameFromTitle(title);
        int iSlot = getSlotFromTitle(title);

        if (gName != null && iSlot != -1) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                ItemEditor.updateUI(topInv, new EditSession(gName, iSlot, currentIcon.clone()));
            });
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
        String gName = getGuiNameFromTitle(title);
        int iSlot = getSlotFromTitle(title);

        if (gName == null || iSlot == -1) return;

        ItemStack currentIcon = inv.getItem(4);
        if (currentIcon == null) return;

        EditSession session = new EditSession(gName, iSlot, currentIcon.clone());

        if (slot == 8) {
            GUI gui = plugin.getGui(gName);
            if (gui != null) {
                if (currentIcon.getType().isAir()) {
                    gui.getItems().remove(iSlot);
                } else {
                    gui.setItem(iSlot, currentIcon.clone());
                }
                plugin.saveGui(gName);
                plugin.setEditMode(player, gName);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                    player.openInventory(plugin.getEditInventory(gName));
                });
            }
            return;
        }

        if (slot == 5) {
            toggleByte(session.getItem(), GUIManager.KEY_REQUIRE_TARGET);
            ItemEditor.updateUI(inv, session);
            return;
        }

        if (slot >= 45 && slot <= 53) {
            if (slot == 53) {
                if (event.isLeftClick()) {
                    session.setLorePage(session.getLorePage() + 1);
                } else if (event.isRightClick()) {
                    session.setLorePage(Math.max(0, session.getLorePage() - 1));
                }
                ItemEditor.updateUI(inv, session);
            } else {
                handleLoreClick(event, player, session, inv);
            }
            return;
        }

        if (slot == 13 || slot == 22 || slot == 31 || slot == 40) {
            NamespacedKey key;
            if (event.isLeftClick()) {
                if (slot == 13) key = GUIManager.KEY_KEEP_OPEN_LEFT;
                else if (slot == 22) key = GUIManager.KEY_KEEP_OPEN_RIGHT;
                else if (slot == 31) key = GUIManager.KEY_KEEP_OPEN_F;
                else key = GUIManager.KEY_KEEP_OPEN_Q;
            } else if (event.isRightClick()) {
                if (slot == 13) key = GUIManager.KEY_KEEP_OPEN_SHIFT_LEFT;
                else if (slot == 22) key = GUIManager.KEY_KEEP_OPEN_SHIFT_RIGHT;
                else if (slot == 31) key = GUIManager.KEY_KEEP_OPEN_SHIFT_F;
                else key = GUIManager.KEY_KEEP_OPEN_SHIFT_Q;
            } else {
                return;
            }

            toggleByte(session.getItem(), key);
            ItemEditor.updateUI(inv, session);
            return;
        }

        EditSession.EditType type = getEditTypeFromSlot(slot);
        if (type != null) {
            if (type.name().startsWith("COST_")) {
                NamespacedKey costKey = ActionKeyUtil.getKeyFromType(type);
                NamespacedKey rewardKey = ActionKeyUtil.getRewardKeyFromCostType(type);
                ItemMeta meta = session.getItem().getItemMeta();
                if (meta == null) return;

                if (event.isShiftClick() && event.isLeftClick()) {
                    meta.getPersistentDataContainer().remove(costKey);
                    session.getItem().setItemMeta(meta);
                    ItemEditor.updateUI(inv, session);
                } else if (event.isShiftClick() && event.isRightClick()) {
                    meta.getPersistentDataContainer().remove(rewardKey);
                    session.getItem().setItemMeta(meta);
                    ItemEditor.updateUI(inv, session);
                } else if (event.isLeftClick()) {
                    openVirtualGUI(player, session, costKey, "Cost");
                } else if (event.isRightClick()) {
                    openVirtualGUI(player, session, rewardKey, "Reward");
                }
                return;
            }

            if (type == EditSession.EditType.ITEM_FLAGS) {
                ItemMeta meta = session.getItem().getItemMeta();
                if (meta != null) {
                    List<ItemFlag> currentFlags = new ArrayList<>(meta.getItemFlags());
                    ItemFlag[] allFlags = ItemFlag.values();
                    if (event.isLeftClick()) {
                        for (ItemFlag flag : allFlags) {
                            if (!currentFlags.contains(flag)) {
                                meta.addItemFlags(flag);
                                break;
                            }
                        }
                    } else if (event.isRightClick()) {
                        if (!currentFlags.isEmpty()) {
                            meta.removeItemFlags(currentFlags.get(currentFlags.size() - 1));
                        }
                    }
                    session.getItem().setItemMeta(meta);
                    ItemEditor.updateUI(inv, session);
                }
                return;
            }

            if (type.name().startsWith("COMMAND_")) {
                if (event.isShiftClick() && event.isLeftClick()) {
                    cycleExecutor(player, session, slot, inv);
                } else if (event.isShiftClick() && event.isRightClick()) {
                    EditSession.EditType cdType = EditSession.EditType.valueOf(type.name().replace("COMMAND", "COOLDOWN"));
                    session.setEditType(cdType);
                    plugin.startChatSession(player, session);
                    player.closeInventory();
                } else if (event.isRightClick()) {
                    removeLastCommand(player, session, slot, inv);
                } else if (event.isLeftClick()) {
                    session.setEditType(type);
                    plugin.startChatSession(player, session);
                    player.closeInventory();
                }
                return;
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
        try { execType = EditSession.EditType.valueOf(execName); } catch (IllegalArgumentException e) { return; }

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
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, cmds.substring(0, lastIndex));
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

    private void toggleByte(ItemStack item, NamespacedKey key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int v = 0;
        if (pdc.has(key, PersistentDataType.INTEGER)) v = pdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
        else if (pdc.has(key, PersistentDataType.BYTE)) { v = pdc.getOrDefault(key, PersistentDataType.BYTE, (byte) 0); pdc.remove(key); }
        pdc.set(key, PersistentDataType.INTEGER, v == 0 ? 1 : 0);
        item.setItemMeta(meta);
    }

    private void resetValue(ItemStack item, EditSession.EditType type) {
        if (type.name().equals("MATERIAL")) { item.setType(Material.STONE); return; }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        NamespacedKey key = ActionKeyUtil.getKeyFromType(type);
        if (key != null) meta.getPersistentDataContainer().remove(key);
        if (type == EditSession.EditType.ITEM_MODEL_ID) {
            meta.setCustomModelData(null);
            meta.getPersistentDataContainer().remove(GUIManager.KEY_ITEM_MODEL);
            try { meta.setItemModel(null); } catch (Throwable ignored) {}
        }
        item.setItemMeta(meta);
    }

    private void handleGuiEditorClick(InventoryClickEvent event, Player player) {
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;
        Inventory topInv = event.getView().getTopInventory();

        if (event.isRightClick() && clickedInv.equals(topInv)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            String guiId = plugin.getEditingGuiName(player);

            if (clicked == null || clicked.getType().isAir()) {
                clicked = new ItemStack(Material.STONE);
            }

            ItemEditor.open(player, new EditSession(guiId, event.getSlot(), clicked.clone()));
            return;
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
                    for (int i = 0; i < topInv.getSize(); i++) {
                        ItemStack item = topInv.getItem(i);
                        if (item == null || item.getType().isAir()) {
                            gui.getItems().remove(i);
                        } else {
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
            case 2: return EditSession.EditType.ITEM_FLAGS;
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