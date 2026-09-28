package com.mushokumagic.mana;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.class_1309;
import net.minecraft.class_2487;

/**
 * Player magic state persisted through Forge's entity persistent-data tag.
 * The same Forge entity extension is available in both Forge and NeoForge 1.20.1.
 */
public final class ManaData {
    private static final String PERSISTED_KEY = "mushoku_magic_mana";
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<class_1309, ManaData> CACHE = new WeakHashMap<>();

    private transient class_2487 persistentData;
    private double mana;
    private double maxMana;
    private double xp;
    private int overloadTicks;
    private String lastSpell;
    private final Map<String, Integer> attempts;
    private final List<String> learned;
    private boolean adminMode;

    public ManaData() {
        this(0.0, 40.0, 0.0, 0, "", Map.of(), List.of(), false);
    }

    public ManaData(double mana, double maxMana, double xp, int overloadTicks, String lastSpell,
                    Map<String, Integer> attempts, List<String> learned, boolean adminMode) {
        this.mana = mana;
        this.maxMana = maxMana <= 0.0 ? 40.0 : maxMana;
        this.xp = xp;
        this.overloadTicks = Math.max(0, overloadTicks);
        this.lastSpell = lastSpell == null ? "" : lastSpell;
        this.attempts = new HashMap<>(attempts == null ? Map.of() : attempts);
        this.learned = new ArrayList<>(learned == null ? List.of() : learned);
        this.adminMode = adminMode;
    }

    public static synchronized void init() {
        CACHE.clear();
    }

    public static synchronized ManaData of(class_1309 entity) {
        ManaData cached = CACHE.get(entity);
        if (cached != null) {
            return cached;
        }

        class_2487 persistent = entity.getPersistentData();
        String json = persistent.method_10558(PERSISTED_KEY);
        ManaData data = null;
        if (!json.isEmpty()) {
            try {
                SavedState saved = GSON.fromJson(json, SavedState.class);
                if (saved != null) {
                    data = saved.toManaData();
                }
            } catch (RuntimeException ignored) {
                // A damaged or older custom-data tag should not prevent the player joining.
            }
        }
        if (data == null) {
            data = new ManaData();
        }
        data.persistentData = persistent;
        CACHE.put(entity, data);
        return data;
    }

    public static synchronized void copyTo(class_1309 original, class_1309 replacement) {
        String saved = original.getPersistentData().method_10558(PERSISTED_KEY);
        if (!saved.isEmpty()) {
            replacement.getPersistentData().method_10582(PERSISTED_KEY, saved);
        }
        CACHE.remove(replacement);
    }

    public void markDirty() {
        if (persistentData == null) {
            return;
        }
        SavedState saved = new SavedState(this);
        persistentData.method_10582(PERSISTED_KEY, GSON.toJson(saved));
    }

    public double getMana() {
        return mana;
    }

    public void setMana(double value) {
        mana = Math.max(0.0, Math.min(value, maxMana));
        markDirty();
    }

    public double getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(double value) {
        maxMana = Math.max(1.0, value);
        markDirty();
    }

    public void addMaxMana(double delta) {
        maxMana = Math.max(1.0, maxMana + delta);
        markDirty();
    }

    public double getXp() {
        return xp;
    }

    public void setXp(double value) {
        xp = Math.max(0.0, value);
        markDirty();
    }

    public void addXp(double value) {
        xp = Math.max(0.0, xp + value);
        markDirty();
    }

    public int getOverloadTicks() {
        return overloadTicks;
    }

    public void setOverloadTicks(int ticks) {
        overloadTicks = Math.max(0, ticks);
        markDirty();
    }

    public boolean isOverloaded() {
        return overloadTicks > 0;
    }

    public String getLastSpell() {
        return lastSpell;
    }

    public void setLastSpell(String spellId) {
        lastSpell = spellId == null ? "" : spellId;
        markDirty();
    }

    public Map<String, Integer> getAttempts() {
        return attempts;
    }

    public int getAttempts(String spellId) {
        return attempts.getOrDefault(spellId, 0);
    }

    public void addAttempt(String spellId) {
        attempts.put(spellId, getAttempts(spellId) + 1);
        markDirty();
    }

    public List<String> getLearned() {
        return learned;
    }

    public boolean hasLearned(String spellId) {
        return adminMode || learned.contains(spellId);
    }

    public boolean isAdminMode() {
        return adminMode;
    }

    public void setAdminMode(boolean value) {
        adminMode = value;
        markDirty();
    }

    public void learn(String spellId) {
        if (!learned.contains(spellId)) {
            learned.add(spellId);
            markDirty();
        }
    }

    private static final class SavedState {
        private double mana;
        private double maxMana = 40.0;
        private double xp;
        private int overloadTicks;
        private String lastSpell = "";
        private Map<String, Integer> attempts = new HashMap<>();
        private List<String> learned = new ArrayList<>();
        private boolean adminMode;

        private SavedState() {
        }

        private SavedState(ManaData data) {
            mana = data.mana;
            maxMana = data.maxMana;
            xp = data.xp;
            overloadTicks = data.overloadTicks;
            lastSpell = data.lastSpell;
            attempts = new HashMap<>(data.attempts);
            learned = new ArrayList<>(data.learned);
            adminMode = data.adminMode;
        }

        private ManaData toManaData() {
            return new ManaData(mana, maxMana, xp, overloadTicks, lastSpell, attempts, learned, adminMode);
        }
    }
}
