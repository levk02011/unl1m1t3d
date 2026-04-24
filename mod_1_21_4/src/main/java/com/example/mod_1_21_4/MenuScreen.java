package com.example.mod_1_21_4;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
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
        int pveColumnX = SIDEBAR_WIDTH + 15;
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
                init();
            }).dimensions(pveColumnX, startY, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleLabel("ancient bot", Mod_1_21_4Client.ancientBotEnabled)), button -> {
                Mod_1_21_4Client.ancientBotEnabled = !Mod_1_21_4Client.ancientBotEnabled;
                ClientPlayerEntity player = MinecraftClient.getInstance().player;
                if (player != null) {
                    if (Mod_1_21_4Client.ancientBotEnabled) {
                        AncientBotHandler.activate(player);
                    } else {
                        AncientBotHandler.deactivate(player);
                    }
                }
                init();
            }).dimensions(pveColumnX, startY + (buttonHeight + spacing) * 1, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleLabel("auto potion", Mod_1_21_4Client.autoPotionEnabled)), button -> {
                Mod_1_21_4Client.autoPotionEnabled = !Mod_1_21_4Client.autoPotionEnabled;
                init();
            }).dimensions(pveColumnX, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleLabel("chorus auto farm", Mod_1_21_4Client.chorusAutoFarmEnabled)), button -> {
                Mod_1_21_4Client.chorusAutoFarmEnabled = !Mod_1_21_4Client.chorusAutoFarmEnabled;
                init();
            }).dimensions(pveColumnX, startY + (buttonHeight + spacing) * 3, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleLabel("auto eat", Mod_1_21_4Client.autoEatEnabled)), button -> {
                Mod_1_21_4Client.autoEatEnabled = !Mod_1_21_4Client.autoEatEnabled;
                init();
            }).dimensions(extraColumnX, startY, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleLabel("auto invis", Mod_1_21_4Client.autoInvisEnabled)), button -> {
                Mod_1_21_4Client.autoInvisEnabled = !Mod_1_21_4Client.autoInvisEnabled;
                init();
            }).dimensions(extraColumnX, startY + (buttonHeight + spacing) * 1, buttonWidth, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleLabel("auto sell", Mod_1_21_4Client.autoSellEnabled)), button -> {
                Mod_1_21_4Client.autoSellEnabled = !Mod_1_21_4Client.autoSellEnabled;
                init();
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
            // About Us section - display info from launcher
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> {
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

        // Render About Us content if in that section (after buttons are rendered)
        if (selectedSection == 2) {
            int textX = SIDEBAR_WIDTH + 20;
            int textY = 60;
            int lineHeight = 10;
            int color = 0xFF000000;
            
            context.drawTextWithShadow(this.textRenderer, "Unl1m1t3d Mod", textX, textY, color);
            context.drawTextWithShadow(this.textRenderer, "Version: 1.0", textX, textY + lineHeight * 1, color);
            context.drawTextWithShadow(this.textRenderer, "A custom Minecraft mod with", textX, textY + lineHeight * 2, color);
            context.drawTextWithShadow(this.textRenderer, "Fabric mod loader support.", textX, textY + lineHeight * 3, color);
            
            context.drawTextWithShadow(this.textRenderer, "Features:", textX, textY + lineHeight * 5, color);
            context.drawTextWithShadow(this.textRenderer, "- Auto Warden Handler", textX + 10, textY + lineHeight * 6, color);
            context.drawTextWithShadow(this.textRenderer, "- Ancient Bot Integration", textX + 10, textY + lineHeight * 7, color);
            context.drawTextWithShadow(this.textRenderer, "- Auto Potion System", textX + 10, textY + lineHeight * 8, color);
            context.drawTextWithShadow(this.textRenderer, "- Chorus Auto Farm", textX + 10, textY + lineHeight * 9, color);
            context.drawTextWithShadow(this.textRenderer, "- Auto Eat & Invis", textX + 10, textY + lineHeight * 10, color);
            context.drawTextWithShadow(this.textRenderer, "- Auto Sell Support", textX + 10, textY + lineHeight * 11, color);
            
            context.drawTextWithShadow(this.textRenderer, "Developed by: Unl1m1t3d Team", textX, textY + lineHeight * 13, color);
        }
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