package com.gmail.bobason01;

import com.gmail.bobason01.importer.DeluxeMenusImporter;
import com.gmail.bobason01.importer.GUIPlusImporter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GUICommand implements CommandExecutor, TabCompleter {

    private final GUIManager plugin;

    public GUICommand(GUIManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (args.length == 0) {
            sender.sendMessage(lang.getMessage("command.unknown_command"));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("open")) {
            if (!sender.hasPermission("guimanager.open")) {
                sender.sendMessage(lang.getMessage("no_permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(lang.getMessage("command.open.usage"));
                return true;
            }
            String guiId = args[1];
            GUI gui = plugin.getGui(guiId);
            if (gui == null) {
                sender.sendMessage(lang.getMessage("gui_not_exist", "{id}", guiId));
                return true;
            }

            Player targetPlayer;
            if (args.length >= 3) {
                if (!sender.hasPermission("guimanager.open.others")) {
                    sender.sendMessage(lang.getMessage("no_permission"));
                    return true;
                }
                targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer == null) {
                    sender.sendMessage(lang.getMessage("player_not_found", "{player}", args[2]));
                    return true;
                }
            } else {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(lang.getMessage("player_only"));
                    return true;
                }
                targetPlayer = (Player) sender;
            }

            plugin.openGui(targetPlayer, guiId);
            if (sender != targetPlayer) {
                sender.sendMessage(lang.getMessage("command.open.success_other", "{id}", guiId, "{player}", targetPlayer.getName()));
            }
            return true;
        }

        if (sub.equals("import")) {
            if (!sender.hasPermission("guimanager.import")) {
                sender.sendMessage(lang.getMessage("no_permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(lang.getMessage("command.import.usage"));
                return true;
            }
            String type = args[1].toLowerCase();
            if (type.equals("guiplus")) {
                new GUIPlusImporter(plugin).importFromGUIPlus(sender);
            } else if (type.equals("deluxemenus")) {
                if (args.length < 3) {
                    sender.sendMessage(lang.getMessage("command.import.usage"));
                    return true;
                }
                File file = new File("plugins/DeluxeMenus/gui_menus", args[2]);
                new DeluxeMenusImporter(plugin).importFromDeluxeMenus((Player) sender, file);
            } else {
                sender.sendMessage(lang.getMessage("command.import.unsupported", "{plugin}", type));
            }
            return true;
        }

        if (sub.equals("list")) {
            if (!sender.hasPermission("guimanager.list")) {
                sender.sendMessage(lang.getMessage("no_permission"));
                return true;
            }
            if (plugin.getGuis().isEmpty()) {
                sender.sendMessage(lang.getMessage("command.list.no_guis"));
                return true;
            }
            StringBuilder list = new StringBuilder(lang.getMessage("command.list.header"));
            for (String id : plugin.getGuis().keySet()) {
                list.append(id).append(" ");
            }
            sender.sendMessage(list.toString());
            return true;
        }

        if (sub.equals("reload")) {
            if (!sender.hasPermission("guimanager.reload")) {
                sender.sendMessage(lang.getMessage("no_permission"));
                return true;
            }
            plugin.saveGuis();
            plugin.loadGuis();
            plugin.getLanguageManager().reload();
            sender.sendMessage(lang.getMessage("command.reload.success"));
            return true;
        }

        if (sub.equals("delete")) {
            if (!sender.hasPermission("guimanager.delete")) {
                sender.sendMessage(lang.getMessage("no_permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(lang.getMessage("command.delete.usage"));
                return true;
            }
            String guiId = args[1];
            if (plugin.getGui(guiId) == null) {
                sender.sendMessage(lang.getMessage("command.delete.gui_not_found", "{id}", guiId));
                return true;
            }
            plugin.removeGui(guiId);
            sender.sendMessage(lang.getMessage("command.delete.success", "{id}", guiId));
            return true;
        }

        if (sub.equals("copy")) {
            if (!sender.hasPermission("guimanager.copy")) {
                sender.sendMessage(lang.getMessage("no_permission"));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(lang.getMessage("command.copy.usage"));
                return true;
            }
            String sourceId = args[1];
            String newId = args[2];
            GUI sourceGui = plugin.getGui(sourceId);
            if (sourceGui == null) {
                sender.sendMessage(lang.getMessage("command.copy.original_not_exist", "{id}", sourceId));
                return true;
            }
            if (plugin.getGui(newId) != null) {
                sender.sendMessage(lang.getMessage("gui_already_exist", "{id}", newId));
                return true;
            }

            GUI newGui = new GUI(sourceGui.getTitle(), sourceGui.getSize());
            newGui.setId(newId);
            newGui.setPermission(sourceGui.getPermission());
            newGui.setTargets(sourceGui.getTargets());
            sourceGui.getItems().forEach((slot, item) -> newGui.setItem(slot, item.clone()));

            plugin.addGui(newId, newGui);
            plugin.saveGui(newId);
            sender.sendMessage(lang.getMessage("command.copy.success", "{original_id}", sourceId, "{new_id}", newId));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("player_only"));
            return true;
        }
        Player player = (Player) sender;

        if (sub.equals("create")) {
            if (!player.hasPermission("guimanager.create")) {
                player.sendMessage(lang.getMessage("no_permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(lang.getMessage("command.create.usage"));
                return true;
            }

            String guiId = args[1];
            if (plugin.getGui(guiId) != null) {
                player.sendMessage(lang.getMessage("command.create.gui_exists", "{id}", guiId));
                return true;
            }

            int rows = 1;
            String title = "Default GUI";

            if (args.length >= 3) {
                try {
                    rows = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(lang.getMessage("command.create.invalid_number"));
                    rows = 1;
                }
            }

            if (args.length >= 4) {
                StringBuilder titleBuilder = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    titleBuilder.append(args[i]).append(" ");
                }
                title = GUIManager.color(titleBuilder.toString().trim());
            }

            if (rows < 1 || rows > 6) {
                player.sendMessage(lang.getMessage("command.create.invalid_rows"));
                return true;
            }

            plugin.createGui(guiId, rows, title);
            player.sendMessage(lang.getMessage("command.create.success", "{id}", guiId));

            GUI createdGui = plugin.getGui(guiId);
            if (createdGui != null) {
                MainGuiEditor.open(player, createdGui);
            }
            return true;
        }

        if (sub.equals("edit")) {
            if (!player.hasPermission("guimanager.edit")) {
                player.sendMessage(lang.getMessage("no_permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(lang.getMessage("command.edit.usage"));
                return true;
            }
            String guiId = args[1];
            GUI gui = plugin.getGui(guiId);
            if (gui == null) {
                player.sendMessage(lang.getMessage("gui_not_exist", "{id}", guiId));
                return true;
            }
            MainGuiEditor.open(player, gui);
            return true;
        }

        sender.sendMessage(lang.getMessage("command.unknown_command"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("create");
            completions.add("edit");
            completions.add("open");
            completions.add("list");
            completions.add("reload");
            completions.add("delete");
            completions.add("copy");
            completions.add("import");
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("edit") || sub.equals("open") || sub.equals("delete") || sub.equals("copy")) {
                completions.addAll(plugin.getGuis().keySet());
            } else if (sub.equals("import")) {
                completions.add("guiplus");
                completions.add("deluxemenus");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            for (int i = 1; i <= 6; i++) {
                completions.add(String.valueOf(i));
            }
        }
        return completions;
    }
}