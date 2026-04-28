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

        if (subCommand.equals("open")) handleOpen(sender, args);
        else if (subCommand.equals("create")) handleCreate(sender, args);
        else if (subCommand.equals("copy")) handleCopy(sender, args);
        else if (subCommand.equals("edit")) handleEdit(sender, args);
        else if (subCommand.equals("delete")) handleDelete(sender, args);
        else if (subCommand.equals("list")) handleList(sender);
        else if (subCommand.equals("reload")) handleReload(sender);
        else if (subCommand.equals("import")) handleImport(sender, args);
        else sendHelp(sender);

        return true;
    }

    private void handleImport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanager.import")) { // 권한 노드 확인
            sender.sendMessage("You don't have permission");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /gui import <guiplus|deluxemenus>");
            return;
        }

        String type = args[1].toLowerCase();

        if (type.equals("guiplus")) {
            // GUIPlusImporter 연결 부분
            new GUIPlusImporter(plugin).importFromGUIPlus(sender);
        } else if (type.equals("deluxemenus")) {
            if (!(sender instanceof Player)) return;
            if (args.length < 3) return;

            String fileName = args[2];
            if (!fileName.endsWith(".yml")) fileName += ".yml";

            File dmFolder = new File(plugin.getDataFolder().getParentFile(), "DeluxeMenus/gui_menus");
            File targetFile = new File(dmFolder, fileName);

            new DeluxeMenusImporter(plugin).importFromDeluxeMenus((Player) sender, targetFile);
        } else {
            sender.sendMessage("Unknown import type: " + type);
        }
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanageropen")) return;
        if (args.length < 2) return;

        String guiId = args[1];
        GUI gui = plugin.getGui(guiId);

        if (gui == null) return;

        Player target;
        if (args.length >= 3) {
            if (!sender.hasPermission("guimanageropenothers")) return;
            target = Bukkit.getPlayer(args[2]);
            if (target == null) return;
        } else {
            if (!(sender instanceof Player)) return;
            target = (Player) sender;
        }

        Inventory inv = plugin.getPlayerSpecificInventory(target, guiId);
        if (inv != null) {
            target.openInventory(inv);
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanagercreate")) return;
        if (args.length < 4) return;

        String id = args[1];
        if (plugin.getGui(id) != null) return;

        int rows;
        try {
            rows = Integer.parseInt(args[2]);
            if (rows < 1 || rows > 6) return;
        } catch (NumberFormatException e) {
            return;
        }

        StringBuilder titleBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) titleBuilder.append(args[i]).append(" ");
        String title = titleBuilder.toString().trim();

        plugin.createGui(id, rows, title);

        if (sender instanceof Player) {
            plugin.setEditMode((Player) sender, id);
            ((Player) sender).openInventory(plugin.getGui(id).getInventory());
        }
    }

    private void handleCopy(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanagercopy")) return;
        if (args.length < 3) return;

        String sourceId = args[1];
        String targetId = args[2];

        if (plugin.getGui(sourceId) == null || plugin.getGui(targetId) != null) return;

        plugin.copyGui(sourceId, targetId);

        if (sender instanceof Player) {
            GUI newGui = plugin.getGui(targetId);
            if (newGui != null) {
                plugin.setEditMode((Player) sender, targetId);
                ((Player) sender).openInventory(newGui.getInventory());
            }
        }
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        if (!sender.hasPermission("guimanageredit")) return;
        if (args.length < 2) return;

        String id = args[1];
        GUI gui = plugin.getGui(id);
        if (gui == null) return;

        Player player = (Player) sender;
        plugin.setEditMode(player, id);
        player.openInventory(gui.getInventory());
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanagerdelete")) return;
        if (args.length < 2) return;

        String id = args[1];
        if (plugin.getGui(id) == null) return;

        plugin.removeGui(id);
        sender.sendMessage("Deleted");
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("guimanagerlist")) return;
        Set<String> guis = plugin.getGuis().keySet();
        sender.sendMessage(String.join(", ", guis));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("guimanagerreload")) return;
        plugin.loadGuis();
        if (plugin.getLanguageManager() != null) plugin.getLanguageManager().reload();
        sender.sendMessage("Reloaded");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("GUI Commands available");
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
            }
        }

        Collections.sort(completions);
        return completions;
    }
}