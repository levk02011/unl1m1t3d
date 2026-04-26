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
    private static boolean lastKeyState = false;

    static boolean autoWardenEnabled;
    static boolean ancientBotEnabled;
    static boolean autoPotionEnabled;
    static boolean chorusAutoFarmEnabled;
    static boolean autoEatEnabled;
    static boolean autoInvisEnabled;
    static boolean autoSellEnabled;

    static int keyAutoWarden = GLFW.GLFW_KEY_UNKNOWN;
    static int keyAncientBot = GLFW.GLFW_KEY_UNKNOWN;
    static int keyAutoPotion = GLFW.GLFW_KEY_UNKNOWN;
    static int keyChorusAutoFarm = GLFW.GLFW_KEY_UNKNOWN;
    static int keyAutoEat = GLFW.GLFW_KEY_UNKNOWN;
    static int keyAutoInvis = GLFW.GLFW_KEY_UNKNOWN;
    static int keyAutoSell = GLFW.GLFW_KEY_UNKNOWN;

    private static boolean lastAutoWardenKeyState = false;
    private static boolean lastAncientBotKeyState = false;
    private static boolean lastAutoPotionKeyState = false;
    private static boolean lastChorusAutoFarmKeyState = false;
    private static boolean lastAutoEatKeyState = false;
    private static boolean lastAutoInvisKeyState = false;
    private static boolean lastAutoSellKeyState = false;

    @Override
    public void onInitializeClient() {
        // Register the key binding
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.mod_1_21_4.open_menu", // The translation key of the keybinding's name
            InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
            GLFW.GLFW_KEY_RIGHT_SHIFT, // The keycode of the key
            "category.mod_1_21_4" // The translation key of the keybinding's category.
        ));

        // Register the event to check for key presses
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client != null && client.currentScreen == null) {
                boolean currentKeyState = keyBinding.isPressed();
                if (currentKeyState && !lastKeyState) {
                    openMenu(client);
                }
                lastKeyState = currentKeyState;
            } else {
                lastKeyState = false;
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
            // Перевіряємо, чи це команда нашого мода
            if (ChatCommandHandler.handleChatCommand(message)) {
                // Команда оброблена, не відправляємо її на сервер
                return false;
            }
            // Дозволяємо стандартне повідомлення
            return true;
        });
    }

    private void openMenu(MinecraftClient client) {
        // Create a simple screen with a menu
        Screen menuScreen = new MenuScreen(Text.literal("Mod Menu"));
        client.setScreen(menuScreen);
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
            default -> keyCode = GLFW.GLFW_KEY_UNKNOWN;
        }
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            return "Not set";
        }
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        return name != null ? name.toUpperCase() : "KEY_" + keyCode;
    }
}
