package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GUICommand implements CommandExecutor, TabCompleter {

    private final GUIManager plugin;

    public GUICommand(GUIManager plugin) {
        this.plugin = plugin;
    }

    @FunctionalInterface
    private interface CommandHandler {
        void handle(Player player, String[] args);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "create":
                wrapAdminCommand(player, this::handleCreate, "guimanager.admin.create", args);
                break;
            case "delete":
                wrapAdminCommand(player, this::handleDelete, "guimanager.admin.delete", args);
                break;
            case "edit":
                wrapAdminCommand(player, this::handleEdit, "guimanager.admin.edit", args);
                break;
            case "editname":
                wrapAdminCommand(player, this::handleEditName, "guimanager.admin.edit", args);
                break;
            case "edittitle":
                wrapAdminCommand(player, this::handleEditTitle, "guimanager.admin.edit", args);
                break;
            case "editid":
                wrapAdminCommand(player, this::handleEditId, "guimanager.admin.edit", args);
                break;
            case "editsize":
                wrapAdminCommand(player, this::handleEditSize, "guimanager.admin.edit", args);
                break;
            case "copy":
                wrapAdminCommand(player, this::handleCopy, "guimanager.admin.copy", args);
                break;
            case "list":
                wrapAdminCommand(player, this::handleList, "guimanager.admin.list", args);
                break;
            case "reload":
                wrapAdminCommand(player, this::handleReload, "guimanager.admin.reload", args);
                break;
            case "import":
                wrapAdminCommand(player, this::handleImport, "guimanager.admin.import", args);
                break;
            case "open":
                handleOpen(player, args);
                break;
            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    private void wrapAdminCommand(Player player, CommandHandler handler, String permission, String[] args) {
        if (!player.hasPermission(permission)) {
            noPermission(player);
            return;
        }
        handler.handle(player, args);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        final List<String> completions = new ArrayList<>();
        final String currentArg = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            final List<String> commands = new ArrayList<>();
            if (sender.hasPermission("guimanager.admin")) {
                commands.addAll(Arrays.asList("create", "delete", "edit", "copy", "list", "reload", "editname", "edittitle", "editid", "editsize", "import"));
            }
            commands.add("open");
            StringUtil.copyPartialMatches(currentArg, commands, completions);
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "delete":
                case "edit":
                case "open":
                case "copy":
                case "editname":
                case "edittitle":
                case "editid":
                case "editsize":
                    StringUtil.copyPartialMatches(currentArg, plugin.getGuis().keySet(), completions);
                    break;
                case "import":
                    StringUtil.copyPartialMatches(currentArg, Arrays.asList("guiplus"), completions);
                    break;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("open")) {
            if (sender.hasPermission("guimanager.open.others")) {
                StringUtil.copyPartialMatches(currentArg, Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), completions);
            }
        }
        return completions;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /gui create <id> <lines> <title>");
            return;
        }
        String id = args[1].toLowerCase();
        if (plugin.getGui(id) != null) {
            player.sendMessage(ChatColor.RED + "A GUI with that ID already exists.");
            return;
        }
        int lines;
        try {
            lines = Integer.parseInt(args[2]);
            if (lines < 1 || lines > 6) {
                player.sendMessage(ChatColor.RED + "Lines must be between 1 and 6.");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Lines must be a number.");
            return;
        }
        String title = ChatColor.translateAlternateColorCodes('&', String.join(" ", Arrays.copyOfRange(args, 3, args.length)));

        GUI gui = new GUI(title, lines * 9);
        plugin.addGui(id, gui);
        plugin.saveGui(id);
        player.sendMessage(ChatColor.GREEN + "GUI '" + id + "' created.");

        plugin.setEditMode(player, id);
        player.openInventory(gui.getInventory());
        player.sendMessage(ChatColor.YELLOW + "Entering edit mode for the new GUI.");
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /gui delete <id>");
            return;
        }
        String id = args[1].toLowerCase();
        if (plugin.getGui(id) == null) {
            player.sendMessage(ChatColor.RED + "That GUI does not exist.");
            return;
        }
        plugin.removeGui(id);
        player.sendMessage(ChatColor.GREEN + "GUI '" + id + "' has been deleted.");
    }

    private void handleEdit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /gui edit <id> [slot]");
            return;
        }
        String id = args[1].toLowerCase();
        GUI gui = plugin.getGui(id);
        if (gui == null) {
            player.sendMessage(ChatColor.RED + "That GUI does not exist.");
            return;
        }
        if (args.length >= 3) {
            try {
                int slot = Integer.parseInt(args[2]);
                if (slot < 0 || slot >= gui.getSize()) {
                    player.sendMessage(ChatColor.RED + "Invalid slot number.");
                    return;
                }
                ItemStack item = gui.getItem(slot);
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(ChatColor.RED + "There is no item in slot " + slot + " to edit.");
                    return;
                }
                EditSession session = new EditSession(id, slot, item.clone());
                ItemEditor.open(player, session);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid slot number provided. Opening main editor instead.");
                openMainEditor(player, id, gui);
            }
        } else {
            openMainEditor(player, id, gui);
        }
    }

    private void openMainEditor(Player player, String id, GUI gui) {
        plugin.setEditMode(player, id);
        player.openInventory(gui.getInventory());
        player.sendMessage(ChatColor.GREEN + "Now editing item layout for '" + id + "'.");
    }

    private void handleEditName(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /gui editname <id> <slot> <new_item_name>");
            return;
        }
        String id = args[1].toLowerCase();
        GUI gui = plugin.getGui(id);
        if (gui == null) {
            player.sendMessage(ChatColor.RED + "That GUI does not exist.");
            return;
        }
        try {
            int slot = Integer.parseInt(args[2]);
            if (slot < 0 || slot >= gui.getSize()) {
                player.sendMessage(ChatColor.RED + "Invalid slot number.");
                return;
            }
            ItemStack item = gui.getItem(slot);
            if (item == null || item.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "There is no item in slot " + slot + ".");
                return;
            }
            String newName = ChatColor.translateAlternateColorCodes('&', String.join(" ", Arrays.copyOfRange(args, 3, args.length)));
            ItemMeta meta = item.getItemMeta();
            if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(item.getType());
            assert meta != null;
            meta.setDisplayName(newName);
            item.setItemMeta(meta);
            plugin.saveGui(id);
            player.sendMessage(ChatColor.GREEN + "Item name in slot " + slot + " of GUI '" + id + "' updated.");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Slot must be a number.");
        }
    }

    private void handleEditTitle(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /gui edittitle <id> <new_title>");
            return;
        }
        String id = args[1].toLowerCase();
        if (plugin.getGui(id) == null) {
            player.sendMessage(ChatColor.RED + "That GUI does not exist.");
            return;
        }
        String newTitle = ChatColor.translateAlternateColorCodes('&', String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
        plugin.updateGuiTitle(id, newTitle);
        plugin.saveGui(id);
        player.sendMessage(ChatColor.GREEN + "GUI '" + id + "' title updated.");
    }

    private void handleEditId(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /gui editid <id> <new_id>");
            return;
        }
        String oldId = args[1].toLowerCase();
        String newId = args[2].toLowerCase();
        if (plugin.getGui(oldId) == null) {
            player.sendMessage(ChatColor.RED + "GUI with ID '" + oldId + "' does not exist.");
            return;
        }
        if (plugin.getGui(newId) != null) {
            player.sendMessage(ChatColor.RED + "A GUI with ID '" + newId + "' already exists.");
            return;
        }
        plugin.updateGuiId(oldId, newId);
        plugin.saveGui(newId);
        player.sendMessage(ChatColor.GREEN + "GUI ID '" + oldId + "' changed to '" + newId + "'.");
    }

    private void handleEditSize(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /gui editsize <id> <lines>");
            return;
        }
        String id = args[1].toLowerCase();
        if (plugin.getGui(id) == null) {
            player.sendMessage(ChatColor.RED + "That GUI does not exist.");
            return;
        }
        try {
            int lines = Integer.parseInt(args[2]);
            if (lines < 1 || lines > 6) {
                player.sendMessage(ChatColor.RED + "Lines must be between 1 and 6.");
                return;
            }
            plugin.updateGuiSize(id, lines);
            plugin.saveGui(id);
            player.sendMessage(ChatColor.GREEN + "GUI '" + id + "' size has been updated to " + lines + " lines (" + (lines * 9) + " slots).");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Lines must be a number.");
        }
    }

    private void handleCopy(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /gui copy <original_id> <new_id>");
            return;
        }
        String originalId = args[1].toLowerCase();
        String newId = args[2].toLowerCase();
        GUI originalGui = plugin.getGui(originalId);
        if (originalGui == null) {
            player.sendMessage(ChatColor.RED + "Original GUI '" + originalId + "' does not exist.");
            return;
        }
        if (plugin.getGui(newId) != null) {
            player.sendMessage(ChatColor.RED + "A GUI with the id '" + newId + "' already exists.");
            return;
        }
        GUI newGui = new GUI(originalGui);
        plugin.addGui(newId, newGui);
        plugin.saveGui(newId);
        player.sendMessage(ChatColor.GREEN + "Successfully copied '" + originalId + "' to '" + newId + "'.");
    }

    private void handleList(Player player, String[] args) {
        if (plugin.getGuis().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "There are no GUIs created yet.");
            return;
        }
        String guiList = String.join(", ", plugin.getGuis().keySet().stream().sorted().collect(Collectors.toList()));
        player.sendMessage(ChatColor.GREEN + "Available GUI IDs: " + ChatColor.WHITE + guiList);
    }

    private void handleReload(Player player, String[] args) {
        plugin.loadGuis();
        player.sendMessage(ChatColor.GREEN + "GUIManager config reloaded.");
    }

    private void handleImport(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /gui import <plugin>");
            player.sendMessage(ChatColor.YELLOW + "Supported plugins: guiplus");
            return;
        }
        String pluginToImport = args[1].toLowerCase();
        if (pluginToImport.equals("guiplus")) {
            GUIPlusImporter importer = new GUIPlusImporter(plugin);
            importer.importFromGUIPlus(player);
        } else {
            player.sendMessage(ChatColor.RED + "Unsupported plugin for import: " + args[1]);
        }
    }

    private void handleOpen(Player sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /gui open <id> [player]");
            return;
        }
        String id = args[1].toLowerCase();
        GUI gui = plugin.getGui(id);
        if (gui == null) {
            sender.sendMessage(ChatColor.RED + "That GUI does not exist.");
            return;
        }

        Player target = sender;
        if (args.length >= 3) {
            if (!sender.hasPermission("guimanager.open.others")) {
                noPermission(sender);
                return;
            }
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[2] + "' not found.");
                return;
            }
        }

        Inventory playerInventory = plugin.getPlayerSpecificInventory(target, id);
        if (playerInventory != null) {
            target.openInventory(playerInventory);
            if (target != sender) {
                sender.sendMessage(ChatColor.GREEN + "Opened GUI '" + id + "' for " + target.getName() + ".");
            }
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "--- GUIManager Help ---");
        if (player.hasPermission("guimanager.admin")) {
            player.sendMessage(ChatColor.GOLD + "/gui create <id> <lines> <title>");
            player.sendMessage(ChatColor.GOLD + "/gui delete <id>");
            player.sendMessage(ChatColor.GOLD + "/gui list");
            player.sendMessage(ChatColor.GOLD + "/gui copy <original_id> <new_id>");
            player.sendMessage(ChatColor.GOLD + "/gui reload");
            player.sendMessage(ChatColor.GOLD + "/gui edit <id> [slot] - (GUI Editor)");
            player.sendMessage(ChatColor.GOLD + "/gui editname <id> <slot> <name> - (Gui Name Change)");
            player.sendMessage(ChatColor.GOLD + "/gui edittitle <id> <title> - (GUI Title Change)");
            player.sendMessage(ChatColor.GOLD + "/gui editid <id> <new_id> - (GUI ID Change)");
            player.sendMessage(ChatColor.GOLD + "/gui editsize <id> <lines> - (GUI Size Change)");
            player.sendMessage(ChatColor.GOLD + "/gui import <guiplus> - (Import from GUIPlus)");
        }
        player.sendMessage(ChatColor.GOLD + "/gui open <id> [player]");
    }

    private void noPermission(Player player) {
        player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
    }
}