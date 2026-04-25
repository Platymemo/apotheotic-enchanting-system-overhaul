package aiefu.eso.client.gui;

import aiefu.eso.ESOCommon;
import aiefu.eso.client.ESOClient;
import aiefu.eso.menu.OverhauledEnchantmentMenu;
import aiefu.eso.network.NetworkManager;
import aiefu.eso.network.packets.EnchantItemData;
import aiefu.eso.recipe.EnchantmentRecipe;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class EnchantingTableScreen extends AbstractContainerScreen<OverhauledEnchantmentMenu> {
    public static final ResourceLocation ENCHANTING_BACKGROUND_TEXTURE = new ResourceLocation(ESOCommon.MOD_ID, "textures/gui/ench_screen.png");
    public static final Style STYLE = Style.EMPTY.withColor(TextColor.fromRgb(5636095));
    public static final List<FormattedCharSequence> EMPTY_MSG = Minecraft.getInstance().font.split(Component.translatable("eso.enchantmentsempty"), 110);
    protected static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("#.##");

    protected EnchantmentListWidget enchantmentsScrollList;
    protected RecipeListWidget recipeViewer;
    protected EditBox searchFilter;
    protected CustomEnchantingButton confirmButton;
    protected CustomEnchantingButton cancelButton;

    protected Enchantment selectedEnchantment;
    protected int selectedOrdinal = -1;
    protected List<FormattedCharSequence> confirmMsg = new ArrayList<>();
    protected MutableComponent displayMsg;
    protected MutableComponent searchHint;
    protected ScreenMode mode = ScreenMode.LIST;
    protected int ticks = 0;

    public EnchantingTableScreen(OverhauledEnchantmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 181;
        this.imageWidth = 218;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    public static DecimalFormat getFormatter() {
        return DECIMAL_FORMATTER;
    }

    @Override
    protected void init() {
        super.init();
        this.confirmButton = this.addWidget(new CustomEnchantingButton(this.leftPos + 60, this.topPos + 92, 30, 12, CommonComponents.GUI_YES, button -> this.confirmSelection()));
        this.cancelButton = this.addWidget(new CustomEnchantingButton(this.leftPos + 130, this.topPos + 92, 30, 12, CommonComponents.GUI_NO, button -> this.setMode(ScreenMode.LIST)));

        this.searchHint = Component.translatable("eso.search");
        this.searchFilter = this.addWidget(new EditBox(this.font, this.leftPos + 81, this.topPos + 9, 123, 10, this.searchHint));
        this.searchFilter.setBordered(false);

        this.enchantmentsScrollList = Objects.requireNonNull(this.addWidget(new EnchantmentListWidget(this.leftPos + 79, this.topPos + 24, 125, 48, Component.literal(""), new ArrayList<>())));
        this.recipeViewer = new RecipeListWidget(this.leftPos + 79, this.topPos + 24, 125, 48, Component.literal(""), this);
        this.setInitialFocus(this.enchantmentsScrollList);
        this.updateButtons();
        this.setMode(ScreenMode.LIST);

        this.menu.addSlotListener(new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu containerToSend, int slotIndex, ItemStack stack) {
                if (slotIndex == 36) {
                    EnchantingTableScreen.this.updateButtons();
                    if (EnchantingTableScreen.this.mode != ScreenMode.RECIPE_VIEW) {
                        EnchantingTableScreen.this.setMode(ScreenMode.LIST);
                    }
                }
            }

            @Override
            public void dataChanged(AbstractContainerMenu containerMenu, int dataSlotIndex, int value) {
            }
        });
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        this.applyMode();
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (this.mode == ScreenMode.LIST) {
            super.slotClicked(slot, slotId, mouseButton, type);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.searchFilter.tick();

        if (this.mode == ScreenMode.RECIPE_VIEW) {
            this.recipeViewer.tick();
        }

        if (this.ticks % 60 == 0) {
            for (EnchButtonWithData button : this.enchantmentsScrollList.enchantmentButtons) {
                if (!button.isHovered()) {
                    continue;
                }

                EnchantmentRecipe recipe = button.getRecipe();
                if (recipe != null) {
                    this.applyTooltip(button);
                }
            }
        }
        this.ticks++;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        if (this.mode == ScreenMode.RECIPE_VIEW) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
            this.recipeViewer.render(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.pose().popPose();
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.searchFilter.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.mode != ScreenMode.CONFIRM) {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
        if (this.mode != ScreenMode.RECIPE_VIEW) {
            this.enchantmentsScrollList.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        if (this.displayMsg != null) {
            int x = this.leftPos + 79;
            this.drawCenteredString(guiGraphics, this.font, this.displayMsg, x + 124 / 2, this.topPos + 75, ESOClient.colorData.getTextActiveColor(), ESOClient.colorData.isDropShadow());
        }
        if (this.searchFilter.getValue().isEmpty() && !this.searchFilter.isFocused()) {
            guiGraphics.drawString(this.font, this.searchHint, this.leftPos + 81, this.topPos + 9, ESOClient.colorData.getSearchBarHintColor(), ESOClient.colorData.isSearchBarHintDropShadow());
        }
        if (this.menu.allEnchantments.isEmpty()) {
            int line = 0;
            int h = (48 - (8 * EMPTY_MSG.size() + (EMPTY_MSG.size() - 1) * 6)) / 2;
            for (FormattedCharSequence sequence : EMPTY_MSG) {
                this.drawCenteredString(guiGraphics, this.font, sequence, this.leftPos + 79 + 124 / 2, this.topPos + 25 + h + 14 * line, ESOClient.colorData.getTextActiveColor(), ESOClient.colorData.isDropShadow());
                line++;
            }
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 1000.0F);
        this.renderConfirmOverlay(guiGraphics);
        this.confirmButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().popPose();
    }

    public void renderConfirmOverlay(GuiGraphics graphics) {
        if (this.mode == ScreenMode.CONFIRM) {
            graphics.blit(ENCHANTING_BACKGROUND_TEXTURE, this.leftPos + 10, this.topPos + 48, 0, 196, 200, 60);
            int h = (42 - (8 * this.confirmMsg.size() + (this.confirmMsg.size() - 1) * 6)) / 2;
            for (int i = 0; i < this.confirmMsg.size(); i++) {
                this.drawCenteredString(graphics, this.font, this.confirmMsg.get(i), this.leftPos + 109, this.topPos + 50 + h + 14 * i, ESOClient.colorData.getTextActiveColor(), ESOClient.colorData.isDropShadow());
            }
        }
    }

    protected void drawCenteredString(GuiGraphics graphics, Font font, Component text, int x, int y, int color, boolean dropShadow) {
        graphics.drawString(font, text, x - font.width(text) / 2, y, color, dropShadow);
    }

    protected void drawCenteredString(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color, boolean dropShadow) {
        graphics.drawString(font, text, x - font.width(text) / 2, y, color, dropShadow);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        KeyMapping recipeKey = ESOClient.recipeKey;
        if (this.searchFilter.isFocused()) {
            if (keyCode == 256) {
                this.searchFilter.setFocused(false);
                return true;
            }

            boolean result = this.searchFilter.keyPressed(keyCode, scanCode, modifiers);
            this.updateButtons();
            return result;
        }
        if (this.mode == ScreenMode.RECIPE_VIEW && (recipeKey.matches(keyCode, scanCode) || keyCode == 256)) {
            this.setMode(ScreenMode.LIST);
            return true;
        }
        if (this.mode == ScreenMode.LIST && recipeKey.matches(keyCode, scanCode)) {
            return this.openRecipeViewForHoveredButton();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.mode == ScreenMode.CONFIRM) {
            return false;
        }
        if (this.mode == ScreenMode.RECIPE_VIEW) {
            this.recipeViewer.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return true;
        }
        this.enchantmentsScrollList.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.mode == ScreenMode.RECIPE_VIEW) {
            this.recipeViewer.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.mode == ScreenMode.RECIPE_VIEW) {
            this.recipeViewer.mouseScrolled(mouseX, mouseY, delta);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.mode == ScreenMode.RECIPE_VIEW) {
            this.recipeViewer.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(ENCHANTING_BACKGROUND_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    public void updateButtons() {
        LocalPlayer player = this.minecraft.player;
        if (player == null || this.minecraft.level == null) {
            this.enchantmentsScrollList.setEnchantmentButtons(List.of());
            this.displayMsg = null;
            return;
        }

        ItemStack stack = this.menu.getTableInv().getItem(0);
        if (stack.isEmpty()) {
            this.enchantmentsScrollList.setEnchantmentButtons(List.of());
            this.displayMsg = null;
            return;
        }

        this.displayMsg = Component.translatable("eso.enchantmentsleft", Math.max(this.menu.getEnchantmentLimit() - this.menu.getCurrentEnchantmentCount(), 0));
        List<OverhauledEnchantmentMenu.DisplayOption> options = new ArrayList<>(this.menu.getDisplayOptions(this.minecraft.level.getRecipeManager(), player));
        options.sort(Comparator.comparing(option -> I18n.get(option.enchantment().getDescriptionId())));

        String filter = this.searchFilter.getValue().toLowerCase(Locale.ROOT);
        ArrayList<EnchButtonWithData> buttons = new ArrayList<>();
        int offset = 0;
        for (OverhauledEnchantmentMenu.DisplayOption option : options) {
            String name = I18n.get(option.enchantment().getDescriptionId()).toLowerCase(Locale.ROOT);
            if (!filter.isBlank() && !name.contains(filter)) {
                continue;
            }

            EnchButtonWithData button = this.createEnchantmentButton(option, offset++);
            this.applyTooltip(button);
            buttons.add(button);
        }

        this.enchantmentsScrollList.setEnchantmentButtons(buttons);
        this.applyMode();
    }

    public Font getFont() {
        return this.font;
    }

    private void confirmSelection() {
        ResourceLocation enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(this.selectedEnchantment);
        if (enchantmentId == null) {
            return;
        }

        this.setMode(ScreenMode.LIST);
        NetworkManager.sendToServer(new EnchantItemData(enchantmentId, this.selectedOrdinal));
        this.updateButtons();
    }

    private boolean openRecipeViewForHoveredButton() {
        for (EnchButtonWithData button : this.enchantmentsScrollList.enchantmentButtons) {
            if (!button.isHovered()) {
                continue;
            }

            EnchantmentRecipe recipe = button.getRecipe();
            if (recipe != null) {
                this.recipeViewer.updateRecipes(recipe, button.getEnchantment());
                this.setMode(ScreenMode.RECIPE_VIEW);
                return true;
            }
            return false;
        }
        return false;
    }

    private EnchButtonWithData createEnchantmentButton(OverhauledEnchantmentMenu.DisplayOption option, int offset) {
        Enchantment enchantment = option.enchantment();
        EnchantmentRecipe recipe = option.recipe();
        int targetLevel = option.targetLevel();
        int maxLevel = recipe != null ? recipe.getMaxLevel(enchantment) : enchantment.getMaxLevel();
        MutableComponent label = EnchantmentRecipe.getFullName(enchantment, targetLevel, maxLevel);

        EnchButtonWithData button = new EnchButtonWithData(
                this.leftPos + 80,
                this.topPos + 25 + 16 * offset,
                123,
                14,
                label,
                pressed -> {
                    this.selectedEnchantment = enchantment;
                    this.selectedOrdinal = option.ordinal();
                    this.confirmMsg = this.font.split(Component.translatable(
                            "eso.applyench.1",
                            Component.translatable(enchantment.getDescriptionId()).withStyle(STYLE),
                            ((MutableComponent) this.menu.getTableInv().getItem(0).getDisplayName()).withStyle(STYLE)
                    ), 190);
                    this.setMode(ScreenMode.CONFIRM);
                },
                recipe,
                enchantment,
                this.menu.allEnchantments.getInt(enchantment),
                option.ordinal(),
                targetLevel
        );
        button.active = option.affordableNow();
        return button;
    }

    private void applyTooltip(EnchButtonWithData button) {
        button.setTooltip(Tooltip.create(this.buildTooltip(button)));
    }

    private MutableComponent buildTooltip(EnchButtonWithData button) {
        Enchantment enchantment = button.getEnchantment();
        EnchantmentRecipe recipe = button.getRecipe();
        int targetLevel = button.getTargetLevel();
        int maxLevel = recipe != null ? recipe.getMaxLevel(enchantment) : enchantment.getMaxLevel();

        MutableComponent tooltip = EnchantmentRecipe.getFullName(enchantment, targetLevel, maxLevel).copy();
        tooltip.withStyle(ChatFormatting.AQUA);
        tooltip.append(CommonComponents.NEW_LINE);
        tooltip.append(ESOClient.getEnchantmentDescription(enchantment));

        Player player = Minecraft.getInstance().player;
        if (player != null && ESOCommon.CONFIG.enableEnchantmentsLeveling.get() && !player.getAbilities().instabuild && targetLevel > this.menu.allEnchantments.getInt(enchantment)) {
            tooltip.append(CommonComponents.NEW_LINE);
            tooltip.append(Component.translatable("eso.knowledgerequired", enchantment.getFullname(targetLevel)).withStyle(ChatFormatting.DARK_RED));
        }

        if (recipe != null) {
            EnchantmentRecipe.LevelData levelData = recipe.getLevel(targetLevel);
            if (levelData != null) {
                tooltip.append(CommonComponents.NEW_LINE);
                tooltip.append(Component.translatable("eso.requires").withStyle(ChatFormatting.GRAY));
                for (var itemCost : levelData.itemCosts()) {
                    MutableComponent itemName;
                    if (itemCost.ingredient().isEmpty()) {
                        itemName = Component.translatable("eso.emptyitem").withStyle(ChatFormatting.DARK_GRAY);
                    } else {
                        Item item = itemCost.ingredient().getItems()[0].getItem();
                        itemName = Component.translatable(item.getDescriptionId());
                        itemName.append(": ").append(Component.literal(String.valueOf(itemCost.count())).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GOLD);
                    }
                    tooltip.append(CommonComponents.NEW_LINE);
                    tooltip.append(itemName);
                }

                int cost = levelData.xpCost();
                if (cost > 0 && player != null) {
                    MutableComponent costMsg;
                    if (recipe.useExpPoints) {
                        int totalXP = EnchantmentUtils.getExperience(player);
                        costMsg = Component.translatable("eso.xprequirementpoints", cost, DECIMAL_FORMATTER.format(EnchantmentUtils.getLevelForExperience(cost)));
                        costMsg.withStyle(cost > totalXP ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GREEN);
                    } else {
                        costMsg = Component.translatable("eso.xprequirementlevels", cost);
                        costMsg.withStyle(cost > player.experienceLevel ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GREEN);
                    }
                    tooltip.append(CommonComponents.NEW_LINE);
                    tooltip.append(costMsg);
                }

                tooltip.append(CommonComponents.NEW_LINE);
                tooltip.append(Component.translatable("eso.tooltip.recipekey", ESOClient.recipeKey.getTranslatedKeyMessage()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        return tooltip;
    }

    private void setMode(ScreenMode mode) {
        this.mode = mode;
        this.applyMode();
    }

    private void applyMode() {
        boolean listInteractive = this.mode == ScreenMode.LIST;
        boolean showConfirm = this.mode == ScreenMode.CONFIRM;
        this.searchFilter.active = listInteractive;
        this.enchantmentsScrollList.active = listInteractive;
        this.confirmButton.active = showConfirm;
        this.confirmButton.visible = showConfirm;
        this.cancelButton.active = showConfirm;
        this.cancelButton.visible = showConfirm;
    }

    private enum ScreenMode {
        LIST,
        CONFIRM,
        RECIPE_VIEW
    }
}
