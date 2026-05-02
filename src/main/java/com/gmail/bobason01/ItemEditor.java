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

public final class ItemEditor {

    public static final String TITLE_PREFIX = "§7[Item Editor] §6";
    public static final String COST_TITLE_PREFIX = "§7[Item Cost Editor] §e";
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

        inv.setItem(0, createOptionItem(Material.NAME_TAG, "§eSet Name", "§bClick to set via chat"));
        inv.setItem(1, createMaterialItem(session));
        inv.setItem(2, createDamageItem(session));
        inv.setItem(3, createItemModelItem(session));
        inv.setItem(4, session.getItem());
        inv.setItem(5, createTargetToggleItem(session));
        inv.setItem(6, createOptionItem(Material.PAPER, "§eNo Permission Message", session, GUIManager.KEY_PERMISSION_MESSAGE, "Default", PersistentDataType.STRING));
        inv.setItem(7, createOptionItem(Material.PLAYER_HEAD, "§eSkull Texture", "§bName or Base64"));
        inv.setItem(8, createOptionItem(Material.OAK_DOOR, "§cBack", "§7Return to GUI"));

        createActionRow(inv, 9, "§aLeft", "§dS-Left", session,
                GUIManager.KEY_COMMAND_LEFT, GUIManager.KEY_COOLDOWN_LEFT, GUIManager.KEY_EXECUTOR_LEFT, GUIManager.KEY_MONEY_COST_LEFT, GUIManager.KEY_COST_LEFT, GUIManager.KEY_PERMISSION_LEFT,
                GUIManager.KEY_COMMAND_SHIFT_LEFT, GUIManager.KEY_COOLDOWN_SHIFT_LEFT, GUIManager.KEY_EXECUTOR_SHIFT_LEFT, GUIManager.KEY_MONEY_COST_SHIFT_LEFT, GUIManager.KEY_COST_SHIFT_LEFT, GUIManager.KEY_PERMISSION_SHIFT_LEFT,
                GUIManager.KEY_KEEP_OPEN_LEFT, GUIManager.KEY_KEEP_OPEN_SHIFT_LEFT);

        createActionRow(inv, 18, "§bRight", "§cS-Right", session,
                GUIManager.KEY_COMMAND_RIGHT, GUIManager.KEY_COOLDOWN_RIGHT, GUIManager.KEY_EXECUTOR_RIGHT, GUIManager.KEY_MONEY_COST_RIGHT, GUIManager.KEY_COST_RIGHT, GUIManager.KEY_PERMISSION_RIGHT,
                GUIManager.KEY_COMMAND_SHIFT_RIGHT, GUIManager.KEY_COOLDOWN_SHIFT_RIGHT, GUIManager.KEY_EXECUTOR_SHIFT_RIGHT, GUIManager.KEY_MONEY_COST_SHIFT_RIGHT, GUIManager.KEY_COST_SHIFT_RIGHT, GUIManager.KEY_PERMISSION_SHIFT_RIGHT,
                GUIManager.KEY_KEEP_OPEN_RIGHT, GUIManager.KEY_KEEP_OPEN_SHIFT_RIGHT);

        createActionRow(inv, 27, "§eF Key", "§6S-F Key", session,
                GUIManager.KEY_COMMAND_F, GUIManager.KEY_COOLDOWN_F, GUIManager.KEY_EXECUTOR_F, GUIManager.KEY_MONEY_COST_F, GUIManager.KEY_COST_F, GUIManager.KEY_PERMISSION_F,
                GUIManager.KEY_COMMAND_SHIFT_F, GUIManager.KEY_COOLDOWN_SHIFT_F, GUIManager.KEY_EXECUTOR_SHIFT_F, GUIManager.KEY_MONEY_COST_SHIFT_F, GUIManager.KEY_COST_SHIFT_F, GUIManager.KEY_PERMISSION_SHIFT_F,
                GUIManager.KEY_KEEP_OPEN_F, GUIManager.KEY_KEEP_OPEN_SHIFT_F);

        createActionRow(inv, 36, "§3Q Key", "§9S-Q Key", session,
                GUIManager.KEY_COMMAND_Q, GUIManager.KEY_COOLDOWN_Q, GUIManager.KEY_EXECUTOR_Q, GUIManager.KEY_MONEY_COST_Q, GUIManager.KEY_COST_Q, GUIManager.KEY_PERMISSION_Q,
                GUIManager.KEY_COMMAND_SHIFT_Q, GUIManager.KEY_COOLDOWN_SHIFT_Q, GUIManager.KEY_EXECUTOR_SHIFT_Q, GUIManager.KEY_MONEY_COST_SHIFT_Q, GUIManager.KEY_COST_SHIFT_Q, GUIManager.KEY_PERMISSION_SHIFT_Q,
                GUIManager.KEY_KEEP_OPEN_Q, GUIManager.KEY_KEEP_OPEN_SHIFT_Q);

        setLoreItems(inv, session);
        player.openInventory(inv);
    }

    private static void createActionRow(Inventory inv, int start, String n1, String n2, EditSession s,
                                        NamespacedKey c1, NamespacedKey cd1, NamespacedKey e1, NamespacedKey m1, NamespacedKey i1, NamespacedKey p1,
                                        NamespacedKey c2, NamespacedKey cd2, NamespacedKey e2, NamespacedKey m2, NamespacedKey i2, NamespacedKey p2,
                                        NamespacedKey k1, NamespacedKey k2) {
        inv.setItem(start, createCommandItem(s, "§f" + n1, c1, cd1, e1));
        inv.setItem(start + 1, createMoneyCostItem(s, m1));
        inv.setItem(start + 2, createItemCostItem(s, i1));

        if (k1 != null) {
            inv.setItem(start + 4, createKeepGuiButton(s, k1, k2));
        } else {
            inv.setItem(start + 4, SEPARATOR_PANE.clone());
        }

        inv.setItem(start + 5, createCommandItem(s, "§f" + n2, c2, cd2, e2));
        inv.setItem(start + 6, createMoneyCostItem(s, m2));
        inv.setItem(start + 7, createItemCostItem(s, i2));

        inv.setItem(start + 3, createOptionItem(Material.IRON_BARS, "§f" + n1 + " Perm", s, p1, "None", PersistentDataType.STRING));
        inv.setItem(start + 8, createOptionItem(Material.IRON_BARS, "§f" + n2 + " Perm", s, p2, "None", PersistentDataType.STRING));
    }

    private static ItemStack createKeepGuiButton(EditSession s, NamespacedKey k1, NamespacedKey k2) {
        PersistentDataContainer pdc = s.getItem().getItemMeta().getPersistentDataContainer();
        boolean b1 = pdc.getOrDefault(k1, PersistentDataType.BYTE, (byte) 0) == 1;
        boolean b2 = pdc.getOrDefault(k2, PersistentDataType.BYTE, (byte) 0) == 1;

        List<String> lore = Arrays.asList(
                b1 ? "§aNormal: ON" : "§cNormal: OFF",
                b2 ? "§aShift: ON" : "§cShift: OFF",
                " ", "§bLeft Click Toggle Normal", "§dRight Click Toggle Shift"
        );
        return createOptionItem(Material.ENDER_EYE, "§eKeep GUI Open", lore);
    }

    private static ItemStack createCommandItem(EditSession s, String n, NamespacedKey ck, NamespacedKey cdk, NamespacedKey ek) {
        PersistentDataContainer pdc = s.getItem().getItemMeta().getPersistentDataContainer();
        List<String> lore = Arrays.asList(
                "§7Cmd " + pdc.getOrDefault(ck, PersistentDataType.STRING, "None"),
                "§7CD " + pdc.getOrDefault(cdk, PersistentDataType.DOUBLE, 0.0),
                "§7Exec " + pdc.getOrDefault(ek, PersistentDataType.STRING, "PLAYER"),
                " ", "§bLeft Edit Cmd", "§cRight Edit CD", "§eS-Left Cycle Exec"
        );
        return createOptionItem(Material.COMMAND_BLOCK, n, lore);
    }

    private static ItemStack createMoneyCostItem(EditSession s, NamespacedKey k) {
        double v = s.getItem().getItemMeta().getPersistentDataContainer().getOrDefault(k, PersistentDataType.DOUBLE, 0.0);
        return createOptionItem(Material.EMERALD, "§eMoney Cost", "§7Cur " + v);
    }

    private static ItemStack createItemCostItem(EditSession s, NamespacedKey k) {
        return createOptionItem(Material.CHEST, "§eItem Cost", "§bClick to set");
    }

    private static ItemStack createTargetToggleItem(EditSession s) {
        byte b = s.getItem().getItemMeta().getPersistentDataContainer().getOrDefault(GUIManager.KEY_REQUIRE_TARGET, PersistentDataType.BYTE, (byte) 0);
        return createOptionItem(b == 1 ? Material.PLAYER_HEAD : Material.SKELETON_SKULL, "§eTarget", b == 1 ? "§aON" : "§cOFF");
    }

    private static ItemStack createMaterialItem(EditSession s) {
        String matName = s.getItem().getType().name();
        return createOptionItem(Material.GRASS_BLOCK, "§eMaterial", "§7Cur " + matName);
    }

    private static ItemStack createDamageItem(EditSession s) {
        int v = ((Damageable) s.getItem().getItemMeta()).getDamage();
        return createOptionItem(Material.ANVIL, "§eDamage", "§7Cur " + v);
    }

    private static ItemStack createItemModelItem(EditSession session) {
        ItemMeta meta = session.getItem().getItemMeta();
        String val = "None";
        if (meta != null) {
            if (meta.hasCustomModelData()) {
                val = "CMD " + meta.getCustomModelData();
            } else if (meta.getPersistentDataContainer().has(GUIManager.KEY_ITEM_MODEL, PersistentDataType.STRING)) {
                val = "Model " + meta.getPersistentDataContainer().get(GUIManager.KEY_ITEM_MODEL, PersistentDataType.STRING);
            }
        }
        List<String> lore = Arrays.asList(
                "§7Cur " + val,
                " ",
                "§bType number for CMD",
                "§dType text for Model ID"
        );
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
            }
        }
        ItemStack a = new ItemStack(Material.ARROW);
        ItemMeta m = a.getItemMeta();
        m.setDisplayName("§ePage " + (page + 1));
        m.getPersistentDataContainer().set(KEY_PAGE, PersistentDataType.INTEGER, page);
        a.setItemMeta(m);
        inv.setItem(53, a);
    }

    private static ItemStack createOptionItem(Material m, String n, String l) { return createOptionItem(m, n, Collections.singletonList(l)); }
    private static <T, Z> ItemStack createOptionItem(Material m, String n, EditSession s, NamespacedKey k, Z d, PersistentDataType<T, Z> t) {
        Z v = s.getItem().getItemMeta().getPersistentDataContainer().getOrDefault(k, t, d);
        return createOptionItem(m, n, "§7Cur " + v);
    }
    private static ItemStack createOptionItem(Material m, String n, List<String> l) {
        ItemStack i = new ItemStack(m); ItemMeta mt = i.getItemMeta();
        if (mt != null) { mt.setDisplayName(n); mt.setLore(l); mt.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); i.setItemMeta(mt); }
        return i;
    }
}