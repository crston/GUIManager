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
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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
        if (args.length == 0) {
            if (sender instanceof Player) {
                sendHelp((Player) sender);
            } else {
                sender.sendMessage(plugin.getLanguageManager().getMessage("command.usage.console_main"));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "open":
                handleOpen(sender, args);
                return true;
            case "reload":
                if (sender.hasPermission("guimanager.admin.reload")) {
                    handleReload(sender, args);
                } else {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
                }
                return true;
            case "list":
                if (sender.hasPermission("guimanager.admin.list")) {
                    handleList(sender, args);
                } else {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
                }
                return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player_only"));
            return true;
        }
        Player player = (Player) sender;

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
            case "import":
                wrapAdminCommand(player, this::handleImport, "guimanager.admin.import", args);
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
                commands.addAll(Arrays.asList("create", "delete", "edit", "copy", "list", "reload", "edittitle", "editid", "editsize", "import"));
            }
            commands.add("open");
            StringUtil.copyPartialMatches(currentArg, commands, completions);
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "delete":
                case "edit":
                case "open":
                case "copy":
                case "edittitle":
                case "editid":
                case "editsize":
                    StringUtil.copyPartialMatches(currentArg, plugin.getGuis().keySet(), completions);
                    break;
                case "import":
                    StringUtil.copyPartialMatches(currentArg, Arrays.asList("guiplus", "deluxemenus"), completions);
                    break;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("open")) {
            if (!(sender instanceof Player) || sender.hasPermission("guimanager.open.others")) {
                StringUtil.copyPartialMatches(currentArg, Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), completions);
            }
        }
        return completions;
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length < 2) {
                player.sendMessage(plugin.getLanguageManager().getMessage("command.open.usage"));
                return;
            }
            String id = args[1].toLowerCase();
            GUI gui = plugin.getGui(id);
            if (gui == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("gui_not_exist", "{id}", id));
                return;
            }
            if (!player.hasPermission("guimanager.admin") && !player.hasPermission("guimanager.open." + id)) {
                noPermission(player);
                return;
            }
            Player target = player;
            if (args.length >= 3) {
                if (!player.hasPermission("guimanager.open.others")) {
                    noPermission(player);
                    return;
                }
                target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("player_not_found", "{player}", args[2]));
                    return;
                }
            }
            Inventory playerInventory = plugin.getPlayerSpecificInventory(target, id);
            if (playerInventory != null) {
                target.openInventory(playerInventory);
                if (target != player) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("command.open.success_other", "{id}", id, "{player}", target.getName()));
                }
            }
        } else {
            if (args.length < 3) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("command.open.usage_console"));
                return;
            }
            String id = args[1].toLowerCase();
            String targetName = args[2];
            GUI gui = plugin.getGui(id);
            if (gui == null) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("gui_not_exist", "{id}", id));
                return;
            }
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_found", "{player}", targetName));
                return;
            }
            Inventory playerInventory = plugin.getPlayerSpecificInventory(target, id);
            if (playerInventory != null) {
                target.openInventory(playerInventory);
            }
        }
    }

    private void handleReload(CommandSender sender, String[] args) {
        plugin.loadGuis();
        plugin.getLanguageManager().reloadLangFile();
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.reload.success"));
    }

    private void handleList(CommandSender sender, String[] args) {
        if (plugin.getGuis().isEmpty()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("command.list.no_guis"));
            return;
        }
        String guiList = String.join(", ", plugin.getGuis().keySet().stream().sorted().collect(Collectors.toList()));
        sender.sendMessage(plugin.getLanguageManager().getMessage("command.list.header") + guiList);
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(plugin.getLanguageManager().getMessage("command.create.usage"));
            return;
        }
        String id = args[1].toLowerCase();
        if (plugin.getGui(id) != null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("gui_already_exist", "{id}", id));
            return;
        }
        int lines;
        try {
            lines = Integer.parseInt(args[2]);
            if (lines < 1 || lines > 6) {
                player.sendMessage(plugin.getLanguageManager().getMessage("command.create.invalid_lines"));
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getLanguageManager().getMessage("command.create.invalid_lines"));
            return;
        }
        String title = ChatColor.translateAlternateColorCodes('&', String.join(" ", Arrays.copyOfRange(args, 3, args.length)));

        GUI gui = new GUI(title, lines * 9);
        plugin.addGui(id, gui);
        plugin.saveGui(id);
        player.sendMessage(plugin.getLanguageManager().getMessage("command.create.success", "{id}", id));

        plugin.setEditMode(player, id);
        player.openInventory(gui.getInventory());
        player.sendMessage(plugin.getLanguageManager().getMessage("command.create.edit_mode"));
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("command.delete.usage"));
            return;
        }
        String id = args[1].toLowerCase();
        if (plugin.getGui(id) == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("gui_not_exist", "{id}", id));
            return;
        }
        plugin.removeGui(id);
        player.sendMessage(plugin.getLanguageManager().getMessage("command.delete.success", "{id}", id));
    }

    private void handleEdit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("command.edit.usage"));
            return;
        }
        String id = args[1].toLowerCase();
        GUI gui = plugin.getGui(id);
        if (gui == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("gui_not_exist", "{id}", id));
            return;
        }
        if (args.length >= 3) {
            try {
                int slot = Integer.parseInt(args[2]);
                if (slot < 0 || slot >= gui.getSize()) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("command.edit.invalid_slot"));
                    openMainEditor(player, id, gui);
                    return;
                }
                ItemStack item = gui.getItem(slot);
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("command.edit.no_item", "{slot}", String.valueOf(slot)));
                    return;
                }
                EditSession session = new EditSession(id, slot, item.clone());
                ItemEditor.open(player, session);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getLanguageManager().getMessage("command.edit.invalid_slot"));
                openMainEditor(player, id, gui);
            }
        } else {
            openMainEditor(player, id, gui);
        }
    }

    private void openMainEditor(Player player, String id, GUI gui) {
        plugin.setEditMode(player, id);
        player.openInventory(gui.getInventory());
        player.sendMessage(plugin.getLanguageManager().getMessage("command.edit.main_editor_info", "{id}", id));
    }

    private void handleEditTitle(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getLanguageManager().getMessage("command.edittitle.usage"));
            return;
        }
        String id = args[1].toLowerCase();
        if (plugin.getGui(id) == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("gui_not_exist", "{id}", id));
            return;
        }
        String newTitle = ChatColor.translateAlternateColorCodes('&', String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
        plugin.updateGuiTitle(id, newTitle);
        plugin.saveGui(id);
        player.sendMessage(plugin.getLanguageManager().getMessage("command.edittitle.success", "{id}", id));
    }

    private void handleEditId(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getLanguageManager().getMessage("command.editid.usage"));
            return;
        }
        String oldId = args[1].toLowerCase();
        String newId = args[2].toLowerCase();
        if (plugin.getGui(oldId) == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("gui_not_exist", "{id}", oldId));
            return;
        }
        if (plugin.getGui(newId) != null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("gui_already_exist", "{id}", newId));
            return;
        }
        plugin.updateGuiId(oldId, newId);
        plugin.saveGui(newId);
        player.sendMessage(plugin.getLanguageManager().getMessage("command.editid.success", "{old_id}", oldId, "{new_id}", newId));
    }

    private void handleEditSize(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getLanguageManager().getMessage("command.editsize.usage"));
            return;
        }
        String id = args[1].toLowerCase();
        if (plugin.getGui(id) == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("gui_not_exist", "{id}", id));
            return;
        }
        try {
            int lines = Integer.parseInt(args[2]);
            if (lines < 1 || lines > 6) {
                player.sendMessage(plugin.getLanguageManager().getMessage("command.editsize.invalid_lines"));
                return;
            }
            plugin.updateGuiSize(id, lines);
            plugin.saveGui(id);
            player.sendMessage(plugin.getLanguageManager().getMessage("command.editsize.success", "{id}", id, "{lines}", String.valueOf(lines), "{slots}", String.valueOf(lines * 9)));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getLanguageManager().getMessage("command.editsize.invalid_lines"));
        }
    }

    private void handleCopy(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getLanguageManager().getMessage("command.copy.usage"));
            return;
        }
        String originalId = args[1].toLowerCase();
        String newId = args[2].toLowerCase();
        GUI originalGui = plugin.getGui(originalId);
        if (originalGui == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("command.copy.original_not_exist", "{id}", originalId));
            return;
        }
        if (plugin.getGui(newId) != null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("gui_already_exist", "{id}", newId));
            return;
        }
        GUI newGui = new GUI(originalGui);
        plugin.addGui(newId, newGui);
        plugin.saveGui(newId);
        player.sendMessage(plugin.getLanguageManager().getMessage("command.copy.success", "{original_id}", originalId, "{new_id}", newId));
    }

    private void handleImport(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("command.import.usage"));
            player.sendMessage(plugin.getLanguageManager().getMessage("command.import.supported"));
            player.sendMessage("§7Supported: §eGUIPlus, DeluxeMenus");
            return;
        }

        String pluginToImport = args[1].toLowerCase();

        switch (pluginToImport) {
            case "guiplus": {
                GUIPlusImporter importer = new GUIPlusImporter(plugin);
                importer.importFromGUIPlus(player);
                break;
            }

            case "deluxemenus": {
                // 자동 전체 변환 모드
                File deluxeFolder = new File(
                        Bukkit.getPluginManager().getPlugin("DeluxeMenus").getDataFolder(),
                        "gui_menus"
                );

                if (!deluxeFolder.exists() || !deluxeFolder.isDirectory()) {
                    player.sendMessage("§cDeluxeMenus menus folder not found.");
                    return;
                }

                File[] files = deluxeFolder.listFiles((dir, name) -> name.endsWith(".yml"));
                if (files == null || files.length == 0) {
                    player.sendMessage("§eNo DeluxeMenus .yml files found in /DeluxeMenus/menus/");
                    return;
                }

                DeluxeMenusImporter importer = new DeluxeMenusImporter(plugin);
                int success = 0;
                int failed = 0;

                for (File file : files) {
                    try {
                        importer.importFromDeluxeMenus(player, file);
                        success++;
                    } catch (Exception e) {
                        failed++;
                        plugin.getLogger().warning("Failed to import: " + file.getName() + " (" + e.getMessage() + ")");
                    }
                }

                player.sendMessage("§aImported §f" + success + "§a menu(s) successfully.");
                if (failed > 0) player.sendMessage("§c" + failed + " file(s) failed to import. Check console for details.");
                break;
            }

            default: {
                player.sendMessage(plugin.getLanguageManager().getMessage("command.import.unsupported", "{plugin}", args[1]));
                player.sendMessage("§7Supported plugins: §eGUIPlus, DeluxeMenus");
                break;
            }
        }
    }

    private void sendHelp(Player player) {
        LanguageManager lm = plugin.getLanguageManager();
        player.sendMessage(ChatColor.YELLOW + lm.getMessage("command.help.header"));
        if (player.hasPermission("guimanager.admin")) {
            player.sendMessage(ChatColor.GOLD + lm.getMessage("command.help.create"));
            player.sendMessage(ChatColor.GOLD + lm.getMessage("command.help.delete"));
            player.sendMessage(ChatColor.GOLD + lm.getMessage("command.help.list"));
            player.sendMessage(ChatColor.GOLD + lm.getMessage("command.help.copy"));
            player.sendMessage(ChatColor.GOLD + lm.getMessage("command.help.reload"));
            player.sendMessage(ChatColor.GOLD + lm.getMessage("command.help.edit"));
            player.sendMessage(ChatColor.GOLD + lm.getMessage("command.help.edittitle"));
            player.sendMessage(ChatColor.GOLD + lm.getMessage("command.help.editid"));
            player.sendMessage(ChatColor.GOLD + lm.getMessage("command.help.editsize"));
            player.sendMessage(ChatColor.GOLD + lm.getMessage("command.help.import"));
        }
        player.sendMessage(ChatColor.GOLD + lm.getMessage("command.help.open"));
    }

    private void noPermission(Player player) {
        player.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
    }
}
