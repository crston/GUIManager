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

    // 텍스처 URL이 포함된 Base64 문자열인지 확인
    public static boolean isTextureMaterialString(String str) {
        return str != null && str.length() > 20 && (str.startsWith("eyJ0ZXh0dXJlcy") || str.contains("textures.minecraft.net"));
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
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwner(name); // Deprecated지만 간단한 구현을 위해 사용. 최신 버전은 setOwningPlayer 권장
            head.setItemMeta(meta);
        }
        return head;
    }

    public static ItemStack createHeadByBase64(String base64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (base64 == null || base64.isEmpty()) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        // Base64가 전체 JSON이 아니라 URL만 있는 경우 등 처리 (필요시)
        // 편의상 URL이 직접 들어온 경우 처리
        if (base64.startsWith("http")) {
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
        } catch (Exception e) {
            // Base64 디코딩 실패 또는 URL 오류 시 무시 (기본 머리 반환)
        }

        head.setItemMeta(meta);
        return head;
    }

    private static String extractUrlFromDecodedJson(String json) {
        try {
            // Simple parsing logic using Gson
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}