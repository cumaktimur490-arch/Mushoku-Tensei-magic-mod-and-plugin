package com.cumaktimur.mushokumagic.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Базовый класс для посохов Mushoku Tensei.
 * Декомпилирован из оригинального JAR и восстановлен.
 * 
 * Множитель магии (magic multiplier) - ключевой параметр, обновленный по ТЗ:
 * - wand: 2
 * - migurd_staff: 15
 * - aqua_hartia_staff: 50
 */
public class StaffItem extends Item {

    // Константы множителей - обновлены по заданию
    public static final int WAND_MULTIPLIER = 2;
    public static final int MIGURD_STAFF_MULTIPLIER = 15;
    public static final int AQUA_HARTIA_STAFF_MULTIPLIER = 50;

    private final int magicMultiplier;
    private final String staffName;

    public StaffItem(Properties properties, int magicMultiplier, String staffName) {
        super(properties);
        this.magicMultiplier = magicMultiplier;
        this.staffName = staffName;
    }

    public int getMagicMultiplier() {
        return magicMultiplier;
    }

    public String getStaffName() {
        return staffName;
    }

    /**
     * Рассчитать усиленный урон магии с учетом множителя посоха
     * @param baseDamage базовый урон заклинания
     * @return усиленный урон
     */
    public float calculateMagicDamage(float baseDamage) {
        return baseDamage * magicMultiplier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mushoku_magic.magic_multiplier", magicMultiplier)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.mushoku_magic.staff." + staffName)
                .withStyle(ChatFormatting.GRAY));

        // Дополнительная информация в зависимости от типа посоха
        if (magicMultiplier == WAND_MULTIPLIER) {
            tooltip.add(Component.translatable("tooltip.mushoku_magic.wand_desc")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else if (magicMultiplier == MIGURD_STAFF_MULTIPLIER) {
            tooltip.add(Component.translatable("tooltip.mushoku_magic.migurd_desc")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else if (magicMultiplier == AQUA_HARTIA_STAFF_MULTIPLIER) {
            tooltip.add(Component.translatable("tooltip.mushoku_magic.aqua_hartia_desc")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide) {
            // Логика использования магии с множителем
            // В оригинальном моде здесь была логика каста заклинаний
            float baseDamage = 5.0f; // базовый урон для примера
            float finalDamage = calculateMagicDamage(baseDamage);
            
            // Отправляем сообщение игроку для демонстрации множителя
            player.displayClientMessage(
                Component.translatable("message.mushoku_magic.cast",
                    staffName, magicMultiplier, finalDamage)
                    .withStyle(ChatFormatting.LIGHT_PURPLE),
                true
            );
        }

        // Кулдаун зависит от силы посоха - более сильные посохи имеют больший кулдаун
        int cooldown = 10 + (magicMultiplier / 5);
        player.getCooldowns().addCooldown(this, cooldown);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        // Более сильные посохи лучше зачаровываются
        return 10 + magicMultiplier;
    }
}
