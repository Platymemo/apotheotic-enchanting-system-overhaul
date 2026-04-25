package aiefu.eso.client.gui;

import aiefu.eso.recipe.EnchantmentRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.jetbrains.annotations.Nullable;

public class EnchButtonWithData extends CustomEnchantingButton {

    @Nullable
    private final EnchantmentRecipe recipe;
    private final EnchantmentInstance enchantmentInstance;
    private final int ordinal;
    private final int targetLevel;

    public EnchButtonWithData(int x, int y, int width, int height, Component message, OnPress onPress, @Nullable EnchantmentRecipe recipe, Enchantment enchantment, int level, int ordinal, int targetLevel) {
        super(x, y, width, height, message, onPress);
        this.recipe = recipe;
        this.enchantmentInstance = new EnchantmentInstance(enchantment, level);
        this.ordinal = ordinal;
        this.targetLevel = targetLevel;
    }

    public @Nullable EnchantmentRecipe getRecipe() {
        return recipe;
    }

    public Enchantment getEnchantment() {
        return enchantmentInstance.enchantment;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public int getTargetLevel() {
        return targetLevel;
    }
}
