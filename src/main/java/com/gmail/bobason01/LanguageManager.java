package com.gmail.bobason01;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class LanguageManager {

    private final GUIManager plugin;
    private final String fileName = "lang.yml";
    private File configFile;
    private FileConfiguration config;
    private final Map<String, String> messages = new HashMap<>();

    public LanguageManager(GUIManager plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), fileName);
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
            setupDefaultMessages();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
        }
        config.options().copyDefaults(true);
        save();
        messages.clear();
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                String value = config.getString(key);
                if (value != null) {
                    messages.put(key, ChatColor.translateAlternateColorCodes('&', value));
                }
            }
        }
    }

    private void setupDefaultMessages() {
        config = YamlConfiguration.loadConfiguration(configFile);
        config.set("error.cooldown", "&cYou must wait {time} more seconds");
        config.set("error.no_permission", "&cYou don't have permission");
        config.set("error.no_economy", "&cEconomy is not enabled on this server");
        config.set("error.not_enough_money", "&cYou don't have enough money");
        config.set("error.not_enough_items", "&cYou don't have the required items");
        config.set("info.input_target", "&aPlease enter the target player name in chat Type cancel to abort");
        config.set("info.cancel_action", "&cAction cancelled");
        config.set("command.no_permission", "&cNo permission");
        config.set("command.gui_not_found", "&cGUI not found");
        config.set("command.player_not_found", "&cPlayer not found");
        config.set("command.reload_success", "&aPlugin reloaded and GUIs saved");
        save();
    }

    private void save() {
        try { config.save(configFile); }
        catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Could not save language file", e); }
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, key);
    }

    public String getMessage(String key, String... placeholders) {
        String msg = getMessage(key);
        if (placeholders.length % 2 != 0) return msg;

        for (int i = 0; i < placeholders.length; i += 2) {
            String target = placeholders[i];
            String replacement = placeholders[i + 1];
            if (target != null && replacement != null) {
                msg = msg.replace(target, replacement);
            }
        }
        return msg;
    }

    public void reload() { load(); }
}