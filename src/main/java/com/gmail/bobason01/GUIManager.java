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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class GUIManager extends JavaPlugin {

    private static GUIManager instance;
    public static Economy econ = null;
    private boolean placeholderApiEnabled = false;
    private BukkitTask autoSaveTask;
    private BukkitTask guiUpdateTask;
    private LanguageManager languageManager;

    public enum ExecutorType { PLAYER, CONSOLE, OP }

    // Namespaced Keys
    public static NamespacedKey KEY_PERMISSION_MESSAGE, KEY_REQUIRE_TARGET, KEY_CUSTOM_MODEL_DATA, KEY_ITEM_DAMAGE, KEY_ITEM_MODEL_ID;

    public static NamespacedKey KEY_COMMAND_LEFT, KEY_PERMISSION_LEFT, KEY_COST_LEFT, KEY_MONEY_COST_LEFT, KEY_COOLDOWN_LEFT, KEY_EXECUTOR_LEFT, KEY_KEEP_OPEN_LEFT;
    public static NamespacedKey KEY_COMMAND_SHIFT_LEFT, KEY_PERMISSION_SHIFT_LEFT, KEY_COST_SHIFT_LEFT, KEY_MONEY_COST_SHIFT_LEFT, KEY_COOLDOWN_SHIFT_LEFT, KEY_EXECUTOR_SHIFT_LEFT, KEY_KEEP_OPEN_SHIFT_LEFT;

    public static NamespacedKey KEY_COMMAND_RIGHT, KEY_PERMISSION_RIGHT, KEY_COST_RIGHT, KEY_MONEY_COST_RIGHT, KEY_COOLDOWN_RIGHT, KEY_EXECUTOR_RIGHT, KEY_KEEP_OPEN_RIGHT;
    public static NamespacedKey KEY_COMMAND_SHIFT_RIGHT, KEY_PERMISSION_SHIFT_RIGHT, KEY_COST_SHIFT_RIGHT, KEY_MONEY_COST_SHIFT_RIGHT, KEY_COOLDOWN_SHIFT_RIGHT, KEY_EXECUTOR_SHIFT_RIGHT, KEY_KEEP_OPEN_SHIFT_RIGHT;

    public static NamespacedKey KEY_COMMAND_F, KEY_PERMISSION_F, KEY_COST_F, KEY_MONEY_COST_F, KEY_COOLDOWN_F, KEY_EXECUTOR_F;
    public static NamespacedKey KEY_COMMAND_SHIFT_F, KEY_PERMISSION_SHIFT_F, KEY_COST_SHIFT_F, KEY_MONEY_COST_SHIFT_F, KEY_COOLDOWN_SHIFT_F, KEY_EXECUTOR_SHIFT_F;

    public static NamespacedKey KEY_COMMAND_Q, KEY_PERMISSION_Q, KEY_COST_Q, KEY_MONEY_COST_Q, KEY_COOLDOWN_Q, KEY_EXECUTOR_Q;
    public static NamespacedKey KEY_COMMAND_SHIFT_Q, KEY_PERMISSION_SHIFT_Q, KEY_COST_SHIFT_Q, KEY_MONEY_COST_SHIFT_Q, KEY_COOLDOWN_SHIFT_Q, KEY_EXECUTOR_SHIFT_Q;

    private final Map<String, GUI> guis = new ConcurrentHashMap<>();
    private File guisFolder;

    private final Map<UUID, String> playersInEditMode = new ConcurrentHashMap<>();
    private final Map<UUID, EditSession> chatEditSessions = new ConcurrentHashMap<>();
    private final Map<UUID, EditSession> playersSettingCost = new ConcurrentHashMap<>();
    private final Map<UUID, TargetInfo> playersAwaitingTarget = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        this.languageManager = new LanguageManager(this);

        if (!setupEconomy()) {
            getLogger().warning("Vault not found Money cost features will be disabled");
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderApiEnabled = true;
            getLogger().info("PlaceholderAPI found Placeholders enabled");
        } else {
            getLogger().info("PlaceholderAPI not found Placeholders disabled");
        }

        initializeKeys();

        this.guisFolder = new File(getDataFolder(), "guis");
        if (!this.guisFolder.exists()) {
            this.guisFolder.mkdirs();
        }

        loadGuis();

        GUIListener guiListener = new GUIListener(this);
        Bukkit.getPluginManager().registerEvents(guiListener, this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this, guiListener), this);
        Bukkit.getPluginManager().registerEvents(new KeybindListener(this, guiListener), this);

        GUICommand guiCommand = new GUICommand(this);
        Objects.requireNonNull(getCommand("gui")).setExecutor(guiCommand);
        Objects.requireNonNull(getCommand("gui")).setTabCompleter(guiCommand);

        long autoSaveInterval = 20L * 60 * 5;
        this.autoSaveTask = Bukkit.getScheduler().runTaskTimer(this, this::saveGuis, autoSaveInterval, autoSaveInterval);

        if (this.placeholderApiEnabled) {
            startGuiUpdateTask();
        }

        getLogger().info("GUIManager enabled");
    }

    @Override
    public void onDisable() {
        if (this.autoSaveTask != null && !this.autoSaveTask.isCancelled()) {
            this.autoSaveTask.cancel();
        }
        if (this.guiUpdateTask != null && !this.guiUpdateTask.isCancelled()) {
            this.guiUpdateTask.cancel();
        }
        getLogger().info("Saving all GUI data before disable");
        saveGuisSync();
        AsyncSaver.shutdown();
        HeadCache.clear();
        getLogger().info("GUIManager disabled");
    }

    private void startGuiUpdateTask() {
        this.guiUpdateTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Inventory openInv = player.getOpenInventory().getTopInventory();

                if (openInv == null || !(openInv.getHolder() instanceof GUIHolder)) {
                    continue;
                }

                GUIHolder holder = (GUIHolder) openInv.getHolder();
                String guiId = holder.getGuiId();

                if (isInEditMode(player)) continue;

                GUI gui = getGui(guiId);
                if (gui == null) continue;

                for (Map.Entry<Integer, ItemStack> entry : gui.getItems().entrySet()) {
                    int slot = entry.getKey();
                    ItemStack templateItem = entry.getValue();
                    if (templateItem == null) continue;

                    ItemStack updatedItem = applyPlaceholders(templateItem.clone(), player);
                    ItemStack currentItem = openInv.getItem(slot);

                    if (!Objects.equals(currentItem, updatedItem)) {
                        openInv.setItem(slot, updatedItem);
                    }
                }
            }
        }, 20L, 20L);
    }

    private ItemStack applyPlaceholders(ItemStack item, Player player) {
        if (!placeholderApiEnabled || item == null || !item.hasItemMeta()) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        boolean changed = false;

        if (meta.hasDisplayName()) {
            String originalName = meta.getDisplayName();
            String newName = PlaceholderAPI.setPlaceholders(player, originalName);
            if (!originalName.equals(newName)) {
                meta.setDisplayName(newName);
                changed = true;
            }
        }

        if (meta.hasLore()) {
            List<String> originalLore = meta.getLore();
            List<String> newLore = new ArrayList<>();
            boolean loreChanged = false;

            if (originalLore != null) {
                for (String line : originalLore) {
                    String newLine = PlaceholderAPI.setPlaceholders(player, line);
                    newLore.add(newLine);
                    if (!line.equals(newLine)) loreChanged = true;
                }
            }

            if (loreChanged) {
                meta.setLore(newLore);
                changed = true;
            }
        }

        if (changed) {
            item.setItemMeta(meta);
        }
        return item;
    }

    public static GUIManager getInstance() {
        return instance;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
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

        KEY_COMMAND_SHIFT_F = new NamespacedKey(this, "cmd_s_f");
        KEY_PERMISSION_SHIFT_F = new NamespacedKey(this, "perm_s_f");
        KEY_COST_SHIFT_F = new NamespacedKey(this, "cost_s_f");
        KEY_MONEY_COST_SHIFT_F = new NamespacedKey(this, "money_cost_s_f");
        KEY_COOLDOWN_SHIFT_F = new NamespacedKey(this, "cooldown_s_f");
        KEY_EXECUTOR_SHIFT_F = new NamespacedKey(this, "executor_s_f");

        KEY_COMMAND_Q = new NamespacedKey(this, "cmd_q");
        KEY_PERMISSION_Q = new NamespacedKey(this, "perm_q");
        KEY_COST_Q = new NamespacedKey(this, "cost_q");
        KEY_MONEY_COST_Q = new NamespacedKey(this, "money_cost_q");
        KEY_COOLDOWN_Q = new NamespacedKey(this, "cooldown_q");
        KEY_EXECUTOR_Q = new NamespacedKey(this, "executor_q");

        KEY_COMMAND_SHIFT_Q = new NamespacedKey(this, "cmd_s_q");
        KEY_PERMISSION_SHIFT_Q = new NamespacedKey(this, "perm_s_q");
        KEY_COST_SHIFT_Q = new NamespacedKey(this, "cost_s_q");
        KEY_MONEY_COST_SHIFT_Q = new NamespacedKey(this, "money_cost_s_q");
        KEY_COOLDOWN_SHIFT_Q = new NamespacedKey(this, "cooldown_s_q");
        KEY_EXECUTOR_SHIFT_Q = new NamespacedKey(this, "executor_s_q");
    }

    public void setCooldown(Player player, String uniqueActionId, double seconds) {
        if (seconds <= 0) return;
        long expiryTime = System.currentTimeMillis() + (long) (seconds * 1000);
        playerCooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(uniqueActionId, expiryTime);
    }

    public long getRemainingCooldownMillis(Player player, String uniqueActionId) {
        Map<String, Long> cooldowns = playerCooldowns.get(player.getUniqueId());
        if (cooldowns == null) return 0;
        long expiryTime = cooldowns.getOrDefault(uniqueActionId, 0L);
        return Math.max(0, expiryTime - System.currentTimeMillis());
    }

    public Inventory getPlayerSpecificInventory(Player player, String guiId) {
        GUI originalGui = getGui(guiId);
        if (originalGui == null) {
            return null;
        }

        String title = originalGui.getTitle();
        if (placeholderApiEnabled) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }

        GUIHolder holder = new GUIHolder(guiId);
        Inventory playerInv = Bukkit.createInventory(holder, originalGui.getSize(), title);
        holder.setInventory(playerInv);

        for (Map.Entry<Integer, ItemStack> entry : originalGui.getItems().entrySet()) {
            ItemStack originalItem = entry.getValue();
            if (originalItem == null) continue;

            ItemStack playerItem = applyPlaceholders(originalItem.clone(), player);
            playerInv.setItem(entry.getKey(), playerItem);
        }

        return playerInv;
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

    public String findGuiIdByTitle(String title) {
        for (Map.Entry<String, GUI> entry : guis.entrySet()) {
            if (entry.getValue().getTitle().equals(title)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void addGui(String id, GUI gui) {
        guis.put(id.toLowerCase(), gui);
    }

    public void createGui(String id, int rows, String title) {
        GUI gui = new GUI(title, rows * 9);
        gui.setId(id);
        addGui(id, gui);
        saveGui(id);
    }

    public void updateGuiTitle(String id, String newTitle) {
        GUI gui = getGui(id);
        if (gui != null) {
            gui.setTitle(newTitle);
        }
    }

    public void updateGuiId(String oldId, String newId) {
        GUI gui = guis.remove(oldId.toLowerCase());
        if (gui != null) {
            guis.put(newId.toLowerCase(), gui);
            gui.setId(newId);

            File oldFile = new File(guisFolder, oldId.toLowerCase() + ".yml");
            if (oldFile.exists()) {
                oldFile.delete();
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
        if (gui != null) {
            File file = new File(guisFolder, id.toLowerCase() + ".yml");
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public void loadGuis() {
        guis.clear();

        if (!guisFolder.exists()) {
            guisFolder.mkdirs();
        }
        File[] files = guisFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String id = file.getName().replace(".yml", "");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            try {
                String title = config.getString("title", "GUI");
                int size = config.getInt("size", 27);
                GUI gui = new GUI(title, size);
                gui.setId(id);

                ConfigurationSection itemsSection = config.getConfigurationSection("items");
                if (itemsSection != null) {
                    for (String slotStr : itemsSection.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotStr);
                            ItemStack direct = itemsSection.getItemStack(slotStr);
                            if (direct != null) {
                                gui.setItem(slot, direct);
                                continue;
                            }
                            ConfigurationSection spec = itemsSection.getConfigurationSection(slotStr);
                            if (spec != null) {
                                ItemStack built = buildItemFromSpec(spec);
                                if (built != null) gui.setItem(slot, built);
                            }
                        } catch (Exception itemEx) {
                            getLogger().warning("Error loading item in slot " + slotStr + " for GUI " + id + " " + itemEx.getMessage());
                        }
                    }
                }
                addGui(id, gui);
            } catch (Exception guiEx) {
                getLogger().log(Level.SEVERE, "Critical error loading GUI " + id + " " + guiEx.getMessage());
            }
        }
        getLogger().info("Loaded " + guis.size() + " GUIs from files");
    }

    private ItemStack buildItemFromSpec(ConfigurationSection spec) {
        String materialField = spec.getString("material", "STONE");
        String skullField = spec.getString("skull", null);

        ItemStack item;

        if (HeadUtils.isTextureMaterialString(materialField) || skullField != null) {
            item = HeadUtils.createHeadBySpec(materialField, skullField);
        } else {
            Material mat = matchMaterial(materialField);
            if (mat == null) mat = Material.STONE;
            item = new ItemStack(mat);
        }

        int amount = Math.max(1, spec.getInt("amount", 1));
        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = spec.getString("name", null);
            if (name != null) meta.setDisplayName(color(name));

            List<String> lore = spec.getStringList("lore");
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<>(lore.size());
                for (String l : lore) colored.add(color(l));
                meta.setLore(colored);
            }

            int cmd = spec.getInt("custom_model_data", spec.getInt("model", spec.getInt("model_data", -1)));
            if (cmd >= 0) {
                meta.setCustomModelData(cmd);
            }

            int damage = spec.getInt("damage", -1);
            if (damage >= 0 && meta instanceof Damageable) {
                try {
                    ((Damageable) meta).setDamage(damage);
                } catch (Throwable ignored) {}
            }

            boolean hideFlags = spec.getBoolean("hide_flags", false);
            if (hideFlags) {
                try {
                    for (ItemFlag f : ItemFlag.values()) meta.addItemFlags(f);
                } catch (Throwable ignored) {}
            }

            item.setItemMeta(meta);
        }

        applyPdcSpec(item, spec);
        return item;
    }

    private void applyPdcSpec(ItemStack item, ConfigurationSection spec) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

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

        applyString(meta, KEY_PERMISSION_MESSAGE, spec.getString("perm.message"));
        applyBoolean(meta, KEY_REQUIRE_TARGET, spec.getBoolean("require_target", false));

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
        s = s.replace("MINECRAFT:", "");
        try {
            return Material.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String color(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }

    public void saveGui(String id) {
        GUI gui = getGui(id);
        if (gui == null) return;

        File file = new File(guisFolder, id.toLowerCase() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("title", gui.getTitle());
        config.set("size", gui.getSize());
        config.set("items", null);
        gui.getItems().forEach((slot, item) -> config.set("items." + slot, item));

        AsyncSaver.enqueue(file, config);
    }

    public void saveGuis() {
        guis.keySet().forEach(this::saveGui);
    }

    private void saveGuisSync() {
        for (String id : guis.keySet()) {
            GUI gui = guis.get(id);
            if (gui == null) continue;
            File file = new File(guisFolder, id.toLowerCase() + ".yml");
            FileConfiguration config = new YamlConfiguration();
            config.set("title", gui.getTitle());
            config.set("size", gui.getSize());
            config.set("items", null);
            gui.getItems().forEach((slot, item) -> config.set("items." + slot, item));
            try {
                config.save(file);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not save GUI to file " + file.getName(), e);
            }
        }
    }
}