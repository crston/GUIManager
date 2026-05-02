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

    /**
     * 실행자 타입을 정의하는 열거형입니다.
     */
    public enum ExecutorType { PLAYER, CONSOLE, OP }

    /**
     * 아이템의 PersistentData에 사용될 키들입니다.
     */
    public static NamespacedKey KEY_PERMISSION_MESSAGE;
    public static NamespacedKey KEY_REQUIRE_TARGET;
    public static NamespacedKey KEY_CUSTOM_MODEL_DATA;
    public static NamespacedKey KEY_ITEM_DAMAGE;
    public static NamespacedKey KEY_ITEM_MODEL;

    public static NamespacedKey KEY_COMMAND_LEFT, KEY_PERMISSION_LEFT, KEY_COST_LEFT, KEY_MONEY_COST_LEFT, KEY_COOLDOWN_LEFT, KEY_EXECUTOR_LEFT, KEY_KEEP_OPEN_LEFT;
    public static NamespacedKey KEY_COMMAND_SHIFT_LEFT, KEY_PERMISSION_SHIFT_LEFT, KEY_COST_SHIFT_LEFT, KEY_MONEY_COST_SHIFT_LEFT, KEY_COOLDOWN_SHIFT_LEFT, KEY_EXECUTOR_SHIFT_LEFT, KEY_KEEP_OPEN_SHIFT_LEFT;
    public static NamespacedKey KEY_COMMAND_RIGHT, KEY_PERMISSION_RIGHT, KEY_COST_RIGHT, KEY_MONEY_COST_RIGHT, KEY_COOLDOWN_RIGHT, KEY_EXECUTOR_RIGHT, KEY_KEEP_OPEN_RIGHT;
    public static NamespacedKey KEY_COMMAND_SHIFT_RIGHT, KEY_PERMISSION_SHIFT_RIGHT, KEY_COST_SHIFT_RIGHT, KEY_MONEY_COST_SHIFT_RIGHT, KEY_COOLDOWN_SHIFT_RIGHT, KEY_EXECUTOR_SHIFT_RIGHT, KEY_KEEP_OPEN_SHIFT_RIGHT;
    public static NamespacedKey KEY_COMMAND_F, KEY_PERMISSION_F, KEY_COST_F, KEY_MONEY_COST_F, KEY_COOLDOWN_F, KEY_EXECUTOR_F, KEY_KEEP_OPEN_F;
    public static NamespacedKey KEY_COMMAND_SHIFT_F, KEY_PERMISSION_SHIFT_F, KEY_COST_SHIFT_F, KEY_MONEY_COST_SHIFT_F, KEY_COOLDOWN_SHIFT_F, KEY_EXECUTOR_SHIFT_F, KEY_KEEP_OPEN_SHIFT_F;
    public static NamespacedKey KEY_COMMAND_Q, KEY_PERMISSION_Q, KEY_COST_Q, KEY_MONEY_COST_Q, KEY_COOLDOWN_Q, KEY_EXECUTOR_Q, KEY_KEEP_OPEN_Q;
    public static NamespacedKey KEY_COMMAND_SHIFT_Q, KEY_PERMISSION_SHIFT_Q, KEY_COST_SHIFT_Q, KEY_MONEY_COST_SHIFT_Q, KEY_COOLDOWN_SHIFT_Q, KEY_EXECUTOR_SHIFT_Q, KEY_KEEP_OPEN_SHIFT_Q;

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

    public void loadGuis() {
        guis.clear();
        metaCache.clearAll();
        File[] files = guisFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            String id = file.getName().replace(".yml", "");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            GUI gui = new GUI(config.getString("title", "GUI"), config.getInt("size", 27));
            gui.setId(id);
            ConfigurationSection items = config.getConfigurationSection("items");
            if (items != null) {
                for (String slotStr : items.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(slotStr);
                        ItemStack item = items.getItemStack(slotStr);
                        if (item != null) gui.setItem(slot, item);
                    } catch (Exception ignored) {}
                }
            }
            addGui(id, gui);
        }
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

    public Inventory getEditInventory(String guiId) {
        GUI gui = getGui(guiId);
        if (gui == null) return null;
        GUIHolder holder = new GUIHolder(guiId);
        Inventory inv = Bukkit.createInventory(holder, gui.getSize(), gui.getTitle());
        holder.setInventory(inv);
        gui.getItems().forEach(inv::setItem);
        return inv;
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

    public void saveGui(String id) {
        GUI gui = getGui(id);
        if (gui == null) return;
        File file = new File(guisFolder, id.toLowerCase() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        config.set("title", gui.getTitle());
        config.set("size", gui.getSize());
        gui.getItems().forEach((slot, item) -> config.set("items." + slot, item));
        try { config.save(file); } catch (IOException ignored) {}
    }

    private void saveGuisSync() { guis.keySet().forEach(this::saveGui); }
    public void saveGuis() { saveGuisSync(); }
    public void addGui(String id, GUI gui) { guis.put(id.toLowerCase(), gui); metaCache.buildForGui(id, gui, this); }
    public GUI getGui(String id) { return guis.get(id.toLowerCase()); }
    public void removeGui(String id) { guis.remove(id.toLowerCase()); new File(guisFolder, id.toLowerCase() + ".yml").delete(); }
    public Map<String, GUI> getGuis() { return guis; }
    public static GUIManager getInstance() { return instance; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public GuiMetaCache getMetaCache() { return metaCache; }

    public void setCooldown(Player p, String actionId, double seconds) {
        if (seconds <= 0) return;
        long expiryTime = System.currentTimeMillis() + (long) (seconds * 1000);
        playerCooldowns.computeIfAbsent(p.getUniqueId(), k -> new CooldownMap()).put(actionId, expiryTime);
    }

    public long getRemainingCooldownMillis(Player p, String actionId) {
        CooldownMap map = playerCooldowns.get(p.getUniqueId());
        return (map == null) ? 0 : map.remainingMillis(actionId);
    }

    public boolean isInEditMode(Player p) { return playersInEditMode.containsKey(p.getUniqueId()); }
    public void setEditMode(Player p, String guiId) { playersInEditMode.put(p.getUniqueId(), guiId); }
    public String getEditingGuiName(Player p) { return playersInEditMode.get(p.getUniqueId()); }
    public void removeEditMode(Player p) { playersInEditMode.remove(p.getUniqueId()); }

    public boolean isSettingCost(Player p) { return playersSettingCost.containsKey(p.getUniqueId()); }
    public void startCostSession(Player p, EditSession s) { playersSettingCost.put(p.getUniqueId(), s); }
    public void endCostSession(Player p) { playersSettingCost.remove(p.getUniqueId()); }

    public boolean hasChatSession(Player p) { return chatEditSessions.containsKey(p.getUniqueId()); }
    public EditSession getChatSession(Player p) { return chatEditSessions.get(p.getUniqueId()); }
    public void startChatSession(Player p, EditSession s) { chatEditSessions.put(p.getUniqueId(), s); }
    public void endChatSession(Player p) { chatEditSessions.remove(p.getUniqueId()); }

    public boolean isAwaitingTarget(Player p) { return playersAwaitingTarget.containsKey(p.getUniqueId()); }
    public TargetInfo getAwaitingTargetInfo(Player p) { return playersAwaitingTarget.get(p.getUniqueId()); }
    public void setAwaitingTarget(Player p, TargetInfo info) { playersAwaitingTarget.put(p.getUniqueId(), info); }
    public void removeAwaitingTarget(Player p) { playersAwaitingTarget.remove(p.getUniqueId()); }

    public void cleanupPlayer(UUID id) {
        playersInEditMode.remove(id);
        chatEditSessions.remove(id);
        playersSettingCost.remove(id);
        playersAwaitingTarget.remove(id);
        playerCooldowns.remove(id);
    }

    private void setupEconomy() { RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class); if (rsp != null) econ = rsp.getProvider(); }

    /**
     * GUI 자동 업데이트 태스크입니다.
     * 편집 모드이거나 편집기 창이 열려있을 때는 업데이트를 중단하여 아이템 이동 방해를 방지합니다.
     */
    private void startGuiUpdateTask() {
        this.guiUpdateTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Inventory inv = player.getOpenInventory().getTopInventory();

                // 1. 홀더가 GUIHolder인지 확인
                // 2. 플레이어가 일반 편집 모드(isInEditMode)가 아닌지 확인
                // 3. 현재 열린 창이 ItemEditorHolder(아이템 설정 창)가 아닌지 확인
                if (inv.getHolder() instanceof GUIHolder && !isInEditMode(player) && !(inv.getHolder() instanceof ItemEditorHolder)) {
                    GUI gui = getGui(((GUIHolder) inv.getHolder()).getGuiId());
                    if (gui != null) {
                        gui.getItems().forEach((slot, item) -> {
                            ItemStack updated = applyPlaceholders(item.clone(), player);
                            ItemStack current = inv.getItem(slot);

                            // 현재 아이템과 업데이트될 아이템이 다를 때만 갱신 (커서 아이템 튕김 방지)
                            if (current == null || !current.equals(updated)) {
                                inv.setItem(slot, updated);
                            }
                        });
                    }
                }
            }
        }, 20L, 20L);
    }
}