package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatListener implements Listener {

    private final GUIManager plugin;
    private final ActionExecutor actionExecutor;

    public ChatListener(GUIManager plugin, GUIListener guiListener) {
        this.plugin = plugin;
        this.actionExecutor = new ActionExecutor(plugin, guiListener);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.isAwaitingTarget(player)) {
            event.setCancelled(true);
            String targetName = event.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> processTargetInput(player, targetName));
            return;
        }
        if (plugin.hasChatSession(player)) {
            event.setCancelled(true);
            String message = event.getMessage();
            // 비동기 채팅에서 아이템 수정은 안전하지 않으므로 메인 스레드에서 실행
            Bukkit.getScheduler().runTask(plugin, () -> processChatEdit(player, message));
        }
    }

    private void processTargetInput(Player player, String targetName) {
        TargetInfo targetInfo = plugin.getAwaitingTargetInfo(player);
        if (targetInfo == null) return;
        plugin.removeAwaitingTarget(player);
        if (targetName.equalsIgnoreCase("cancel") || targetName.equalsIgnoreCase("취소")) {
            player.sendMessage(ChatColor.YELLOW + "Target selection cancelled.");
            return;
        }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found or is offline.");
            return;
        }
        actionExecutor.executeCommand(player, targetInfo.command(), targetInfo.executor(), target.getName());
    }

    private void processChatEdit(Player player, String message) {
        EditSession session = plugin.getChatSession(player);
        if (session == null) return;

        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("취소")) {
            player.sendMessage(ChatColor.YELLOW + "Editing cancelled.");
            plugin.endChatSession(player);
            ItemEditor.open(player, session);
            return;
        }

        switch (session.getEditType()) {
            case NAME:
                handleNameEdit(player, session, message);
                break;
            case CUSTOM_MODEL_DATA:
            case ITEM_DAMAGE:
                handleIntegerEdit(player, session, message);
                break;
            case LORE_ADD:
            case LORE_EDIT:
                handleLoreEdit(player, session, message);
                break;
            case SKULL: // [추가] 머리 텍스처 처리
                handleSkullEdit(player, session, message);
                break;
            case MONEY_COST_LEFT: case MONEY_COST_RIGHT: case MONEY_COST_SHIFT_LEFT: case MONEY_COST_SHIFT_RIGHT:
            case MONEY_COST_F: case MONEY_COST_SHIFT_F: case MONEY_COST_Q: case MONEY_COST_SHIFT_Q:
            case COOLDOWN_LEFT: case COOLDOWN_RIGHT: case COOLDOWN_SHIFT_LEFT: case COOLDOWN_SHIFT_RIGHT:
            case COOLDOWN_F: case COOLDOWN_SHIFT_F: case COOLDOWN_Q: case COOLDOWN_SHIFT_Q:
                handleDoubleEdit(player, session, message);
                break;
            default:
                handleStringEdit(player, session, message);
                break;
        }

        // GUI 원본 업데이트
        GUI gui = plugin.getGui(session.getGuiName());
        if (gui != null) {
            gui.setItem(session.getSlot(), session.getItem());
        }

        plugin.endChatSession(player);
        ItemEditor.open(player, session);
    }

    // [추가] 머리 수정 핸들러
    private void handleSkullEdit(Player p, EditSession s, String msg) {
        ItemStack currentItem = s.getItem();
        ItemStack newHead;

        // Base64인지 닉네임인지 판별하여 생성
        if (msg.length() > 16) {
            newHead = HeadUtils.createHeadByBase64(msg);
        } else {
            newHead = HeadUtils.createHeadByName(msg);
        }

        if (newHead == null) {
            p.sendMessage(ChatColor.RED + "Failed to create skull.");
            return;
        }

        ItemMeta oldMeta = currentItem.getItemMeta();
        ItemMeta newMeta = newHead.getItemMeta();

        // 기존 메타 데이터(이름, 로어, PDC 설정 등)를 새 머리로 복사
        if (oldMeta != null && newMeta != null) {
            if (oldMeta.hasDisplayName()) newMeta.setDisplayName(oldMeta.getDisplayName());
            if (oldMeta.hasLore()) newMeta.setLore(oldMeta.getLore());
            if (oldMeta.hasCustomModelData()) newMeta.setCustomModelData(oldMeta.getCustomModelData());

            // 핵심: 액션, 비용 등 플러그인 설정 데이터(PDC) 복사
            copyPersistentData(oldMeta.getPersistentDataContainer(), newMeta.getPersistentDataContainer());
        }

        newHead.setItemMeta(newMeta);
        newHead.setAmount(currentItem.getAmount());

        s.setItem(newHead);
        p.sendMessage(ChatColor.GREEN + "Skull texture updated!");
    }

    private void handleStringEdit(Player p, EditSession s, String msg) {
        ItemMeta meta = s.getItem().getItemMeta();
        if (meta == null) return;
        NamespacedKey key = ActionKeyUtil.getKeyFromType(s.getEditType());
        if (key == null) return;
        if (msg.equalsIgnoreCase("delete") || msg.equalsIgnoreCase("삭제") || msg.equalsIgnoreCase("none")) {
            meta.getPersistentDataContainer().remove(key);
            p.sendMessage(ChatColor.GREEN + "Setting removed.");
        } else {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, msg);
            p.sendMessage(ChatColor.GREEN + "Setting saved: " + msg);
        }
        s.getItem().setItemMeta(meta);
    }

    private void handleIntegerEdit(Player p, EditSession s, String msg) {
        ItemMeta meta = s.getItem().getItemMeta();
        if (meta == null) return;
        NamespacedKey key = ActionKeyUtil.getKeyFromType(s.getEditType());
        if (key == null) return;
        if (msg.equalsIgnoreCase("delete") || msg.equalsIgnoreCase("삭제")) {
            meta.getPersistentDataContainer().remove(key);
            if(s.getEditType() == EditSession.EditType.CUSTOM_MODEL_DATA) meta.setCustomModelData(null);
            else if (meta instanceof Damageable) ((Damageable) meta).setDamage(0);
            p.sendMessage(ChatColor.GREEN + "Value removed.");
        } else {
            try {
                int value = Integer.parseInt(msg);
                if (value < 0) {
                    p.sendMessage(ChatColor.RED + "Value cannot be negative.");
                    return;
                }
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
                if(s.getEditType() == EditSession.EditType.CUSTOM_MODEL_DATA) meta.setCustomModelData(value);
                else if (meta instanceof Damageable) ((Damageable) meta).setDamage(value);
                p.sendMessage(ChatColor.GREEN + "Value set to " + value);
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "Invalid number format.");
            }
        }
        s.getItem().setItemMeta(meta);
    }

    private void handleDoubleEdit(Player p, EditSession s, String msg) {
        ItemMeta meta = s.getItem().getItemMeta();
        if (meta == null) return;
        NamespacedKey key = ActionKeyUtil.getKeyFromType(s.getEditType());
        if (key == null) return;
        if (msg.equalsIgnoreCase("delete") || msg.equalsIgnoreCase("삭제")) {
            meta.getPersistentDataContainer().remove(key);
            p.sendMessage(ChatColor.GREEN + "Value removed.");
        } else {
            try {
                double value = Double.parseDouble(msg);
                if (value < 0) {
                    p.sendMessage(ChatColor.RED + "Value cannot be negative.");
                    return;
                }
                meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
                p.sendMessage(ChatColor.GREEN + "Value set to " + value);
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "Invalid number format.");
            }
        }
        s.getItem().setItemMeta(meta);
    }

    private void handleNameEdit(Player p, EditSession s, String msg) {
        ItemMeta meta = s.getItem().getItemMeta();
        if (meta == null) return;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', msg));
        s.getItem().setItemMeta(meta);
        p.sendMessage(ChatColor.GREEN + "Display name changed.");
    }

    private void handleLoreEdit(Player p, EditSession s, String msg) {
        ItemMeta meta = s.getItem().getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.getLore())) : new ArrayList<>();
        String text = ChatColor.translateAlternateColorCodes('&', msg);
        if (s.getEditType() == EditSession.EditType.LORE_ADD) {
            lore.add(text);
            p.sendMessage(ChatColor.GREEN + "Lore line added.");
        } else {
            int line = s.getLoreLineEditIndex();
            if (line >= 0 && line < lore.size()) {
                lore.set(line, text);
                p.sendMessage(ChatColor.GREEN + "Lore line " + (line + 1) + " changed.");
            } else {
                p.sendMessage(ChatColor.RED + "Invalid lore line to edit.");
                return;
            }
        }
        meta.setLore(lore);
        s.getItem().setItemMeta(meta);
    }

    // [중요] 기존 아이템의 PDC 데이터를 새 아이템으로 복사하는 메서드
    private void copyPersistentData(PersistentDataContainer source, PersistentDataContainer target) {
        List<NamespacedKey> allKeys = getAllKeys();
        for (NamespacedKey key : allKeys) {
            if (source.has(key, PersistentDataType.STRING)) {
                target.set(key, PersistentDataType.STRING, Objects.requireNonNull(source.get(key, PersistentDataType.STRING)));
            } else if (source.has(key, PersistentDataType.DOUBLE)) {
                target.set(key, PersistentDataType.DOUBLE, Objects.requireNonNull(source.get(key, PersistentDataType.DOUBLE)));
            } else if (source.has(key, PersistentDataType.BYTE)) {
                target.set(key, PersistentDataType.BYTE, Objects.requireNonNull(source.get(key, PersistentDataType.BYTE)));
            } else if (source.has(key, PersistentDataType.INTEGER)) {
                target.set(key, PersistentDataType.INTEGER, Objects.requireNonNull(source.get(key, PersistentDataType.INTEGER)));
            }
        }
    }

    // 복사할 키 목록
    private List<NamespacedKey> getAllKeys() {
        List<NamespacedKey> keys = new ArrayList<>();
        keys.add(GUIManager.KEY_PERMISSION_MESSAGE);
        keys.add(GUIManager.KEY_REQUIRE_TARGET);
        keys.add(GUIManager.KEY_CUSTOM_MODEL_DATA);
        keys.add(GUIManager.KEY_ITEM_MODEL_ID);

        // Left
        keys.add(GUIManager.KEY_COMMAND_LEFT); keys.add(GUIManager.KEY_EXECUTOR_LEFT); keys.add(GUIManager.KEY_COOLDOWN_LEFT);
        keys.add(GUIManager.KEY_MONEY_COST_LEFT); keys.add(GUIManager.KEY_COST_LEFT); keys.add(GUIManager.KEY_KEEP_OPEN_LEFT);

        // Shift+Left
        keys.add(GUIManager.KEY_COMMAND_SHIFT_LEFT); keys.add(GUIManager.KEY_EXECUTOR_SHIFT_LEFT); keys.add(GUIManager.KEY_COOLDOWN_SHIFT_LEFT);
        keys.add(GUIManager.KEY_MONEY_COST_SHIFT_LEFT); keys.add(GUIManager.KEY_COST_SHIFT_LEFT); keys.add(GUIManager.KEY_KEEP_OPEN_SHIFT_LEFT);

        // Right
        keys.add(GUIManager.KEY_COMMAND_RIGHT); keys.add(GUIManager.KEY_EXECUTOR_RIGHT); keys.add(GUIManager.KEY_COOLDOWN_RIGHT);
        keys.add(GUIManager.KEY_MONEY_COST_RIGHT); keys.add(GUIManager.KEY_COST_RIGHT); keys.add(GUIManager.KEY_KEEP_OPEN_RIGHT);

        // Shift+Right
        keys.add(GUIManager.KEY_COMMAND_SHIFT_RIGHT); keys.add(GUIManager.KEY_EXECUTOR_SHIFT_RIGHT); keys.add(GUIManager.KEY_COOLDOWN_SHIFT_RIGHT);
        keys.add(GUIManager.KEY_MONEY_COST_SHIFT_RIGHT); keys.add(GUIManager.KEY_COST_SHIFT_RIGHT); keys.add(GUIManager.KEY_KEEP_OPEN_SHIFT_RIGHT);

        // F
        keys.add(GUIManager.KEY_COMMAND_F); keys.add(GUIManager.KEY_EXECUTOR_F); keys.add(GUIManager.KEY_COOLDOWN_F);
        keys.add(GUIManager.KEY_MONEY_COST_F); keys.add(GUIManager.KEY_COST_F); keys.add(GUIManager.KEY_PERMISSION_F);

        // Shift+F
        keys.add(GUIManager.KEY_COMMAND_SHIFT_F); keys.add(GUIManager.KEY_EXECUTOR_SHIFT_F); keys.add(GUIManager.KEY_COOLDOWN_SHIFT_F);
        keys.add(GUIManager.KEY_MONEY_COST_SHIFT_F); keys.add(GUIManager.KEY_COST_SHIFT_F); keys.add(GUIManager.KEY_PERMISSION_SHIFT_F);

        // Q
        keys.add(GUIManager.KEY_COMMAND_Q); keys.add(GUIManager.KEY_EXECUTOR_Q); keys.add(GUIManager.KEY_COOLDOWN_Q);
        keys.add(GUIManager.KEY_MONEY_COST_Q); keys.add(GUIManager.KEY_COST_Q); keys.add(GUIManager.KEY_PERMISSION_Q);

        // Shift+Q
        keys.add(GUIManager.KEY_COMMAND_SHIFT_Q); keys.add(GUIManager.KEY_EXECUTOR_SHIFT_Q); keys.add(GUIManager.KEY_COOLDOWN_SHIFT_Q);
        keys.add(GUIManager.KEY_MONEY_COST_SHIFT_Q); keys.add(GUIManager.KEY_COST_SHIFT_Q); keys.add(GUIManager.KEY_PERMISSION_SHIFT_Q);

        return keys;
    }
}