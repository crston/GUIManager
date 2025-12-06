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
        // 1. 파일이 없으면 생성
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
        }

        // 2. 디스크에 있는 파일 로드
        config = YamlConfiguration.loadConfiguration(configFile);

        // 3. JAR 내부의 원본 파일 로드 (누락된 키 확인용)
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            // 한글 깨짐 방지를 위해 UTF-8로 읽기
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
        }

        // 4. [핵심 기능] 누락된 키가 있다면 기본값(Defaults)을 현재 설정(Config)으로 복사
        config.options().copyDefaults(true);

        // 5. 변경사항 저장 (누락된 키가 추가됨)
        save();

        // 6. 메모리에 메시지 캐싱 (성능 최적화)
        messages.clear();
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                String value = config.getString(key);
                if (value != null) {
                    messages.put(key, ChatColor.translateAlternateColorCodes('&', value));
                }
            }
        }

        plugin.getLogger().info("Loaded " + messages.size() + " messages.");
    }

    private void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + fileName, e);
        }
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, key); // 키가 없으면 키 자체를 반환 (오류 방지)
    }

    public String getMessage(String key, String... placeholders) {
        String msg = getMessage(key);
        if (placeholders.length % 2 != 0) return msg; // 플레이스홀더 짝이 안 맞으면 그대로 반환

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