package com.cumaktimur.mushokumagic.item;

import com.cumaktimur.mushokumagic.MushokuMagicMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Регистрация предметов мода.
 * Восстановлено из декомпилированных классов оригинального JAR.
 * 
 * Посохи обновлены по ТЗ:
 * - wand = x2
 * - migurd_staff = x15  
 * - aqua_hartia_staff = x50
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MushokuMagicMod.MODID);

    // Палочка - базовый магический предмет, множитель ×2
    public static final RegistryObject<Item> WAND = ITEMS.register("wand",
            () -> new StaffItem(new Item.Properties()
                    .stacksTo(1)
                    .durability(250),
                    StaffItem.WAND_MULTIPLIER,
                    "wand"));

    // Посох Мигурда - средний уровень, множитель ×15
    // В аниме/ранобэ посохи мигурдов известны своим усилением магии
    public static final RegistryObject<Item> MIGURD_STAFF = ITEMS.register("migurd_staff",
            () -> new StaffItem(new Item.Properties()
                    .stacksTo(1)
                    .durability(1000),
                    StaffItem.MIGURD_STAFF_MULTIPLIER,
                    "migurd_staff"));

    // Посох Аква Хартия (Aqua Hartia) - легендарный посох Рокси/Рудэуса, множитель ×50
    // Самый сильный посох в моде, как и в оригинальном произведении
    public static final RegistryObject<Item> AQUA_HARTIA_STAFF = ITEMS.register("aqua_hartia_staff",
            () -> new StaffItem(new Item.Properties()
                    .stacksTo(1)
                    .durability(5000)
                    .fireResistant(),
                    StaffItem.AQUA_HARTIA_STAFF_MULTIPLIER,
                    "aqua_hartia_staff"));

    // Дополнительные предметы для крафта (восстановлены из ресурсов)
    public static final RegistryObject<Item> MANA_CRYSTAL = ITEMS.register("mana_crystal",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MIGURD_CRYSTAL = ITEMS.register("migurd_crystal",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> AQUA_CRYSTAL = ITEMS.register("aqua_crystal",
            () -> new Item(new Item.Properties().fireResistant()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
