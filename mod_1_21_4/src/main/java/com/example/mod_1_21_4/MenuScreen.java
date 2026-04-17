package com.example.mod_1_21_4;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MenuScreen extends Screen {
    public MenuScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        // Add a close button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> {
            this.close();
        }).dimensions(this.width / 2 - 100, this.height / 2 - 10, 200, 20).build());

        // Add other menu items here
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Option 1"), button -> {
            // Handle option 1
        }).dimensions(this.width / 2 - 100, this.height / 2 - 40, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Option 2"), button -> {
            // Handle option 2
        }).dimensions(this.width / 2 - 100, this.height / 2 - 70, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}