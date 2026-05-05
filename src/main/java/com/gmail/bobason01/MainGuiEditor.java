package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MainGuiEditor {

    public static void open(Player player, GUI gui) {
        MainGuiEditorHolder holder = new MainGuiEditorHolder(gui.getId());
        Inventory inv = Bukkit.createInventory(holder, 45, "§8[GUI Settings] §6" + gui.getId());
        holder.setInventory(inv);

        inv.setItem(11, createOption(Material.NAME_TAG, "§eEdit Title", "§7Current " + gui.getTitle(), "§bClick to edit via chat"));

        inv.setItem(13, createOption(Material.CHEST, "§eEdit Rows", "§7Current " + (gui.getSize() / 9), "§bLeft Click +1 Row", "§cRight Click -1 Row"));

        String p = gui.getPermission() == null || gui.getPermission().isEmpty() ? "None" : gui.getPermission();
        inv.setItem(15, createOption(Material.IRON_BARS, "§eEdit Permission", "§7Current " + p, "§bClick to edit via chat"));

        String t = gui.getTargets() == null || gui.getTargets().isEmpty() ? "None" : gui.getTargets();
        inv.setItem(29, createOption(Material.PLAYER_HEAD, "§eEdit Targets", "§7Current " + t, "§bClick to edit via chat"));

        inv.setItem(33, createOption(Material.DIAMOND_BLOCK, "§aStart GUI Edit", "§7Click to open layout editor"));

        player.openInventory(inv);
    }

    private static ItemStack createOption(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
}