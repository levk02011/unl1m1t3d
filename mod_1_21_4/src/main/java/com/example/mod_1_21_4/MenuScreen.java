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

    private int selectedSection = 0;
    private String capturingBindFunction;

    public MenuScreen(Text title) {
        super(title);
    }

    private static String toggleLabel(String name, boolean enabled) {
        return name + (enabled ? " : ON" : " : OFF");
    }

    @Override
    protected void init() {
        this.clearChildren();

        int sidebarX = 10;
        int startY = 50;
        int sectionWidth = 110;
        int buttonWidth = 110;
        int buttonHeight = 20;
        int spacing = 5;
        int columnSpacing = 10;

        int sectionsColumnX = sidebarX + 10;
        int pveColumnX = sectionsColumnX + sectionWidth + columnSpacing;
        int extraColumnX = pveColumnX + buttonWidth + columnSpacing;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("function"), button -> {
            selectedSection = 0;
            init();
        }).dimensions(sectionsColumnX, startY, sectionWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("bind"), button -> {
            selectedSection = 1;
            init();
        }).dimensions(sectionsColumnX, startY + (buttonHeight + spacing) * 1, sectionWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("settings & about us"), button -> {
            selectedSection = 2;
            init();
        }).dimensions(sectionsColumnX, startY + (buttonHeight + spacing) * 2, sectionWidth, buttonHeight).build());

        if (selectedSection == 0) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleLabel("auto warden", Mod_1_21_4Client.autoWardenEnabled)), button -> {
                Mod_1_21_4Client.autoWardenEnabled = !Mod_1_21_4Client.autoWardenEnabled;
                button.setMessage(Text.literal(toggleLabel("auto warden", Mod_1_21_4Client.autoWardenEnabled)));
            }).dimensions(pveColumnX, startY, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleLabel("ancient bot", Mod_1_21_4Client.ancientBotEnabled)), button -> {
                Mod_1_21_4Client.ancientBotEnabled = !Mod_1_21_4Client.ancientBotEnabled;
                button.setMessage(Text.literal(toggleLabel("ancient bot", Mod_1_21_4Client.ancientBotEnabled)));
            }).dimensions(pveColumnX, startY + (buttonHeight + spacing) * 1, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleLabel("auto potion", Mod_1_21_4Client.autoPotionEnabled)), button -> {
                Mod_1_21_4Client.autoPotionEnabled = !Mod_1_21_4Client.autoPotionEnabled;
                button.setMessage(Text.literal(toggleLabel("auto potion", Mod_1_21_4Client.autoPotionEnabled)));
            }).dimensions(pveColumnX, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleLabel("chorus auto farm", Mod_1_21_4Client.chorusAutoFarmEnabled)), button -> {
                Mod_1_21_4Client.chorusAutoFarmEnabled = !Mod_1_21_4Client.chorusAutoFarmEnabled;
                button.setMessage(Text.literal(toggleLabel("chorus auto farm", Mod_1_21_4Client.chorusAutoFarmEnabled)));
            }).dimensions(pveColumnX, startY + (buttonHeight + spacing) * 3, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleLabel("auto eat", Mod_1_21_4Client.autoEatEnabled)), button -> {
                Mod_1_21_4Client.autoEatEnabled = !Mod_1_21_4Client.autoEatEnabled;
                button.setMessage(Text.literal(toggleLabel("auto eat", Mod_1_21_4Client.autoEatEnabled)));
            }).dimensions(extraColumnX, startY, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleLabel("auto invis", Mod_1_21_4Client.autoInvisEnabled)), button -> {
                Mod_1_21_4Client.autoInvisEnabled = !Mod_1_21_4Client.autoInvisEnabled;
                button.setMessage(Text.literal(toggleLabel("auto invis", Mod_1_21_4Client.autoInvisEnabled)));
            }).dimensions(extraColumnX, startY + (buttonHeight + spacing) * 1, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleLabel("auto sell", Mod_1_21_4Client.autoSellEnabled)), button -> {
                Mod_1_21_4Client.autoSellEnabled = !Mod_1_21_4Client.autoSellEnabled;
                button.setMessage(Text.literal(toggleLabel("auto sell", Mod_1_21_4Client.autoSellEnabled)));
            }).dimensions(extraColumnX, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight).build());
        } else if (selectedSection == 1) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal(getBindButtonLabel("auto warden")), button -> {
                capturingBindFunction = "auto warden";
                init();
            }).dimensions(pveColumnX, startY, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(getBindButtonLabel("ancient bot")), button -> {
                capturingBindFunction = "ancient bot";
                init();
            }).dimensions(pveColumnX, startY + (buttonHeight + spacing) * 1, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(getBindButtonLabel("auto potion")), button -> {
                capturingBindFunction = "auto potion";
                init();
            }).dimensions(pveColumnX, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(getBindButtonLabel("chorus auto farm")), button -> {
                capturingBindFunction = "chorus auto farm";
                init();
            }).dimensions(pveColumnX, startY + (buttonHeight + spacing) * 3, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(getBindButtonLabel("auto eat")), button -> {
                capturingBindFunction = "auto eat";
                init();
            }).dimensions(extraColumnX, startY, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(getBindButtonLabel("auto invis")), button -> {
                capturingBindFunction = "auto invis";
                init();
            }).dimensions(extraColumnX, startY + (buttonHeight + spacing) * 1, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(getBindButtonLabel("auto sell")), button -> {
                capturingBindFunction = "auto sell";
                init();
            }).dimensions(extraColumnX, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight).build());
        } else {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Section settings & about us"), button -> {
                selectedSection = 0;
                init();
            }).dimensions(pveColumnX, startY, buttonWidth, buttonHeight).build());
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> {
            this.close();
        }).dimensions(sidebarX, this.height - 30, SIDEBAR_WIDTH - 20, 20).build());
    }

    private String getBindButtonLabel(String functionName) {
        if (functionName.equals(capturingBindFunction)) {
            return functionName + " : PRESS ANY KEY";
        }
        return functionName + " : " + Mod_1_21_4Client.getBindKeyName(functionName);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw sidebar background
        context.fill(0, 0, SIDEBAR_WIDTH, this.height, SIDEBAR_BG_COLOR);
        
        // Draw content area background
        context.fill(SIDEBAR_WIDTH, 0, this.width, this.height, CONTENT_BG_COLOR);
        
        // Draw vertical divider line
        context.fill(SIDEBAR_WIDTH, 0, SIDEBAR_WIDTH + 2, this.height, DIVIDER_COLOR);

        // Draw titles
        context.drawTextWithShadow(this.textRenderer, "UNLIMITED", 15, 15, 0xFF000000);
        context.drawTextWithShadow(this.textRenderer, "РОЗДІЛИ", 20, 35, 0xFF000000);
        context.drawTextWithShadow(this.textRenderer, "PVE", SIDEBAR_WIDTH + 15, 35, 0xFF000000);
        context.drawTextWithShadow(this.textRenderer, "Додаткові", SIDEBAR_WIDTH + 145, 35, 0xFF000000);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturingBindFunction != null) {
            Mod_1_21_4Client.setBindKey(capturingBindFunction, keyCode);
            capturingBindFunction = null;
            init();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}