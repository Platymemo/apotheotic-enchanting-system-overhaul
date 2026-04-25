package aiefu.eso.data.enchantability;

import aiefu.eso.ESOCommon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EnchantabilityOverrides {
    public static final String RESOURCE = "enchantability";

    private final Map<ResourceLocation, EnchantabilityData> enchantabilityData;

    public EnchantabilityOverrides() {
        this.enchantabilityData = new HashMap<>();
    }

    public static EnchantabilityData getDefaultEnchantabilityData() {
        return new EnchantabilityData(
                Ingredient.EMPTY,
                ESOCommon.CONFIG.maxEnchantments.get(),
                ESOCommon.CONFIG.maxCurses.get(),
                ESOCommon.CONFIG.enchantmentLimitIncreasePerCurse.get()
        );
    }

    public EnchantabilityData getEnchantabilityData(ItemStack stack) {
        return this.enchantabilityData.values().stream()
                .filter(data -> data.test(stack))
                .findFirst()
                .orElse(getDefaultEnchantabilityData());
    }

    public Map<ResourceLocation, EnchantabilityData> getEnchantabilityDataMap() {
        return this.enchantabilityData;
    }

    public Collection<EnchantabilityData> values() {
        return this.enchantabilityData.values();
    }
}
