package com.gmail.bobason01;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class GUIManager extends JavaPlugin {

    private static GUIManager instance;
    public static Economy econ = null;

    // General Item Keys
    public static NamespacedKey KEY_PERMISSION_MESSAGE, KEY_REQUIRE_TARGET, KEY_CUSTOM_MODEL_DATA, KEY_ITEM_DAMAGE, KEY_ITEM_MODEL_ID;

    // Action Specific Keys
    public static NamespacedKey KEY_COMMAND_LEFT, KEY_PERMISSION_LEFT, KEY_COST_LEFT, KEY_MONEY_COST_LEFT;
    public static NamespacedKey KEY_COMMAND_SHIFT_LEFT, KEY_PERMISSION_SHIFT_LEFT, KEY_COST_SHIFT_LEFT, KEY_MONEY_COST_SHIFT_LEFT;
    public static NamespacedKey KEY_COMMAND_RIGHT, KEY_PERMISSION_RIGHT, KEY_COST_RIGHT, KEY_MONEY_COST_RIGHT;
    public static NamespacedKey KEY_COMMAND_SHIFT_RIGHT, KEY_PERMISSION_SHIFT_RIGHT, KEY_COST_SHIFT_RIGHT, KEY_MONEY_COST_SHIFT_RIGHT;
    public static NamespacedKey KEY_COMMAND_F, KEY_PERMISSION_F, KEY_COST_F, KEY_MONEY_COST_F;
    public static NamespacedKey KEY_COMMAND_SHIFT_F, KEY_PERMISSION_SHIFT_F, KEY_COST_SHIFT_F, KEY_MONEY_COST_SHIFT_F;
    public static NamespacedKey KEY_COMMAND_Q, KEY_PERMISSION_Q, KEY_COST_Q, KEY_MONEY_COST_Q;
    public static NamespacedKey KEY_COMMAND_SHIFT_Q, KEY_PERMISSION_SHIFT_Q, KEY_COST_SHIFT_Q, KEY_MONEY_COST_SHIFT_Q;


    private final Map<String, GUI> guis = new ConcurrentHashMap<>();
    private final Map<String, String> titleToGuiIdCache = new ConcurrentHashMap<>();
    private File guiFile;
    private FileConfiguration guiConfig;

    private final Map<UUID, String> playersInEditMode = new ConcurrentHashMap<>();
    private final Map<UUID, EditSession> chatEditSessions = new ConcurrentHashMap<>();
    private final Map<UUID, EditSession> playersSettingCost = new ConcurrentHashMap<>();
    private final Map<UUID, TargetInfo> playersAwaitingTarget = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        if (!setupEconomy()) {
            getLogger().warning("Vault not found! Money cost features will be disabled.");
        }
        initializeKeys();
        createGuiConfig();
        loadGuis();

        GUIListener guiListener = new GUIListener(this);
        Bukkit.getPluginManager().registerEvents(guiListener, this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this, guiListener), this);
        Bukkit.getPluginManager().registerEvents(new KeybindListener(this, guiListener), this);

        GUICommand guiCommand = new GUICommand(this);
        Objects.requireNonNull(getCommand("gui")).setExecutor(guiCommand);
        Objects.requireNonNull(getCommand("gui")).setTabCompleter(guiCommand);
        getLogger().info("GUIManager has been enabled.");
    }

    @Override
    public void onDisable() {
        saveGuis();
        getLogger().info("GUIManager has been disabled.");
    }

    public static GUIManager getInstance() {
        return instance;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private void initializeKeys() {
        KEY_PERMISSION_MESSAGE = new NamespacedKey(this, "perm_msg");
        KEY_REQUIRE_TARGET = new NamespacedKey(this, "req_target");
        KEY_CUSTOM_MODEL_DATA = new NamespacedKey(this, "model_data");
        KEY_ITEM_DAMAGE = new NamespacedKey(this, "item_damage");
        KEY_ITEM_MODEL_ID = new NamespacedKey(this, "item_model_id");
        KEY_COMMAND_LEFT = new NamespacedKey(this, "cmd_left");
        KEY_PERMISSION_LEFT = new NamespacedKey(this, "perm_left");
        KEY_COST_LEFT = new NamespacedKey(this, "cost_left");
        KEY_MONEY_COST_LEFT = new NamespacedKey(this, "money_cost_left");
        KEY_COMMAND_SHIFT_LEFT = new NamespacedKey(this, "cmd_s_left");
        KEY_PERMISSION_SHIFT_LEFT = new NamespacedKey(this, "perm_s_left");
        KEY_COST_SHIFT_LEFT = new NamespacedKey(this, "cost_s_left");
        KEY_MONEY_COST_SHIFT_LEFT = new NamespacedKey(this, "money_cost_s_left");
        KEY_COMMAND_RIGHT = new NamespacedKey(this, "cmd_right");
        KEY_PERMISSION_RIGHT = new NamespacedKey(this, "perm_right");
        KEY_COST_RIGHT = new NamespacedKey(this, "cost_right");
        KEY_MONEY_COST_RIGHT = new NamespacedKey(this, "money_cost_right");
        KEY_COMMAND_SHIFT_RIGHT = new NamespacedKey(this, "cmd_s_right");
        KEY_PERMISSION_SHIFT_RIGHT = new NamespacedKey(this, "perm_s_right");
        KEY_COST_SHIFT_RIGHT = new NamespacedKey(this, "cost_s_right");
        KEY_MONEY_COST_SHIFT_RIGHT = new NamespacedKey(this, "money_cost_s_right");
        KEY_COMMAND_F = new NamespacedKey(this, "cmd_f");
        KEY_PERMISSION_F = new NamespacedKey(this, "perm_f");
        KEY_COST_F = new NamespacedKey(this, "cost_f");
        KEY_MONEY_COST_F = new NamespacedKey(this, "money_cost_f");
        KEY_COMMAND_SHIFT_F = new NamespacedKey(this, "cmd_s_f");
        KEY_PERMISSION_SHIFT_F = new NamespacedKey(this, "perm_s_f");
        KEY_COST_SHIFT_F = new NamespacedKey(this, "cost_s_f");
        KEY_MONEY_COST_SHIFT_F = new NamespacedKey(this, "money_cost_s_f");
        KEY_COMMAND_Q = new NamespacedKey(this, "cmd_q");
        KEY_PERMISSION_Q = new NamespacedKey(this, "perm_q");
        KEY_COST_Q = new NamespacedKey(this, "cost_q");
        KEY_MONEY_COST_Q = new NamespacedKey(this, "money_cost_q");
        KEY_COMMAND_SHIFT_Q = new NamespacedKey(this, "cmd_s_q");
        KEY_PERMISSION_SHIFT_Q = new NamespacedKey(this, "perm_s_q");
        KEY_COST_SHIFT_Q = new NamespacedKey(this, "cost_s_q");
        KEY_MONEY_COST_SHIFT_Q = new NamespacedKey(this, "money_cost_s_q");
    }

    public boolean isInEditMode(Player p) { return playersInEditMode.containsKey(p.getUniqueId()); }
    public void setEditMode(Player p, String guiName) { playersInEditMode.put(p.getUniqueId(), guiName); }
    public String getEditingGuiName(Player p) { return playersInEditMode.get(p.getUniqueId()); }
    public void removeEditMode(Player p) { playersInEditMode.remove(p.getUniqueId()); }
    public boolean hasChatSession(Player p) { return chatEditSessions.containsKey(p.getUniqueId()); }
    public EditSession getChatSession(Player p) { return chatEditSessions.get(p.getUniqueId()); }
    public void startChatSession(Player p, EditSession s) { chatEditSessions.put(p.getUniqueId(), s); }
    public void endChatSession(Player p) { chatEditSessions.remove(p.getUniqueId()); }
    public boolean isSettingCost(Player p) { return playersSettingCost.containsKey(p.getUniqueId()); }
    public EditSession getCostSession(Player p) { return playersSettingCost.get(p.getUniqueId()); }
    public void startCostSession(Player p, EditSession s) { playersSettingCost.put(p.getUniqueId(), s); }
    public void endCostSession(Player p) { playersSettingCost.remove(p.getUniqueId()); }
    public boolean isAwaitingTarget(Player p) { return playersAwaitingTarget.containsKey(p.getUniqueId()); }
    public TargetInfo getAwaitingTargetInfo(Player p) { return playersAwaitingTarget.get(p.getUniqueId()); }
    public void setAwaitingTarget(Player p, TargetInfo info) { playersAwaitingTarget.put(p.getUniqueId(), info); }
    public void removeAwaitingTarget(Player p) { playersAwaitingTarget.remove(p.getUniqueId()); }

    public Map<String, GUI> getGuis() { return guis; }
    public GUI getGui(String id) { return guis.get(id.toLowerCase()); }
    public String getGuiIdByTitle(String title) { return titleToGuiIdCache.get(title); }

    public void addGui(String id, GUI gui) {
        guis.put(id.toLowerCase(), gui);
        titleToGuiIdCache.put(gui.getTitle(), id.toLowerCase());
    }

    public void updateGuiTitle(String id, String newTitle) {
        GUI gui = getGui(id);
        if (gui != null) {
            titleToGuiIdCache.remove(gui.getTitle());
            gui.setTitle(newTitle);
            titleToGuiIdCache.put(newTitle, id.toLowerCase());
        }
    }

    public void updateGuiId(String oldId, String newId) {
        GUI gui = guis.remove(oldId.toLowerCase());
        if (gui != null) {
            guis.put(newId.toLowerCase(), gui);
            if (titleToGuiIdCache.containsKey(gui.getTitle())) {
                titleToGuiIdCache.put(gui.getTitle(), newId.toLowerCase());
            }
        }
    }

    public void updateGuiSize(String id, int newLines) {
        GUI gui = getGui(id);
        if (gui != null) {
            gui.setSize(newLines * 9);
        }
    }

    public void removeGui(String id) {
        GUI gui = guis.remove(id.toLowerCase());
        if (gui != null) titleToGuiIdCache.remove(gui.getTitle());
        if (guiConfig != null) {
            guiConfig.set(id.toLowerCase(), null);
        }
    }

    public void loadGuis() {
        guis.clear();
        titleToGuiIdCache.clear();
        createGuiConfig();
        for (String key : guiConfig.getKeys(false)) {
            try {
                ConfigurationSection section = guiConfig.getConfigurationSection(key);
                if (section == null) continue;
                String title = section.getString("title", "GUI");
                int size = section.getInt("size", 27);
                GUI gui = new GUI(title, size);
                ConfigurationSection itemsSection = section.getConfigurationSection("items");
                if (itemsSection != null) {
                    for (String slotStr : itemsSection.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotStr);
                            ItemStack item = itemsSection.getItemStack(slotStr);
                            if (item != null) gui.setItem(slot, item);
                        } catch (Exception itemEx) {
                            getLogger().warning("Error loading item in slot '" + slotStr + "' for GUI '" + key + "': " + itemEx.getMessage());
                        }
                    }
                }
                addGui(key, gui);
            } catch (Exception guiEx) {
                getLogger().severe("Critical error loading GUI '" + key + "': " + guiEx.getMessage());
            }
        }
    }

    public void saveGuis() {
        if (guiConfig == null) createGuiConfig();
        for (String key : guiConfig.getKeys(false)) {
            guiConfig.set(key, null);
        }
        guis.forEach((id, gui) -> {
            ConfigurationSection section = guiConfig.createSection(id);
            section.set("title", gui.getTitle());
            section.set("size", gui.getSize());
            gui.getItems().forEach((slot, item) -> section.set("items." + slot, item));
        });
        saveGuiConfig();
    }

    private void createGuiConfig() {
        guiFile = new File(getDataFolder(), "guis.yml");
        if (!guiFile.exists()) {
            try {
                if (!getDataFolder().exists()) getDataFolder().mkdirs();
                if (guiFile.createNewFile()) getLogger().info("guis.yml not found, creating a new one.");
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create guis.yml!", e);
            }
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
    }

    private void saveGuiConfig() {
        if (guiConfig == null || guiFile == null) return;
        try {
            guiConfig.save(guiFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save guis.yml", e);
        }
    }
}