package com.example.mod_1_21_4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Автоматичний продаж предметів на /ah з сумами з JSON
 */
public class AutoSellHandler {
    private static final String PRICES_FILE = "mods/mod_1_21_4/prices.json";
    private static Map<String, Integer> priceCache = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Set<String> searchingItems = new HashSet<>(); // Предмети на пошук

    static {
        loadPrices();
    }

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // Сканувати інвентар на предмети для продажу
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            
            if (!stack.isEmpty() && shouldSellItem(stack)) {
                String itemName = stack.getName().getString();
                
                if (priceCache.containsKey(itemName)) {
                    // Ми знаємо ціну - продаємо
                    int price = priceCache.get(itemName);
                    executeAhSell(player, itemName, price, stack.getCount());
                } else if (!searchingItems.contains(itemName)) {
                    // Не знаємо ціну - починаємо пошук
                    searchingItems.add(itemName);
                    executeAhSearch(player, itemName);
                }
            }
        }
    }

    private static void executeAhSell(ClientPlayerEntity player, String itemName, int price, int count) {
        int totalPrice = price * count;
        String command = "/ah sell " + totalPrice;
        player.networkHandler.sendChatCommand(command.substring(1));
        player.sendMessage(Text.literal("§e[Auto Sell] Продаю: " + itemName + " × " + count + " за " + totalPrice), false);
    }

    private static void executeAhSearch(ClientPlayerEntity player, String itemName) {
        String command = "/ah search " + itemName;
        player.networkHandler.sendChatCommand(command.substring(1));
        player.sendMessage(Text.literal("§6[Auto Sell] Шукаю ціну для: " + itemName), false);
    }

    private static boolean shouldSellItem(ItemStack stack) {
        // Не продавати розповсюджені предмети (приклад)
        if (stack.getItem().toString().contains("dirt") ||
            stack.getItem().toString().contains("stone")) {
            return false;
        }
        return true;
    }

    public static void recordPrice(String itemName, int price) {
        if (!priceCache.containsKey(itemName) || priceCache.get(itemName) > price) {
            priceCache.put(itemName, price);
            savePrices();
            searchingItems.remove(itemName); // Видалити з пошуку
        }
    }

    private static void loadPrices() {
        try {
            File file = new File(PRICES_FILE);
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    Map<String, ?> loaded = GSON.fromJson(reader, Map.class);
                    if (loaded != null) {
                        for (Map.Entry<String, ?> entry : loaded.entrySet()) {
                            if (entry.getValue() instanceof Number) {
                                priceCache.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void savePrices() {
        try {
            File file = new File(PRICES_FILE);
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(priceCache, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
