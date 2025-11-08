package com.gmail.bobason01;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

/**
 * HeadUtils — Compatible with Spigot, Paper, Purpur 1.21+
 * Uses new ResolvableProfile system without network access
 */
public final class HeadUtils {

    private HeadUtils() {}

    private static boolean ready = false;
    private static Field profileField;
    private static Constructor<?> resolvableProfileCtor;
    private static Constructor<?> gameProfileCtor;
    private static Method getProperties;
    private static Method putProperty;
    private static Constructor<?> propertyCtor;

    private static void ensureReady() throws Exception {
        if (ready) return;
        try {
            Class<?> craftMeta = Class.forName("org.bukkit.craftbukkit.inventory.CraftMetaSkull");
            profileField = craftMeta.getDeclaredField("profile");
            profileField.setAccessible(true);

            // NMS / Authlib classes
            Class<?> resolvableProfileClass = Class.forName("net.minecraft.world.item.component.ResolvableProfile");
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyMapClass = Class.forName("com.mojang.authlib.properties.PropertyMap");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");

            // Constructors and methods
            gameProfileCtor = gameProfileClass.getConstructor(UUID.class, String.class);
            getProperties = gameProfileClass.getMethod("getProperties");
            propertyCtor = propertyClass.getConstructor(String.class, String.class);
            putProperty = propertyMapClass.getMethod("put", Object.class, Object.class);
            resolvableProfileCtor = resolvableProfileClass.getConstructor(gameProfileClass);

            ready = true;
            System.out.println("[GUIManager] Using 1.21+ ResolvableProfile injection.");
        } catch (Throwable t) {
            throw new RuntimeException("HeadUtils init failed", t);
        }
    }

    public static ItemStack createHeadBySpec(String materialField, String skullField) {
        String spec = skullField != null && !skullField.isEmpty() ? skullField : materialField;
        if (spec == null || spec.isEmpty()) return new ItemStack(Material.STONE);

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        try {
            ensureReady();
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta == null) return item;

            String base64 = toBase64IfNeeded(spec);
            if (base64 == null) return item;

            Object resolvableProfile = createResolvableProfile(base64);
            profileField.set(meta, resolvableProfile);

            item.setItemMeta(meta);
        } catch (Throwable t) {
            System.out.println("[GUIManager] HeadUtils failed offline: " + t.getMessage());
        }
        return item;
    }

    private static Object createResolvableProfile(String base64) throws Exception {
        UUID id = UUID.nameUUIDFromBytes(base64.getBytes(StandardCharsets.UTF_8));
        Object gameProfile = gameProfileCtor.newInstance(id, "Offline_" + Integer.toHexString(base64.hashCode()));
        Object props = getProperties.invoke(gameProfile);
        Object texture = propertyCtor.newInstance("textures", base64);
        putProperty.invoke(props, "textures", texture);
        return resolvableProfileCtor.newInstance(gameProfile);
    }

    private static String toBase64IfNeeded(String spec) {
        if (spec == null || spec.isEmpty()) return null;
        String s = spec.trim();

        if (s.toLowerCase(Locale.ROOT).startsWith("base64:"))
            return s.substring("base64:".length()).trim();

        if (s.toLowerCase(Locale.ROOT).startsWith("texture-")) {
            String hash = s.substring("texture-".length()).trim();
            return encodeToBase64("http://textures.minecraft.net/texture/" + hash);
        }

        if (s.toLowerCase(Locale.ROOT).startsWith("url:")) {
            String url = s.substring("url:".length()).trim();
            return encodeToBase64(url);
        }

        if (s.startsWith("http://") || s.startsWith("https://")) {
            return encodeToBase64(s);
        }

        return null;
    }

    private static String encodeToBase64(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isTextureMaterialString(String s) {
        if (s == null) return false;
        String v = s.trim().toLowerCase(Locale.ROOT);
        return v.startsWith("texture-") || v.startsWith("url:") || v.startsWith("base64:");
    }
}
