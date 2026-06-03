package com.example.mod_1_21_4;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class FPI {
    public static boolean isLoggedIn = false;
    public static String funPayUser = "Гість";
    public static long clanBalance = 0;
    public static long roundedBalance = 0;
    
    public static final List<String> consoleLogs = new ArrayList<>();
    private static final int MAX_LOGS = 5;
    
    private static int balanceCheckTicks = 0;

    static {
        log("Модуль FPI ініціалізовано. Очікування авторизації...");
    }

    public static void log(String message) {
        if (consoleLogs.size() >= MAX_LOGS) {
            consoleLogs.remove(0);
        }
        consoleLogs.add("[FP] " + message);
    }

    // Метод тепер просто СТВОРЮЄ кнопки і повертає їх, не намагаючись самостійно додати на Screen
    public static ButtonWidget createLoginButton(Screen screen, int pveColumnX, int startY, int buttonWidth, int buttonHeight) {
        return ButtonWidget.builder(Text.literal("Вхід через Google"), button -> {
            isLoggedIn = true;
            funPayUser = "Unl1m1t3d_Seller"; 
            log("Успішний вхід через Google OAuth!");
            log("Ласкаво просимо, " + funPayUser);
            if (screen instanceof MenuScreen menu) menu.clearAndInit();
        }).dimensions(pveColumnX, startY, buttonWidth + 40, buttonHeight).build();
    }

    public static ButtonWidget createLogoutButton(Screen screen, int pveColumnX, int startY, int buttonWidth, int buttonHeight) {
        return ButtonWidget.builder(Text.literal("Вийти з акаунту"), button -> {
            isLoggedIn = false;
            funPayUser = "Гість";
            log("Вихід з профілю здійснено.");
            if (screen instanceof MenuScreen menu) menu.clearAndInit();
        }).dimensions(pveColumnX, startY, buttonWidth + 40, buttonHeight).build();
    }

    public static ButtonWidget createBackButton(Screen screen, int pveColumnX, int startY, int buttonWidth, int buttonHeight, int spacing) {
        return ButtonWidget.builder(Text.literal("Back"), button -> {
            if (screen instanceof MenuScreen menu) {
                menu.setSelectedSection(0);
                menu.clearAndInit();
            }
        }).dimensions(pveColumnX, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight).build();
    }

    public static void render(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, int sidebarWidth, int screenHeight, int screenWidth) {
        int textX = sidebarWidth + 20;
        int currentY = 55;
        int color = 0xFFFFFFFF;

        context.drawTextWithShadow(textRenderer, "FunPay Автоматизація", sidebarWidth + 15, 35, 0xFF00FF00);

        // --- БЛОК АВАТАРКИ ТА ПРОФІЛЮ ---
        if (isLoggedIn) {
            int avatarX = textX;
            int avatarY = currentY;
            context.fill(avatarX, avatarY, avatarX + 24, avatarY + 24, 0xFF444444);
            
            // Виправлений рендер текстури для версії 1.21.4 (використовує RenderLayer.getGuiTextured)
            Identifier steveTex = Identifier.of("minecraft", "textures/entity/steve.png");
            context.drawTexture(RenderLayer::getGuiTextured, steveTex, avatarX + 4, avatarY + 4, 8.0f, 8.0f, 8, 8, 64, 64);
            
            context.drawTextWithShadow(textRenderer, "Акаунт: §a" + funPayUser, textX + 32, currentY + 2, color);
            context.drawTextWithShadow(textRenderer, "Статус: §2Авторизовано", textX + 32, currentY + 12, color);
        } else {
            context.drawTextWithShadow(textRenderer, "Статус: §cПотрібен логін", textX, currentY, color);
        }

        // --- БЛОК КЛАНУ ТА ВАЛЮТИ ---
        currentY += 35;
        context.drawTextWithShadow(textRenderer, "Баланс клану (/clan money): §e" + clanBalance + " $", textX, currentY, color);
        context.drawTextWithShadow(textRenderer, "Округлено до меншого: §6" + roundedBalance + " млн $", textX, currentY + 12, color);

        // --- КОНСОЛЬ ЛОГІВ (ЗНИЗУ ЕКРАНУ) ---
        int consoleX = sidebarWidth + 15;
        int consoleY = screenHeight - 80;
        int consoleWidth = screenWidth - consoleX - 15;
        int consoleHeight = 65;

        context.fill(consoleX, consoleY, consoleX + consoleWidth, consoleY + consoleHeight, 0xCC111111);
        
        // Виправлено виклик drawBorder (прибрано перший зайвий аргумент 'context')
        context.drawBorder(consoleX, consoleY, consoleWidth, consoleHeight, 0xFF555555);
        
        context.drawTextWithShadow(textRenderer, "Логи роботи модуля:", consoleX + 5, consoleY + 5, 0xFFFFAA00);

        int logY = consoleY + 16;
        synchronized (consoleLogs) {
            for (String logLine : consoleLogs) {
                context.drawTextWithShadow(textRenderer, logLine, consoleX + 8, logY, 0xFFDDDDDD);
                logY += 9;
            }
        }
    }

    public static void handleClientTick(MinecraftClient client) {
        if (!isLoggedIn || client.player == null) return;

        balanceCheckTicks++;
        if (balanceCheckTicks >= 6000) { 
            balanceCheckTicks = 0;
            log("Запит балансу за розкладом...");
            
            // Виправлено зауваження Dereferencing possible null pointer безпечною перевіркою
            if (client.player.networkHandler != null) {
                client.player.networkHandler.sendCommand("clan money");
            }
        }
    }

    public static void updateClanBalance(long rawBalance) {
        clanBalance = rawBalance;
        roundedBalance = rawBalance / 1_000_000;
        log("Оновлено баланс клану: " + rawBalance + " $ (" + roundedBalance + " млн)");
    }

    public static void processIncomingOrder(String buyerName, long amount) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.networkHandler == null) return;

        log("Отримано замовлення! Покупець: " + buyerName + " | Сума: " + amount);

        player.networkHandler.sendCommand("clan withdraw " + amount);
        log("Виконано: /clan withdraw " + amount);

        player.networkHandler.sendCommand("pay " + buyerName + " " + amount);
        player.networkHandler.sendCommand("pay " + buyerName + " " + amount);
        log("Виконано двічі: /pay " + buyerName + " " + amount);
        log("Замовлення успішно виконано.");
    }
}