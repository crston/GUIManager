package com.gmail.bobason01;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LanguageManager {

    private final GUIManager plugin;
    private FileConfiguration langConfig = null;
    private File langFile = null;

    public LanguageManager(GUIManager plugin) {
        this.plugin = plugin;
        saveDefaultLangFile();
        reloadLangFile();
    }

    public void reloadLangFile() {
        if (langFile == null) {
            langFile = new File(plugin.getDataFolder(), "lang.yml");
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        InputStream defLangStream = plugin.getResource("lang.yml");
        if (defLangStream != null) {
            langConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defLangStream, StandardCharsets.UTF_8)));
        }
    }

    public void saveDefaultLangFile() {
        if (langFile == null) {
            langFile = new File(plugin.getDataFolder(), "lang.yml");
        }
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }
    }

    public String getMessage(String path) {
        String message = getLangConfig().getString(path);
        if (message == null) {
            return ChatColor.RED + "Missing message in lang.yml: " + path;
        }
        return ChatColor.translateAlternateColorCodes('&', getLangConfig().getString("prefix", "&e[GUI] &f") + message);
    }

    private FileConfiguration getLangConfig() {
        if (langConfig == null) {
            reloadLangFile();
        }
        return langConfig;
    }

    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length && replacements[i] != null && replacements[i+1] != null) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
    }
}