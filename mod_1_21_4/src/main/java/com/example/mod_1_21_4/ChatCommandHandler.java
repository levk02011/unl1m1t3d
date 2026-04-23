package com.example.mod_1_21_4;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Обробляє чат-команди мода
 */
public class ChatCommandHandler {
    
    public static boolean handleChatCommand(String message) {
        // Перевірка на .chorpos1 команду
        if (message.startsWith(".chorpos1")) {
            return handleChorpos1();
        }
        
        // Перевірка на .chorpos2 команду
        if (message.startsWith(".chorpos2")) {
            return handleChorpos2();
        }
        
        // Перевірка на .an(число) команду
        if (message.startsWith(".an")) {
            return handleAnarchyNumber(message);
        }
        
        return false;
    }
    
    private static boolean handleChorpos1() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            // Отримуємо координати гравця
            ModConfig.chorusPos1.x = (int) player.getX();
            ModConfig.chorusPos1.y = (int) player.getY();
            ModConfig.chorusPos1.z = (int) player.getZ();
            
            // Виводимо повідомлення про успіх
            player.sendMessage(
                Text.literal("§a✓ Успіх! Точка 1 збережено ( " + ModConfig.chorusPos1.toString() + " )"),
                false
            );
            return true;
        }
        return false;
    }
    
    private static boolean handleChorpos2() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            // Отримуємо координати гравця
            ModConfig.chorusPos2.x = (int) player.getX();
            ModConfig.chorusPos2.y = (int) player.getY();
            ModConfig.chorusPos2.z = (int) player.getZ();
            
            // Виводимо повідомлення про успіх
            player.sendMessage(
                Text.literal("§a✓ Успіх! Точка 2 збережено ( " + ModConfig.chorusPos2.toString() + " )"),
                false
            );
            return true;
        }
        return false;
    }
    
    private static boolean handleAnarchyNumber(String message) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            try {
                String numberStr = null;
                
                // Спробуємо парсити формат .an(число)
                int openParen = message.indexOf('(');
                int closeParen = message.indexOf(')');
                
                if (openParen != -1 && closeParen != -1 && closeParen > openParen) {
                    numberStr = message.substring(openParen + 1, closeParen).trim();
                } else {
                    // Спробуємо парсити формат .anЧисло
                    numberStr = message.substring(3).trim(); // Пропускаємо ".an"
                }
                
                if (numberStr != null && !numberStr.isEmpty()) {
                    ModConfig.anarchyNumber = Integer.parseInt(numberStr);
                    
                    // Виводимо повідомлення про успіх
                    player.sendMessage(
                        Text.literal("§a✓ Успіх! Номер анархії - " + ModConfig.anarchyNumber + " збережено"),
                        false
                    );
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(
                    Text.literal("§cПомилка! Введи коректне число"),
                    false
                );
                return true;
            }
        }
        return false;
    }
}
