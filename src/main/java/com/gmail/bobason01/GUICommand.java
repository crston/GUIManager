package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /gui <create|edit|open|list|reload|delete|copy|import>");
            return true;
        }

        String sub = args[0].toLowerCase();

        // [OPEN] - GUI 열기
        if (sub.equals("open")) {
            if (!sender.hasPermission("guimanager.open")) {
                sender.sendMessage(ChatColor.RED + "No permission");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /gui open <gui id> [player]");
                return true;
            }
            String guiId = args[1];
            GUI gui = plugin.getGui(guiId);
            if (gui == null) {
                sender.sendMessage(ChatColor.RED + "GUI not found");
                return true;
            }

            Player targetPlayer;
            if (args.length >= 3) {
                if (!sender.hasPermission("guimanager.open.others")) {
                    sender.sendMessage(ChatColor.RED + "No permission to open for others");
                    return true;
                }
                targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found");
                    return true;
                }
            } else {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Console must specify a player");
                    return true;
                }
                targetPlayer = (Player) sender;
            }

            targetPlayer.openInventory(plugin.getPlayerSpecificInventory(targetPlayer, guiId));
            return true;
        }

        // [IMPORT] - 다른 플러그인에서 가져오기
        if (sub.equals("import")) {
            if (!sender.hasPermission("guimanager.import")) {
                sender.sendMessage(ChatColor.RED + "No permission");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /gui import <guiplus|deluxemenus> [filename]");
                return true;
            }
            String type = args[1].toLowerCase();
            if (type.equals("guiplus")) {
                new GUIPlusImporter(plugin).importFromGUIPlus(sender);
            } else if (type.equals("deluxemenus")) {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /gui import deluxemenus <filename.yml>");
                    return true;
                }
                File file = new File("plugins/DeluxeMenus/gui_menus", args[2]);
                new DeluxeMenusImporter(plugin).importFromDeluxeMenus((Player) sender, file);
            }
            return true;
        }

        // [LIST] - 목록 확인
        if (sub.equals("list")) {
            if (!sender.hasPermission("guimanager.list")) {
                sender.sendMessage(ChatColor.RED + "No permission");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + "Available GUIs:");
            for (String id : plugin.getGuis().keySet()) {
                sender.sendMessage(ChatColor.YELLOW + "- " + id);
            }
            return true;
        }

        // [RELOAD] - 리로드
        if (sub.equals("reload")) {
            if (!sender.hasPermission("guimanager.reload")) {
                sender.sendMessage(ChatColor.RED + "No permission");
                return true;
            }
            plugin.saveGuis();
            plugin.loadGuis();
            sender.sendMessage(ChatColor.GREEN + "Plugin reloaded and GUIs saved");
            return true;
        }

        // [DELETE] - 삭제
        if (sub.equals("delete")) {
            if (!sender.hasPermission("guimanager.delete")) {
                sender.sendMessage(ChatColor.RED + "No permission");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /gui delete <gui id>");
                return true;
            }
            String guiId = args[1];
            if (plugin.getGui(guiId) == null) {
                sender.sendMessage(ChatColor.RED + "GUI not found");
                return true;
            }
            plugin.removeGui(guiId);
            sender.sendMessage(ChatColor.GREEN + "GUI '" + guiId + "' has been deleted");
            return true;
        }

        // [COPY] - 복사
        if (sub.equals("copy")) {
            if (!sender.hasPermission("guimanager.copy")) {
                sender.sendMessage(ChatColor.RED + "No permission");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /gui copy <source id> <new id>");
                return true;
            }
            String sourceId = args[1];
            String newId = args[2];
            GUI sourceGui = plugin.getGui(sourceId);
            if (sourceGui == null) {
                sender.sendMessage(ChatColor.RED + "Source GUI not found");
                return true;
            }
            if (plugin.getGui(newId) != null) {
                sender.sendMessage(ChatColor.RED + "Target ID already exists");
                return true;
            }

            GUI newGui = new GUI(sourceGui.getTitle(), sourceGui.getSize());
            newGui.setId(newId);
            newGui.setPermission(sourceGui.getPermission());
            newGui.setTargets(sourceGui.getTargets());
            sourceGui.getItems().forEach((slot, item) -> newGui.setItem(slot, item.clone()));

            plugin.addGui(newId, newGui);
            plugin.saveGui(newId);
            sender.sendMessage(ChatColor.GREEN + "GUI copied from '" + sourceId + "' to '" + newId + "'");
            return true;
        }

        // 플레이어 전용 명령어 체크
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use create and edit commands");
            return true;
        }
        Player player = (Player) sender;

        // [CREATE] - 생성 (유연한 인수 처리 반영)
        if (sub.equals("create")) {
            if (!player.hasPermission("guimanager.create")) {
                player.sendMessage(ChatColor.RED + "No permission");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /gui create <id> [rows] [title]");
                return true;
            }

            String guiId = args[1];
            if (plugin.getGui(guiId) != null) {
                player.sendMessage(ChatColor.RED + "GUI already exists");
                return true;
            }

            int rows = 1; // 기본값
            String title = "§rDefault GUI"; // 기본값

            // 인수가 3개 이상일 때 (rows 입력됨)
            if (args.length >= 3) {
                try {
                    rows = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Rows must be a number. Defaulting to 1.");
                    rows = 1;
                }
            }

            // 인수가 4개 이상일 때 (title 입력됨)
            if (args.length >= 4) {
                StringBuilder titleBuilder = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    titleBuilder.append(args[i]).append(" ");
                }
                title = ChatColor.translateAlternateColorCodes('&', titleBuilder.toString().trim());
            }

            if (rows < 1 || rows > 6) {
                player.sendMessage(ChatColor.RED + "Rows must be between 1 and 6");
                return true;
            }

            // 생성 및 저장
            plugin.createGui(guiId, rows, title);
            player.sendMessage(ChatColor.GREEN + "GUI '" + guiId + "' created successfully");

            // 생성 후 즉시 편집기 열기
            GUI createdGui = plugin.getGui(guiId);
            if (createdGui != null) {
                MainGuiEditor.open(player, createdGui);
            }
            return true;
        }

        // [EDIT] - 편집
        if (sub.equals("edit")) {
            if (!player.hasPermission("guimanager.edit")) {
                player.sendMessage(ChatColor.RED + "No permission");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /gui edit <id>");
                return true;
            }
            String guiId = args[1];
            GUI gui = plugin.getGui(guiId);
            if (gui == null) {
                player.sendMessage(ChatColor.RED + "GUI not found");
                return true;
            }
            MainGuiEditor.open(player, gui);
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("create"); completions.add("edit"); completions.add("open");
            completions.add("list"); completions.add("reload"); completions.add("delete");
            completions.add("copy"); completions.add("import");
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("edit") || sub.equals("open") || sub.equals("delete") || sub.equals("copy")) {
                completions.addAll(plugin.getGuis().keySet());
            } else if (sub.equals("import")) {
                completions.add("guiplus");
                completions.add("deluxemenus");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            for (int i = 1; i <= 6; i++) completions.add(String.valueOf(i));
        }
        return completions;
    }
}