package com.gmail.bobason01;

import com.gmail.bobason01.utils.MetaExtractor;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GuiMetaCache {

    private final Map<String, Map<Integer, GuiItemMeta>> cache = new ConcurrentHashMap<>();

    public void buildForGui(String guiId, GUI gui, GUIManager plugin) {
        Map<Integer, GuiItemMeta> metaMap = new ConcurrentHashMap<>();
        for (Map.Entry<Integer, ItemStack> entry : gui.getItems().entrySet()) {
            metaMap.put(entry.getKey(), MetaExtractor.extract(plugin, entry.getValue()));
        }
        cache.put(guiId, metaMap);
    }

    public GuiItemMeta.Variant getVariant(String guiId, int slot, String actionKey) {
        Map<Integer, GuiItemMeta> guiCache = cache.get(guiId);
        if (guiCache == null) return null;
        GuiItemMeta meta = guiCache.get(slot);
        if (meta == null) return null;
        return meta.get(actionKey);
    }

    public void clearAll() {
        cache.clear();
    }
}