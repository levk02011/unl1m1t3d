package com.example.mod_1_21_4;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

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

    private static boolean lastAutoWardenKeyState = false;
    private static boolean lastAncientBotKeyState = false;
    private static boolean lastAutoPotionKeyState = false;
    private static boolean lastChorusAutoFarmKeyState = false;
    private static boolean lastAutoEatKeyState = false;
    private static boolean lastAutoInvisKeyState = false;
    private static boolean lastAutoSellKeyState = false;
    private static boolean lastNetherWartFarmKeyState = false;
    private static boolean lastFunPayKeyState = false;
    
    // Для контролю відкриття меню
    private static boolean lastMenuKeyState = false;

    @Override
    public void onInitializeClient() {
        // Register the key binding
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.mod_1_21_4.open_menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.mod_1_21_4"
        ));

        // Register the event to check for key presses using direct window input
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client != null && client.currentScreen == null && client.player != null) {
                long window = client.getWindow().getHandle();
                
                // Прямо перевіряємо натиск Right Shift
                boolean currentMenuKeyState = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
                
                // Відкрити меню коли клавіша щойно натиснена
                if (currentMenuKeyState && !lastMenuKeyState) {
                    Screen menuScreen = new MenuScreen(Text.literal("Mod Menu"));
                    client.setScreen(menuScreen);
                }
                
                lastMenuKeyState = currentMenuKeyState;
            } else {
                lastMenuKeyState = false;
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client != null) {
                checkFunctionBinds(client);
                // Виконуємо Ancient Bot tick
                if (ancientBotEnabled && client.player != null) {
                    AncientBotHandler.tick(client);
                }
                // Виконуємо Auto Eat tick
                if (autoEatEnabled && client.player != null) {
                    AutoEatHandler.tick(client);
                }
                // Виконуємо Auto Invis tick
                if (autoInvisEnabled && client.player != null) {
                    AutoInvisHandler.tick(client);
                }
                // Виконуємо Auto Sell tick
                if (autoSellEnabled && client.player != null) {
                    AutoSellHandler.tick(client);
                }
            }
        });

        // Реєстрація обробки чат-команд
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            // ГЛОБАЛЬНИЙ ЗАХИСТ: якщо текст починається з крапки, ми ЗАВЖДИ блокуємо його відправку.
            if (message.trim().startsWith(".")) {
                ChatCommandHandler.handleChatCommand(message);
                return false; // false = скасувати відправку пакету на сервер
            }
            return true;
        });

        // Додаткова реєстрація для блокування команд у чаті (якщо гравець пише через скісну риску /)
        ClientSendMessageEvents.ALLOW_COMMAND.register((command) -> {
            String lowerCmd = command.trim().toLowerCase();
    
            // Блокуємо команди мода від відправки на сервер
            if (lowerCmd.startsWith("whyexit") || lowerCmd.startsWith("chorpos1") || 
                lowerCmd.startsWith("chorpos2") || lowerCmd.startsWith("an")) {
                // Обробляємо як чат-команду
                ChatCommandHandler.handleChatCommand("." + command.trim());
                return false; // Блокуємо відправку на сервер
            }
            return true;
        });
    }

    private void checkFunctionBinds(MinecraftClient client) {
        long window = client.getWindow().getHandle();
        ClientPlayerEntity player = client.player;

        lastAutoWardenKeyState = tickBind(window, keyAutoWarden, lastAutoWardenKeyState, () -> autoWardenEnabled = !autoWardenEnabled);
        
        lastAncientBotKeyState = tickBind(window, keyAncientBot, lastAncientBotKeyState, () -> {
            ancientBotEnabled = !ancientBotEnabled;
            if (ancientBotEnabled && player != null) {
                AncientBotHandler.activate(player);
            } else if (!ancientBotEnabled && player != null) {
                AncientBotHandler.deactivate(player);
            }
        });
        
        lastAutoPotionKeyState = tickBind(window, keyAutoPotion, lastAutoPotionKeyState, () -> autoPotionEnabled = !autoPotionEnabled);
        lastChorusAutoFarmKeyState = tickBind(window, keyChorusAutoFarm, lastChorusAutoFarmKeyState, () -> chorusAutoFarmEnabled = !chorusAutoFarmEnabled);
        lastAutoEatKeyState = tickBind(window, keyAutoEat, lastAutoEatKeyState, () -> autoEatEnabled = !autoEatEnabled);
        lastAutoInvisKeyState = tickBind(window, keyAutoInvis, lastAutoInvisKeyState, () -> autoInvisEnabled = !autoInvisEnabled);
        lastAutoSellKeyState = tickBind(window, keyAutoSell, lastAutoSellKeyState, () -> autoSellEnabled = !autoSellEnabled);
        lastNetherWartFarmKeyState = tickBind(window, keyNetherWartFarm, lastNetherWartFarmKeyState, () -> netherWartFarmEnabled = !netherWartFarmEnabled);
        lastFunPayKeyState = tickBind(window, keyFunPay, lastFunPayKeyState, () -> funPayEnabled = !funPayEnabled);
    }

    private boolean tickBind(long window, int keyCode, boolean lastState, Runnable action) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            return false;
        }
        boolean currentState = InputUtil.isKeyPressed(window, keyCode);
        if (currentState && !lastState) {
            action.run();
        }
        return currentState;
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
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        return name != null ? name.toUpperCase() : "KEY_" + keyCode;
    }
}
