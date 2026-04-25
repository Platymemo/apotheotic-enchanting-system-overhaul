package aiefu.eso.client.gui;

import aiefu.eso.client.ESOClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public class EnchantmentListWidget extends AbstractScrollWidget {

    public final List<EnchButtonWithData> enchantmentButtons;

    public EnchantmentListWidget(int x, int y, int width, int height, Component message, List<EnchButtonWithData> enchantmentButtons) {
        super(x, y, width, height, message);
        this.enchantmentButtons = enchantmentButtons;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active) {
            return false;
        }
        if (this.withinContentAreaPoint(mouseX, mouseY)) {
            this.enchantmentButtons.forEach(entry -> entry.mouseClicked(mouseX, mouseY + this.scrollAmount(), button));
        } else {
            this.setFocused(false);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected int getInnerHeight() {
        return this.enchantmentButtons.size() * 16;
    }

    @Override
    protected double scrollRate() {
        return 16.0D;
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.active || !this.withinContentAreaPoint(mouseX, mouseY)) {
            mouseX = -1;
            mouseY = -1;
        }

        for (EnchButtonWithData button : this.enchantmentButtons) {
            button.render(guiGraphics, mouseX, (int) (mouseY + this.scrollAmount()), partialTick);
        }
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), ESOClient.colorData.getBackgroundColor());
    }

    @Override
    protected void renderDecorations(GuiGraphics guiGraphics) {
        if (this.scrollbarVisible()) {
            this.renderScrollBar(guiGraphics);
        }
    }

    protected void renderScrollBar(GuiGraphics guiGraphics) {
        int height = this.getScrollBarHeight();
        int minX = this.getX() + this.width;
        int maxX = this.getX() + this.width + 8;
        int minY = Math.max(this.getY(), (int) this.scrollAmount() * (this.height - height) / this.getMaxScrollAmount() + this.getY());
        int maxY = minY + height;
        guiGraphics.fill(minX, minY, maxX, maxY, ESOClient.colorData.getSliderOuterColor());
        guiGraphics.fill(minX + 1, minY + 1, maxX - 1, maxY - 1, ESOClient.colorData.getSliderInnerColor());
    }

    public void resetScrollAmount() {
        this.setScrollAmount(0);
    }

    public int getScrollBarHeight() {
        return Mth.clamp((int) ((float) (this.height * this.height) / (float) this.getContentHeight()), 32, this.height);
    }

    public int getContentHeight() {
        return this.getInnerHeight() + 4;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public void setEnchantmentButtons(List<EnchButtonWithData> enchantmentButtons) {
        this.enchantmentButtons.clear();
        this.enchantmentButtons.addAll(enchantmentButtons);
        this.resetScrollAmount();
    }
}
