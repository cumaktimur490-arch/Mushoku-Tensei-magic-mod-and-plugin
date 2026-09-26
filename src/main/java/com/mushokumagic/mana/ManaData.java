/*
 * Decompiled with CFR.
 */
package com.mushokumagic.mana;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mushokumagic.MushokuMagic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.class_1309;
import net.minecraft.class_2960;

public final class ManaData {
    public static AttachmentType<ManaData> TYPE;
    private double mana;
    private double maxMana;
    private double xp;
    private int overloadTicks;
    private String lastSpell;
    private final Map<String, Integer> attempts;
    private final List<String> learned;
    private boolean adminMode;
    public static final Codec<ManaData> CODEC;

    public ManaData() {
        this(0.0, 40.0, 0.0, 0, "", (Map<String, Integer>)Map.of(), (List<String>)List.of(), false);
    }

    public ManaData(double mana, double maxMana, double xp, int overloadTicks, String lastSpell, Map<String, Integer> attempts, List<String> learned, boolean adminMode) {
        this.mana = mana;
        this.maxMana = maxMana <= 0.0 ? 40.0 : maxMana;
        this.xp = xp;
        this.overloadTicks = Math.max((int)0, (int)overloadTicks);
        this.lastSpell = lastSpell == null ? "" : lastSpell;
        this.attempts = new HashMap(attempts);
        this.learned = new ArrayList(learned);
        this.adminMode = adminMode;
    }

    public static void init() {
        TYPE = AttachmentRegistry.create((class_2960)MushokuMagic.id("mana_data"), builder -> builder.initializer(ManaData::new).persistent(CODEC).copyOnDeath());
    }

    public static ManaData of(class_1309 entity) {
        return (ManaData)entity.getAttachedOrCreate(TYPE);
    }

    public void markDirty() {
    }

    public double getMana() {
        return this.mana;
    }

    public void setMana(double value) {
        this.mana = Math.max((double)0.0, (double)Math.min((double)value, (double)this.maxMana));
    }

    public double getMaxMana() {
        return this.maxMana;
    }

    public void setMaxMana(double value) {
        this.maxMana = Math.max((double)1.0, (double)value);
    }

    public void addMaxMana(double delta) {
        this.maxMana = Math.max((double)1.0, (double)(this.maxMana + delta));
    }

    public double getXp() {
        return this.xp;
    }

    public void setXp(double value) {
        this.xp = Math.max((double)0.0, (double)value);
    }

    public void addXp(double value) {
        this.xp = Math.max((double)0.0, (double)(this.xp + value));
    }

    public int getOverloadTicks() {
        return this.overloadTicks;
    }

    public void setOverloadTicks(int ticks) {
        this.overloadTicks = Math.max((int)0, (int)ticks);
    }

    public boolean isOverloaded() {
        return this.overloadTicks > 0;
    }

    public String getLastSpell() {
        return this.lastSpell;
    }

    public void setLastSpell(String spellId) {
        this.lastSpell = spellId == null ? "" : spellId;
    }

    public Map<String, Integer> getAttempts() {
        return this.attempts;
    }

    public int getAttempts(String spellId) {
        return (Integer)this.attempts.getOrDefault((Object)spellId, (Object)0);
    }

    public void addAttempt(String spellId) {
        this.attempts.put((Object)spellId, (Object)(this.getAttempts(spellId) + 1));
    }

    public List<String> getLearned() {
        return this.learned;
    }

    public boolean hasLearned(String spellId) {
        return this.adminMode || this.learned.contains((Object)spellId);
    }

    public boolean isAdminMode() {
        return this.adminMode;
    }

    public void setAdminMode(boolean value) {
        this.adminMode = value;
    }

    public void learn(String spellId) {
        if (!this.learned.contains((Object)spellId)) {
            this.learned.add((Object)spellId);
        }
    }

    static {
        CODEC = RecordCodecBuilder.create(instance -> instance.group((App)Codec.DOUBLE.optionalFieldOf("mana", (Object)0.0).forGetter(ManaData::getMana), (App)Codec.DOUBLE.optionalFieldOf("max_mana", (Object)40.0).forGetter(ManaData::getMaxMana), (App)Codec.DOUBLE.optionalFieldOf("xp", (Object)0.0).forGetter(ManaData::getXp), (App)Codec.INT.optionalFieldOf("overload", (Object)0).forGetter(ManaData::getOverloadTicks), (App)Codec.STRING.optionalFieldOf("last_spell", (Object)"").forGetter(ManaData::getLastSpell), (App)Codec.unboundedMap((Codec)Codec.STRING, (Codec)Codec.INT).optionalFieldOf("attempts", (Object)Map.of()).forGetter(ManaData::getAttempts), (App)Codec.STRING.listOf().optionalFieldOf("learned", (Object)List.of()).forGetter(ManaData::getLearned), (App)Codec.BOOL.optionalFieldOf("admin_mode", (Object)false).forGetter(ManaData::isAdminMode)).apply((Applicative)instance, ManaData::new));
    }
}
