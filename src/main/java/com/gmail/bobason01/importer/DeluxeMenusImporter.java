package com.gmail.bobason01.importer;

import com.gmail.bobason01.GUIManager;
import com.gmail.bobason01.utils.AsyncSaver;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class DeluxeMenusImporter {

    private final GUIManager plugin;

    public DeluxeMenusImporter(GUIManager plugin) {
        this.plugin = plugin;
    }

    public void importFromDeluxeMenus(Player sender, File deluxeMenuFile) {
        if (!deluxeMenuFile.exists()) {
            sender.sendMessage("DeluxeMenus file not found");
            return;
        }

        try {
            FileConfiguration deluxe = YamlConfiguration.loadConfiguration(deluxeMenuFile);
            String guiId = deluxeMenuFile.getName().replace(".yml", "").toLowerCase(Locale.ROOT);
            String title = deluxe.getString("menu_title", "Imported Menu");
            int size = deluxe.getInt("size", 54);

            File target = new File(plugin.getDataFolder(), "guis/" + guiId + "_imported.yml");
            if (target.exists()) {
                guiId = guiId + "_imported_" + System.currentTimeMillis();
                target = new File(plugin.getDataFolder(), "guis/" + guiId + ".yml");
            }

            FileConfiguration newGui = new YamlConfiguration();
            newGui.set("title", ChatColor.translateAlternateColorCodes('&', title));
            newGui.set("size", size);

            ConfigurationSection items = deluxe.getConfigurationSection("items");
            if (items != null) {
                ConfigurationSection guiItems = newGui.createSection("items");

                for (String key : items.getKeys(false)) {
                    ConfigurationSection src = items.getConfigurationSection(key);
                    if (src == null) continue;

                    try {
                        int slot = src.getInt("slot", -1);
                        if (slot < 0 || slot >= size) continue;

                        ConfigurationSection dst = guiItems.createSection(String.valueOf(slot));

                        String mat = src.getString("material", "STONE");
                        String skull = null;

                        if (src.contains("skull_texture"))
                            skull = src.getString("skull_texture");
                        else if (src.contains("head"))
                            skull = src.getString("head");

                        if (mat != null) {
                            mat = mat.trim();

                            if (mat.startsWith("texture") || mat.startsWith("base64") || mat.startsWith("http")) {
                                dst.set("material", "PLAYER_HEAD");
                                dst.set("skull", mat);
                            } else if (skull != null && (skull.startsWith("texture") || skull.startsWith("base64") || skull.startsWith("http"))) {
                                dst.set("material", "PLAYER_HEAD");
                                dst.set("skull", skull);
                            } else if (mat.equalsIgnoreCase("PLAYER_HEAD") && skull != null && !skull.isEmpty()) {
                                dst.set("material", "PLAYER_HEAD");
                                dst.set("skull", skull);
                            } else {
                                dst.set("material", mat.isEmpty() ? "STONE" : mat);
                            }
                        } else {
                            dst.set("material", "STONE");
                        }

                        String name = src.getString("display_name", null);
                        if (name != null) dst.set("name", name);

                        List<String> lore = src.getStringList("lore");
                        if (lore != null && !lore.isEmpty()) dst.set("lore", lore);

                        if (src.contains("model")) dst.set("custom_model_data", src.getInt("model"));
                        if (src.contains("damage")) dst.set("damage", src.getInt("damage"));

                        mapCommands(dst, src.getStringList("left_click_commands"), "left");
                        mapCommands(dst, src.getStringList("right_click_commands"), "right");
                        mapCommands(dst, src.getStringList("shift_left_click_commands"), "shift_left");
                        mapCommands(dst, src.getStringList("shift_right_click_commands"), "shift_right");

                        if (src.contains("money_cost")) dst.set("money_cost", src.getDouble("money_cost"));
                        if (src.contains("cooldown")) dst.set("cooldown.left", src.getDouble("cooldown"));

                        dst.set("hide_flags", true);

                    } catch (Throwable t) {
                        plugin.getLogger().warning("Error parsing item");
                    }
                }
            }

            AsyncSaver.enqueue(target, newGui);
            sender.sendMessage("Successfully imported");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Import error", e);
            sender.sendMessage("Error occurred check console");
        }
    }

    private void mapCommands(ConfigurationSection dst, List<String> commands, String actionKey) {
        if (commands == null || commands.isEmpty()) return;

        List<String> parsed = new ArrayList<>();
        for (String cmd : commands) {
            if (cmd == null || cmd.isEmpty()) continue;
            cmd = ChatColor.stripColor(cmd.trim());

            if (cmd.startsWith("[player]")) {
                parsed.add(cmd.replace("[player]", "").trim());
                dst.set("actions." + actionKey + ".executor", "PLAYER");
            } else if (cmd.startsWith("[console]")) {
                parsed.add(cmd.replace("[console]", "").trim());
                dst.set("actions." + actionKey + ".executor", "CONSOLE");
            } else if (cmd.startsWith("[op]")) {
                parsed.add(cmd.replace("[op]", "").trim());
                dst.set("actions." + actionKey + ".executor", "OP");
            } else if (cmd.startsWith("[message]")) {
                parsed.add("msg" + cmd.replace("[message]", "").trim());
                dst.set("actions." + actionKey + ".executor", "PLAYER");
            } else if (cmd.startsWith("[close]")) {
                parsed.add("close");
                dst.set("actions." + actionKey + ".executor", "PLAYER");
            } else {
                parsed.add(cmd);
            }
        }

        if (!parsed.isEmpty()) {
            dst.set("actions." + actionKey + ".command", String.join(";", parsed));
        }
    }
}