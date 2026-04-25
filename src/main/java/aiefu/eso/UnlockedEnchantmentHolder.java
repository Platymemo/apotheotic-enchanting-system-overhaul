package aiefu.eso;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.item.enchantment.Enchantment;

public interface UnlockedEnchantmentHolder {
    Object2IntOpenHashMap<Enchantment> enchantment_overhaul$getUnlockedEnchantments();
    void enchantment_overhaul$setUnlockedEnchantments(Object2IntOpenHashMap<Enchantment> map);
}
