/*
 * Decompiled with CFR.
 */
package com.mushokumagic.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mushokumagic.MushokuMagic;
import com.mushokumagic.spell.SpellRegistry;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

public final class MagicConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static MagicConfig instance = new MagicConfig();
    public double startMaxMana = 40.0;
    public double regenAmount = 5.0;
    public int regenIntervalSeconds = 10;
    public double overloadMaxBonus = 10.0;
    public int overloadSeconds = 180;
    public int guaranteedAfterAttempts = 6;
    public double failChance = 0.79;
    public double killXp = 1.0;
    public double bossMaxManaBonus = 30.0;
    public double learnMaxManaBonus = 15.0;
    public double xpThresholdBase = 100.0;
    public double xpThresholdPerMaxMana = 2.0;
    public List<String> bosses = new ArrayList((Collection)List.of("minecraft:ender_dragon", "minecraft:wither", "minecraft:warden"));
    public double wordBonusPerWord = 0.1;
    public double maxWordBonus = 1.0;
    public Map<String, KeywordDef> keywords = MagicConfig.defaultKeywords();
    public List<RankDef> ranks = MagicConfig.defaultRanks();
    public Map<String, Double> wandMultipliers = MagicConfig.defaultWandMultipliers();
    public boolean wandRightClickCasts = true;
    public double wandRightClickCooldownSeconds = 2.0;
    /** Allow fire spells to ignite or destroy blocks; disable for a safer server. */
    public boolean fireSpellsModifyBlocks = true;
    public boolean announceOnJoin = true;
    /** One-time migration marker for the anime spell pack. */
    public int spellPackVersion = 0;
    public List<SpellDef> spells = MagicConfig.defaultSpells();

    private static Map<String, Double> defaultWandMultipliers() {
        LinkedHashMap<String, Double> multipliers = new LinkedHashMap<>();
        multipliers.put("1", 2.0);
        multipliers.put("2", 15.0);
        multipliers.put("3", 50.0);
        return multipliers;
    }

    private static Map<String, KeywordDef> defaultKeywords() {
        LinkedHashMap map = new LinkedHashMap();
        map.put("explosion", new KeywordDef("explosion", (List<String>)List.of("\u0432\u0437\u0440\u044b\u0432", "\u0432\u0437\u0440\u044b\u0432\u043d\u043e\u0439", "\u0432\u0437\u043e\u0440\u0432\u0438\u0441\u044c", "explosion", "explosive"), 1.5, 1.25, 1.0, true, false));
        map.put("fast", new KeywordDef("fast", (List<String>)List.of("\u0431\u044b\u0441\u0442\u0440\u044b\u0439", "\u0431\u044b\u0441\u0442\u0440\u043e", "\u0443\u0441\u043a\u043e\u0440\u044c", "fast", "quick"), 1.0, 1.0, 0.5, false, false));
        map.put("silent", new KeywordDef("silent", (List<String>)List.of("\u0442\u0438\u0445\u0438\u0439", "\u0442\u0438\u0445\u043e", "\u0431\u0435\u0437\u0437\u0432\u0443\u0447\u043d\u043e", "silent", "quiet"), 1.0, 1.0, 1.0, false, true));
        return map;
    }

    private static List<RankDef> defaultRanks() {
        return new ArrayList((Collection)List.of(new RankDef("beginner", 40.0, 209.0, 1.0), new RankDef("average", 210.0, 329.0, 1.1), new RankDef("advanced", 330.0, 499.0, 1.2), new RankDef("saint", 500.0, 739.0, 1.35), new RankDef("royal", 740.0, 989.0, 1.5), new RankDef("imperial", 990.0, 1499.0, 1.7), new RankDef("divine", 1500.0, -1.0, 2.0)));
    }

    private static void addAnimeSpellAliases(SpellDef spell) {
        if (spell == null || spell.id == null) {
            return;
        }
        switch (spell.id) {
            case "explosive_fireball" -> MagicConfig.addPhrases(spell, "exodus flame", "nuclear explosion");
            case "ice_needle" -> MagicConfig.addPhrases(spell, "icicle lance", "ice lance");
            case "stone_ball" -> MagicConfig.addPhrases(spell, "stone cannon", "rock bullet");
            case "stone_wall" -> MagicConfig.addPhrases(spell, "earth wall");
            case "swamp" -> MagicConfig.addPhrases(spell, "quagmire");
        }
    }

    private static void addPhrases(SpellDef spell, String... phrases) {
        if (spell.phrases == null) {
            spell.phrases = new ArrayList<>();
        }
        for (String phrase : phrases) {
            if (!spell.phrases.contains(phrase)) {
                spell.phrases.add(phrase);
            }
        }
    }

    private static void addMissingDefaultSpells(List<SpellDef> spells, String... ids) {
        Set<String> requestedIds = Set.of(ids);
        Set<String> presentIds = new HashSet<>();
        for (SpellDef spell : spells) {
            if (spell != null && spell.id != null) {
                presentIds.add(spell.id);
            }
        }
        for (SpellDef spell : MagicConfig.defaultSpells()) {
            if (requestedIds.contains(spell.id) && presentIds.add(spell.id)) {
                spells.add(spell);
            }
        }
    }

    private static List<SpellDef> defaultSpells() {
        ArrayList list = new ArrayList();
        list.add(new SpellDef("fire_bolt", "fire", 50.0, 40, 4.0, 2.5, (List<String>)List.of("\u043e\u0433\u043e\u043d\u044c \u043f\u043e\u0440\u0430\u0437\u0438 \u0446\u0435\u043b\u044c", "\u043e\u0433\u043d\u0435\u043d\u043d\u044b\u0439 \u0441\u043d\u0430\u0440\u044f\u0434", "\u043e\u0433\u043d\u0435\u043d\u043d\u044b\u0439 \u0448\u0430\u0440", "\u0444\u0430\u0435\u0440\u0431\u043e\u043b", "fire bolt", "fireball")));
        list.add(new SpellDef("light", "fire", 20.0, 0, 1.0, 0.0, (List<String>)List.of("\u0441\u0432\u0435\u0442 \u043e\u0441\u0432\u0435\u0442\u0438 \u043c\u043e\u0439 \u043f\u0443\u0442\u044c", "\u043e\u0433\u043e\u043d\u0451\u043a", "\u043e\u0433\u043e\u043d\u0435\u043a", "\u0441\u0432\u0435\u0442", "light")));
        list.add(new SpellDef("explosive_fireball", "fire", 160.0, 40, 6.0, 4.0, (List<String>)List.of("\u043f\u043b\u0430\u043c\u044f \u0432\u0437\u043e\u0440\u0432\u0438\u0441\u044c \u0441\u0438\u043b\u043e\u0439", "\u0432\u0437\u0440\u044b\u0432\u043d\u043e\u0439 \u0444\u0430\u0435\u0440\u0431\u043e\u043b", "\u043e\u0433\u043d\u0435\u043d\u043d\u044b\u0439 \u0432\u0437\u0440\u044b\u0432", "explosive fireball", "fire blast")));
        list.add(new SpellDef("water_ball", "water", 10.0, 10, 0.0, 3.0, (List<String>)List.of("\u0432\u043e\u0434\u0430 \u043e\u0431\u0440\u0443\u0448\u044c\u0441\u044f", "\u0432\u043e\u0434\u044f\u043d\u043e\u0439 \u0448\u0430\u0440", "\u0432\u043e\u0442\u0435\u0440\u0431\u043e\u043b", "water ball")));
        list.add(new SpellDef("water_cannon", "water", 90.0, 30, 4.5, 1.4, (List<String>)List.of("water cannon", "water cannon technique", "\u0432\u043e\u0434\u044f\u043d\u0430\u044f \u043f\u0443\u0448\u043a\u0430", "\u0432\u043e\u0434\u044f\u043d\u043e\u0439 \u043f\u043e\u0442\u043e\u043a")));
        list.add(new SpellDef("cumulonimbus", "water", 220.0, 60, 3.0, 12.0, (List<String>)List.of("cumulonimbus", "storm cloud", "heavy rain", "\u043a\u0443\u043c\u0443\u043b\u043e\u043d\u0438\u043c\u0431\u0443\u0441", "\u0433\u0440\u043e\u0437\u043e\u0432\u0430\u044f \u0442\u0443\u0447\u0430", "\u043b\u0438\u0432\u0435\u043d\u044c")));
        list.add(new SpellDef("ice_needle", "water", 50.0, 15, 2.0, 1.0, (List<String>)List.of("\u0432\u043e\u0434\u0430 \u0437\u0430\u0441\u0442\u044b\u043d\u044c \u0438\u0433\u043b\u043e\u0439", "\u043b\u0435\u0434\u044f\u043d\u0430\u044f \u0438\u0433\u043b\u0430", "\u043b\u0435\u0434\u044f\u043d\u0430\u044f \u0441\u043f\u0438\u0446\u0430", "ice needle")));
        list.add(new SpellDef("water_wall", "water", 90.0, 40, 1.0, 0.0, (List<String>)List.of("\u0432\u043e\u0434\u0430 \u0432\u0441\u0442\u0430\u043d\u044c \u0441\u0442\u0435\u043d\u043e\u0439", "\u0432\u043e\u0434\u044f\u043d\u0430\u044f \u0441\u0442\u0435\u043d\u0430", "water wall")));
        list.add(new SpellDef("stone_ball", "earth", 25.0, 15, 1.0, 1.0, (List<String>)List.of("\u0437\u0435\u043c\u043b\u044f \u0441\u043e\u0436\u043c\u0438\u0441\u044c \u0432 \u043a\u0430\u043c\u0435\u043d\u044c", "\u043a\u0430\u043c\u0435\u043d\u043d\u044b\u0439 \u0448\u0430\u0440", "\u043a\u0430\u043c\u0435\u043d\u044c \u0448\u0430\u0440", "stone ball")));
        list.add(new SpellDef("earth_hedgehog", "earth", 100.0, 40, 3.0, 5.0, (List<String>)List.of("earth hedgehog", "earth spike", "earth spikes", "spike field", "\u0437\u0435\u043c\u043b\u044f\u043d\u043e\u0439 \u0451\u0436", "\u0437\u0435\u043c\u043b\u044f\u043d\u044b\u0435 \u0448\u0438\u043f\u044b")));
        list.add(new SpellDef("stone_wall", "earth", 160.0, 40, 1.0, 0.0, (List<String>)List.of("\u0437\u0435\u043c\u043b\u044f \u0432\u0441\u0442\u0430\u043d\u044c \u043f\u0440\u0435\u0434\u043e \u043c\u043d\u043e\u0439", "\u043a\u0430\u043c\u0435\u043d\u043d\u0430\u044f \u0441\u0442\u0435\u043d\u0430", "stone wall")));
        list.add(new SpellDef("swamp", "earth", 200.0, 60, 1.0, 10.0, (List<String>)List.of("\u0437\u0435\u043c\u043b\u044f \u0440\u0430\u0437\u0432\u0435\u0440\u0437\u043d\u0438\u0441\u044c \u0442\u043e\u043f\u044c\u044e", "\u0431\u043e\u043b\u043e\u0442\u043e", "\u0442\u043e\u043f\u044c", "swamp")));
        list.add(new SpellDef("gust", "wind", 45.0, 10, 1.0, 5.0, (List<String>)List.of("\u0432\u0435\u0442\u0435\u0440 \u043e\u0442\u0442\u043e\u043b\u043a\u043d\u0438 \u0432\u0440\u0430\u0433\u043e\u0432", "\u043f\u043e\u0440\u044b\u0432 \u0432\u0435\u0442\u0440\u0430", "\u0432\u0435\u0442\u0435\u0440", "gust", "gust of wind")));
        list.add(new SpellDef("updraft", "wind", 45.0, 10, 1.0, 0.0, (List<String>)List.of("\u0432\u0435\u0442\u0435\u0440 \u043f\u043e\u0434\u043d\u0438\u043c\u0438 \u043c\u0435\u043d\u044f", "\u0432\u043e\u0441\u0445\u043e\u0434\u044f\u0449\u0438\u0439 \u043f\u043e\u0442\u043e\u043a", "updraft")));
        list.add(new SpellDef("haste", "wind", 50.0, 0, 1.0, 0.0, (List<String>)List.of("\u0432\u0435\u0442\u0435\u0440 \u0434\u0430\u0439 \u043c\u043d\u0435 \u0441\u043a\u043e\u0440\u043e\u0441\u0442\u044c", "\u0443\u0441\u043a\u043e\u0440\u0435\u043d\u0438\u0435", "haste")));
        list.add(new SpellDef("heal_basic", "healing", 35.0, 20, 1.0, 0.0, (List<String>)List.of("\u0434\u0443\u0445\u0438 \u0438\u0441\u0446\u0435\u043b\u0438\u0442\u0435 \u043c\u043e\u0438 \u0440\u0430\u043d\u044b", "\u0431\u0430\u0437\u043e\u0432\u043e\u0435 \u043b\u0435\u0447\u0435\u043d\u0438\u0435", "\u043b\u0435\u0447\u0435\u043d\u0438\u0435", "\u0438\u0441\u0446\u0435\u043b\u0435\u043d\u0438\u0435", "basic healing")));
        list.add(new SpellDef("heal_strong", "healing", 90.0, 40, 1.0, 0.0, (List<String>)List.of("\u0434\u0443\u0445\u0438 \u0437\u0430\u0449\u0438\u0442\u0438\u0442\u0435 \u043c\u043e\u0451 \u0442\u0435\u043b\u043e", "\u0443\u043a\u0440\u0435\u043f\u043b\u044f\u044e\u0449\u0435\u0435 \u043b\u0435\u0447\u0435\u043d\u0438\u0435", "strong healing")));
        list.add(new SpellDef("heal_full", "healing", 130.0, 60, 1.0, 0.0, (List<String>)List.of("\u0434\u0443\u0445\u0438 \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u0435 \u043c\u043e\u044e \u043f\u043b\u043e\u0442\u044c", "\u043f\u043e\u043b\u043d\u043e\u0435 \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0435", "full healing")));
        list.add(new SpellDef("repair_item", "repair", 100.0, 60, 0.5, 0.0, (List<String>)List.of("\u0434\u0443\u0445\u0438 \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u0435 \u043f\u0440\u0435\u0434\u043c\u0435\u0442", "\u043f\u043e\u0447\u0438\u043d\u043a\u0430", "\u0440\u0435\u043c\u043e\u043d\u0442", "repair item")));
        for (Object value : list) {
            MagicConfig.addAnimeSpellAliases((SpellDef)value);
        }
        return list;
    }

    public static MagicConfig get() {
        return instance;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("mushoku_magic.json");
    }

    public static void load() {
        MagicConfig loaded = new MagicConfig();
        Path path = MagicConfig.configPath();
        try {
            String json;
            MagicConfig parsed;
            if (Files.exists((Path)path, (LinkOption[])new LinkOption[0]) && (parsed = (MagicConfig)GSON.fromJson(json = Files.readString((Path)path, (Charset)StandardCharsets.UTF_8), MagicConfig.class)) != null) {
                loaded = parsed;
            }
        }
        catch (Exception e) {
            MushokuMagic.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0447\u0438\u0442\u0430\u0442\u044c \u043a\u043e\u043d\u0444\u0438\u0433, \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u044e \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f \u043f\u043e \u0443\u043c\u043e\u043b\u0447\u0430\u043d\u0438\u044e", (Throwable)e);
            loaded = new MagicConfig();
        }
        loaded.normalize();
        instance = loaded;
        SpellRegistry.rebuild(MagicConfig.instance.spells);
        MagicConfig.save();
    }

    public static void reload() {
        MagicConfig.load();
    }

    public static void save() {
        try {
            Path path = MagicConfig.configPath();
            Files.createDirectories((Path)path.getParent(), (FileAttribute[])new FileAttribute[0]);
            Files.writeString((Path)path, (CharSequence)GSON.toJson(instance), (Charset)StandardCharsets.UTF_8, (OpenOption[])new OpenOption[0]);
        }
        catch (Exception e) {
            MushokuMagic.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u043a\u043e\u043d\u0444\u0438\u0433", (Throwable)e);
        }
    }

    private void normalize() {
        if (this.startMaxMana <= 0.0) {
            this.startMaxMana = 40.0;
        }
        if (this.regenIntervalSeconds <= 0) {
            this.regenIntervalSeconds = 10;
        }
        if (this.overloadSeconds <= 0) {
            this.overloadSeconds = 180;
        }
        if (this.guaranteedAfterAttempts < 1) {
            this.guaranteedAfterAttempts = 6;
        }
        this.failChance = Math.max((double)0.0, (double)Math.min((double)1.0, (double)this.failChance));
        if (this.ranks == null || this.ranks.isEmpty()) {
            this.ranks = MagicConfig.defaultRanks();
        }
        if (this.keywords == null || this.keywords.isEmpty()) {
            this.keywords = MagicConfig.defaultKeywords();
        }
        Map<String, Double> defaultWandMultipliers = MagicConfig.defaultWandMultipliers();
        if (this.wandMultipliers == null
                || this.wandMultipliers.isEmpty()
                || this.wandMultipliers.equals(Map.of("1", 1.2, "2", 1.5, "3", 2.0))) {
            // Upgrade the previous defaults while preserving custom multipliers.
            this.wandMultipliers = defaultWandMultipliers;
        } else {
            this.wandMultipliers = new LinkedHashMap<>(this.wandMultipliers);
            this.wandMultipliers.putIfAbsent("1", defaultWandMultipliers.get("1"));
            this.wandMultipliers.putIfAbsent("2", defaultWandMultipliers.get("2"));
            this.wandMultipliers.putIfAbsent("3", defaultWandMultipliers.get("3"));
        }
        if (this.bosses == null) {
            this.bosses = new ArrayList((Collection)List.of("minecraft:ender_dragon", "minecraft:wither", "minecraft:warden"));
        }
        if (this.spells == null || this.spells.isEmpty()) {
            this.spells = MagicConfig.defaultSpells();
        }
        this.spells = new ArrayList<>(this.spells);
        if (this.spellPackVersion < 1) {
            for (SpellDef spell : this.spells) {
                MagicConfig.addAnimeSpellAliases(spell);
            }
            MagicConfig.addMissingDefaultSpells(this.spells, "water_cannon", "cumulonimbus");
            this.spellPackVersion = 1;
        }
        if (this.spellPackVersion < 2) {
            MagicConfig.addMissingDefaultSpells(this.spells, "earth_hedgehog");
            this.spellPackVersion = 2;
        }
        for (SpellDef spell : this.spells) {
            if (spell == null) continue;
            if (spell.phrases == null) {
                spell.phrases = new ArrayList();
            }
            if (!spell.phrases.isEmpty()) continue;
            spell.phrases.add(spell.id.replace('_', ' '));
        }
    }

    public double wandMultiplier(int tier) {
        Double value = (Double)this.wandMultipliers.get(String.valueOf((int)tier));
        return value == null ? 1.0 : value;
    }

    public static final class KeywordDef {
        public String id = "keyword";
        public List<String> words = new ArrayList();
        public double radiusMultiplier = 1.0;
        public double damageMultiplier = 1.0;
        public double castTimeMultiplier = 1.0;
        public boolean explosion = false;
        public boolean silent = false;

        public KeywordDef() {
        }

        public KeywordDef(String id, List<String> words, double radiusMultiplier, double damageMultiplier, double castTimeMultiplier, boolean explosion, boolean silent) {
            this.id = id;
            this.words = new ArrayList(words);
            this.radiusMultiplier = radiusMultiplier;
            this.damageMultiplier = damageMultiplier;
            this.castTimeMultiplier = castTimeMultiplier;
            this.explosion = explosion;
            this.silent = silent;
        }
    }

    public static final class RankDef {
        public String id = "beginner";
        public double min = 40.0;
        public double max = 209.0;
        public double multiplier = 1.0;

        public RankDef() {
        }

        public RankDef(String id, double min, double max, double multiplier) {
            this.id = id;
            this.min = min;
            this.max = max;
            this.multiplier = multiplier;
        }
    }

    public static final class SpellDef {
        public String id = "spell";
        public String element = "fire";
        public double cost = 10.0;
        public int castTicks = 0;
        public double power = 1.0;
        public double radius = 0.0;
        public List<String> phrases = new ArrayList();

        public SpellDef() {
        }

        public SpellDef(String id, String element, double cost, int castTicks, double power, double radius, List<String> phrases) {
            this.id = id;
            this.element = element;
            this.cost = cost;
            this.castTicks = castTicks;
            this.power = power;
            this.radius = radius;
            this.phrases = new ArrayList(phrases);
        }
    }
}
