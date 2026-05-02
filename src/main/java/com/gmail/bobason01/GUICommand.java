package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
            case "copy":
                handleCopy(sender, args);
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
            case "import":
                handleImport(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleImport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanager.import")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /gui import <guiplus|deluxemenus>");
            return;
        }

        String type = args[1].toLowerCase();

        if (type.equals("guiplus")) {
            new GUIPlusImporter(plugin).importFromGUIPlus(sender);
        } else if (type.equals("deluxemenus")) {
            if (!(sender instanceof Player)) return;
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /gui import deluxemenus <filename>");
                return;
            }

            String fileName = args[2];
            if (!fileName.endsWith(".yml")) fileName += ".yml";

            File dmFolder = new File(plugin.getDataFolder().getParentFile(), "DeluxeMenus/gui_menus");
            File targetFile = new File(dmFolder, fileName);

            new DeluxeMenusImporter(plugin).importFromDeluxeMenus((Player) sender, targetFile);
        } else {
            sender.sendMessage("§cUnknown import type: " + type);
        }
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanager.open")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /gui open <id> [player]");
            return;
        }

        String guiId = args[1];
        GUI gui = plugin.getGui(guiId);

        if (gui == null) {
            sender.sendMessage("§cGUI not found.");
            return;
        }

        Player target;
        if (args.length >= 3) {
            if (!sender.hasPermission("guimanager.open.others")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
                return;
            }
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return;
            }
        } else {
            if (!(sender instanceof Player)) return;
            target = (Player) sender;
        }

        plugin.removeEditMode(target);

        Inventory inv = plugin.getPlayerSpecificInventory(target, guiId);
        if (inv != null) {
            target.openInventory(inv);
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanager.create")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /gui create <id> <rows> <title>");
            return;
        }

        String id = args[1];
        if (plugin.getGui(id) != null) {
            sender.sendMessage("§cGUI ID already exists.");
            return;
        }

        int rows;
        try {
            rows = Integer.parseInt(args[2]);
            if (rows < 1 || rows > 6) {
                sender.sendMessage("§cRows must be 1-6.");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid row number.");
            return;
        }

        StringBuilder titleBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) titleBuilder.append(args[i]).append(" ");
        String title = titleBuilder.toString().trim();

        plugin.createGui(id, rows, title);
        sender.sendMessage("§aGUI '" + id + "' created.");

        if (sender instanceof Player) {
            Player player = (Player) sender;
            plugin.setEditMode(player, id);
            Inventory inv = plugin.getEditInventory(id);
            if (inv != null) player.openInventory(inv);
        }
    }

    private void handleCopy(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanager.copy")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /gui copy <source> <target>");
            return;
        }

        String sourceId = args[1];
        String targetId = args[2];

        if (plugin.getGui(sourceId) == null) {
            sender.sendMessage("§cSource GUI not found.");
            return;
        }
        if (plugin.getGui(targetId) != null) {
            sender.sendMessage("§cTarget ID already exists.");
            return;
        }

        plugin.copyGui(sourceId, targetId);
        sender.sendMessage("§aGUI copied from " + sourceId + " to " + targetId);

        if (sender instanceof Player) {
            Player player = (Player) sender;
            plugin.setEditMode(player, targetId);
            Inventory inv = plugin.getEditInventory(targetId);
            if (inv != null) player.openInventory(inv);
        }
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        if (!sender.hasPermission("guimanager.edit")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /gui edit <id>");
            return;
        }

        String id = args[1];
        GUI gui = plugin.getGui(id);
        if (gui == null) {
            sender.sendMessage("§cGUI not found.");
            return;
        }

        Player player = (Player) sender;
        plugin.setEditMode(player, id);

        Inventory inv = plugin.getEditInventory(id);
        if (inv != null) {
            player.openInventory(inv);
            sender.sendMessage("§eEditing GUI: " + id);
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanager.delete")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /gui delete <id>");
            return;
        }

        String id = args[1];
        if (plugin.getGui(id) == null) {
            sender.sendMessage("§cGUI not found.");
            return;
        }

        plugin.removeGui(id);
        sender.sendMessage("§aGUI '" + id + "' deleted.");
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("guimanager.list")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return;
        }
        Set<String> guis = plugin.getGuis().keySet();
        sender.sendMessage("§6Registered GUIs: §f" + String.join(", ", guis));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("guimanager.reload")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return;
        }
        plugin.loadGuis();
        if (plugin.getLanguageManager() != null) plugin.getLanguageManager().reload();
        sender.sendMessage("§aConfiguration and GUIs reloaded.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m-------§r §6GUI Manager Help §8§m-------");
        sender.sendMessage("§e/gui open <id> [player] §7- Open a GUI");
        sender.sendMessage("§e/gui edit <id> §7- Edit a GUI");
        sender.sendMessage("§e/gui create <id> <rows> <title> §7- Create a new GUI");
        sender.sendMessage("§e/gui copy <source> <target> §7- Copy a GUI");
        sender.sendMessage("§e/gui delete <id> §7- Delete a GUI");
        sender.sendMessage("§e/gui list §7- List all GUIs");
        sender.sendMessage("§e/gui reload §7- Reload configuration");
        sender.sendMessage("§e/gui import <type> §7- Import from other plugins");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String[] subs = {"open", "create", "copy", "edit", "delete", "list", "reload", "import"};
            for (String s : subs) if (s.startsWith(args[0].toLowerCase())) completions.add(s);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("open") || sub.equals("edit") || sub.equals("delete") || sub.equals("copy")) {
                for (String id : plugin.getGuis().keySet()) {
                    if (id.toLowerCase().startsWith(args[1].toLowerCase())) completions.add(id);
                }
            } else if (sub.equals("import")) {
                if ("deluxemenus".startsWith(args[1].toLowerCase())) completions.add("deluxemenus");
                if ("guiplus".startsWith(args[1].toLowerCase())) completions.add("guiplus");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("open")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) completions.add(p.getName());
            }
        }

        Collections.sort(completions);
        return completions;
    }
}