package com.gmail.bobason01;

import org.bukkit.Material;
import java.util.HashMap;
import java.util.Map;

public final class GuiItemMeta {

    public static final String LEFT = "LEFT";
    public static final String SHIFT_LEFT = "SHIFT_LEFT";
    public static final String RIGHT = "RIGHT";
    public static final String SHIFT_RIGHT = "SHIFT_RIGHT";
    public static final String F = "F";
    public static final String SHIFT_F = "SHIFT_F";
    public static final String Q = "Q";
    public static final String SHIFT_Q = "SHIFT_Q";

    private final Map<String, Variant> variants = new HashMap<>();
    private Material materialSnapshot;

    public GuiItemMeta() {}

    public void setMaterialSnapshot(Material m) { this.materialSnapshot = m; }
    public Material getMaterialSnapshot() { return materialSnapshot; }

    public Variant getOrCreate(String key) {
        return variants.computeIfAbsent(key, k -> new Variant());
    }

    public Variant get(String key) {
        return variants.get(key);
    }

    public static final class Variant {
        public String command;
        public String permission;
        public String itemCostBase64;
        public double moneyCost;
        public double cooldownSeconds;
        public GUIManager.ExecutorType executor;
        public boolean keepOpen;
        public boolean requireTarget;

        public Variant() {}
    }
}
