package aiefu.eso.data.itemdata;

import aiefu.eso.client.gui.EnchantingTableScreen;
import aiefu.eso.recipe.EnchantmentRecipe;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;

public class RecipeViewerData {
    protected EnchantmentRecipe.LevelData levelData;
    protected Enchantment enchantment;
    protected boolean useExp;
    protected Component desc;
    protected final ItemStack result;
    protected int maxCycle;
    protected int cycle = 0;

    public RecipeViewerData(EnchantmentRecipe.LevelData levelData, Enchantment enchantment, boolean useExp) {
        this.levelData = levelData;
        this.enchantment = enchantment;
        this.useExp = useExp;
        this.result = Items.ENCHANTED_BOOK.getDefaultInstance();
        EnchantedBookItem.addEnchantment(this.result, new EnchantmentInstance(enchantment, levelData.level()));
        this.composeDescription();
        this.updateMaxCycle();
    }

    private void updateMaxCycle() {
        this.maxCycle = this.levelData.itemCosts().stream().mapToInt(pair -> pair.ingredient().getItems().length).max().orElse(1);
    }

    private void composeDescription() {
        MutableComponent c = Component.translatable("eso.rv.level", this.levelData.level());
        int xpCost = this.levelData.xpCost();
        if (xpCost > 0) {
            if (this.useExp) {
                c.append(CommonComponents.SPACE);
                c.append(Component.translatable("eso.rv.xpreql", xpCost, EnchantingTableScreen.getFormatter().format(EnchantmentUtils.getLevelForExperience(xpCost))));
            } else {
                c.append(CommonComponents.SPACE);
                c.append(Component.translatable("eso.rv.xpreqp", xpCost));
            }
        }
        this.desc = c;
    }

    public void next() {
        this.cycle++;
    }

    public EnchantmentRecipe.LevelData getLevelData() {
        return levelData;
    }

    public int getXp() {
        return this.levelData.xpCost();
    }

    public ItemStack getResult() {
        return this.result;
    }

    public List<ItemStack> getStacks() {
        List<ItemStack> stacks = new ArrayList<>(this.levelData.itemCosts().size());
        for (EnchantmentRecipe.ItemCost itemCost : this.levelData.itemCosts()) {
            ItemStack stack = itemCost.getStack(this.cycle);
            stack.setCount(itemCost.count());

            stacks.add(stack);
        }

        return stacks;
    }

    public Component getDesc() {
        return desc;
    }
}
