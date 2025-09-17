package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ActionExecutor {

    private final GUIManager plugin;
    private final GUIListener guiListener;

    public ActionExecutor(GUIManager plugin, GUIListener guiListener) {
        this.plugin = plugin;
        this.guiListener = guiListener;
    }

    public void execute(Player player, String guiId, int slot, ItemStack item, ClickType clickType) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        NamespacedKey commandKey = ActionKeyUtil.getCommandKey(clickType);
        if (!pdc.has(commandKey, PersistentDataType.STRING)) return;

        String actionId = guiId + ":" + slot + ":" + clickType.name();

        NamespacedKey permKey = ActionKeyUtil.getPermissionKey(clickType);
        NamespacedKey moneyCostKey = ActionKeyUtil.getMoneyCostKey(clickType);
        NamespacedKey itemCostKey = ActionKeyUtil.getItemCostKey(clickType);
        NamespacedKey cooldownKey = ActionKeyUtil.getCooldownKey(clickType);
        NamespacedKey executorKey = ActionKeyUtil.getExecutorKey(clickType);

        execute(player, pdc, actionId, commandKey, permKey, moneyCostKey, itemCostKey, cooldownKey, executorKey);
    }

    public void execute(Player player, ItemStack item, ActionKeyUtil.KeyAction keyAction) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        NamespacedKey commandKey = ActionKeyUtil.getCommandKey(keyAction);
        if (!pdc.has(commandKey, PersistentDataType.STRING)) return;

        String command = pdc.get(commandKey, PersistentDataType.STRING);
        if (command == null) return;
        String actionId = command + ":" + keyAction.name();

        NamespacedKey permKey = ActionKeyUtil.getPermissionKey(keyAction);
        NamespacedKey moneyCostKey = ActionKeyUtil.getMoneyCostKey(keyAction);
        NamespacedKey itemCostKey = ActionKeyUtil.getItemCostKey(keyAction);
        NamespacedKey cooldownKey = ActionKeyUtil.getCooldownKey(keyAction);
        NamespacedKey executorKey = ActionKeyUtil.getExecutorKey(keyAction);

        execute(player, pdc, actionId, commandKey, permKey, moneyCostKey, itemCostKey, cooldownKey, executorKey);
    }

    private void execute(Player player, PersistentDataContainer pdc, String actionId, NamespacedKey commandKey, NamespacedKey permKey, NamespacedKey moneyCostKey, NamespacedKey itemCostKey, NamespacedKey cooldownKey, NamespacedKey executorKey) {
        long remainingCooldown = plugin.getRemainingCooldownMillis(player, actionId);
        if (remainingCooldown > 0) {
            player.sendMessage(ChatColor.RED + "You must wait " + String.format("%.1f", remainingCooldown / 1000.0) + " more seconds.");
            return;
        }

        String permission = pdc.get(permKey, PersistentDataType.STRING);
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            String noPermMsg = pdc.get(GUIManager.KEY_PERMISSION_MESSAGE, PersistentDataType.STRING);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermMsg != null ? noPermMsg : "&cYou don't have permission."));
            return;
        }

        if (!guiListener.checkAndTakeCosts(player, pdc, moneyCostKey, itemCostKey)) {
            return;
        }

        if (pdc.has(cooldownKey, PersistentDataType.DOUBLE)) {
            plugin.setCooldown(player, actionId, pdc.get(cooldownKey, PersistentDataType.DOUBLE));
        }

        String command = pdc.get(commandKey, PersistentDataType.STRING);
        if (command == null || command.isEmpty()) return;

        Byte requireTarget = pdc.get(GUIManager.KEY_REQUIRE_TARGET, PersistentDataType.BYTE);
        String executorTypeName = pdc.get(executorKey, PersistentDataType.STRING);
        GUIManager.ExecutorType executor = executorTypeName != null ? GUIManager.ExecutorType.valueOf(executorTypeName) : GUIManager.ExecutorType.PLAYER;

        if (requireTarget != null && requireTarget == 1 && command.contains("{target}")) {
            plugin.setAwaitingTarget(player, new TargetInfo(command, executor));
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "Please enter the target player's name in chat. Type 'cancel' to abort.");
        } else {
            player.closeInventory();
            executeCommand(player, command, executor, null);
        }
    }

    public void executeCommand(Player player, String command, GUIManager.ExecutorType executor, String targetName) {
        String finalCommand = command.replace("%player%", player.getName());
        if (targetName != null) {
            finalCommand = finalCommand.replace("{target}", targetName);
        }

        String commandToExecute = finalCommand;
        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (executor) {
                case CONSOLE:
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute);
                    break;
                case OP:
                    boolean wasOp = player.isOp();
                    try {
                        player.setOp(true);
                        player.performCommand(commandToExecute);
                    } finally {
                        if (!wasOp) {
                            player.setOp(false);
                        }
                    }
                    break;
                case PLAYER:
                default:
                    player.performCommand(commandToExecute);
                    break;
            }
        });
    }
}