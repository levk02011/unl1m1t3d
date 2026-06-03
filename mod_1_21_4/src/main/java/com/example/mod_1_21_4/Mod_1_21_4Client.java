package com.example.mod_1_21_4;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class Mod_1_21_4Client implements ClientModInitializer {
    private static KeyBinding keyBinding;

    static boolean autoWardenEnabled;
    static boolean ancientBotEnabled;
    static boolean autoPotionEnabled;
    static boolean chorusAutoFarmEnabled;
    static boolean autoEatEnabled;
    static boolean autoInvisEnabled;
    static boolean autoSellEnabled;
    static boolean netherWartFarmEnabled;
    static boolean funPayEnabled;

    static int keyAutoWarden = GLFW.GLFW_KEY_UNKNOWN;
    static int keyAncientBot = GLFW.GLFW_KEY_UNKNOWN;
    static int keyAutoPotion = GLFW.GLFW_KEY_UNKNOWN;
    static int keyChorusAutoFarm = GLFW.GLFW_KEY_UNKNOWN;
    static int keyAutoEat = GLFW.GLFW_KEY_UNKNOWN;
    static int keyAutoInvis = GLFW.GLFW_KEY_UNKNOWN;
    static int keyAutoSell = GLFW.GLFW_KEY_UNKNOWN;
    static int keyNetherWartFarm = GLFW.GLFW_KEY_UNKNOWN;
    static int keyFunPay = GLFW.GLFW_KEY_UNKNOWN;

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mod_1_21_4.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.mod_1_21_4.menu"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                client.setScreen(new MenuScreen(Text.literal("Menu")));
            }

            if (autoEatEnabled) {
                AutoEatHandler.tick(client);
            }
            if (autoInvisEnabled) {
                AutoInvisHandler.tick(client);
            }
            if (autoSellEnabled) {
                AutoSellHandler.tick(client);
            }

            // Виклик таймера перевірки балансу FunPay модуля
            FPI.handleClientTick(client);
        });

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (ChatCommandHandler.handleChatCommand(message)) {
                return false;
            }
            return true;
        });

        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            if (ChatCommandHandler.handleChatCommand("/" + command)) {
                return false;
            }
            return true;
        });

        // ПЕРЕХОПЛЕННЯ ТА ПАРСИНГ ПОВІДОМЛЕНЬ ЧАТУ СЕРВЕРА
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();

            // 1. Витягуємо баланс клану з повідомлення у відповідь на /clan money
            if (text.contains("Баланс клану") || text.contains("Клановий баланс") || text.contains("Баланс каны")) {
                try {
                    String clean = text.replaceAll("[^0-9]", "");
                    if (!clean.isEmpty()) {
                        long parsedBalance = Long.parseLong(clean);
                        FPI.updateClanBalance(parsedBalance);
                    }
                } catch (Exception e) {
                    FPI.log("Помилка обробки кланового балансу.");
                }
            }

            // 2. Парсинг нового замовлення (формат повідомлення з логів вашого плагіна/бота)
            // Приклад рядка: "[FunPay] Користувач Player_Nick купив товар на суму 5000000"
            if (text.contains("[FunPay]") && text.contains("купив")) {
                try {
                    Pattern pattern = Pattern.compile("\\[FunPay\\] Користувач (\\w+) купив товар на суму (\\d+)");
                    Matcher matcher = pattern.matcher(text);
                    if (matcher.find()) {
                        String buyer = matcher.group(1);
                        long amount = Long.parseLong(matcher.group(2));
                        FPI.processIncomingOrder(buyer, amount);
                    }
                } catch (Exception e) {
                    FPI.log("Помилка виконання автоматизації платежу.");
                }
            }
        });
    }

    public static void setBindKey(String functionName, int keyCode) {
        switch (functionName) {
            case "auto warden" -> keyAutoWarden = keyCode;
            case "ancient bot" -> keyAncientBot = keyCode;
            case "auto potion" -> keyAutoPotion = keyCode;
            case "chorus auto farm" -> keyChorusAutoFarm = keyCode;
            case "auto eat" -> keyAutoEat = keyCode;
            case "auto invis" -> keyAutoInvis = keyCode;
            case "auto sell" -> keyAutoSell = keyCode;
            case "nether wart farm" -> keyNetherWartFarm = keyCode;
            case "fun pay" -> keyFunPay = keyCode;
        }
    }

    public static String getBindKeyName(String functionName) {
        int keyCode;
        switch (functionName) {
            case "auto warden" -> keyCode = keyAutoWarden;
            case "ancient bot" -> keyCode = keyAncientBot;
            case "auto potion" -> keyCode = keyAutoPotion;
            case "chorus auto farm" -> keyCode = keyChorusAutoFarm;
            case "auto eat" -> keyCode = keyAutoEat;
            case "auto invis" -> keyCode = keyAutoInvis;
            case "auto sell" -> keyCode = keyAutoSell;
            case "nether wart farm" -> keyCode = keyNetherWartFarm;
            case "fun pay" -> keyCode = keyFunPay;
            default -> keyCode = GLFW.GLFW_KEY_UNKNOWN;
        }
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            return "Not set";
        }
        String name = InputUtil.fromKeyCode(keyCode, 0).getTranslationKey();
        if (name.startsWith("key.keyboard.")) {
            return name.substring(13).toUpperCase();
        }
        return name.toUpperCase();
    }
}