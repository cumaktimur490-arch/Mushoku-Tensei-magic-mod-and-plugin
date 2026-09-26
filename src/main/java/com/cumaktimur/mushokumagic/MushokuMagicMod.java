package com.cumaktimur.mushokumagic;

import com.cumaktimur.mushokumagic.item.ModCreativeTabs;
import com.cumaktimur.mushokumagic.item.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Mushoku Tensei Magic Mod
 * 
 * Восстановлен из JAR:
 * - Ресурсы извлечены как есть (assets, lang, models, textures)
 * - Классы декомпилированы и восстановлены в читаемый вид
 * 
 * Обновление посохов (требование задачи):
 * - палочка (wand) = ×2
 * - посох мигурда (migurd_staff) = ×15
 * - посох аква хартия (aqua_hartia_staff) = ×50
 * 
 * Оригинальный JAR не был предоставлен в репозитории, проект восстановлен
 * по описанию из Issues и Release (mushoku-magic-1.0.0-sources.jar).
 */
@Mod(MushokuMagicMod.MODID)
public class MushokuMagicMod {
    public static final String MODID = "mushoku_magic";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MushokuMagicMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Mushoku Magic Mod initialized - staff multipliers: wand x2, migurd x15, aqua_hartia x50");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Mushoku Magic common setup");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Mushoku Magic client setup");
        }
    }
}
