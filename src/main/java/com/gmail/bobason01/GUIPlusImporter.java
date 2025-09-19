package com.gmail.bobason01;

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
            sender.sendMessage(ChatColor.RED + "GUIPlus 'inventorys' folder not found at 'plugins/GUIPlus/inventorys'.");
            return;
        }

        File[] files = guiPlusFolder.listFiles();
        if (files == null || files.length == 0) {
            sender.sendMessage(ChatColor.RED + "No files found in GUIPlus 'inventorys' folder.");
            return;
        }

        int success = 0;
        int failed = 0;
        int skipped = 0;

        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }

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
                sender.sendMessage(ChatColor.RED + "Failed to import " + file.getName() + ": " + e.getMessage());
                plugin.getLogger().log(Level.WARNING, "GUIPlus import failed for " + file.getName(), e);
            }
        }
        sender.sendMessage(ChatColor.GREEN + "Import complete. Success: " + success + ", Failed: " + failed + ", Skipped: " + skipped);
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

                    String firstKey = itemDataSection.getKeys(false).stream().findFirst().orElse(null);
                    if (firstKey == null) continue;

                    ConfigurationSection itemDefinition = itemDataSection.getConfigurationSection(firstKey);
                    if (itemDefinition != null) {
                        ItemStack item = convertItem(itemDefinition);
                        gui.setItem(slot, item);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid slot number '" + slotStr + "' in file " + file.getName());
                }
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

        Object rawMetaObject = itemSection.get("meta");
        if (rawMetaObject instanceof ItemMeta) {
            ItemMeta sourceMeta = (ItemMeta) rawMetaObject;
            if (sourceMeta.hasDisplayName()) meta.setDisplayName(sourceMeta.getDisplayName());
            if (sourceMeta.hasLore()) meta.setLore(sourceMeta.getLore());
            if (sourceMeta.hasCustomModelData()) meta.setCustomModelData(sourceMeta.getCustomModelData());
        } else if (rawMetaObject instanceof Map) {
            Map<String, Object> metaMap = (Map<String, Object>) rawMetaObject;
            if (metaMap.containsKey("display-name")) meta.setDisplayName(toLegacyText((String) metaMap.get("display-name")));
            if (metaMap.containsKey("lore")) {
                List<String> legacyLore = new ArrayList<>();
                ((List<?>) metaMap.get("lore")).forEach(line -> legacyLore.add(toLegacyText((String) line)));
                meta.setLore(legacyLore);
            }
            if (metaMap.containsKey("custom-model-data")) {
                if (metaMap.get("custom-model-data") instanceof Integer) {
                    meta.setCustomModelData((Integer) metaMap.get("custom-model-data"));
                }
            }
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (meta.hasCustomModelData()) {
            pdc.set(GUIManager.KEY_CUSTOM_MODEL_DATA, PersistentDataType.INTEGER, meta.getCustomModelData());
        }

        convertAction(pdc, itemDef, "leftaction", GUIManager.KEY_COMMAND_LEFT, GUIManager.KEY_MONEY_COST_LEFT, GUIManager.KEY_PERMISSION_LEFT, GUIManager.KEY_COOLDOWN_LEFT, GUIManager.KEY_EXECUTOR_LEFT, GUIManager.KEY_KEEP_OPEN_LEFT);
        convertAction(pdc, itemDef, "rightaction", GUIManager.KEY_COMMAND_RIGHT, GUIManager.KEY_MONEY_COST_RIGHT, GUIManager.KEY_PERMISSION_RIGHT, GUIManager.KEY_COOLDOWN_RIGHT, GUIManager.KEY_EXECUTOR_RIGHT, GUIManager.KEY_KEEP_OPEN_RIGHT);
        convertAction(pdc, itemDef, "shiftleftaction", GUIManager.KEY_COMMAND_SHIFT_LEFT, GUIManager.KEY_MONEY_COST_SHIFT_LEFT, GUIManager.KEY_PERMISSION_SHIFT_LEFT, GUIManager.KEY_COOLDOWN_SHIFT_LEFT, GUIManager.KEY_EXECUTOR_SHIFT_LEFT, GUIManager.KEY_KEEP_OPEN_SHIFT_LEFT);
        convertAction(pdc, itemDef, "shiftrightaction", GUIManager.KEY_COMMAND_SHIFT_RIGHT, GUIManager.KEY_MONEY_COST_SHIFT_RIGHT, GUIManager.KEY_PERMISSION_SHIFT_RIGHT, GUIManager.KEY_COOLDOWN_SHIFT_RIGHT, GUIManager.KEY_EXECUTOR_SHIFT_RIGHT, GUIManager.KEY_KEEP_OPEN_SHIFT_RIGHT);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private void convertAction(PersistentDataContainer pdc, ConfigurationSection itemDef, String actionKey, NamespacedKey commandKey, NamespacedKey moneyCostKey, NamespacedKey permissionKey, NamespacedKey cooldownKey, NamespacedKey executorKey, NamespacedKey keepOpenKey) {
        if (itemDef.isConfigurationSection(actionKey)) {
            // closeInv 설정 변환 (GUIPlus: closeInv: true -> GUIManager: keepOpen: false)
            boolean closeInv = itemDef.getBoolean(actionKey + ".closeInv", true); // GUIPlus의 기본값은 닫는 것(true)으로 가정
            byte keepOpenValue = (byte) (closeInv ? 0 : 1); // closeInv가 true이면 keepOpen은 0 (false)
            pdc.set(keepOpenKey, PersistentDataType.BYTE, keepOpenValue);

            if (itemDef.isSet(actionKey + ".commands")) {
                String command = itemDef.getStringList(actionKey + ".commands").stream().findFirst().orElse("");
                if (!command.isEmpty()) {
                    GUIManager.ExecutorType executor = GUIManager.ExecutorType.PLAYER;
                    String finalCommand = command.trim();

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
            }
            if (itemDef.isSet(actionKey + ".price.money")) {
                double money = itemDef.getDouble(actionKey + ".price.money");
                if (money > 0) {
                    pdc.set(moneyCostKey, PersistentDataType.DOUBLE, money);
                }
            }
            if (itemDef.isSet(actionKey + ".permission")) {
                String permission = itemDef.getString(actionKey + ".permission");
                if (permission != null && !permission.isEmpty()) {
                    pdc.set(permissionKey, PersistentDataType.STRING, permission);
                }
            }
            if (itemDef.isSet(actionKey + ".cooldown")) {
                double cooldownMillis = itemDef.getDouble(actionKey + ".cooldown");
                if (cooldownMillis > 0) {
                    pdc.set(cooldownKey, PersistentDataType.DOUBLE, cooldownMillis / 1000.0);
                }
            }
        } else {
            // actionKey 섹션이 없으면 기본값(닫지 않음)으로 설정
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