package aiefu.eso;

import aiefu.eso.data.enchantability.EnchantabilityData;
import aiefu.eso.data.enchantability.EnchantabilityOverrides;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;

public class Utils {
    public static int getEnchantmentsLimit(int curses, EnchantabilityData data) {
        return ESOCommon.CONFIG.enableCursesAmplifier.get() ? data.getMaxEnchantments() + Math.min(curses, data.getMaxCurses()) * data.getCurseMultiplier() : data.getMaxEnchantments();
    }

    public static EnchantabilityData getEnchantabilityData(ItemStack stack) {
        return ESOCommon.CONFIG.enableEnchantability.get()
                ? ESOCommon.ENCHANTABILITY_OVERRIDES.getEnchantabilityData(stack)
                : EnchantabilityOverrides.getDefaultEnchantabilityData();
    }

    public static int getCurrentLimit(int appliedEnchantments, int curses) {
        return ESOCommon.CONFIG.enableCursesAmplifier.get() ? appliedEnchantments - curses : appliedEnchantments;
    }

    public static boolean containsSameEnchantmentsOfSameLevel(Map<Enchantment, Integer> m1, Map<Enchantment, Integer> m2) {
        if (m1.size() != m2.size()) {
            return false;
        } else {
            for (Map.Entry<Enchantment, Integer> e : m1.entrySet()) {
                if (!e.getValue().equals(m2.get(e.getKey()))) {
                    return false;
                }
            }
            return true;
        }
    }
}
