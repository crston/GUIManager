package com.gmail.bobason01;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class HeadUtils {

    public static boolean isTextureMaterialString(String str) {
        return str != null && str.length() > 20 && (str.indexOf("eyJ0ZXh0dXJlcy") == 0 || str.indexOf("textures.minecraft.net") != -1);
    }

    public static ItemStack createHeadBySpec(String materialStr, String skullField) {
        if (skullField != null && !skullField.isEmpty()) {
            if (skullField.length() > 16) {
                return createHeadByBase64(skullField);
            } else {
                return createHeadByName(skullField);
            }
        }
        if (isTextureMaterialString(materialStr)) {
            return createHeadByBase64(materialStr);
        }
        return new ItemStack(Material.PLAYER_HEAD);
    }

    public static ItemStack createHeadByName(String name) {
        // 플레이어 이름 기반 캐싱 시도
        if (HeadCache.has(name)) return HeadCache.getHead(name);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwner(name);
            head.setItemMeta(meta);
        }

        HeadCache.cacheHead(name, head);
        return head;
    }

    public static ItemStack createHeadByBase64(String base64) {
        // Base64 기반 캐싱 확인
        if (HeadCache.has(base64)) return HeadCache.getHead(base64);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (base64 == null || base64.isEmpty()) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        if (base64.indexOf("http") == 0) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + base64 + "\"}}}";
            base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }

        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();

            String decoded = new String(Base64.getDecoder().decode(base64));
            String url = extractUrlFromDecodedJson(decoded);
            if (url != null) {
                textures.setSkin(new URL(url));
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            }
        } catch (Exception ignored) {
        }

        head.setItemMeta(meta);
        // 생성된 머리 캐싱
        HeadCache.cacheHead(base64, head);
        return head;
    }

    private static String extractUrlFromDecodedJson(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}