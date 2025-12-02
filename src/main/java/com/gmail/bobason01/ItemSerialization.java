package com.gmail.bobason01;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ItemSerialization {

    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(items.length);

            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }

            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static ItemStack[] itemStackArrayFromBase64(String data) throws java.io.IOException {
        if (data == null || data.isEmpty()) return new ItemStack[0];

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

            ItemStack[] items = new ItemStack[dataInput.readInt()];

            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new java.io.IOException("Unable to decode class type.", e);
        }
    }

    public static List<String> getCostDisplay(String base64Data) {
        List<String> displayList = new ArrayList<>();

        if (base64Data == null || base64Data.isEmpty()) {
            displayList.add(ChatColor.GRAY + "None");
            return displayList;
        }

        try {
            ItemStack[] items = itemStackArrayFromBase64(base64Data);
            if (items == null || items.length == 0) {
                displayList.add(ChatColor.GRAY + "None");
                return displayList;
            }

            List<ItemStack> consolidated = new ArrayList<>();

            for (ItemStack current : items) {
                if (current == null || current.getType() == Material.AIR) continue;

                boolean merged = false;
                for (ItemStack existing : consolidated) {
                    if (existing.isSimilar(current)) {
                        existing.setAmount(existing.getAmount() + current.getAmount());
                        merged = true;
                        break;
                    }
                }

                if (!merged) {
                    consolidated.add(current.clone());
                }
            }

            if (consolidated.isEmpty()) {
                displayList.add(ChatColor.GRAY + "None");
                return displayList;
            }

            for (ItemStack item : consolidated) {
                String name;
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    name = item.getItemMeta().getDisplayName();
                } else {
                    name = capitalize(item.getType().name().replace("_", " "));
                }
                displayList.add(ChatColor.WHITE + "- " + name + " x" + item.getAmount());
            }

        } catch (Exception e) {
            displayList.add(ChatColor.RED + "Error loading items");
        }

        return displayList;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : str.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextUpper = true;
            } else if (nextUpper) {
                c = Character.toTitleCase(c);
                nextUpper = false;
            } else {
                c = Character.toLowerCase(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }
}