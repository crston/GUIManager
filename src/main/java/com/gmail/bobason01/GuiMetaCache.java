package com.gmail.bobason01;

import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiMetaCache {

    private final Map<String, Map<Integer, GuiItemMeta>> cache = new ConcurrentHashMap<>();

    public void clearAll() {
        cache.clear();
    }

    public void remove(String guiId) {
        cache.remove(guiId.toLowerCase());
    }

    public void rename(String oldId, String newId) {
        Map<Integer, GuiItemMeta> m = cache.remove(oldId.toLowerCase());
        if (m != null) cache.put(newId.toLowerCase(), m);
    }

    public void buildForGui(String guiId, GUI gui, GUIManager plugin) {
        Map<Integer, GuiItemMeta> map = new ConcurrentHashMap<>();
        for (Map.Entry<Integer, ItemStack> e : gui.getItems().entrySet()) {
            map.put(e.getKey(), MetaExtractor.extract(plugin, e.getValue()));
        }
        cache.put(guiId.toLowerCase(), map);
    }

    public GuiItemMeta.Variant getVariant(String guiId, int slot, String key) {
        Map<Integer, GuiItemMeta> m = cache.get(guiId.toLowerCase());
        if (m == null) return null;
        GuiItemMeta gm = m.get(slot);
        if (gm == null) return null;
        return gm.get(key);
    }
}
