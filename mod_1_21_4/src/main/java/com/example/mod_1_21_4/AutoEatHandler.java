package com.example.mod_1_21_4;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/**
 * Автоматичне їдання коли рівень голоду падає
 */
public class AutoEatHandler {
    private static final int HUNGER_THRESHOLD = 2; // Їсти коли голод впав на 1-2 одиниці
    private static boolean lastFoodState = false;
    private static int eatingTicks = 0;
    private static final int EATING_DURATION = 32; // Тики їдення

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        HungerManager hungerManager = player.getHungerManager();
        int currentHunger = hungerManager.getFoodLevel();

        // Якщо вже їємо - продовжуємо
        if (eatingTicks > 0) {
            eatingTicks--;
            player.setCurrentHand(net.minecraft.util.Hand.MAIN_HAND);
            return;
        }

        // Перевірити чи голод низький
        if (currentHunger <= HUNGER_THRESHOLD && !lastFoodState) {
            lastFoodState = true;
            eatFood(player);
        } else if (currentHunger > HUNGER_THRESHOLD + 2) {
            lastFoodState = false;
        }
    }

    private static void eatFood(ClientPlayerEntity player) {
        ItemStack foodItem = findFoodInInventory(player);

        if (foodItem == null || foodItem.isEmpty()) {
            player.sendMessage(Text.literal("§c⚠ Auto Eat: їжа не знайдена!"), false);
            return;
        }

        // Знайти слот їжі
        int slotIndex = findInventorySlot(player, foodItem);
        if (slotIndex == -1) return;

        // Перемістити їжу в гарячі слоти (слоти 0-8)
        if (slotIndex >= 9) {
            // Поміняти з першим вільним гарячим слотом
            swapToHotbar(player, slotIndex);
        }

        // Установити вибраний слот
        player.getInventory().selectedSlot = Math.min(slotIndex, 8);

        // Почати їсти - триватиме EATING_DURATION тиків
        eatingTicks = EATING_DURATION;
        player.setCurrentHand(net.minecraft.util.Hand.MAIN_HAND);
        player.sendMessage(Text.literal("§a🍖 Auto Eat: їм!"), true);
    }

    private static void swapToHotbar(ClientPlayerEntity player, int slotIndex) {
        // Знайти перший вільний слот в гарячих
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isEmpty()) {
                // Поміняти
                ItemStack temp = player.getInventory().getStack(slotIndex);
                player.getInventory().setStack(slotIndex, player.getInventory().getStack(i));
                player.getInventory().setStack(i, temp);
                return;
            }
        }
    }

    private static ItemStack findFoodInInventory(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isFoodItem(stack)) {
                return stack;
            }
        }
        return null;
    }

    private static boolean isFoodItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Перевіряємо чи ItemStack має можливість бути з'їденим
        return stack.getItem().getComponents().get(net.minecraft.component.DataComponentTypes.FOOD) != null;
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
