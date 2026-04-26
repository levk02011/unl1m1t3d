package com.example.mod_1_21_4;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.text.Text;

/**
 * Автоматичне пиття зілля невидимості за 10 секунд до кінця ефекту
 */
public class AutoInvisHandler {
    private static final long INVIS_THRESHOLD_MS = 10000; // 10 секунд у мс
    private static boolean lastInvisState = false;

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // Перевірити чи є ефект невидимості
        boolean hasInvis = player.getStatusEffect(StatusEffects.INVISIBILITY) != null;

        if (hasInvis) {
            // Отримати залишок часу ефекту
            int duration = player.getStatusEffect(StatusEffects.INVISIBILITY).getDuration();
            long durationMs = (long) duration * 50; // Один тик = 50мс

            // Якщо залишилось менше 10 секунд - пити нове зілля
            if (durationMs <= INVIS_THRESHOLD_MS && !lastInvisState) {
                lastInvisState = true;
                drinkInvisibilityPotion(player);
            }
        } else {
            lastInvisState = false;
        }
    }

    private static void drinkInvisibilityPotion(ClientPlayerEntity player) {
        ItemStack potionItem = findInvisibilityPotion(player);

        if (potionItem == null || potionItem.isEmpty()) {
            player.sendMessage(Text.literal("§c⚠ Auto Invis: зілля невидимості не знайдено!"), false);
            return;
        }

        int slotIndex = findInventorySlot(player, potionItem);
        if (slotIndex != -1) {
            player.getInventory().selectedSlot = slotIndex;
            player.setCurrentHand(net.minecraft.util.Hand.MAIN_HAND);
            player.sendMessage(Text.literal("§6👻 Auto Invis: п'ю зілля невидимості!"), true);
        }
    }

    private static ItemStack findInvisibilityPotion(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof PotionItem) {
                // Перевірити чи це зілля невидимості
                if (PotionUtil.getPotion(stack) == Potions.INVISIBILITY ||
                    PotionUtil.getPotion(stack) == Potions.LONG_INVISIBILITY) {
                    return stack;
                }
            }
        }
        return null;
    }

    private static int findInventorySlot(ClientPlayerEntity player, ItemStack target) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                return i;
            }
        }
        return -1;
    }
}
