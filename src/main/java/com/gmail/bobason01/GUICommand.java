package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GUICommand implements CommandExecutor, TabCompleter {

    private final GUIManager plugin;

    public GUICommand(GUIManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "open":
                handleOpen(sender, args);
                break;
            case "create":
                handleCreate(sender, args);
                break;
            case "edit":
                handleEdit(sender, args);
                break;
            case "delete":
                handleDelete(sender, args);
                break;
            case "list":
                handleList(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            default:
                sender.sendMessage(plugin.getLanguageManager().getMessage("command.unknown_command"));
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanager.open")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.no_permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.usage_open"));
            return;
        }

        String guiId = args[1];
        GUI gui = plugin.getGui(guiId);

        if (gui == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.gui_not_found", "{id}", guiId));
            return;
        }

        Player target;
        if (args.length >= 3) {
            if (!sender.hasPermission("guimanager.open.others")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("command.no_permission"));
                return;
            }
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("command.player_not_found"));
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Console must specify a player name");
                return;
            }
            target = (Player) sender;
        }

        Inventory inv = plugin.getPlayerSpecificInventory(target, guiId);
        if (inv != null) {
            target.openInventory(inv);
        } else {
            sender.sendMessage("Failed to create GUI inventory");
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanager.create")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.no_permission"));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.usage_create"));
            return;
        }

        String id = args[1];
        if (plugin.getGui(id) != null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.gui_exists", "{id}", id));
            return;
        }

        int rows;
        try {
            rows = Integer.parseInt(args[2]);
            if (rows < 1 || rows > 6) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("command.invalid_rows"));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.invalid_number"));
            return;
        }

        StringBuilder titleBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            titleBuilder.append(args[i]).append(" ");
        }
        String title = titleBuilder.toString().trim();

        plugin.createGui(id, rows, title);
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.created", "{id}", id));

        if (sender instanceof Player) {
            plugin.setEditMode((Player) sender, id);
            ((Player) sender).openInventory(plugin.getGui(id).getInventory());
        }
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can edit GUIs");
            return;
        }

        if (!sender.hasPermission("guimanager.edit")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.no_permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.usage_edit"));
            return;
        }

        String id = args[1];
        GUI gui = plugin.getGui(id);

        if (gui == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.gui_not_found", "{id}", id));
            return;
        }

        Player player = (Player) sender;
        plugin.setEditMode(player, id);
        player.openInventory(gui.getInventory());
        player.sendMessage(plugin.getLanguageManager().getMessage("command.edit_mode", "{id}", id));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanager.delete")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.no_permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.usage_delete"));
            return;
        }

        String id = args[1];
        if (plugin.getGui(id) == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.gui_not_found", "{id}", id));
            return;
        }

        plugin.removeGui(id);
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.deleted", "{id}", id));
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("guimanager.list")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.no_permission"));
            return;
        }

        Set<String> guis = plugin.getGuis().keySet();
        if (guis.isEmpty()) {
            sender.sendMessage("No GUIs loaded");
        } else {
            sender.sendMessage("Loaded GUIs " + String.join(", ", guis));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("guimanager.reload")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.no_permission"));
            return;
        }

        plugin.loadGuis();
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.reloaded"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("GUIManager Help");
        sender.sendMessage("/gui open <id> [player]");
        sender.sendMessage("/gui create <id> <rows> <title>");
        sender.sendMessage("/gui edit <id>");
        sender.sendMessage("/gui delete <id>");
        sender.sendMessage("/gui list");
        sender.sendMessage("/gui reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            if (sender.hasPermission("guimanager.open")) subCommands.add("open");
            if (sender.hasPermission("guimanager.create")) subCommands.add("create");
            if (sender.hasPermission("guimanager.edit")) subCommands.add("edit");
            if (sender.hasPermission("guimanager.delete")) subCommands.add("delete");
            if (sender.hasPermission("guimanager.list")) subCommands.add("list");
            if (sender.hasPermission("guimanager.reload")) subCommands.add("reload");

            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("open") || sub.equals("edit") || sub.equals("delete")) {
                for (String id : plugin.getGuis().keySet()) {
                    if (id.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(id);
                    }
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("open") && sender.hasPermission("guimanager.open.others")) {
                return null;
            }
        }

        return completions;
    }
}