package com.gmail.bobason01;

import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GUIManager extends JavaPlugin {

    private static GUIManager instance;
    public static Economy econ = null;
    private boolean placeholderApiEnabled = false;
    private BukkitTask autoSaveTask;
    private BukkitTask guiUpdateTask;
    private final GuiMetaCache metaCache = new GuiMetaCache();

    public enum ExecutorType { PLAYER, CONSOLE, OP }

    // NamespacedKey 선언 (생략 없음)
    public static NamespacedKey KEY_PERMISSION_MESSAGE, KEY_REQUIRE_TARGET, KEY_CUSTOM_MODEL_DATA, KEY_ITEM_MODEL;
    public static NamespacedKey KEY_COMMAND_LEFT, KEY_PERMISSION_LEFT, KEY_COST_LEFT, KEY_REWARD_LEFT, KEY_MONEY_COST_LEFT, KEY_COOLDOWN_LEFT, KEY_EXECUTOR_LEFT, KEY_KEEP_OPEN_LEFT;
    public static NamespacedKey KEY_COMMAND_SHIFT_LEFT, KEY_PERMISSION_SHIFT_LEFT, KEY_COST_SHIFT_LEFT, KEY_REWARD_SHIFT_LEFT, KEY_MONEY_COST_SHIFT_LEFT, KEY_COOLDOWN_SHIFT_LEFT, KEY_EXECUTOR_SHIFT_LEFT, KEY_KEEP_OPEN_SHIFT_LEFT;
    public static NamespacedKey KEY_COMMAND_RIGHT, KEY_PERMISSION_RIGHT, KEY_COST_RIGHT, KEY_REWARD_RIGHT, KEY_MONEY_COST_RIGHT, KEY_COOLDOWN_RIGHT, KEY_EXECUTOR_RIGHT, KEY_KEEP_OPEN_RIGHT;
    public static NamespacedKey KEY_COMMAND_SHIFT_RIGHT, KEY_PERMISSION_SHIFT_RIGHT, KEY_COST_SHIFT_RIGHT, KEY_REWARD_SHIFT_RIGHT, KEY_MONEY_COST_SHIFT_RIGHT, KEY_COOLDOWN_SHIFT_RIGHT, KEY_EXECUTOR_SHIFT_RIGHT, KEY_KEEP_OPEN_SHIFT_RIGHT;
    public static NamespacedKey KEY_COMMAND_F, KEY_PERMISSION_F, KEY_COST_F, KEY_REWARD_F, KEY_MONEY_COST_F, KEY_COOLDOWN_F, KEY_EXECUTOR_F, KEY_KEEP_OPEN_F;
    public static NamespacedKey KEY_COMMAND_SHIFT_F, KEY_PERMISSION_SHIFT_F, KEY_COST_SHIFT_F, KEY_REWARD_SHIFT_F, KEY_MONEY_COST_SHIFT_F, KEY_COOLDOWN_SHIFT_F, KEY_EXECUTOR_SHIFT_F, KEY_KEEP_OPEN_SHIFT_F;
    public static NamespacedKey KEY_COMMAND_Q, KEY_PERMISSION_Q, KEY_COST_Q, KEY_REWARD_Q, KEY_MONEY_COST_Q, KEY_COOLDOWN_Q, KEY_EXECUTOR_Q, KEY_KEEP_OPEN_Q;
    public static NamespacedKey KEY_COMMAND_SHIFT_Q, KEY_PERMISSION_SHIFT_Q, KEY_COST_SHIFT_Q, KEY_REWARD_SHIFT_Q, KEY_MONEY_COST_SHIFT_Q, KEY_COOLDOWN_SHIFT_Q, KEY_EXECUTOR_SHIFT_Q, KEY_KEEP_OPEN_SHIFT_Q;

    private final Map<String, GUI> guis = new ConcurrentHashMap<>();
    private File guisFolder;

    private final Map<UUID, String> playersInEditMode = new ConcurrentHashMap<>();
    private final Map<UUID, EditSession> chatEditSessions = new ConcurrentHashMap<>();
    private final Map<UUID, TargetInfo> playersAwaitingTarget = new ConcurrentHashMap<>();
    private final Map<UUID, CooldownMap> playerCooldowns = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        setupEconomy();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) placeholderApiEnabled = true;
        initializeKeys();
        guisFolder = new File(getDataFolder(), "guis");
        if (!guisFolder.exists()) guisFolder.mkdirs();
        loadGuis();
        GUIListener guiListener = new GUIListener(this);
        Bukkit.getPluginManager().registerEvents(guiListener, this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this, guiListener), this);
        GUICommand cmd = new GUICommand(this);
        getCommand("gui").setExecutor(cmd);
        getCommand("gui").setTabCompleter(cmd);
        autoSaveTask = Bukkit.getScheduler().runTaskTimer(this, this::saveGuis, 6000L, 6000L);
        if (placeholderApiEnabled) startGuiUpdateTask();
    }

    @Override
    public void onDisable() {
        saveGuisSync();
        metaCache.clearAll();
        HeadCache.clear();
    }

    /**
     * Java 9+ Matcher API를 사용한 HEX 컬러 변환 (기울임 방지)
     */
    public static String color(String msg) {
        if (msg == null || msg.isEmpty()) return msg;
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(msg);
        StringBuilder buffer = new StringBuilder(msg.length() + 16);
        while (matcher.find()) {
            String colorCode = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : colorCode.toCharArray()) replacement.append('§').append(c);
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public ItemStack applyPlaceholders(ItemStack item, Player player) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();

        // 이름 처리
        if (meta.hasDisplayName()) {
            String name = meta.getDisplayName();
            if (placeholderApiEnabled) name = PlaceholderAPI.setPlaceholders(player, name);
            meta.setDisplayName(color(name) + "§r");
        }

        // 로어 처리 (각 줄마다 기울임 해제 강제 적용)
        if (meta.hasLore()) {
            List<String> updatedLore = new ArrayList<>();
            for (String line : meta.getLore()) {
                String translated = line;
                if (placeholderApiEnabled) translated = PlaceholderAPI.setPlaceholders(player, translated);
                updatedLore.add("§r" + color(translated) + "§r");
            }
            meta.setLore(updatedLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    public void initializeKeys() {
        KEY_PERMISSION_MESSAGE = new NamespacedKey(this, "perm_msg");
        KEY_REQUIRE_TARGET = new NamespacedKey(this, "req_target");
        KEY_CUSTOM_MODEL_DATA = new NamespacedKey(this, "model_data");
        KEY_ITEM_MODEL = new NamespacedKey(this, "item_model");
        KEY_COMMAND_LEFT = new NamespacedKey(this, "cmd_left");
        KEY_PERMISSION_LEFT = new NamespacedKey(this, "perm_left");
        KEY_COST_LEFT = new NamespacedKey(this, "cost_left");
        KEY_REWARD_LEFT = new NamespacedKey(this, "reward_left");
        KEY_MONEY_COST_LEFT = new NamespacedKey(this, "money_cost_left");
        KEY_COOLDOWN_LEFT = new NamespacedKey(this, "cooldown_left");
        KEY_EXECUTOR_LEFT = new NamespacedKey(this, "executor_left");
        KEY_KEEP_OPEN_LEFT = new NamespacedKey(this, "keep_open_left");
        KEY_COMMAND_SHIFT_LEFT = new NamespacedKey(this, "cmd_s_left");
        KEY_PERMISSION_SHIFT_LEFT = new NamespacedKey(this, "perm_s_left");
        KEY_COST_SHIFT_LEFT = new NamespacedKey(this, "cost_s_left");
        KEY_REWARD_SHIFT_LEFT = new NamespacedKey(this, "reward_s_left");
        KEY_MONEY_COST_SHIFT_LEFT = new NamespacedKey(this, "money_cost_s_left");
        KEY_COOLDOWN_SHIFT_LEFT = new NamespacedKey(this, "cooldown_s_left");
        KEY_EXECUTOR_SHIFT_LEFT = new NamespacedKey(this, "executor_s_left");
        KEY_KEEP_OPEN_SHIFT_LEFT = new NamespacedKey(this, "keep_open_s_left");
        KEY_COMMAND_RIGHT = new NamespacedKey(this, "cmd_right");
        KEY_PERMISSION_RIGHT = new NamespacedKey(this, "perm_right");
        KEY_COST_RIGHT = new NamespacedKey(this, "cost_right");
        KEY_REWARD_RIGHT = new NamespacedKey(this, "reward_right");
        KEY_MONEY_COST_RIGHT = new NamespacedKey(this, "money_cost_right");
        KEY_COOLDOWN_RIGHT = new NamespacedKey(this, "cooldown_right");
        KEY_EXECUTOR_RIGHT = new NamespacedKey(this, "executor_right");
        KEY_KEEP_OPEN_RIGHT = new NamespacedKey(this, "keep_open_right");
        KEY_COMMAND_SHIFT_RIGHT = new NamespacedKey(this, "cmd_s_right");
        KEY_PERMISSION_SHIFT_RIGHT = new NamespacedKey(this, "perm_s_right");
        KEY_COST_SHIFT_RIGHT = new NamespacedKey(this, "cost_s_right");
        KEY_REWARD_SHIFT_RIGHT = new NamespacedKey(this, "reward_s_right");
        KEY_MONEY_COST_SHIFT_RIGHT = new NamespacedKey(this, "money_cost_s_right");
        KEY_COOLDOWN_SHIFT_RIGHT = new NamespacedKey(this, "cooldown_s_right");
        KEY_EXECUTOR_SHIFT_RIGHT = new NamespacedKey(this, "executor_s_right");
        KEY_KEEP_OPEN_SHIFT_RIGHT = new NamespacedKey(this, "keep_open_s_right");
        KEY_COMMAND_F = new NamespacedKey(this, "cmd_f");
        KEY_PERMISSION_F = new NamespacedKey(this, "perm_f");
        KEY_COST_F = new NamespacedKey(this, "cost_f");
        KEY_REWARD_F = new NamespacedKey(this, "reward_f");
        KEY_MONEY_COST_F = new NamespacedKey(this, "money_cost_f");
        KEY_COOLDOWN_F = new NamespacedKey(this, "cooldown_f");
        KEY_EXECUTOR_F = new NamespacedKey(this, "executor_f");
        KEY_KEEP_OPEN_F = new NamespacedKey(this, "keep_open_f");
        KEY_COMMAND_SHIFT_F = new NamespacedKey(this, "cmd_s_f");
        KEY_PERMISSION_SHIFT_F = new NamespacedKey(this, "perm_s_f");
        KEY_COST_SHIFT_F = new NamespacedKey(this, "cost_s_f");
        KEY_REWARD_SHIFT_F = new NamespacedKey(this, "reward_s_f");
        KEY_MONEY_COST_SHIFT_F = new NamespacedKey(this, "money_cost_s_f");
        KEY_COOLDOWN_SHIFT_F = new NamespacedKey(this, "cooldown_s_f");
        KEY_EXECUTOR_SHIFT_F = new NamespacedKey(this, "executor_s_f");
        KEY_KEEP_OPEN_SHIFT_F = new NamespacedKey(this, "keep_open_s_f");
        KEY_COMMAND_Q = new NamespacedKey(this, "cmd_q");
        KEY_PERMISSION_Q = new NamespacedKey(this, "perm_q");
        KEY_COST_Q = new NamespacedKey(this, "cost_q");
        KEY_REWARD_Q = new NamespacedKey(this, "reward_q");
        KEY_MONEY_COST_Q = new NamespacedKey(this, "money_cost_q");
        KEY_COOLDOWN_Q = new NamespacedKey(this, "cooldown_q");
        KEY_EXECUTOR_Q = new NamespacedKey(this, "executor_q");
        KEY_KEEP_OPEN_Q = new NamespacedKey(this, "keep_open_q");
        KEY_COMMAND_SHIFT_Q = new NamespacedKey(this, "cmd_s_q");
        KEY_PERMISSION_SHIFT_Q = new NamespacedKey(this, "perm_s_q");
        KEY_COST_SHIFT_Q = new NamespacedKey(this, "cost_s_q");
        KEY_REWARD_SHIFT_Q = new NamespacedKey(this, "reward_s_q");
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
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            GUI gui = new GUI(config.getString("title", "GUI"), config.getInt("size", 27));
            gui.setId(file.getName().replace(".yml", ""));
            gui.setPermission(config.getString("permission", ""));
            gui.setTargets(config.getString("targets", ""));
            ConfigurationSection items = config.getConfigurationSection("items");
            if (items != null) {
                for (String slotStr : items.getKeys(false)) {
                    gui.setItem(Integer.parseInt(slotStr), items.getItemStack(slotStr));
                }
            }
            addGui(gui.getId(), gui);
        }
    }

    public void saveGui(String id) {
        GUI gui = getGui(id);
        if (gui == null) return;
        metaCache.buildForGui(id, gui, this);
        File file = new File(guisFolder, id.toLowerCase() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        config.set("title", gui.getTitle());
        config.set("size", gui.getSize());
        config.set("permission", gui.getPermission());
        config.set("targets", gui.getTargets());
        gui.getItems().forEach((slot, item) -> config.set("items." + slot, item));
        try { config.save(file); } catch (IOException ignored) {}
    }

    public Inventory getEditInventory(String guiId) {
        GUI gui = getGui(guiId);
        if (gui == null) return null;
        GUIHolder holder = new GUIHolder(guiId);
        Inventory inv = Bukkit.createInventory(holder, gui.getSize(), color(gui.getTitle()));
        holder.setInventory(inv);
        gui.getItems().forEach((slot, item) -> { if (slot < inv.getSize()) inv.setItem(slot, item); });
        return inv;
    }

    public Inventory getPlayerSpecificInventory(Player player, String guiId) {
        GUI gui = getGui(guiId);
        if (gui == null) return null;
        String title = gui.getTitle();
        if (placeholderApiEnabled) title = PlaceholderAPI.setPlaceholders(player, title);
        GUIHolder holder = new GUIHolder(guiId);
        Inventory inv = Bukkit.createInventory(holder, gui.getSize(), color(title));
        holder.setInventory(inv);
        gui.getItems().forEach((slot, item) -> {
            if (slot < inv.getSize()) inv.setItem(slot, applyPlaceholders(item.clone(), player));
        });
        return inv;
    }

    private void startGuiUpdateTask() {
        guiUpdateTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Inventory inv = player.getOpenInventory().getTopInventory();
                if (inv.getHolder() instanceof GUIHolder && !isInEditMode(player)) {
                    GUI gui = getGui(((GUIHolder) inv.getHolder()).getGuiId());
                    if (gui != null) {
                        gui.getItems().forEach((slot, item) -> {
                            if (slot < inv.getSize()) {
                                ItemStack updated = applyPlaceholders(item.clone(), player);
                                ItemStack current = inv.getItem(slot);
                                if (current == null || !current.equals(updated)) inv.setItem(slot, updated);
                            }
                        });
                    }
                }
            }
        }, 20L, 20L);
    }

    public void saveGuisSync() { guis.keySet().forEach(this::saveGui); }
    public void saveGuis() { saveGuisSync(); }
    public void addGui(String id, GUI gui) { guis.put(id.toLowerCase(), gui); metaCache.buildForGui(id, gui, this); }
    public GUI getGui(String id) { return guis.get(id.toLowerCase()); }
    public void removeGui(String id) { guis.remove(id.toLowerCase()); File file = new File(guisFolder, id.toLowerCase() + ".yml"); if (file.exists()) file.delete(); }
    public void createGui(String id, int rows, String title) { GUI gui = new GUI(title, rows * 9); gui.setId(id); addGui(id, gui); saveGui(id); }
    public void setCooldown(Player p, String actionId, double seconds) { if (seconds <= 0) return; long expiryTime = System.currentTimeMillis() + (long) (seconds * 1000); playerCooldowns.computeIfAbsent(p.getUniqueId(), k -> new CooldownMap()).put(actionId, expiryTime); }
    public long getRemainingCooldownMillis(Player p, String actionId) { CooldownMap map = playerCooldowns.get(p.getUniqueId()); return (map == null) ? 0 : map.remainingMillis(actionId); }
    public boolean isInEditMode(Player p) { return playersInEditMode.containsKey(p.getUniqueId()); }
    public void setEditMode(Player p, String guiId) { playersInEditMode.put(p.getUniqueId(), guiId); }
    public String getEditingGuiName(Player p) { return playersInEditMode.get(p.getUniqueId()); }
    public void removeEditMode(Player p) { playersInEditMode.remove(p.getUniqueId()); }
    public boolean hasChatSession(Player p) { return chatEditSessions.containsKey(p.getUniqueId()); }
    public EditSession getChatSession(Player p) { return chatEditSessions.get(p.getUniqueId()); }
    public void startChatSession(Player p, EditSession s) { chatEditSessions.put(p.getUniqueId(), s); }
    public void endChatSession(Player p) { chatEditSessions.remove(p.getUniqueId()); }
    public boolean isAwaitingTarget(Player p) { return playersAwaitingTarget.containsKey(p.getUniqueId()); }
    public TargetInfo getAwaitingTargetInfo(Player p) { return playersAwaitingTarget.get(p.getUniqueId()); }
    public void setAwaitingTarget(Player p, TargetInfo info) { playersAwaitingTarget.put(p.getUniqueId(), info); }
    public void removeAwaitingTarget(Player p) { playersAwaitingTarget.remove(p.getUniqueId()); }
    public void cleanupPlayer(UUID id) { playersInEditMode.remove(id); chatEditSessions.remove(id); playersAwaitingTarget.remove(id); playerCooldowns.remove(id); }
    public GuiMetaCache getMetaCache() { return metaCache; }
    public static GUIManager getInstance() { return instance; }
    private void setupEconomy() { RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class); if (rsp != null) econ = rsp.getProvider(); }
    public Map<String, GUI> getGuis() { return guis; }
}