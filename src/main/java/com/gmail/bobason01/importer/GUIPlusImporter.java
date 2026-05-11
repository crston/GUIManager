package com.gmail.bobason01.importer;

import com.gmail.bobason01.GUI;
import com.gmail.bobason01.GUIManager;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class GUIPlusImporter {

    private final GUIManager plugin;

    public GUIPlusImporter(GUIManager plugin) {
        this.plugin = plugin;
    }

    public void importFromGUIPlus(CommandSender sender) {
        File guiPlusFolder = new File("plugins/GUIPlus/inventorys");
        if (!guiPlusFolder.exists() || !guiPlusFolder.isDirectory()) {
            sender.sendMessage(ChatColor.RED + "GUIPlus folder not found");
            return;
        }

        File[] files = guiPlusFolder.listFiles();
        if (files == null || files.length == 0) {
            sender.sendMessage(ChatColor.RED + "No files found");
            return;
        }

        int success = 0;
        int failed = 0;
        int skipped = 0;

        for (File file : files) {
            if (file.isDirectory() || !file.getName().endsWith(".yml")) continue;

            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                if (!config.isConfigurationSection("inv")) {
                    skipped++;
                    continue;
                }

                if (convertFile(file, config)) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                plugin.getLogger().log(Level.WARNING, "Import failed for " + file.getName(), e);
            }
        }
        sender.sendMessage(ChatColor.GREEN + "Import complete. Success: " + success + ", Failed: " + failed);
    }

    private boolean convertFile(File file, FileConfiguration config) {
        ConfigurationSection invSection = config.getConfigurationSection("inv");
        if (invSection == null) return false;

        String id = invSection.getString("id");
        String title = invSection.getString("title");
        int size = invSection.getInt("size");

        if (id == null || title == null) return false;

        GUI gui = new GUI(ChatColor.translateAlternateColorCodes('&', title), size);

        ConfigurationSection itemsSection = invSection.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String slotStr : itemsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotStr);
                    ConfigurationSection itemDataSection = itemsSection.getConfigurationSection(slotStr);
                    if (itemDataSection == null) continue;

                    String firstKey = itemDataSection.getKeys(false).iterator().next();
                    ConfigurationSection itemDefinition = itemDataSection.getConfigurationSection(firstKey);
                    if (itemDefinition != null) {
                        gui.setItem(slot, convertItem(itemDefinition));
                    }
                } catch (Exception ignored) {}
            }
        }
        plugin.addGui(id, gui);
        plugin.saveGui(id);
        return true;
    }

    @SuppressWarnings("unchecked")
    private ItemStack convertItem(ConfigurationSection itemDef) {
        ConfigurationSection itemSection = itemDef.getConfigurationSection("item");
        if (itemSection == null) return new ItemStack(Material.AIR);

        Material type = Material.matchMaterial(itemSection.getString("type", "STONE"));
        ItemStack itemStack = new ItemStack(type != null ? type : Material.STONE);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        Object rawMeta = itemSection.get("meta");
        if (rawMeta instanceof ItemMeta) {
            ItemMeta sourceMeta = (ItemMeta) rawMeta;
            if (sourceMeta.hasDisplayName()) meta.setDisplayName(sourceMeta.getDisplayName());
            if (sourceMeta.hasLore()) meta.setLore(sourceMeta.getLore());
            if (sourceMeta.hasCustomModelData()) meta.setCustomModelData(sourceMeta.getCustomModelData());
        } else if (rawMeta instanceof Map) {
            Map<String, Object> metaMap = (Map<String, Object>) rawMeta;
            if (metaMap.containsKey("display-name")) meta.setDisplayName(toLegacyText((String) metaMap.get("display-name")));
            if (metaMap.containsKey("lore")) {
                List<String> legacyLore = new ArrayList<>();
                ((List<?>) metaMap.get("lore")).forEach(line -> legacyLore.add(toLegacyText((String) line)));
                meta.setLore(legacyLore);
            }
            if (metaMap.get("custom-model-data") instanceof Integer) {
                meta.setCustomModelData((Integer) metaMap.get("custom-model-data"));
            }
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (meta.hasCustomModelData()) {
            pdc.set(GUIManager.KEY_CUSTOM_MODEL_DATA, PersistentDataType.INTEGER, meta.getCustomModelData());
        }

        // 액션 변환
        convertAction(pdc, itemDef, "leftaction", GUIManager.KEY_COMMAND_LEFT, GUIManager.KEY_MONEY_COST_LEFT, GUIManager.KEY_PERMISSION_LEFT, GUIManager.KEY_COOLDOWN_LEFT, GUIManager.KEY_EXECUTOR_LEFT, GUIManager.KEY_KEEP_OPEN_LEFT);
        convertAction(pdc, itemDef, "rightaction", GUIManager.KEY_COMMAND_RIGHT, GUIManager.KEY_MONEY_COST_RIGHT, GUIManager.KEY_PERMISSION_RIGHT, GUIManager.KEY_COOLDOWN_RIGHT, GUIManager.KEY_EXECUTOR_RIGHT, GUIManager.KEY_KEEP_OPEN_RIGHT);
        convertAction(pdc, itemDef, "shiftleftaction", GUIManager.KEY_COMMAND_SHIFT_LEFT, GUIManager.KEY_MONEY_COST_SHIFT_LEFT, GUIManager.KEY_PERMISSION_SHIFT_LEFT, GUIManager.KEY_COOLDOWN_SHIFT_LEFT, GUIManager.KEY_EXECUTOR_SHIFT_LEFT, GUIManager.KEY_KEEP_OPEN_SHIFT_LEFT);
        convertAction(pdc, itemDef, "shiftrightaction", GUIManager.KEY_COMMAND_SHIFT_RIGHT, GUIManager.KEY_MONEY_COST_SHIFT_RIGHT, GUIManager.KEY_PERMISSION_SHIFT_RIGHT, GUIManager.KEY_COOLDOWN_SHIFT_RIGHT, GUIManager.KEY_EXECUTOR_SHIFT_RIGHT, GUIManager.KEY_KEEP_OPEN_SHIFT_RIGHT);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private void convertAction(PersistentDataContainer pdc, ConfigurationSection itemDef, String actionKey, NamespacedKey commandKey, NamespacedKey moneyCostKey, NamespacedKey permissionKey, NamespacedKey cooldownKey, NamespacedKey executorKey, NamespacedKey keepOpenKey) {
        if (itemDef.isConfigurationSection(actionKey)) {
            ConfigurationSection section = itemDef.getConfigurationSection(actionKey);
            boolean closeInv = section.getBoolean("closeInv", true);
            pdc.set(keepOpenKey, PersistentDataType.BYTE, (byte) (closeInv ? 0 : 1));

            List<String> commands = section.getStringList("commands");
            if (!commands.isEmpty()) {
                String rawCommand = commands.get(0);
                GUIManager.ExecutorType executor = GUIManager.ExecutorType.PLAYER;
                String finalCommand = rawCommand.trim();

                if (finalCommand.startsWith("<server>")) {
                    executor = GUIManager.ExecutorType.CONSOLE;
                    finalCommand = finalCommand.substring(8).trim();
                } else if (finalCommand.startsWith("<op>")) {
                    executor = GUIManager.ExecutorType.OP;
                    finalCommand = finalCommand.substring(4).trim();
                } else if (finalCommand.startsWith("<msg>")) {
                    executor = GUIManager.ExecutorType.CONSOLE;
                    finalCommand = "tell %player% " + finalCommand.substring(5).trim();
                }

                finalCommand = finalCommand.replace("<player>", "%player%");
                pdc.set(commandKey, PersistentDataType.STRING, finalCommand);
                pdc.set(executorKey, PersistentDataType.STRING, executor.name());
            }

            double money = section.getDouble("price.money");
            if (money > 0) pdc.set(moneyCostKey, PersistentDataType.DOUBLE, money);

            String perm = section.getString("permission");
            if (perm != null && !perm.isEmpty()) pdc.set(permissionKey, PersistentDataType.STRING, perm);

            double cooldown = section.getDouble("cooldown");
            if (cooldown > 0) pdc.set(cooldownKey, PersistentDataType.DOUBLE, cooldown / 1000.0);
        } else {
            pdc.set(keepOpenKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    private String toLegacyText(String json) {
        if (json == null || json.isEmpty()) return "";
        try {
            return TextComponent.toLegacyText(ComponentSerializer.parse(json));
        } catch (Exception e) {
            return ChatColor.translateAlternateColorCodes('&', json);
        }
    }
}