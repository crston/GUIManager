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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class GUIListener implements Listener {

    private final GUIManager plugin;
    private final ActionExecutor actionExecutor;

    public GUIListener(GUIManager plugin) {
        this.plugin = plugin;
        this.actionExecutor = new ActionExecutor(plugin, this);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();

        if (plugin.isSettingCost(player) && title.startsWith(ItemEditor.COST_TITLE_PREFIX)) {
            event.setCancelled(false);
            return;
        }

        if (plugin.isInEditMode(player) && isGuiEditor(player, title)) {
            handleGuiEditorClick(event);
        } else if (title.startsWith(ItemEditor.TITLE_PREFIX)) {
            handleItemEditorClick(event, player);
        } else {
            handleNormalGuiClick(event, player);
        }
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
            EditSession session = plugin.getCostSession(player);
            if (session == null) return;

            ItemStack targetItem = session.getItem();
            ItemMeta meta = targetItem.getItemMeta();
            if (meta == null) return;

            List<ItemStack> costs = new ArrayList<>();
            for (ItemStack item : event.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    costs.add(item.clone());
                }
            }

            NamespacedKey key = ActionKeyUtil.getKeyFromType(session.getEditType());

            try {
                if (costs.isEmpty()) {
                    meta.getPersistentDataContainer().remove(key);
                    player.sendMessage(plugin.getLanguageManager().getMessage("editor.cost_set") + " Cleared");
                } else {
                    String serialized = ItemSerialization.itemStackArrayToBase64(costs.toArray(new ItemStack[0]));
                    meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, serialized);
                    player.sendMessage(plugin.getLanguageManager().getMessage("editor.cost_set"));
                }

                targetItem.setItemMeta(meta);

                GUI gui = plugin.getGui(session.getGuiName());
                if (gui != null) gui.setItem(session.getSlot(), targetItem);

                session.setItem(targetItem);

            } catch (Exception e) {
                player.sendMessage(plugin.getLanguageManager().getMessage("editor.cost_save_fail"));
                e.printStackTrace();
            }

            plugin.endCostSession(player);
            Bukkit.getScheduler().runTask(plugin, () -> ItemEditor.open(player, session));

        } else if (title.startsWith(ItemEditor.TITLE_PREFIX)) {
            if (plugin.hasChatSession(player) || plugin.isSettingCost(player)) {
                return;
            }
            try {
                String guiName = title.replace(ItemEditor.TITLE_PREFIX, "").split(" ")[0];
                int itemSlot = Integer.parseInt(title.replaceAll(".*Slot (\\d+).*", "$1"));

                ItemStack updatedItem = event.getInventory().getItem(4);
                if (updatedItem == null) return;

                GUI gui = plugin.getGui(guiName);
                if (gui != null) {
                    gui.setItem(itemSlot, updatedItem);
                    plugin.saveGui(guiName);
                    player.sendMessage(plugin.getLanguageManager().getMessage("editor.item_saved"));
                }
            } catch (Exception e) {
                player.sendMessage(plugin.getLanguageManager().getMessage("editor.item_auto_save_fail"));
                plugin.getLogger().log(Level.WARNING, "Could not auto-save item from ItemEditor for player " + player.getName(), e);
            }
        }
    }

    private void handleGuiEditorClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            event.setCancelled(false);
            return;
        }

        boolean isTopInventory = event.getView().getTopInventory().equals(event.getClickedInventory());

        if (isTopInventory && event.isRightClick()) {
            ItemStack item = event.getCurrentItem();
            if (item != null && !item.getType().isAir()) {
                event.setCancelled(true);
                Player player = (Player) event.getWhoClicked();
                String guiName = plugin.getEditingGuiName(player);
                if (guiName == null) return;
                EditSession session = new EditSession(guiName, event.getSlot(), item.clone());
                ItemEditor.open(player, session);
                return;
            }
        }
        event.setCancelled(false);
    }

    private void handleItemEditorClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null || clickedItem == null || clickedItem.getType().isAir()) return;

        String title = event.getView().getTitle();
        Inventory editorInv = event.getView().getTopInventory();

        ItemStack currentIcon = editorInv.getItem(4);
        if (currentIcon == null) return;

        if (!clickedInventory.equals(editorInv)) {
            ItemStack newIcon = clickedItem.clone();
            newIcon.setAmount(1);
            ItemMeta oldMeta = currentIcon.getItemMeta();
            if (oldMeta != null) {
                newIcon.setItemMeta(oldMeta.clone());
            }
            editorInv.setItem(4, newIcon);
            return;
        }

        int slot = event.getSlot();
        String guiName = title.replace(ItemEditor.TITLE_PREFIX, "").split(" ")[0];
        int itemSlot = Integer.parseInt(title.replaceAll(".*Slot (\\d+).*", "$1"));

        EditSession session = new EditSession(guiName, itemSlot, currentIcon.clone());

        ItemStack pageArrow = editorInv.getItem(53);
        if (pageArrow != null && pageArrow.hasItemMeta()) {
            Integer page = pageArrow.getItemMeta().getPersistentDataContainer().get(ItemEditor.KEY_PAGE, PersistentDataType.INTEGER);
            if (page != null) {
                session.setLorePage(page);
            }
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
                    return;
                }
            } else if (event.isShiftClick() && event.isLeftClick()) {
                cycleExecutor(player, session, slot);
                return;
            }
        }

        EditSession.EditType type = getEditTypeFromSlot(slot);
        if (type != null) {
            session.setEditType(type);
            if (type.name().startsWith("COST") && !type.name().contains("MONEY")) {
                openItemCostEditor(player, session);
            } else if (type.name().contains("MONEY_COST") && GUIManager.econ == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("editor.require_vault"));
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

    private void handleNormalGuiClick(InventoryClickEvent event, Player player) {
        Inventory topInv = event.getView().getTopInventory();

        if (topInv.getHolder() instanceof GUIHolder) {
            GUIHolder holder = (GUIHolder) topInv.getHolder();
            String guiId = holder.getGuiId();

            if (event.getClickedInventory() != topInv && event.getAction().toString().contains("MOVE_TO_OTHER_INVENTORY")) {
                event.setCancelled(true);
                return;
            }

            if (event.getClickedInventory() != topInv) {
                return;
            }

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
            if (item == null) return;

            actionExecutor.execute(player, guiId, event.getSlot(), item, event.getClick());
        }
    }

    private void openItemCostEditor(Player player, EditSession session) {
        plugin.startCostSession(player, session);
        String costTitle = ItemEditor.COST_TITLE_PREFIX + session.getEditType().name();
        Inventory costInv = Bukkit.createInventory(null, 54, costTitle);
        try {
            NamespacedKey key = ActionKeyUtil.getKeyFromType(session.getEditType());
            PersistentDataContainer pdc = Objects.requireNonNull(session.getItem().getItemMeta()).getPersistentDataContainer();

            if (pdc.has(key, PersistentDataType.STRING)) {
                String data = pdc.get(key, PersistentDataType.STRING);
                if (data != null && !data.isEmpty()) {
                    ItemStack[] costs = ItemSerialization.itemStackArrayFromBase64(data);
                    if (costs != null) {
                        costInv.setContents(Arrays.copyOf(costs, costInv.getSize()));
                    }
                }
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
        if (key == null) return;

        ItemMeta meta = session.getItem().getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String currentExecutorName = pdc.getOrDefault(key, PersistentDataType.STRING, GUIManager.ExecutorType.PLAYER.name());
        GUIManager.ExecutorType currentExecutor;
        try {
            currentExecutor = GUIManager.ExecutorType.valueOf(currentExecutorName);
        } catch(IllegalArgumentException e) {
            currentExecutor = GUIManager.ExecutorType.PLAYER;
        }

        GUIManager.ExecutorType[] allTypes = GUIManager.ExecutorType.values();
        int nextOrdinal = (currentExecutor.ordinal() + 1) % allTypes.length;
        GUIManager.ExecutorType nextExecutor = allTypes[nextOrdinal];

        pdc.set(key, PersistentDataType.STRING, nextExecutor.name());
        session.getItem().setItemMeta(meta);

        player.getOpenInventory().getTopInventory().setItem(4, session.getItem());

        ItemEditor.open(player, session);
    }

    private void toggleByte(ItemStack item, NamespacedKey key, Player player) {
        if (item == null || !item.hasItemMeta() || key == null) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = Objects.requireNonNull(meta).getPersistentDataContainer();
        byte currentState = pdc.getOrDefault(key, PersistentDataType.BYTE, (byte) 0);
        pdc.set(key, PersistentDataType.BYTE, (byte) (currentState == 0 ? 1 : 0));
        item.setItemMeta(meta);

        if (player != null && player.getOpenInventory().getTopInventory() != null) {
            if (player.getOpenInventory().getTitle().startsWith(ItemEditor.TITLE_PREFIX)) {
                player.getOpenInventory().getTopInventory().setItem(4, item);
            }
        }
    }

    private void handleLoreRemove(Player player, EditSession session) {
        int lineIndex = session.getLoreLineEditIndex();
        if (lineIndex == -1) return;
        ItemMeta meta = session.getItem().getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = new ArrayList<>(Objects.requireNonNull(meta.getLore()));
            if (lineIndex < lore.size()) {
                lore.remove(lineIndex);
                meta.setLore(lore.isEmpty() ? null : lore);
                session.getItem().setItemMeta(meta);
                player.sendMessage(plugin.getLanguageManager().getMessage("editor.lore_removed", "{line}", String.valueOf(lineIndex + 1)));
                player.getOpenInventory().getTopInventory().setItem(4, session.getItem());
            }
        }
        ItemEditor.open(player, session);
    }

    private void sendChatPrompt(Player player, EditSession.EditType type) {
        String msg;
        if (type.name().startsWith("COOLDOWN_")) {
            msg = plugin.getLanguageManager().getMessage("editor.prompt.cooldown");
        } else {
            switch (type) {
                case NAME:
                    msg = plugin.getLanguageManager().getMessage("editor.prompt.name");
                    break;
                case CUSTOM_MODEL_DATA:
                case ITEM_DAMAGE:
                    msg = plugin.getLanguageManager().getMessage("editor.prompt.number");
                    break;
                case ITEM_MODEL_ID:
                    msg = plugin.getLanguageManager().getMessage("editor.prompt.model_id");
                    break;
                case LORE_ADD:
                    msg = plugin.getLanguageManager().getMessage("editor.prompt.lore_add");
                    break;
                case LORE_EDIT:
                    msg = plugin.getLanguageManager().getMessage("editor.prompt.lore_edit");
                    break;
                case SKULL:
                    msg = plugin.getLanguageManager().getMessage("editor.prompt.skull", "Enter player name or Base64 texture");
                    break;
                case MONEY_COST_LEFT: case MONEY_COST_RIGHT: case MONEY_COST_SHIFT_LEFT: case MONEY_COST_SHIFT_RIGHT:
                case MONEY_COST_F: case MONEY_COST_SHIFT_F: case MONEY_COST_Q: case MONEY_COST_SHIFT_Q:
                    msg = plugin.getLanguageManager().getMessage("editor.prompt.money");
                    break;
                default:
                    msg = plugin.getLanguageManager().getMessage("editor.prompt.general");
                    break;
            }
        }
        player.sendMessage(msg);
        player.sendMessage(plugin.getLanguageManager().getMessage("editor.prompt.cancel"));
    }

    private boolean isGuiEditor(Player p, String title) {
        String id = plugin.getEditingGuiName(p);
        if (id == null) return false;
        GUI gui = plugin.getGui(id);
        return gui != null && gui.getTitle().equals(title);
    }

    public boolean checkAndTakeCosts(Player player, PersistentDataContainer pdc, NamespacedKey moneyCostKey, NamespacedKey itemCostKey) {
        if (pdc.has(itemCostKey, PersistentDataType.STRING)) {
            String serializedCosts = pdc.get(itemCostKey, PersistentDataType.STRING);
            if (serializedCosts != null && !serializedCosts.isEmpty()) {
                try {
                    ItemStack[] costs = ItemSerialization.itemStackArrayFromBase64(serializedCosts);
                    if (!hasItems(player.getInventory(), costs)) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("check.no_item"));
                        return false;
                    }
                } catch (Exception e) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("check.error_item"));
                    return false;
                }
            }
        }
        if (GUIManager.econ != null && pdc.has(moneyCostKey, PersistentDataType.DOUBLE)) {
            double cost = pdc.getOrDefault(moneyCostKey, PersistentDataType.DOUBLE, 0.0);
            if (cost > 0 && !GUIManager.econ.has(player, cost)) {
                player.sendMessage(plugin.getLanguageManager().getMessage("check.no_money"));
                return false;
            }
        }
        if (pdc.has(itemCostKey, PersistentDataType.STRING)) {
            String serializedCosts = pdc.get(itemCostKey, PersistentDataType.STRING);
            if (serializedCosts != null && !serializedCosts.isEmpty()) {
                try {
                    removeStaticItems(player, ItemSerialization.itemStackArrayFromBase64(serializedCosts), plugin);
                } catch (Exception ignored) {}
            }
        }
        if (GUIManager.econ != null && pdc.has(moneyCostKey, PersistentDataType.DOUBLE)) {
            double cost = pdc.getOrDefault(moneyCostKey, PersistentDataType.DOUBLE, 0.0);
            if (cost > 0) GUIManager.econ.withdrawPlayer(player, cost);
        }
        return true;
    }

    private boolean hasItems(Inventory inventory, ItemStack[] requiredItems) {
        for (ItemStack requiredItem : requiredItems) {
            if (requiredItem == null || requiredItem.getType() == Material.AIR) continue;
            if (!containsAtLeastStrict(inventory, requiredItem, requiredItem.getAmount())) {
                return false;
            }
        }
        return true;
    }

    private boolean containsAtLeastStrict(Inventory inventory, ItemStack costItem, int amountNeeded) {
        int found = 0;
        for (ItemStack invItem : inventory.getContents()) {
            if (invItem == null || invItem.getType() != costItem.getType()) continue;
            boolean isMatch = true;
            if (costItem.hasItemMeta()) {
                if (!invItem.hasItemMeta()) {
                    isMatch = false;
                } else {
                    ItemMeta costMeta = costItem.getItemMeta();
                    ItemMeta invMeta = invItem.getItemMeta();
                    if (costMeta.hasDisplayName()) {
                        if (!invMeta.hasDisplayName() || !invMeta.getDisplayName().equals(costMeta.getDisplayName())) {
                            isMatch = false;
                        }
                    }
                    if (costMeta.hasCustomModelData()) {
                        if (!invMeta.hasCustomModelData() || invMeta.getCustomModelData() != costMeta.getCustomModelData()) {
                            isMatch = false;
                        }
                    }
                }
            }
            if (isMatch) {
                found += invItem.getAmount();
            }
        }
        return found >= amountNeeded;
    }

    public static void removeStaticItems(Player player, ItemStack[] items, GUIManager plugin) {
        if (items == null || items.length == 0) return;
        for (ItemStack costItem : items) {
            if (costItem == null || costItem.getType().isAir()) continue;
            int remaining = costItem.getAmount();

            for (ItemStack invItem : player.getInventory().getContents()) {
                if (invItem == null || invItem.getType() != costItem.getType()) continue;

                boolean isMatch = true;
                if (costItem.hasItemMeta()) {
                    if (!invItem.hasItemMeta()) {
                        isMatch = false;
                    } else {
                        ItemMeta invMeta = invItem.getItemMeta();
                        ItemMeta costMeta = costItem.getItemMeta();

                        if (costMeta.hasDisplayName()) {
                            if (!invMeta.hasDisplayName() || !invMeta.getDisplayName().equals(costMeta.getDisplayName())) {
                                isMatch = false;
                            }
                        }
                        if (costMeta.hasCustomModelData()) {
                            if (!invMeta.hasCustomModelData() || invMeta.getCustomModelData() != costMeta.getCustomModelData()) {
                                isMatch = false;
                            }
                        }
                    }
                }

                if (!isMatch) continue;

                int invAmt = invItem.getAmount();
                if (invAmt >= remaining) {
                    invItem.setAmount(invAmt - remaining);
                    remaining = 0;
                    break;
                } else {
                    remaining -= invAmt;
                    invItem.setAmount(0);
                }
            }

            if (remaining > 0) {
                player.sendMessage(plugin.getLanguageManager().getMessage("check.remove_fail"));
            }
        }
        player.updateInventory();
    }

    public boolean checkAndTakeCosts(Player player,
                                     PersistentDataContainer pdc,
                                     NamespacedKey moneyCostKey,
                                     NamespacedKey itemCostKey,
                                     double moneyOverride,
                                     String itemCostBase64) {
        return CostBridge.checkAndTake(player, plugin, pdc, moneyCostKey, itemCostKey, moneyOverride, itemCostBase64);
    }
}