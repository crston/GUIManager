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
    private String prefix = "";

    public LanguageManager(GUIManager plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), fileName);
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
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
        this.prefix = ChatColor.translateAlternateColorCodes('&', config.getString("prefix", "&e[GUI] &f"));

        for (String key : config.getKeys(true)) {
            if (config.isString(key) && !key.equals("prefix")) {
                String value = config.getString(key);
                if (value != null) {
                    messages.put(key, ChatColor.translateAlternateColorCodes('&', value));
                }
            }
        }
    }

    private void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save language file", e);
        }
    }

    public String getRawMessage(String key) {
        return messages.getOrDefault(key, key);
    }

    public String getMessage(String key, String... placeholders) {
        String msg = getRawMessage(key);
        if (msg.equals(key)) return msg;

        for (int i = 0; i < placeholders.length; i += 2) {
            String target = placeholders[i];
            String replacement = placeholders[i + 1];
            if (target != null && replacement != null) {
                msg = msg.replace(target, replacement);
            }
        }
        return prefix + msg;
    }

    public String getUnprefixedMessage(String key, String... placeholders) {
        String msg = getRawMessage(key);
        if (msg.equals(key)) return msg;

        for (int i = 0; i < placeholders.length; i += 2) {
            String target = placeholders[i];
            String replacement = placeholders[i + 1];
            if (target != null && replacement != null) {
                msg = msg.replace(target, replacement);
            }
        }
        return msg;
    }

    public void reload() {
        load();
    }
}