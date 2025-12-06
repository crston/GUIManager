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
                sender.sendMessage(plugin.getLanguageManager().getMessage("command.unknown_command"));
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
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.import.usage"));
            return;
        }

        String type = args[1].toLowerCase();

        if (type.equals("guiplus")) {
            // GUIPlus Importer 호출
            new GUIPlusImporter(plugin).importFromGUIPlus(sender);

        } else if (type.equals("deluxemenus")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("player_only"));
                return;
            }

            if (args.length < 3) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("command.import.usage"));
                return;
            }

            String fileName = args[2];
            if (!fileName.endsWith(".yml")) fileName += ".yml";

            // DeluxeMenus/gui_menus 폴더 경로 추정
            File dmFolder = new File(plugin.getDataFolder().getParentFile(), "DeluxeMenus/gui_menus");
            File targetFile = new File(dmFolder, fileName);

            // DeluxeMenus Importer 호출
            new DeluxeMenusImporter(plugin).importFromDeluxeMenus((Player) sender, targetFile);

        } else {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.import.unsupported", "{plugin}", type));
        }
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanager.open")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.open.usage"));
            return;
        }

        String guiId = args[1];
        GUI gui = plugin.getGui(guiId);

        if (gui == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("gui_not_exist", "{id}", guiId));
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
                sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_found", "{player}", args[2]));
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("player_only"));
                return;
            }
            target = (Player) sender;
        }

        Inventory inv = plugin.getPlayerSpecificInventory(target, guiId);
        if (inv != null) {
            target.openInventory(inv);
            if (args.length >= 3) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("command.open.success_other", "{id}", guiId, "{player}", target.getName()));
            }
        } else {
            sender.sendMessage("§cFailed to create GUI inventory.");
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanager.create")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.create.usage"));
            return;
        }

        String id = args[1];
        if (plugin.getGui(id) != null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.create.gui_exists", "{id}", id));
            return;
        }

        int rows;
        try {
            rows = Integer.parseInt(args[2]);
            if (rows < 1 || rows > 6) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("command.create.invalid_rows"));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.create.invalid_number"));
            return;
        }

        StringBuilder titleBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            titleBuilder.append(args[i]).append(" ");
        }
        String title = titleBuilder.toString().trim();

        plugin.createGui(id, rows, title);
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.create.success", "{id}", id));

        if (sender instanceof Player) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.create.edit_mode"));
            plugin.setEditMode((Player) sender, id);
            ((Player) sender).openInventory(plugin.getGui(id).getInventory());
        }
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player_only"));
            return;
        }

        if (!sender.hasPermission("guimanager.edit")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.edit.usage"));
            return;
        }

        String id = args[1];
        GUI gui = plugin.getGui(id);

        if (gui == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("gui_not_exist", "{id}", id));
            return;
        }

        Player player = (Player) sender;
        plugin.setEditMode(player, id);
        player.openInventory(gui.getInventory());
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.edit.edit_mode", "{id}", id));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guimanager.delete")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.delete.usage"));
            return;
        }

        String id = args[1];
        if (plugin.getGui(id) == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("gui_not_exist", "{id}", id));
            return;
        }

        plugin.removeGui(id);
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.delete.success", "{id}", id));
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("guimanager.list")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return;
        }

        Set<String> guis = plugin.getGuis().keySet();
        if (guis.isEmpty()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.list.no_guis"));
        } else {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.list.header") + String.join(", ", guis));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("guimanager.reload")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return;
        }

        plugin.loadGuis();
        // LanguageManager 리로드 기능이 있다면 호출 (권장)
        if (plugin.getLanguageManager() != null) {
            plugin.getLanguageManager().reload();
        }
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.reload.success"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.help.header"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.help.open"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.help.create"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.help.edit"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.help.delete"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.help.list"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.help.reload"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.help.import"));
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
            if (sender.hasPermission("guimanager.import")) subCommands.add("import");

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
            } else if (sub.equals("import")) {
                if ("deluxemenus".startsWith(args[1].toLowerCase())) completions.add("deluxemenus");
                if ("guiplus".startsWith(args[1].toLowerCase())) completions.add("guiplus");
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("open") && sender.hasPermission("guimanager.open.others")) {
                return null; // Bukkit handles player list
            } else if (args[0].equalsIgnoreCase("import") && args[1].equalsIgnoreCase("deluxemenus")) {
                File dmFolder = new File(plugin.getDataFolder().getParentFile(), "DeluxeMenus/gui_menus");
                if (dmFolder.exists() && dmFolder.isDirectory()) {
                    String[] files = dmFolder.list((dir, name) -> name.endsWith(".yml"));
                    if (files != null) {
                        for (String f : files) {
                            if (f.toLowerCase().startsWith(args[2].toLowerCase())) {
                                completions.add(f);
                            }
                        }
                    }
                }
            }
        }

        Collections.sort(completions);
        return completions;
    }
}