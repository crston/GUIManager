package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ItemEditor {

    public static final String TITLE_PREFIX = "§7[Item Editor] §6";
    public static final NamespacedKey KEY_PAGE = new NamespacedKey(GUIManager.getInstance(), "editor_page");

    private static final ItemStack SEPARATOR_PANE;
    static {
        SEPARATOR_PANE = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = SEPARATOR_PANE.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); SEPARATOR_PANE.setItemMeta(meta); }
    }

    public static void open(Player player, EditSession session) {
        String title = TITLE_PREFIX + session.getGuiName() + " Slot " + session.getSlot();
        ItemEditorHolder holder = new ItemEditorHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);
        updateUI(inv, session);
        player.openInventory(inv);
    }

    public static void updateUI(Inventory inv, EditSession session) {
        inv.setItem(0, createOptionItem(Material.NAME_TAG, "§eSet Name", "§bClick to set via chat"));
        inv.setItem(1, createMaterialItem(session));
        inv.setItem(2, createItemFlagsItem(session));
        inv.setItem(3, createItemModelItem(session));
        inv.setItem(4, session.getItem());
        inv.setItem(5, createTargetToggleItem(session));
        inv.setItem(6, createOptionItem(Material.PAPER, "§eNo Permission Message", session, GUIManager.KEY_PERMISSION_MESSAGE, "Default", PersistentDataType.STRING));
        inv.setItem(7, createOptionItem(Material.PLAYER_HEAD, "§eSkull Texture", "§bName or Base64"));
        inv.setItem(8, createOptionItem(Material.OAK_DOOR, "§cBack", "§7Return to GUI"));

        createActionRow(inv, 9, "§aLeft", "§dS-Left", session,
                GUIManager.KEY_COMMAND_LEFT, GUIManager.KEY_COOLDOWN_LEFT, GUIManager.KEY_EXECUTOR_LEFT, GUIManager.KEY_MONEY_COST_LEFT, GUIManager.KEY_COST_LEFT, GUIManager.KEY_REWARD_LEFT, GUIManager.KEY_PERMISSION_LEFT,
                GUIManager.KEY_COMMAND_SHIFT_LEFT, GUIManager.KEY_COOLDOWN_SHIFT_LEFT, GUIManager.KEY_EXECUTOR_SHIFT_LEFT, GUIManager.KEY_MONEY_COST_SHIFT_LEFT, GUIManager.KEY_COST_SHIFT_LEFT, GUIManager.KEY_REWARD_SHIFT_LEFT, GUIManager.KEY_PERMISSION_SHIFT_LEFT,
                GUIManager.KEY_KEEP_OPEN_LEFT, GUIManager.KEY_KEEP_OPEN_SHIFT_LEFT);

        createActionRow(inv, 18, "§bRight", "§cS-Right", session,
                GUIManager.KEY_COMMAND_RIGHT, GUIManager.KEY_COOLDOWN_RIGHT, GUIManager.KEY_EXECUTOR_RIGHT, GUIManager.KEY_MONEY_COST_RIGHT, GUIManager.KEY_COST_RIGHT, GUIManager.KEY_REWARD_RIGHT, GUIManager.KEY_PERMISSION_RIGHT,
                GUIManager.KEY_COMMAND_SHIFT_RIGHT, GUIManager.KEY_COOLDOWN_SHIFT_RIGHT, GUIManager.KEY_EXECUTOR_SHIFT_RIGHT, GUIManager.KEY_MONEY_COST_SHIFT_RIGHT, GUIManager.KEY_COST_SHIFT_RIGHT, GUIManager.KEY_REWARD_SHIFT_RIGHT, GUIManager.KEY_PERMISSION_SHIFT_RIGHT,
                GUIManager.KEY_KEEP_OPEN_RIGHT, GUIManager.KEY_KEEP_OPEN_SHIFT_RIGHT);

        createActionRow(inv, 27, "§eF Key", "§6S-F Key", session,
                GUIManager.KEY_COMMAND_F, GUIManager.KEY_COOLDOWN_F, GUIManager.KEY_EXECUTOR_F, GUIManager.KEY_MONEY_COST_F, GUIManager.KEY_COST_F, GUIManager.KEY_REWARD_F, GUIManager.KEY_PERMISSION_F,
                GUIManager.KEY_COMMAND_SHIFT_F, GUIManager.KEY_COOLDOWN_SHIFT_F, GUIManager.KEY_EXECUTOR_SHIFT_F, GUIManager.KEY_MONEY_COST_SHIFT_F, GUIManager.KEY_COST_SHIFT_F, GUIManager.KEY_REWARD_SHIFT_F, GUIManager.KEY_PERMISSION_SHIFT_F,
                GUIManager.KEY_KEEP_OPEN_F, GUIManager.KEY_KEEP_OPEN_SHIFT_F);

        createActionRow(inv, 36, "§3Q Key", "§9S-Q Key", session,
                GUIManager.KEY_COMMAND_Q, GUIManager.KEY_COOLDOWN_Q, GUIManager.KEY_EXECUTOR_Q, GUIManager.KEY_MONEY_COST_Q, GUIManager.KEY_COST_Q, GUIManager.KEY_REWARD_Q, GUIManager.KEY_PERMISSION_Q,
                GUIManager.KEY_COMMAND_SHIFT_Q, GUIManager.KEY_COOLDOWN_SHIFT_Q, GUIManager.KEY_EXECUTOR_SHIFT_Q, GUIManager.KEY_MONEY_COST_SHIFT_Q, GUIManager.KEY_COST_SHIFT_Q, GUIManager.KEY_REWARD_SHIFT_Q, GUIManager.KEY_PERMISSION_SHIFT_Q,
                GUIManager.KEY_KEEP_OPEN_Q, GUIManager.KEY_KEEP_OPEN_SHIFT_Q);

        setLoreItems(inv, session);
    }

    private static void createActionRow(Inventory inv, int start, String n1, String n2, EditSession s,
                                        NamespacedKey c1, NamespacedKey cd1, NamespacedKey e1, NamespacedKey m1, NamespacedKey i1, NamespacedKey r1, NamespacedKey p1,
                                        NamespacedKey c2, NamespacedKey cd2, NamespacedKey e2, NamespacedKey m2, NamespacedKey i2, NamespacedKey r2, NamespacedKey p2,
                                        NamespacedKey k1, NamespacedKey k2) {
        inv.setItem(start, createCommandItem(s, "§f" + n1, c1, cd1, e1));
        inv.setItem(start + 1, createMoneyCostItem(s, m1));
        inv.setItem(start + 2, createItemCostItem(s, i1, r1));
        if (k1 != null) inv.setItem(start + 4, createKeepGuiButton(s, k1, k2));
        else inv.setItem(start + 4, SEPARATOR_PANE.clone());
        inv.setItem(start + 5, createCommandItem(s, "§f" + n2, c2, cd2, e2));
        inv.setItem(start + 6, createMoneyCostItem(s, m2));
        inv.setItem(start + 7, createItemCostItem(s, i2, r2));
        inv.setItem(start + 3, createOptionItem(Material.IRON_BARS, "§f" + n1 + " Permission", s, p1, "None", PersistentDataType.STRING));
        inv.setItem(start + 8, createOptionItem(Material.IRON_BARS, "§f" + n2 + " Permission", s, p2, "None", PersistentDataType.STRING));
    }

    private static int getLegacyOrInt(PersistentDataContainer pdc, NamespacedKey key) {
        if (pdc.has(key, PersistentDataType.INTEGER)) return pdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
        else if (pdc.has(key, PersistentDataType.BYTE)) return pdc.getOrDefault(key, PersistentDataType.BYTE, (byte) 0);
        return 0;
    }

    private static ItemStack createKeepGuiButton(EditSession s, NamespacedKey k1, NamespacedKey k2) {
        PersistentDataContainer pdc = s.getItem().getItemMeta().getPersistentDataContainer();
        boolean b1 = getLegacyOrInt(pdc, k1) == 1;
        boolean b2 = getLegacyOrInt(pdc, k2) == 1;
        List<String> lore = Arrays.asList(b1 ? "§aNormal: ON" : "§cNormal: OFF", b2 ? "§aShift: ON" : "§cShift: OFF", " ", "§bLeft Click Toggle Normal", "§dRight Click Toggle Shift");
        return createOptionItem(Material.ENDER_EYE, "§eKeep GUI Open", lore);
    }

    private static ItemStack createCommandItem(EditSession s, String n, NamespacedKey ck, NamespacedKey cdk, NamespacedKey ek) {
        PersistentDataContainer pdc = s.getItem().getItemMeta().getPersistentDataContainer();
        String cmds = pdc.getOrDefault(ck, PersistentDataType.STRING, "None");
        double cd = pdc.getOrDefault(cdk, PersistentDataType.DOUBLE, 0.0);
        String exec = pdc.getOrDefault(ek, PersistentDataType.STRING, "PLAYER");
        List<String> lore = new ArrayList<>();
        lore.add("§7Cooldown " + cd);
        lore.add("§7Executor " + exec);
        lore.add(" ");
        lore.add("§7Commands List");
        if (!cmds.equals("None") && !cmds.isEmpty()) {
            String[] arr = cmds.split(";;");
            for (int i = 0; i < arr.length; i++) lore.add("§f" + (i + 1) + ". " + arr[i]);
        } else lore.add("§fNone");
        lore.add(" ");
        lore.add("§bLeft Click Add Cmd");
        lore.add("§cRight Click Remove Last Cmd");
        lore.add("§eShift-Left Cycle Executor");
        lore.add("§dShift-Right Edit Cooldown");
        return createOptionItem(Material.COMMAND_BLOCK, n, lore);
    }

    private static ItemStack createMoneyCostItem(EditSession s, NamespacedKey k) {
        double v = s.getItem().getItemMeta().getPersistentDataContainer().getOrDefault(k, PersistentDataType.DOUBLE, 0.0);
        return createOptionItem(Material.EMERALD, "§eMoney Cost", "§7" + v);
    }

    private static ItemStack createItemCostItem(EditSession s, NamespacedKey costKey, NamespacedKey rewardKey) {
        PersistentDataContainer pdc = s.getItem().getItemMeta().getPersistentDataContainer();
        boolean hasCost = pdc.has(costKey, PersistentDataType.STRING);
        boolean hasReward = pdc.has(rewardKey, PersistentDataType.STRING);
        List<String> lore = new ArrayList<>();
        lore.add(hasCost ? "§aCost: Configured" : "§cCost: None");
        lore.add(hasReward ? "§aReward: Configured" : "§cReward: None");
        lore.add(" ");
        lore.add("§bLeft Click Set Cost");
        lore.add("§dRight Click Set Reward");
        lore.add("§eShift-Left Clear Cost");
        lore.add("§cShift-Right Clear Reward");
        return createOptionItem(Material.CHEST, "§eItem Cost & Reward", lore);
    }

    private static ItemStack createTargetToggleItem(EditSession s) {
        int b = getLegacyOrInt(s.getItem().getItemMeta().getPersistentDataContainer(), GUIManager.KEY_REQUIRE_TARGET);
        return createOptionItem(b == 1 ? Material.PLAYER_HEAD : Material.SKELETON_SKULL, "§eTarget", b == 1 ? "§aON" : "§cOFF");
    }

    private static ItemStack createMaterialItem(EditSession s) {
        String matName = s.getItem().getType().name();
        return createOptionItem(Material.GRASS_BLOCK, "§eMaterial", "§7" + matName);
    }

    private static ItemStack createItemFlagsItem(EditSession s) {
        ItemMeta meta = s.getItem().getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add("§7Flags");
        if (meta != null && !meta.getItemFlags().isEmpty()) {
            for (ItemFlag flag : meta.getItemFlags()) lore.add("§f- " + flag.name());
        } else lore.add("§fNone");
        lore.add(" ");
        lore.add("§bLeft Click Add Flag");
        lore.add("§cRight Click Remove Flag");
        return createOptionItem(Material.ITEM_FRAME, "§eItem Flags", lore);
    }

    private static ItemStack createItemModelItem(EditSession session) {
        ItemMeta meta = session.getItem().getItemMeta();
        String val = "None";
        if (meta != null) {
            if (meta.hasCustomModelData()) val = "CMD " + meta.getCustomModelData();
            else if (meta.getPersistentDataContainer().has(GUIManager.KEY_ITEM_MODEL, PersistentDataType.STRING)) val = "Model " + meta.getPersistentDataContainer().get(GUIManager.KEY_ITEM_MODEL, PersistentDataType.STRING);
        }
        List<String> lore = Arrays.asList("§7" + val, " ", "§bType number for CMD", "§dType text for Model ID");
        return createOptionItem(Material.PAINTING, "§eModel or CMD", lore);
    }

    private static void setLoreItems(Inventory inv, EditSession s) {
        int page = s.getLorePage();
        inv.setItem(45, createOptionItem(Material.WRITABLE_BOOK, "§6Add Lore", "§bClick to add"));
        List<String> lore = s.getItem().getItemMeta().hasLore() ? s.getItem().getItemMeta().getLore() : new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            int idx = (page * 7) + i;
            if (idx < lore.size()) {
                inv.setItem(46 + i, createOptionItem(Material.PAPER, "§eLore " + (idx + 1), Arrays.asList("§7" + lore.get(idx), "§bLeft Edit", "§cRight Remove")));
            } else inv.setItem(46 + i, null);
        }
        ItemStack a = new ItemStack(Material.ARROW);
        ItemMeta m = a.getItemMeta();
        m.setDisplayName("§eLore Page Control");
        m.setLore(Arrays.asList("§7Current Page: " + (page + 1), " ", "§bLeft Click: Next Page", "§dRight Click: Previous Page"));
        m.getPersistentDataContainer().set(KEY_PAGE, PersistentDataType.INTEGER, page);
        a.setItemMeta(m);
        inv.setItem(53, a);
    }

    private static ItemStack createOptionItem(Material m, String n, String l) { return createOptionItem(m, n, Collections.singletonList(l)); }
    private static <T, Z> ItemStack createOptionItem(Material m, String n, EditSession s, NamespacedKey k, Z d, PersistentDataType<T, Z> t) {
        Z v = s.getItem().getItemMeta().getPersistentDataContainer().getOrDefault(k, t, d);
        return createOptionItem(m, n, "§7" + v);
    }
    private static ItemStack createOptionItem(Material m, String n, List<String> l) {
        ItemStack i = new ItemStack(m); ItemMeta mt = i.getItemMeta();
        if (mt != null) { mt.setDisplayName(n); mt.setLore(l); mt.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); i.setItemMeta(mt); }
        return i;
    }
}