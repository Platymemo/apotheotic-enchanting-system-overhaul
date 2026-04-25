package aiefu.eso.client.gui;

import aiefu.eso.ESOCommon;
import aiefu.eso.client.ESOClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public class CustomEnchantingButton extends Button {
    public static final ResourceLocation ENCH_BUTTONS = new ResourceLocation(ESOCommon.MOD_ID, "textures/gui/ench_buttons.png");

    public CustomEnchantingButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        guiGraphics.blitNineSliced(ENCH_BUTTONS, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0, this.getTextureY());
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int color = this.active ? ESOClient.colorData.getTextActiveColor() : ESOClient.colorData.getTextInactiveColor();
        this.drawCenteredLabel(guiGraphics, minecraft.font, this.getMessage(), color | Mth.ceil(this.alpha * 255.0F) << 24);
    }

    private void drawCenteredLabel(GuiGraphics graphics, Font font, Component text, int color) {
        int maxTextWidth = this.width - 5;
        String visible = font.plainSubstrByWidth(text.getString(), maxTextWidth);
        if (visible.length() < text.getString().length()) {
            visible = font.plainSubstrByWidth(text.getString(), Math.max(0, maxTextWidth - font.width("..."))) + "...";
        }

        int centerX = this.getX() + this.width / 2;
        int centerY = (this.getY() + this.getY() + this.getHeight() - 9) / 2 + 1;
        this.drawCenteredString(graphics, font, Component.literal(visible), centerX, centerY, color, false);
    }

    private void drawCenteredString(GuiGraphics graphics, Font font, Component text, int x, int y, int color, boolean shadow) {
        FormattedCharSequence formattedCharSequence = text.getVisualOrderText();
        graphics.drawString(font, formattedCharSequence, x - font.width(formattedCharSequence) / 2, y, color, shadow);
    }

    public int getTextureY() {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (this.isHoveredOrFocused()) { //W148 H197
            i = 2;
        }

        return i * 20;
    }

}
