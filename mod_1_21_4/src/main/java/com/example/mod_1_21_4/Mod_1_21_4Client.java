package com.example.mod_1_21_4;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class Mod_1_21_4Client implements ClientModInitializer {
    private static KeyBinding keyBinding;
    private static boolean lastKeyState = false;

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
    }

    private void openMenu(MinecraftClient client) {
        // Create a simple screen with a menu
        Screen menuScreen = new MenuScreen(Text.literal("Mod Menu"));
        client.setScreen(menuScreen);
    }
}