package com.example.mod_1_21_4;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MenuScreen extends Screen {
    private static final int SIDEBAR_WIDTH = 150;
    private static final int DIVIDER_COLOR = 0xFF000000;
    private static final int SIDEBAR_BG_COLOR = 0xFFCCCCCC;
    private static final int CONTENT_BG_COLOR = 0xFFAAAAAA;

    public MenuScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        int sidebarX = 10;
        int startY = 50;
        int buttonHeight = 20;
        int spacing = 5;

        // Add Function button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("function"), button -> {
            // Handle Function
        }).dimensions(sidebarX, startY, SIDEBAR_WIDTH - 20, buttonHeight).build());

        // Add Binds button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("binds"), button -> {
            // Handle Bind
        }).dimensions(sidebarX, startY + buttonHeight + spacing, SIDEBAR_WIDTH - 20, buttonHeight).build());

        // Add Settings & About Us button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Settings &\nAbout Us"), button -> {
            // Handle Settings & About Us
        }).dimensions(sidebarX, startY + (buttonHeight + spacing) * 2, SIDEBAR_WIDTH - 20, buttonHeight * 2).build());

        // Add a close button at the bottom
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> {
            this.close();
        }).dimensions(sidebarX, this.height - 30, SIDEBAR_WIDTH - 20, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw sidebar background
        context.fill(0, 0, SIDEBAR_WIDTH, this.height, SIDEBAR_BG_COLOR);
        
        // Draw content area background
        context.fill(SIDEBAR_WIDTH, 0, this.width, this.height, CONTENT_BG_COLOR);
        
        // Draw vertical divider line
        context.fill(SIDEBAR_WIDTH, 0, SIDEBAR_WIDTH + 2, this.height, DIVIDER_COLOR);

        // Draw title
        context.drawTextWithShadow(this.textRenderer, "UNLIMITED", 15, 15, 0xFF000000);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}