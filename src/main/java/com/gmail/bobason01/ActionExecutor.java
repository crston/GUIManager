package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ActionExecutor {

    private final GUIManager plugin;
    private final GUIListener guiListener;

    public ActionExecutor(GUIManager plugin, GUIListener guiListener) {
        this.plugin = plugin;
        this.guiListener = guiListener;
    }

    public void execute(Player player, String guiId, int slot, ItemStack item, ClickType clickType) {
        if (item == null || item.getType().isAir()) return;

        String actionKey = getActionKey(clickType, player.isSneaking());
        if (actionKey == null) return;

        GuiItemMeta.Variant variant = plugin.getMetaCache().getVariant(guiId, slot, actionKey);

        if (variant == null || variant.command == null || variant.command.isEmpty()) {
            return;
        }

        String actionId = guiId + slot + clickType.name() + (player.isSneaking() ? "SHIFT" : "");

        executeVariantLogic(player, variant, actionId);
    }

    public void execute(Player player, ItemStack item, ActionKeyUtil.KeyAction keyAction) {
        if (item == null || item.getType().isAir()) return;

        String actionKey = keyAction.name();
        GuiItemMeta meta = MetaExtractor.extract(plugin, item);
        GuiItemMeta.Variant variant = meta.get(actionKey);

        if (variant == null || variant.command == null || variant.command.isEmpty()) {
            return;
        }

        String actionId = player.getName() + actionKey + variant.command.hashCode();

        executeVariantLogic(player, variant, actionId);
    }

    private void executeVariantLogic(Player player, GuiItemMeta.Variant variant, String actionId) {
        long remainingCooldown = plugin.getRemainingCooldownMillis(player, actionId);
        if (remainingCooldown > 0) {
            double sec = remainingCooldown / 1000.0;
            double rounded = Math.round(sec * 10.0) / 10.0;
            player.sendMessage(ChatColor.RED + "You must wait " + rounded + " more seconds");
            return;
        }

        if (variant.permission != null && !variant.permission.isEmpty() && !player.hasPermission(variant.permission)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou don't have permission"));
            return;
        }

        if (!CostBridge.checkAndTake(player, plugin, variant.moneyCost, variant.parsedItemCosts)) {
            return;
        }

        if (variant.parsedItemRewards != null && variant.parsedItemRewards.length > 0) {
            for (ItemStack reward : variant.parsedItemRewards) {
                if (reward != null && !reward.getType().isAir()) {
                    player.getInventory().addItem(reward.clone()).values().forEach(leftover -> {
                        player.getWorld().dropItem(player.getLocation(), leftover);
                    });
                }
            }
        }

        if (variant.cooldownSeconds > 0) {
            plugin.setCooldown(player, actionId, variant.cooldownSeconds);
        }

        String command = variant.command;

        if (variant.requireTarget && command.indexOf("{target}") != -1) {
            plugin.setAwaitingTarget(player, new TargetInfo(command, variant.executor));
            if (!variant.keepOpen) player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "Please enter the target player name in chat Type cancel to abort");
        } else {
            if (!variant.keepOpen) player.closeInventory();
            executeCommand(player, command, variant.executor, null);
        }
    }

    private String getActionKey(ClickType t, boolean isSneaking) {
        if (t == ClickType.SWAP_OFFHAND) return isSneaking ? GuiItemMeta.SHIFT_F : GuiItemMeta.F;
        if (t == ClickType.DROP) return GuiItemMeta.Q;
        if (t == ClickType.CONTROL_DROP) return GuiItemMeta.SHIFT_Q;
        if (t.isShiftClick()) return t.isLeftClick() ? GuiItemMeta.SHIFT_LEFT : GuiItemMeta.SHIFT_RIGHT;
        if (t.isLeftClick()) return GuiItemMeta.LEFT;
        if (t.isRightClick()) return GuiItemMeta.RIGHT;
        return null;
    }

    public void executeCommand(Player player, String command, GUIManager.ExecutorType executor, String targetName) {
        String finalCommand = command.replace("%player%", player.getName());
        if (targetName != null) finalCommand = finalCommand.replace("{target}", targetName);

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
                        if (!wasOp) player.setOp(false);
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