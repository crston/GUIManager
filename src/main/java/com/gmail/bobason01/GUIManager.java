package com.gmail.bobason01;

import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class GUIManager extends JavaPlugin {

    private static GUIManager instance;
    public static Economy econ = null;
    private boolean placeholderApiEnabled = false;
    private BukkitTask autoSaveTask;
    private BukkitTask guiUpdateTask;
    private LanguageManager languageManager;
    private final GuiMetaCache metaCache = new GuiMetaCache();

    public enum ExecutorType { PLAYER, CONSOLE, OP }

    public static NamespacedKey KEY_PERMISSION_MESSAGE;
    public static NamespacedKey KEY_REQUIRE_TARGET;
    public static NamespacedKey KEY_CUSTOM_MODEL_DATA;
    public static NamespacedKey KEY_ITEM_DAMAGE;
    public static NamespacedKey KEY_ITEM_MODEL;

    public static NamespacedKey KEY_COMMAND_LEFT;
    public static NamespacedKey KEY_PERMISSION_LEFT;
    public static NamespacedKey KEY_COST_LEFT;
    public static NamespacedKey KEY_MONEY_COST_LEFT;
    public static NamespacedKey KEY_COOLDOWN_LEFT;
    public static NamespacedKey KEY_EXECUTOR_LEFT;
    public static NamespacedKey KEY_KEEP_OPEN_LEFT;

    public static NamespacedKey KEY_COMMAND_SHIFT_LEFT;
    public static NamespacedKey KEY_PERMISSION_SHIFT_LEFT;
    public static NamespacedKey KEY_COST_SHIFT_LEFT;
    public static NamespacedKey KEY_MONEY_COST_SHIFT_LEFT;
    public static NamespacedKey KEY_COOLDOWN_SHIFT_LEFT;
    public static NamespacedKey KEY_EXECUTOR_SHIFT_LEFT;
    public static NamespacedKey KEY_KEEP_OPEN_SHIFT_LEFT;

    public static NamespacedKey KEY_COMMAND_RIGHT;
    public static NamespacedKey KEY_PERMISSION_RIGHT;
    public static NamespacedKey KEY_COST_RIGHT;
    public static NamespacedKey KEY_MONEY_COST_RIGHT;
    public static NamespacedKey KEY_COOLDOWN_RIGHT;
    public static NamespacedKey KEY_EXECUTOR_RIGHT;
    public static NamespacedKey KEY_KEEP_OPEN_RIGHT;

    public static NamespacedKey KEY_COMMAND_SHIFT_RIGHT;
    public static NamespacedKey KEY_PERMISSION_SHIFT_RIGHT;
    public static NamespacedKey KEY_COST_SHIFT_RIGHT;
    public static NamespacedKey KEY_MONEY_COST_SHIFT_RIGHT;
    public static NamespacedKey KEY_COOLDOWN_SHIFT_RIGHT;
    public static NamespacedKey KEY_EXECUTOR_SHIFT_RIGHT;
    public static NamespacedKey KEY_KEEP_OPEN_SHIFT_RIGHT;

    public static NamespacedKey KEY_COMMAND_F;
    public static NamespacedKey KEY_PERMISSION_F;
    public static NamespacedKey KEY_COST_F;
    public static NamespacedKey KEY_MONEY_COST_F;
    public static NamespacedKey KEY_COOLDOWN_F;
    public static NamespacedKey KEY_EXECUTOR_F;
    public static NamespacedKey KEY_KEEP_OPEN_F;

    public static NamespacedKey KEY_COMMAND_SHIFT_F;
    public static NamespacedKey KEY_PERMISSION_SHIFT_F;
    public static NamespacedKey KEY_COST_SHIFT_F;
    public static NamespacedKey KEY_MONEY_COST_SHIFT_F;
    public static NamespacedKey KEY_COOLDOWN_SHIFT_F;
    public static NamespacedKey KEY_EXECUTOR_SHIFT_F;
    public static NamespacedKey KEY_KEEP_OPEN_SHIFT_F;

    public static NamespacedKey KEY_COMMAND_Q;
    public static NamespacedKey KEY_PERMISSION_Q;
    public static NamespacedKey KEY_COST_Q;
    public static NamespacedKey KEY_MONEY_COST_Q;
    public static NamespacedKey KEY_COOLDOWN_Q;
    public static NamespacedKey KEY_EXECUTOR_Q;
    public static NamespacedKey KEY_KEEP_OPEN_Q;

    public static NamespacedKey KEY_COMMAND_SHIFT_Q;
    public static NamespacedKey KEY_PERMISSION_SHIFT_Q;
    public static NamespacedKey KEY_COST_SHIFT_Q;
    public static NamespacedKey KEY_MONEY_COST_SHIFT_Q;
    public static NamespacedKey KEY_COOLDOWN_SHIFT_Q;
    public static NamespacedKey KEY_EXECUTOR_SHIFT_Q;
    public static NamespacedKey KEY_KEEP_OPEN_SHIFT_Q;

    private final Map<String, GUI> guis = new ConcurrentHashMap<>();
    private File guisFolder;

    private final Map<UUID, String> playersInEditMode = new ConcurrentHashMap<>();
    private final Map<UUID, EditSession> chatEditSessions = new ConcurrentHashMap<>();
    private final Map<UUID, EditSession> playersSettingCost = new ConcurrentHashMap<>();
    private final Map<UUID, TargetInfo> playersAwaitingTarget = new ConcurrentHashMap<>();

    private final Map<UUID, CooldownMap> playerCooldowns = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        this.languageManager = new LanguageManager(this);
        setupEconomy();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderApiEnabled = true;
        }

        initializeKeys();
        this.guisFolder = new File(getDataFolder(), "guis");
        if (!this.guisFolder.exists()) this.guisFolder.mkdirs();

        loadGuis();

        GUIListener guiListener = new GUIListener(this);
        Bukkit.getPluginManager().registerEvents(guiListener, this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this, guiListener), this);
        Bukkit.getPluginManager().registerEvents(new KeybindListener(this, guiListener), this);

        GUICommand guiCommand = new GUICommand(this);
        if (getCommand("gui") != null) {
            getCommand("gui").setExecutor(guiCommand);
            getCommand("gui").setTabCompleter(guiCommand);
        }

        this.autoSaveTask = Bukkit.getScheduler().runTaskTimer(this, this::saveGuis, 6000L, 6000L);
        if (this.placeholderApiEnabled) startGuiUpdateTask();
    }

    @Override
    public void onDisable() {
        if (this.autoSaveTask != null) this.autoSaveTask.cancel();
        if (this.guiUpdateTask != null) this.guiUpdateTask.cancel();
        saveGuisSync();
        AsyncSaver.shutdown();
        metaCache.clearAll();
        HeadCache.clear();
    }

    public void initializeKeys() {
        KEY_PERMISSION_MESSAGE = new NamespacedKey(this, "perm_msg");
        KEY_REQUIRE_TARGET = new NamespacedKey(this, "req_target");
        KEY_CUSTOM_MODEL_DATA = new NamespacedKey(this, "model_data");
        KEY_ITEM_DAMAGE = new NamespacedKey(this, "item_damage");
        KEY_ITEM_MODEL = new NamespacedKey(this, "item_model");

        KEY_COMMAND_LEFT = new NamespacedKey(this, "cmd_left");
        KEY_PERMISSION_LEFT = new NamespacedKey(this, "perm_left");
        KEY_COST_LEFT = new NamespacedKey(this, "cost_left");
        KEY_MONEY_COST_LEFT = new NamespacedKey(this, "money_cost_left");
        KEY_COOLDOWN_LEFT = new NamespacedKey(this, "cooldown_left");
        KEY_EXECUTOR_LEFT = new NamespacedKey(this, "executor_left");
        KEY_KEEP_OPEN_LEFT = new NamespacedKey(this, "keep_open_left");

        KEY_COMMAND_SHIFT_LEFT = new NamespacedKey(this, "cmd_s_left");
        KEY_PERMISSION_SHIFT_LEFT = new NamespacedKey(this, "perm_s_left");
        KEY_COST_SHIFT_LEFT = new NamespacedKey(this, "cost_s_left");
        KEY_MONEY_COST_SHIFT_LEFT = new NamespacedKey(this, "money_cost_s_left");
        KEY_COOLDOWN_SHIFT_LEFT = new NamespacedKey(this, "cooldown_s_left");
        KEY_EXECUTOR_SHIFT_LEFT = new NamespacedKey(this, "executor_s_left");
        KEY_KEEP_OPEN_SHIFT_LEFT = new NamespacedKey(this, "keep_open_s_left");

        KEY_COMMAND_RIGHT = new NamespacedKey(this, "cmd_right");
        KEY_PERMISSION_RIGHT = new NamespacedKey(this, "perm_right");
        KEY_COST_RIGHT = new NamespacedKey(this, "cost_right");
        KEY_MONEY_COST_RIGHT = new NamespacedKey(this, "money_cost_right");
        KEY_COOLDOWN_RIGHT = new NamespacedKey(this, "cooldown_right");
        KEY_EXECUTOR_RIGHT = new NamespacedKey(this, "executor_right");
        KEY_KEEP_OPEN_RIGHT = new NamespacedKey(this, "keep_open_right");

        KEY_COMMAND_SHIFT_RIGHT = new NamespacedKey(this, "cmd_s_right");
        KEY_PERMISSION_SHIFT_RIGHT = new NamespacedKey(this, "perm_s_right");
        KEY_COST_SHIFT_RIGHT = new NamespacedKey(this, "cost_s_right");
        KEY_MONEY_COST_SHIFT_RIGHT = new NamespacedKey(this, "money_cost_s_right");
        KEY_COOLDOWN_SHIFT_RIGHT = new NamespacedKey(this, "cooldown_s_right");
        KEY_EXECUTOR_SHIFT_RIGHT = new NamespacedKey(this, "executor_s_right");
        KEY_KEEP_OPEN_SHIFT_RIGHT = new NamespacedKey(this, "keep_open_s_right");

        KEY_COMMAND_F = new NamespacedKey(this, "cmd_f");
        KEY_PERMISSION_F = new NamespacedKey(this, "perm_f");
        KEY_COST_F = new NamespacedKey(this, "cost_f");
        KEY_MONEY_COST_F = new NamespacedKey(this, "money_cost_f");
        KEY_COOLDOWN_F = new NamespacedKey(this, "cooldown_f");
        KEY_EXECUTOR_F = new NamespacedKey(this, "executor_f");
        KEY_KEEP_OPEN_F = new NamespacedKey(this, "keep_open_f");

        KEY_COMMAND_SHIFT_F = new NamespacedKey(this, "cmd_s_f");
        KEY_PERMISSION_SHIFT_F = new NamespacedKey(this, "perm_s_f");
        KEY_COST_SHIFT_F = new NamespacedKey(this, "cost_s_f");
        KEY_MONEY_COST_SHIFT_F = new NamespacedKey(this, "money_cost_s_f");
        KEY_COOLDOWN_SHIFT_F = new NamespacedKey(this, "cooldown_s_f");
        KEY_EXECUTOR_SHIFT_F = new NamespacedKey(this, "executor_s_f");
        KEY_KEEP_OPEN_SHIFT_F = new NamespacedKey(this, "keep_open_s_f");

        KEY_COMMAND_Q = new NamespacedKey(this, "cmd_q");
        KEY_PERMISSION_Q = new NamespacedKey(this, "perm_q");
        KEY_COST_Q = new NamespacedKey(this, "cost_q");
        KEY_MONEY_COST_Q = new NamespacedKey(this, "money_cost_q");
        KEY_COOLDOWN_Q = new NamespacedKey(this, "cooldown_q");
        KEY_EXECUTOR_Q = new NamespacedKey(this, "executor_q");
        KEY_KEEP_OPEN_Q = new NamespacedKey(this, "keep_open_q");

        KEY_COMMAND_SHIFT_Q = new NamespacedKey(this, "cmd_s_q");
        KEY_PERMISSION_SHIFT_Q = new NamespacedKey(this, "perm_s_q");
        KEY_COST_SHIFT_Q = new NamespacedKey(this, "cost_s_q");
        KEY_MONEY_COST_SHIFT_Q = new NamespacedKey(this, "money_cost_s_q");
        KEY_COOLDOWN_SHIFT_Q = new NamespacedKey(this, "cooldown_s_q");
        KEY_EXECUTOR_SHIFT_Q = new NamespacedKey(this, "executor_s_q");
        KEY_KEEP_OPEN_SHIFT_Q = new NamespacedKey(this, "keep_open_s_q");
    }

    public void setCooldown(Player p, String actionId, double seconds) {
        if (seconds <= 0) return;
        long expiryTime = System.currentTimeMillis() + (long) (seconds * 1000);
        playerCooldowns.computeIfAbsent(p.getUniqueId(), k -> new CooldownMap()).put(actionId, expiryTime);
    }

    public long getRemainingCooldownMillis(Player p, String actionId) {
        CooldownMap map = playerCooldowns.get(p.getUniqueId());
        return (map == null) ? 0 : map.remainingMillis(actionId);
    }

    public boolean isAwaitingTarget(Player p) { return playersAwaitingTarget.containsKey(p.getUniqueId()); }
    public TargetInfo getAwaitingTargetInfo(Player p) { return playersAwaitingTarget.get(p.getUniqueId()); }
    public void setAwaitingTarget(Player p, TargetInfo info) { playersAwaitingTarget.put(p.getUniqueId(), info); }
    public void removeAwaitingTarget(Player p) { playersAwaitingTarget.remove(p.getUniqueId()); }
    public GuiMetaCache getMetaCache() { return metaCache; }
    public Map<String, GUI> getGuis() { return guis; }

    public void loadGuis() {
        guis.clear();
        metaCache.clearAll();
        File[] files = guisFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            String id = file.getName().replace(".yml", "");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            try {
                GUI gui = new GUI(config.getString("title", "GUI"), config.getInt("size", 27));
                gui.setId(id);
                ConfigurationSection items = config.getConfigurationSection("items");
                if (items != null) {
                    for (String slotStr : items.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotStr);
                            ItemStack directItem = items.getItemStack(slotStr);
                            if (directItem != null) {
                                gui.setItem(slot, directItem);
                                continue;
                            }
                            ConfigurationSection spec = items.getConfigurationSection(slotStr);
                            if (spec != null) {
                                ItemStack builtItem = buildItemFromSpec(spec);
                                if (builtItem != null) gui.setItem(slot, builtItem);
                            }
                        } catch (Exception ignored) {}
                    }
                }
                addGui(id, gui);
            } catch (Exception ignored) {}
        }
    }

    private ItemStack buildItemFromSpec(ConfigurationSection spec) {
        String matField = spec.getString("material", "STONE");
        String skullField = spec.getString("skull", null);
        ItemStack item;

        if (HeadUtils.isTextureMaterialString(matField) || skullField != null) {
            item = HeadUtils.createHeadBySpec(matField, skullField);
        } else {
            Material mat = matchMaterial(matField);
            item = new ItemStack(mat == null ? Material.STONE : mat);
        }

        item.setAmount(Math.max(1, spec.getInt("amount", 1)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = spec.getString("name", null);
            if (name != null) meta.setDisplayName(color(name));
            List<String> lore = spec.getStringList("lore");
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<>(lore.size());
                for (String s : lore) colored.add(color(s));
                meta.setLore(colored);
            }
            int cmd = spec.getInt("custom_model_data", spec.getInt("model_data", -1));
            if (cmd >= 0) meta.setCustomModelData(cmd);
            int damage = spec.getInt("damage", -1);
            if (damage >= 0 && meta instanceof Damageable) ((Damageable) meta).setDamage(damage);
            if (spec.getBoolean("hide_flags", false)) {
                for (ItemFlag f : ItemFlag.values()) meta.addItemFlags(f);
            }
            item.setItemMeta(meta);
        }
        applyPdcSpec(item, spec);
        return item;
    }

    private void applyPdcSpec(ItemStack item, ConfigurationSection spec) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();

        applyString(meta, KEY_COMMAND_LEFT, spec.getString("actions.left.command"));
        applyString(meta, KEY_COMMAND_RIGHT, spec.getString("actions.right.command"));
        applyString(meta, KEY_COMMAND_SHIFT_LEFT, spec.getString("actions.shift_left.command"));
        applyString(meta, KEY_COMMAND_SHIFT_RIGHT, spec.getString("actions.shift_right.command"));
        applyString(meta, KEY_COMMAND_F, spec.getString("actions.f.command"));
        applyString(meta, KEY_COMMAND_SHIFT_F, spec.getString("actions.shift_f.command"));
        applyString(meta, KEY_COMMAND_Q, spec.getString("actions.q.command"));
        applyString(meta, KEY_COMMAND_SHIFT_Q, spec.getString("actions.shift_q.command"));

        applyString(meta, KEY_EXECUTOR_LEFT, spec.getString("actions.left.executor", "PLAYER"));
        applyString(meta, KEY_EXECUTOR_RIGHT, spec.getString("actions.right.executor", "PLAYER"));
        applyString(meta, KEY_EXECUTOR_SHIFT_LEFT, spec.getString("actions.shift_left.executor", "PLAYER"));
        applyString(meta, KEY_EXECUTOR_SHIFT_RIGHT, spec.getString("actions.shift_right.executor", "PLAYER"));
        applyString(meta, KEY_EXECUTOR_F, spec.getString("actions.f.executor", "PLAYER"));
        applyString(meta, KEY_EXECUTOR_SHIFT_F, spec.getString("actions.shift_f.executor", "PLAYER"));
        applyString(meta, KEY_EXECUTOR_Q, spec.getString("actions.q.executor", "PLAYER"));
        applyString(meta, KEY_EXECUTOR_SHIFT_Q, spec.getString("actions.shift_q.executor", "PLAYER"));

        applyString(meta, KEY_PERMISSION_LEFT, spec.getString("perm.left"));
        applyString(meta, KEY_PERMISSION_RIGHT, spec.getString("perm.right"));
        applyString(meta, KEY_PERMISSION_SHIFT_LEFT, spec.getString("perm.shift_left"));
        applyString(meta, KEY_PERMISSION_SHIFT_RIGHT, spec.getString("perm.shift_right"));
        applyString(meta, KEY_PERMISSION_F, spec.getString("perm.f"));
        applyString(meta, KEY_PERMISSION_SHIFT_F, spec.getString("perm.shift_f"));
        applyString(meta, KEY_PERMISSION_Q, spec.getString("perm.q"));
        applyString(meta, KEY_PERMISSION_SHIFT_Q, spec.getString("perm.shift_q"));

        applyDouble(meta, KEY_COOLDOWN_LEFT, spec.getDouble("cooldown.left", 0.0));
        applyDouble(meta, KEY_COOLDOWN_RIGHT, spec.getDouble("cooldown.right", 0.0));
        applyDouble(meta, KEY_COOLDOWN_SHIFT_LEFT, spec.getDouble("cooldown.shift_left", 0.0));
        applyDouble(meta, KEY_COOLDOWN_SHIFT_RIGHT, spec.getDouble("cooldown.shift_right", 0.0));
        applyDouble(meta, KEY_COOLDOWN_F, spec.getDouble("cooldown.f", 0.0));
        applyDouble(meta, KEY_COOLDOWN_SHIFT_F, spec.getDouble("cooldown.shift_f", 0.0));
        applyDouble(meta, KEY_COOLDOWN_Q, spec.getDouble("cooldown.q", 0.0));
        applyDouble(meta, KEY_COOLDOWN_SHIFT_Q, spec.getDouble("cooldown.shift_q", 0.0));

        applyBoolean(meta, KEY_KEEP_OPEN_LEFT, spec.getBoolean("keep_open.left", false));
        applyBoolean(meta, KEY_KEEP_OPEN_RIGHT, spec.getBoolean("keep_open.right", false));
        applyBoolean(meta, KEY_KEEP_OPEN_SHIFT_LEFT, spec.getBoolean("keep_open.shift_left", false));
        applyBoolean(meta, KEY_KEEP_OPEN_SHIFT_RIGHT, spec.getBoolean("keep_open.shift_right", false));
        applyBoolean(meta, KEY_KEEP_OPEN_F, spec.getBoolean("keep_open.f", false));
        applyBoolean(meta, KEY_KEEP_OPEN_SHIFT_F, spec.getBoolean("keep_open.shift_f", false));
        applyBoolean(meta, KEY_KEEP_OPEN_Q, spec.getBoolean("keep_open.q", false));
        applyBoolean(meta, KEY_KEEP_OPEN_SHIFT_Q, spec.getBoolean("keep_open.shift_q", false));

        applyString(meta, KEY_PERMISSION_MESSAGE, spec.getString("perm.message"));
        applyBoolean(meta, KEY_REQUIRE_TARGET, spec.getBoolean("require_target", false));

        String itemModel = spec.getString("item_model");
        if (itemModel != null && !itemModel.isEmpty()) {
            meta.getPersistentDataContainer().set(KEY_ITEM_MODEL, org.bukkit.persistence.PersistentDataType.STRING, itemModel);
            try {
                NamespacedKey modelKey = NamespacedKey.fromString(itemModel);
                if (modelKey != null) meta.setItemModel(modelKey);
            } catch (Throwable ignored) {}
        }
        if (meta.hasCustomModelData()) {
            meta.getPersistentDataContainer().set(KEY_CUSTOM_MODEL_DATA, org.bukkit.persistence.PersistentDataType.INTEGER, meta.getCustomModelData());
        }
        item.setItemMeta(meta);
    }

    private void applyString(ItemMeta meta, NamespacedKey key, String val) {
        if (val != null && !val.isEmpty()) meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, val);
    }
    private void applyDouble(ItemMeta meta, NamespacedKey key, double val) {
        if (val > 0) meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.DOUBLE, val);
    }
    private void applyBoolean(ItemMeta meta, NamespacedKey key, boolean val) {
        if (val) meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
    }

    private Material matchMaterial(String name) {
        if (name == null) return null;
        String s = name.trim().toUpperCase(Locale.ROOT);
        if (s.startsWith("MINECRAFT:")) s = s.substring(10);
        try { return Material.valueOf(s); } catch (Exception e) { return null; }
    }

    private String color(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }

    public void saveGui(String id) {
        GUI gui = getGui(id);
        if (gui == null) return;
        metaCache.buildForGui(id, gui, this);
        File file = new File(guisFolder, id.toLowerCase() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        config.set("title", gui.getTitle());
        config.set("size", gui.getSize());
        gui.getItems().forEach((slot, item) -> config.set("items." + slot, item));
        AsyncSaver.enqueue(file, config);
    }

    public void createGui(String id, int rows, String title) {
        GUI gui = new GUI(title, rows * 9);
        gui.setId(id);
        addGui(id, gui);
        saveGui(id);
    }

    public void copyGui(String sourceId, String targetId) {
        GUI sourceGui = getGui(sourceId);
        if (sourceGui == null) return;
        GUI targetGui = new GUI(sourceGui.getTitle(), sourceGui.getSize());
        targetGui.setId(targetId);
        sourceGui.getItems().forEach((slot, item) -> targetGui.setItem(slot, item.clone()));
        addGui(targetId, targetGui);
        saveGui(targetId);
    }

    public void removeGui(String id) {
        guis.remove(id.toLowerCase());
        metaCache.remove(id);
        File file = new File(guisFolder, id.toLowerCase() + ".yml");
        if (file.exists()) file.delete();
    }

    public void saveGuis() { guis.keySet().forEach(this::saveGui); }

    private void saveGuisSync() {
        guis.forEach((id, gui) -> {
            File file = new File(guisFolder, id.toLowerCase() + ".yml");
            FileConfiguration config = new YamlConfiguration();
            config.set("title", gui.getTitle());
            config.set("size", gui.getSize());
            gui.getItems().forEach((slot, item) -> config.set("items." + slot, item));
            try { config.save(file); } catch (IOException e) { getLogger().log(Level.SEVERE, "Error saving GUI", e); }
        });
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) econ = rsp.getProvider();
    }

    public Inventory getPlayerSpecificInventory(Player player, String guiId) {
        GUI gui = getGui(guiId);
        if (gui == null) return null;
        String title = placeholderApiEnabled ? PlaceholderAPI.setPlaceholders(player, gui.getTitle()) : gui.getTitle();
        GUIHolder holder = new GUIHolder(guiId);
        Inventory inv = Bukkit.createInventory(holder, gui.getSize(), title);
        holder.setInventory(inv);
        gui.getItems().forEach((slot, item) -> inv.setItem(slot, applyPlaceholders(item.clone(), player)));
        return inv;
    }

    /**
     * 편집용 인벤토리를 생성합니다. Holder를 주입하여 GUIListener가 편집 모드를 감지할 수 있게 합니다.
     */
    public Inventory getEditInventory(String guiId) {
        GUI gui = getGui(guiId);
        if (gui == null) return null;

        GUIHolder holder = new GUIHolder(guiId);
        Inventory inv = Bukkit.createInventory(holder, gui.getSize(), gui.getTitle());
        holder.setInventory(inv);

        // 편집 모드에서는 원본 아이템을 그대로 배치합니다.
        gui.getItems().forEach(inv::setItem);
        return inv;
    }

    public ItemStack applyPlaceholders(ItemStack item, Player player) {
        if (!placeholderApiEnabled || item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta.hasDisplayName()) meta.setDisplayName(PlaceholderAPI.setPlaceholders(player, meta.getDisplayName()));
        if (meta.hasLore()) {
            List<String> lore = new ArrayList<>();
            for (String s : meta.getLore()) lore.add(PlaceholderAPI.setPlaceholders(player, s));
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    public String findGuiIdByTitle(String title) {
        if (title == null) return null;
        for (Map.Entry<String, GUI> entry : guis.entrySet()) {
            if (entry.getValue().getTitle().equals(title)) return entry.getKey();
        }
        return null;
    }

    public static GUIManager getInstance() { return instance; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public GUI getGui(String id) { return guis.get(id.toLowerCase()); }
    public void addGui(String id, GUI gui) { guis.put(id.toLowerCase(), gui); metaCache.buildForGui(id, gui, this); }
    public boolean isInEditMode(Player p) { return playersInEditMode.containsKey(p.getUniqueId()); }
    public void setEditMode(Player p, String guiName) { playersInEditMode.put(p.getUniqueId(), guiName); }
    public String getEditingGuiName(Player p) { return playersInEditMode.get(p.getUniqueId()); }
    public void removeEditMode(Player p) { playersInEditMode.remove(p.getUniqueId()); }
    public boolean isSettingCost(Player p) { return playersSettingCost.containsKey(p.getUniqueId()); }
    public EditSession getCostSession(Player p) { return playersSettingCost.get(p.getUniqueId()); }
    public void startCostSession(Player p, EditSession s) { playersSettingCost.put(p.getUniqueId(), s); }
    public void endCostSession(Player p) { playersSettingCost.remove(p.getUniqueId()); }
    public boolean hasChatSession(Player p) { return chatEditSessions.containsKey(p.getUniqueId()); }
    public EditSession getChatSession(Player p) { return chatEditSessions.get(p.getUniqueId()); }
    public void startChatSession(Player p, EditSession s) { chatEditSessions.put(p.getUniqueId(), s); }
    public void endChatSession(Player p) { chatEditSessions.remove(p.getUniqueId()); }

    public void cleanupPlayer(UUID uuid) {
        playersInEditMode.remove(uuid);
        chatEditSessions.remove(uuid);
        playersSettingCost.remove(uuid);
        playerCooldowns.remove(uuid);
        playersAwaitingTarget.remove(uuid);
    }

    private void startGuiUpdateTask() {
        this.guiUpdateTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Inventory inv = player.getOpenInventory().getTopInventory();
                if (inv.getHolder() instanceof GUIHolder && !isInEditMode(player)) {
                    GUI gui = getGui(((GUIHolder) inv.getHolder()).getGuiId());
                    if (gui != null) gui.getItems().forEach((slot, item) -> inv.setItem(slot, applyPlaceholders(item.clone(), player)));
                }
            }
        }, 20L, 20L);
    }
}