package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class GUIListener implements Listener {

    private final GUIManager plugin;
    private final ActionExecutor actionExecutor;

    public GUIListener(GUIManager plugin) {
        this.plugin = plugin;
        this.actionExecutor = new ActionExecutor(plugin, this);
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
            handleItemEditorClick(event, player);
            return;
        }

        if (plugin.isInEditMode(player) && isGuiEditor(player, title)) {
            handleGuiEditorClick(event);
        } else {
            handleNormalGuiClick(event, player);
        }
    }

    private void handleItemEditorClick(InventoryClickEvent event, Player player) {
        Inventory editorInv = event.getView().getTopInventory();
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        String title = event.getView().getTitle();
        ItemStack cursorItem = event.getCursor();
        ItemStack clickedItem = event.getCurrentItem();
        int slot = event.getSlot();

        if (!clickedInv.equals(editorInv)) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                if (clickedItem != null && !clickedItem.getType().isAir()) {
                    updateAndRefreshEditor(player, title, editorInv, clickedItem);
                }
            } else {
                event.setCancelled(false);
            }
            return;
        }

        event.setCancelled(true);

        if (slot == 4) {
            if (cursorItem != null && !cursorItem.getType().isAir()) {
                updateAndRefreshEditor(player, title, editorInv, cursorItem);
                event.setCursor(null);
            }
            return;
        }

        if (clickedItem == null || clickedItem.getType().isAir()) return;

        String guiName = title.replace(ItemEditor.TITLE_PREFIX, "").split(" ")[0];
        int itemSlot = Integer.parseInt(title.replaceAll(".*Slot (\\d+).*", "$1"));
        ItemStack currentIcon = editorInv.getItem(4);
        if (currentIcon == null) return;

        EditSession session = new EditSession(guiName, itemSlot, currentIcon.clone());

        ItemStack pageArrow = editorInv.getItem(53);
        if (pageArrow != null && pageArrow.hasItemMeta()) {
            Integer page = pageArrow.getItemMeta().getPersistentDataContainer().get(ItemEditor.KEY_PAGE, PersistentDataType.INTEGER);
            if (page != null) session.setLorePage(page);
        }

        if (slot == 8) {
            GUI gui = plugin.getGui(session.getGuiName());
            if (gui != null) {
                gui.setItem(session.getSlot(), currentIcon);
                plugin.saveGui(session.getGuiName());
                player.sendMessage(plugin.getLanguageManager().getMessage("editor.item_saved"));
                plugin.setEditMode(player, session.getGuiName());
                player.openInventory(gui.getInventory());
            }
            return;
        }

        if (slot == 53) {
            if (event.isShiftClick()) {
                if (session.getLorePage() > 0) session.setLorePage(session.getLorePage() - 1);
            } else {
                session.setLorePage(session.getLorePage() + 1);
            }
            ItemEditor.open(player, session);
            return;
        }

        if (slot >= 45 && slot <= 52) {
            handleLoreClick(event, player, session);
            return;
        }

        if (clickedItem.getType() == Material.COMMAND_BLOCK) {
            if (event.isRightClick()) {
                EditSession.EditType type = getCooldownEditTypeFromSlot(slot);
                if (type != null) {
                    session.setEditType(type);
                    plugin.startChatSession(player, session);
                    player.closeInventory();
                    sendChatPrompt(player, type);
                }
            } else if (event.isShiftClick() && event.isLeftClick()) {
                cycleExecutor(player, session, slot);
            }
            return;
        }

        EditSession.EditType type = getEditTypeFromSlot(slot);
        if (type != null) {
            session.setEditType(type);
            if (type.name().startsWith("COST") && !type.name().contains("MONEY")) {
                openItemCostEditor(player, session);
            } else if (type == EditSession.EditType.REQUIRE_TARGET || type.name().startsWith("KEEP_OPEN")) {
                toggleByte(session.getItem(), ActionKeyUtil.getKeyFromType(type), player);
                ItemEditor.open(player, session);
            } else {
                plugin.startChatSession(player, session);
                player.closeInventory();
                sendChatPrompt(player, type);
            }
        }
    }

    private void updateAndRefreshEditor(Player player, String title, Inventory editorInv, ItemStack sourceItem) {
        ItemStack currentIcon = editorInv.getItem(4);
        if (currentIcon == null) return;

        ItemStack newIcon = sourceItem.clone();
        newIcon.setAmount(1);

        ItemMeta currentMeta = currentIcon.getItemMeta();
        ItemMeta sourceMeta = newIcon.getItemMeta();

        if (currentMeta != null && sourceMeta != null) {
            // 기존 에디터의 정보(이름, 로어) 유지
            if (currentMeta.hasDisplayName()) sourceMeta.setDisplayName(currentMeta.getDisplayName());
            if (currentMeta.hasLore()) sourceMeta.setLore(currentMeta.getLore());

            // 만약 새로 들고온 아이템에 CustomModelData 가 있다면 그것을 우선 적용 (아이콘 변경이 목적이므로)
            // 없다면 기존 에디터의 모델 데이터를 유지
            if (!sourceMeta.hasCustomModelData() && currentMeta.hasCustomModelData()) {
                sourceMeta.setCustomModelData(currentMeta.getCustomModelData());
            }

            // 기존 에디터의 PDC 데이터(명령어, 비용 등) 복사
            ActionKeyUtil.copyPersistentData(currentMeta.getPersistentDataContainer(), sourceMeta.getPersistentDataContainer());
        }
        newIcon.setItemMeta(sourceMeta);

        String guiName = title.replace(ItemEditor.TITLE_PREFIX, "").split(" ")[0];
        int itemSlot = Integer.parseInt(title.replaceAll(".*Slot (\\d+).*", "$1"));

        EditSession session = new EditSession(guiName, itemSlot, newIcon);
        session.setItem(newIcon);

        ItemStack pageArrow = editorInv.getItem(53);
        if (pageArrow != null && pageArrow.hasItemMeta()) {
            Integer page = pageArrow.getItemMeta().getPersistentDataContainer().get(ItemEditor.KEY_PAGE, PersistentDataType.INTEGER);
            if (page != null) session.setLorePage(page);
        }

        ItemEditor.open(player, session);
        player.sendMessage("§aIcon Updated! Material: §f" + newIcon.getType() + " §7/ CMD: §f" + (sourceMeta != null && sourceMeta.hasCustomModelData() ? sourceMeta.getCustomModelData() : "None"));
    }

    private boolean isGuiEditor(Player p, String title) {
        String id = plugin.getEditingGuiName(p);
        if (id == null) return false;
        GUI gui = plugin.getGui(id);
        return gui != null && gui.getTitle().equals(title);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        if (plugin.isInEditMode(player) && isGuiEditor(player, title)) {
            String guiId = plugin.getEditingGuiName(player);
            GUI gui = plugin.getGui(guiId);
            if (gui != null) {
                gui.updateFromInventory(event.getInventory());
                plugin.saveGui(guiId);
                player.sendMessage(plugin.getLanguageManager().getMessage("editor.saved", "{id}", guiId));
            }
            plugin.removeEditMode(player);
        } else if (plugin.isSettingCost(player) && title.startsWith(ItemEditor.COST_TITLE_PREFIX)) {
            handleCostClose(player, event.getInventory());
        }
    }

    private void handleCostClose(Player player, Inventory costInv) {
        EditSession session = plugin.getCostSession(player);
        if (session == null) return;

        ItemStack targetItem = session.getItem();
        ItemMeta meta = targetItem.getItemMeta();
        if (meta == null) return;

        List<ItemStack> costs = new ArrayList<>();
        for (ItemStack item : costInv.getContents()) {
            if (item != null && !item.getType().isAir()) costs.add(item.clone());
        }

        NamespacedKey key = ActionKeyUtil.getKeyFromType(session.getEditType());
        try {
            if (costs.isEmpty()) {
                meta.getPersistentDataContainer().remove(key);
            } else {
                String data = ItemSerialization.itemStackArrayToBase64(costs.toArray(new ItemStack[0]));
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, data);
            }
            targetItem.setItemMeta(meta);
            session.setItem(targetItem);
            GUI gui = plugin.getGui(session.getGuiName());
            if (gui != null) gui.setItem(session.getSlot(), targetItem);
        } catch (Exception e) {
            e.printStackTrace();
        }

        plugin.endCostSession(player);
        Bukkit.getScheduler().runTask(plugin, () -> ItemEditor.open(player, session));
    }

    private void handleNormalGuiClick(InventoryClickEvent event, Player player) {
        Inventory topInv = event.getView().getTopInventory();
        if (topInv.getHolder() instanceof GUIHolder) {
            GUIHolder holder = (GUIHolder) topInv.getHolder();
            String guiId = holder.getGuiId();

            if (event.getClickedInventory() != topInv && event.getAction().toString().contains("MOVE_TO_OTHER_INVENTORY")) {
                event.setCancelled(true);
                return;
            }

            if (event.getClickedInventory() != topInv) return;

            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;

            actionExecutor.execute(player, guiId, event.getSlot(), item, event.getClick());
        } else {
            String title = event.getView().getTitle();
            String guiId = plugin.findGuiIdByTitle(title);
            if (guiId == null) return;

            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;

            actionExecutor.execute(player, guiId, event.getSlot(), item, event.getClick());
        }
    }

    private void openItemCostEditor(Player player, EditSession session) {
        plugin.startCostSession(player, session);
        String costTitle = ItemEditor.COST_TITLE_PREFIX + session.getEditType().name();
        Inventory costInv = Bukkit.createInventory(null, 54, costTitle);
        try {
            NamespacedKey key = ActionKeyUtil.getKeyFromType(session.getEditType());
            ItemMeta meta = session.getItem().getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                ItemStack[] costs = ItemSerialization.itemStackArrayFromBase64(data);
                if (costs != null) costInv.setContents(Arrays.copyOf(costs, 54));
            }
        } catch (Exception ignored) {}
        player.openInventory(costInv);
    }

    private void handleLoreClick(InventoryClickEvent event, Player player, EditSession session) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;
        int slot = event.getSlot();

        if (clickedItem.getType() == Material.WRITABLE_BOOK) {
            session.setEditType(EditSession.EditType.LORE_ADD);
            plugin.startChatSession(player, session);
            player.closeInventory();
            sendChatPrompt(player, EditSession.EditType.LORE_ADD);
        } else if (clickedItem.getType() == Material.PAPER) {
            int lineIndex = (session.getLorePage() * 7) + (slot - 46);
            session.setLoreLineEditIndex(lineIndex);

            if (event.isLeftClick()) {
                session.setEditType(EditSession.EditType.LORE_EDIT);
                plugin.startChatSession(player, session);
                player.closeInventory();
                sendChatPrompt(player, EditSession.EditType.LORE_EDIT);
            } else if (event.isRightClick()) {
                handleLoreRemove(player, session);
            }
        }
    }

    private void handleLoreRemove(Player player, EditSession session) {
        int lineIndex = session.getLoreLineEditIndex();
        ItemMeta meta = session.getItem().getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = new ArrayList<>(Objects.requireNonNull(meta.getLore()));
            if (lineIndex >= 0 && lineIndex < lore.size()) {
                lore.remove(lineIndex);
                meta.setLore(lore.isEmpty() ? null : lore);
                session.getItem().setItemMeta(meta);
                player.getOpenInventory().getTopInventory().setItem(4, session.getItem());
            }
        }
        ItemEditor.open(player, session);
    }

    private void handleGuiEditorClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        boolean isTop = event.getView().getTopInventory().equals(event.getClickedInventory());
        if (isTop && event.isRightClick()) {
            ItemStack item = event.getCurrentItem();
            if (item != null && !item.getType().isAir()) {
                event.setCancelled(true);
                Player player = (Player) event.getWhoClicked();
                String guiName = plugin.getEditingGuiName(player);
                if (guiName == null) return;
                EditSession session = new EditSession(guiName, event.getSlot(), item.clone());
                ItemEditor.open(player, session);
            }
        } else {
            event.setCancelled(false);
        }
    }

    public static void removeStaticItems(Player player, ItemStack[] items, GUIManager plugin) {
        if (items == null || items.length == 0) return;
        Inventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (ItemStack costItem : items) {
            if (costItem == null || costItem.getType().isAir()) continue;
            int remaining = costItem.getAmount();
            for (int i = 0; i < contents.length; i++) {
                ItemStack invItem = contents[i];
                if (invItem == null || invItem.getType() != costItem.getType()) continue;
                boolean isMatch = true;
                if (costItem.hasItemMeta()) {
                    if (!invItem.hasItemMeta()) isMatch = false;
                    else {
                        ItemMeta costMeta = costItem.getItemMeta();
                        ItemMeta invMeta = invItem.getItemMeta();
                        if (costMeta.hasDisplayName()) {
                            if (!invMeta.hasDisplayName() || !invMeta.getDisplayName().equals(costMeta.getDisplayName())) isMatch = false;
                        }
                        if (isMatch && costMeta.hasCustomModelData()) {
                            if (!invMeta.hasCustomModelData() || invMeta.getCustomModelData() != costMeta.getCustomModelData()) isMatch = false;
                        }
                    }
                }
                if (!isMatch) continue;
                int invAmt = invItem.getAmount();
                if (invAmt > remaining) {
                    invItem.setAmount(invAmt - remaining);
                    inv.setItem(i, invItem);
                    remaining = 0;
                    break;
                } else {
                    remaining -= invAmt;
                    inv.setItem(i, null);
                }
            }
        }
        player.updateInventory();
    }

    private void cycleExecutor(Player player, EditSession session, int slot) {
        EditSession.EditType commandType;
        switch (slot) {
            case 9: commandType = EditSession.EditType.COMMAND_LEFT; break;
            case 14: commandType = EditSession.EditType.COMMAND_SHIFT_LEFT; break;
            case 18: commandType = EditSession.EditType.COMMAND_RIGHT; break;
            case 23: commandType = EditSession.EditType.COMMAND_SHIFT_RIGHT; break;
            case 27: commandType = EditSession.EditType.COMMAND_F; break;
            case 32: commandType = EditSession.EditType.COMMAND_SHIFT_F; break;
            case 36: commandType = EditSession.EditType.COMMAND_Q; break;
            case 41: commandType = EditSession.EditType.COMMAND_SHIFT_Q; break;
            default: return;
        }
        EditSession.EditType executorType = EditSession.EditType.valueOf(commandType.name().replace("COMMAND_", "EXECUTOR_"));
        NamespacedKey key = ActionKeyUtil.getKeyFromType(executorType);
        ItemMeta meta = session.getItem().getItemMeta();
        if (meta == null) return;
        String current = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.STRING, "PLAYER");
        String next = current.equals("PLAYER") ? "CONSOLE" : (current.equals("CONSOLE") ? "OP" : "PLAYER");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, next);
        session.getItem().setItemMeta(meta);
        ItemEditor.open(player, session);
    }

    private void toggleByte(ItemStack item, NamespacedKey key, Player player) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        byte current = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.BYTE, (byte) 0);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) (current == 0 ? 1 : 0));
        item.setItemMeta(meta);

        String title = player.getOpenInventory().getTitle();
        String guiName = title.replace(ItemEditor.TITLE_PREFIX, "").split(" ")[0];
        int itemSlot = Integer.parseInt(title.replaceAll(".*Slot (\\d+).*", "$1"));
        ItemEditor.open(player, new EditSession(guiName, itemSlot, item));
    }

    private EditSession.EditType getEditTypeFromSlot(int slot) {
        switch (slot) {
            case 0: return EditSession.EditType.NAME;
            case 1: return EditSession.EditType.CUSTOM_MODEL_DATA;
            case 2: return EditSession.EditType.ITEM_DAMAGE;
            case 5: return EditSession.EditType.REQUIRE_TARGET;
            case 6: return EditSession.EditType.PERMISSION_MESSAGE;
            case 7: return EditSession.EditType.SKULL;
            case 9: return EditSession.EditType.COMMAND_LEFT;
            case 10: return EditSession.EditType.MONEY_COST_LEFT;
            case 11: return EditSession.EditType.COST_LEFT;
            case 12: return EditSession.EditType.KEEP_OPEN_LEFT;
            case 14: return EditSession.EditType.COMMAND_SHIFT_LEFT;
            case 15: return EditSession.EditType.MONEY_COST_SHIFT_LEFT;
            case 16: return EditSession.EditType.COST_SHIFT_LEFT;
            case 17: return EditSession.EditType.KEEP_OPEN_SHIFT_LEFT;
            case 18: return EditSession.EditType.COMMAND_RIGHT;
            case 19: return EditSession.EditType.MONEY_COST_RIGHT;
            case 20: return EditSession.EditType.COST_RIGHT;
            case 21: return EditSession.EditType.KEEP_OPEN_RIGHT;
            case 23: return EditSession.EditType.COMMAND_SHIFT_RIGHT;
            case 24: return EditSession.EditType.MONEY_COST_SHIFT_RIGHT;
            case 25: return EditSession.EditType.COST_SHIFT_RIGHT;
            case 26: return EditSession.EditType.KEEP_OPEN_SHIFT_RIGHT;
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

    private EditSession.EditType getCooldownEditTypeFromSlot(int slot) {
        switch (slot) {
            case 9: return EditSession.EditType.COOLDOWN_LEFT;
            case 14: return EditSession.EditType.COOLDOWN_SHIFT_LEFT;
            case 18: return EditSession.EditType.COOLDOWN_RIGHT;
            case 23: return EditSession.EditType.COOLDOWN_SHIFT_RIGHT;
            case 27: return EditSession.EditType.COOLDOWN_F;
            case 32: return EditSession.EditType.COOLDOWN_SHIFT_F;
            case 36: return EditSession.EditType.COOLDOWN_Q;
            case 41: return EditSession.EditType.COOLDOWN_SHIFT_Q;
            default: return null;
        }
    }

    private void sendChatPrompt(Player player, EditSession.EditType type) {
        player.sendMessage("§eEnter value in chat. Type 'cancel' to abort.");
    }
}