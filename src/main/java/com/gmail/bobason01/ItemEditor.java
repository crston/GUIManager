package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ItemEditor {

    public static final String TITLE_PREFIX = "§7[Item Editor] §6";
    public static final String COST_TITLE_PREFIX = "§7[Item Cost Editor] §e";
    public static final NamespacedKey KEY_PAGE = new NamespacedKey(GUIManager.getInstance(), "editor_page");

    private static final int LORE_SLOTS_PER_PAGE = 7;
    private static final ItemStack SEPARATOR_PANE;

    static {
        SEPARATOR_PANE = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = SEPARATOR_PANE.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            SEPARATOR_PANE.setItemMeta(meta);
        }
    }

    public static void open(Player player, EditSession session) {
        String title = TITLE_PREFIX + session.getGuiName() + " (Slot " + session.getSlot() + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Row 1: General Settings
        inv.setItem(0, createOptionItem(Material.NAME_TAG, "§eSet Name", "§b(Click to set via chat)"));
        inv.setItem(1, createModelDataItem(session));
        inv.setItem(2, createDamageItem(session));
        inv.setItem(3, createOptionItem(Material.BOOK, "§eItem Model ID", session, GUIManager.KEY_ITEM_MODEL_ID, "None", PersistentDataType.STRING));
        inv.setItem(4, session.getItem());
        inv.setItem(5, createTargetToggleItem(session));
        inv.setItem(6, createOptionItem(Material.PAPER, "§eNo-Permission Message", session, GUIManager.KEY_PERMISSION_MESSAGE, "Default message", PersistentDataType.STRING));
        inv.setItem(7, createOptionItem(Material.PLAYER_HEAD, "§eSet Skull Texture", "§b(Click to set Name or Base64)"));
        inv.setItem(8, createOptionItem(Material.OAK_DOOR, "§cBack", "§7Returns to the GUI editor."));

        // Row 2: Left / Shift+Left
        createClickActionRow(inv, 9, "§aLeft-Click", "§dShift+Left-Click",
                EditSession.EditType.COMMAND_LEFT, EditSession.EditType.MONEY_COST_LEFT, EditSession.EditType.COST_LEFT, EditSession.EditType.KEEP_OPEN_LEFT, EditSession.EditType.COOLDOWN_LEFT, EditSession.EditType.EXECUTOR_LEFT,
                EditSession.EditType.COMMAND_SHIFT_LEFT, EditSession.EditType.MONEY_COST_SHIFT_LEFT, EditSession.EditType.COST_SHIFT_LEFT, EditSession.EditType.KEEP_OPEN_SHIFT_LEFT, EditSession.EditType.COOLDOWN_SHIFT_LEFT, EditSession.EditType.EXECUTOR_SHIFT_LEFT,
                session);

        // Row 3: Right / Shift+Right
        createClickActionRow(inv, 18, "§bRight-Click", "§cShift+Right-Click",
                EditSession.EditType.COMMAND_RIGHT, EditSession.EditType.MONEY_COST_RIGHT, EditSession.EditType.COST_RIGHT, EditSession.EditType.KEEP_OPEN_RIGHT, EditSession.EditType.COOLDOWN_RIGHT, EditSession.EditType.EXECUTOR_RIGHT,
                EditSession.EditType.COMMAND_SHIFT_RIGHT, EditSession.EditType.MONEY_COST_SHIFT_RIGHT, EditSession.EditType.COST_SHIFT_RIGHT, EditSession.EditType.KEEP_OPEN_SHIFT_RIGHT, EditSession.EditType.COOLDOWN_SHIFT_RIGHT, EditSession.EditType.EXECUTOR_SHIFT_RIGHT,
                session);

        // Row 4: F / Shift+F
        createKeybindActionRow(inv, 27, "§eF-Key", "§6Shift+F-Key",
                EditSession.EditType.COMMAND_F, EditSession.EditType.MONEY_COST_F, EditSession.EditType.COST_F, EditSession.EditType.PERMISSION_F, EditSession.EditType.COOLDOWN_F, EditSession.EditType.EXECUTOR_F,
                EditSession.EditType.COMMAND_SHIFT_F, EditSession.EditType.MONEY_COST_SHIFT_F, EditSession.EditType.COST_SHIFT_F, EditSession.EditType.PERMISSION_SHIFT_F, EditSession.EditType.COOLDOWN_SHIFT_F, EditSession.EditType.EXECUTOR_SHIFT_F,
                session);

        // Row 5: Q / Shift+Q
        createKeybindActionRow(inv, 36, "§3Q-Key", "§9Shift+Q-Key",
                EditSession.EditType.COMMAND_Q, EditSession.EditType.MONEY_COST_Q, EditSession.EditType.COST_Q, EditSession.EditType.PERMISSION_Q, EditSession.EditType.COOLDOWN_Q, EditSession.EditType.EXECUTOR_Q,
                EditSession.EditType.COMMAND_SHIFT_Q, EditSession.EditType.MONEY_COST_SHIFT_Q, EditSession.EditType.COST_SHIFT_Q, EditSession.EditType.PERMISSION_SHIFT_Q, EditSession.EditType.COOLDOWN_SHIFT_Q, EditSession.EditType.EXECUTOR_SHIFT_Q,
                session);

        setLoreItemsAndPagination(inv, session);
        player.openInventory(inv);
    }

    private static void createClickActionRow(Inventory inv, int startSlot, String name1, String name2,
                                             EditSession.EditType cmd1, EditSession.EditType moneyEq1, EditSession.EditType itemEq1, EditSession.EditType keepOpen1, EditSession.EditType cd1, EditSession.EditType exec1,
                                             EditSession.EditType cmd2, EditSession.EditType moneyEq2, EditSession.EditType itemEq2, EditSession.EditType keepOpen2, EditSession.EditType cd2, EditSession.EditType exec2,
                                             EditSession s) {
        inv.setItem(startSlot,     createCommandItem(s, name1 + ": Action", ActionKeyUtil.getKeyFromType(cmd1), ActionKeyUtil.getKeyFromType(cd1), ActionKeyUtil.getKeyFromType(exec1)));
        inv.setItem(startSlot + 1, createMoneyCostItem(s, ActionKeyUtil.getKeyFromType(moneyEq1)));
        inv.setItem(startSlot + 2, createItemCostItem(s, ActionKeyUtil.getKeyFromType(itemEq1)));
        inv.setItem(startSlot + 3, createKeepOpenItem(s, ActionKeyUtil.getKeyFromType(keepOpen1)));
        inv.setItem(startSlot + 4, SEPARATOR_PANE.clone());
        inv.setItem(startSlot + 5, createCommandItem(s, name2 + ": Action", ActionKeyUtil.getKeyFromType(cmd2), ActionKeyUtil.getKeyFromType(cd2), ActionKeyUtil.getKeyFromType(exec2)));
        inv.setItem(startSlot + 6, createMoneyCostItem(s, ActionKeyUtil.getKeyFromType(moneyEq2)));
        inv.setItem(startSlot + 7, createItemCostItem(s, ActionKeyUtil.getKeyFromType(itemEq2)));
        inv.setItem(startSlot + 8, createKeepOpenItem(s, ActionKeyUtil.getKeyFromType(keepOpen2)));
    }

    private static void createKeybindActionRow(Inventory inv, int startSlot, String name1, String name2,
                                               EditSession.EditType cmd1, EditSession.EditType moneyEq1, EditSession.EditType itemEq1, EditSession.EditType perm1, EditSession.EditType cd1, EditSession.EditType exec1,
                                               EditSession.EditType cmd2, EditSession.EditType moneyEq2, EditSession.EditType itemEq2, EditSession.EditType perm2, EditSession.EditType cd2, EditSession.EditType exec2,
                                               EditSession s) {
        inv.setItem(startSlot,     createCommandItem(s, name1 + ": Action", ActionKeyUtil.getKeyFromType(cmd1), ActionKeyUtil.getKeyFromType(cd1), ActionKeyUtil.getKeyFromType(exec1)));
        inv.setItem(startSlot + 1, createMoneyCostItem(s, ActionKeyUtil.getKeyFromType(moneyEq1)));
        inv.setItem(startSlot + 2, createItemCostItem(s, ActionKeyUtil.getKeyFromType(itemEq1)));
        inv.setItem(startSlot + 3, createOptionItem(Material.IRON_BARS, "§f" + name1 + ": Permission", s, ActionKeyUtil.getKeyFromType(perm1), "None", PersistentDataType.STRING));
        inv.setItem(startSlot + 4, SEPARATOR_PANE.clone());
        inv.setItem(startSlot + 5, createCommandItem(s, name2 + ": Action", ActionKeyUtil.getKeyFromType(cmd2), ActionKeyUtil.getKeyFromType(cd2), ActionKeyUtil.getKeyFromType(exec2)));
        inv.setItem(startSlot + 6, createMoneyCostItem(s, ActionKeyUtil.getKeyFromType(moneyEq2)));
        inv.setItem(startSlot + 7, createItemCostItem(s, ActionKeyUtil.getKeyFromType(itemEq2)));
        inv.setItem(startSlot + 8, createOptionItem(Material.IRON_BARS, "§f" + name2 + ": Permission", s, ActionKeyUtil.getKeyFromType(perm2), "None", PersistentDataType.STRING));
    }

    private static void setLoreItemsAndPagination(Inventory inv, EditSession session) {
        ItemStack item = session.getItem();
        int page = session.getLorePage();

        inv.setItem(45, createOptionItem(Material.WRITABLE_BOOK, "§6Add Lore Line", "§b(Click to add a new line)"));

        List<String> lore = (item != null && item.hasItemMeta() && Objects.requireNonNull(item.getItemMeta()).hasLore())
                ? new ArrayList<>(Objects.requireNonNull(item.getItemMeta().getLore()))
                : new ArrayList<>();

        int totalPages = Math.max(0, (lore.size() - 1) / LORE_SLOTS_PER_PAGE);

        if (page > totalPages) {
            page = totalPages;
            session.setLorePage(page);
        }

        int startIndex = page * LORE_SLOTS_PER_PAGE;
        for (int i = 0; i < LORE_SLOTS_PER_PAGE; i++) {
            int loreIndex = startIndex + i;
            if (loreIndex < lore.size()) {
                ItemStack loreItem = new ItemStack(Material.PAPER);
                ItemMeta loreMeta = loreItem.getItemMeta();
                Objects.requireNonNull(loreMeta).setDisplayName("§eLore Line " + (loreIndex + 1));
                loreMeta.setLore(Arrays.asList("§7" + lore.get(loreIndex), " ", "§bLeft-Click to Edit", "§cRight-Click to Remove"));
                loreItem.setItemMeta(loreMeta);
                inv.setItem(46 + i, loreItem);
            } else {
                inv.setItem(46 + i, null);
            }
        }

        ItemStack pageButton = new ItemStack(Material.ARROW);
        ItemMeta meta = pageButton.getItemMeta();
        Objects.requireNonNull(meta).setDisplayName("§ePage Navigation");
        List<String> loreList = new ArrayList<>();
        loreList.add("§7Page " + (page + 1) + " of " + (totalPages + 1));
        loreList.add(" ");
        loreList.add("§bLeft-Click for Next Page");
        loreList.add("§dShift-Click for Previous Page");
        meta.setLore(loreList);

        meta.getPersistentDataContainer().set(KEY_PAGE, PersistentDataType.INTEGER, page);

        pageButton.setItemMeta(meta);
        inv.setItem(53, pageButton);
    }

    private static <T, Z> ItemStack createOptionItem(Material material, String name, EditSession session, NamespacedKey key, Z def, PersistentDataType<T, Z> type) {
        PersistentDataContainer pdc = Objects.requireNonNull(session.getItem().getItemMeta()).getPersistentDataContainer();
        Z value = pdc.getOrDefault(key, type, def);
        return createOptionItem(material, name, Arrays.asList("§7Current: §f" + value, "§b(Click to edit)"));
    }

    private static ItemStack createCommandItem(EditSession session, String name, NamespacedKey commandKey, NamespacedKey cooldownKey, NamespacedKey executorKey) {
        ItemMeta meta = session.getItem().getItemMeta();
        if (meta == null) {
            return createOptionItem(Material.BARRIER, "§c" + name, Collections.singletonList("§cError: Missing ItemMeta"));
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String command = pdc.getOrDefault(commandKey, PersistentDataType.STRING, "None");
        double cooldown = pdc.getOrDefault(cooldownKey, PersistentDataType.DOUBLE, 0.0);
        String executorName;

        if (pdc.has(executorKey, PersistentDataType.BYTE)) {
            byte executorOrdinal = pdc.getOrDefault(executorKey, PersistentDataType.BYTE, (byte) 0);
            if (executorOrdinal >= 0 && executorOrdinal < GUIManager.ExecutorType.values().length) {
                executorName = GUIManager.ExecutorType.values()[executorOrdinal].name();
            } else {
                executorName = GUIManager.ExecutorType.PLAYER.name();
            }
            pdc.remove(executorKey);
            pdc.set(executorKey, PersistentDataType.STRING, executorName);
            session.getItem().setItemMeta(meta);
        } else {
            executorName = pdc.getOrDefault(executorKey, PersistentDataType.STRING, GUIManager.ExecutorType.PLAYER.name());
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7Command: §f" + command);
        lore.add("§7Cooldown: §f" + cooldown + "s");
        lore.add("§7Executor: §f" + executorName);
        lore.add(" ");
        lore.add("§bLeft-Click: §fEdit Command");
        lore.add("§cRight-Click: §fEdit Cooldown");
        lore.add("§eShift+Left-Click: §fCycle Executor");

        return createOptionItem(Material.COMMAND_BLOCK, "§f" + name, lore);
    }

    private static ItemStack createKeepOpenItem(EditSession session, NamespacedKey key) {
        PersistentDataContainer pdc = Objects.requireNonNull(session.getItem().getItemMeta()).getPersistentDataContainer();
        boolean enabled = pdc.getOrDefault(key, PersistentDataType.BYTE, (byte) 0) == 1;
        Material material = enabled ? Material.ENDER_EYE : Material.ENDER_PEARL;
        String name = "§eKeep GUI Open";
        List<String> lore = Arrays.asList(
                enabled ? "§7Status: §aEnabled" : "§7Status: §cDisabled",
                "§fKeeps the GUI open after clicking.", " ", "§b(Click to toggle)");
        return createOptionItem(material, name, lore);
    }

    private static ItemStack createOptionItem(Material material, String name, String lore) {
        return createOptionItem(material, name, Collections.singletonList(lore));
    }

    private static ItemStack createItemCostItem(EditSession s, NamespacedKey key) {
        PersistentDataContainer pdc = Objects.requireNonNull(s.getItem().getItemMeta()).getPersistentDataContainer();
        List<String> lore = new ArrayList<>(Arrays.asList("§b(Click to set items)", " "));

        if (pdc.has(key, PersistentDataType.STRING)) {
            String base64 = pdc.get(key, PersistentDataType.STRING);
            if (base64 != null && !base64.isEmpty()) {
                lore.add("§7Current Cost:");
                lore.addAll(ItemSerialization.getCostDisplay(base64));
            } else {
                lore.add("§7Current Cost: §cNone");
            }
        } else {
            lore.add("§7Current Cost: §cNone");
        }
        return createOptionItem(Material.CHEST, "Set Item Cost", lore);
    }

    private static ItemStack createMoneyCostItem(EditSession s, NamespacedKey key) {
        if (GUIManager.econ == null) {
            return createOptionItem(Material.GRAY_DYE, "§cSet Money Cost", Arrays.asList("§7Vault plugin not found.", "§7This feature is disabled."));
        }
        PersistentDataContainer pdc = Objects.requireNonNull(s.getItem().getItemMeta()).getPersistentDataContainer();
        double value = pdc.getOrDefault(key, PersistentDataType.DOUBLE, 0.0);
        return createOptionItem(Material.EMERALD, "Set Money Cost", Arrays.asList("§7Current: §f" + value, "§b(Click to set via chat)"));
    }

    private static ItemStack createTargetToggleItem(EditSession session) {
        PersistentDataContainer pdc = Objects.requireNonNull(session.getItem().getItemMeta()).getPersistentDataContainer();
        boolean enabled = pdc.getOrDefault(GUIManager.KEY_REQUIRE_TARGET, PersistentDataType.BYTE, (byte) 0) == 1;
        return createOptionItem(enabled ? Material.PLAYER_HEAD : Material.SKELETON_SKULL, "§eRequire Target Player", Arrays.asList(
                enabled ? "§7Status: §aEnabled" : "§7Status: §cDisabled", "§fPrompts for a player name", "§ffor {target} placeholder.", " ", "§b(Click to toggle)"));
    }

    private static ItemStack createModelDataItem(EditSession session) {
        int modelData = Objects.requireNonNull(session.getItem().getItemMeta()).getPersistentDataContainer().getOrDefault(GUIManager.KEY_CUSTOM_MODEL_DATA, PersistentDataType.INTEGER, 0);
        return createOptionItem(Material.ITEM_FRAME, "§eCustom Model Data", Arrays.asList("§7Current: §f" + (modelData == 0 ? "None" : modelData), "§b(Click to set)"));
    }

    private static ItemStack createDamageItem(EditSession session) {
        ItemMeta meta = session.getItem().getItemMeta();
        int damage = (meta instanceof Damageable) ? ((Damageable) meta).getDamage() : 0;
        return createOptionItem(Material.ANVIL, "§eItem Damage (Durability)", Arrays.asList("§7Current: §f" + damage, "§b(Click to set)"));
    }

    private static ItemStack createOptionItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}