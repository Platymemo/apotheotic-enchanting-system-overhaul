package aiefu.eso.mixin;

import aiefu.eso.ESOCommon;
import aiefu.eso.Utils;
import aiefu.eso.data.enchantability.EnchantabilityData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixins extends ItemCombinerMenu {

    @Shadow
    @Final
    private DataSlot cost;

    public AnvilMenuMixins(@Nullable MenuType<?> type, int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(type, containerId, playerInventory, access);
    }

    @Inject(method = "createResult", at = @At(value = "INVOKE", target = "net/minecraft/world/inventory/AnvilMenu.broadcastChanges()V", shift = At.Shift.BEFORE))
    public void patchResultStack(CallbackInfo ci) {
        if (!this.player.getAbilities().instabuild) {
            ItemStack result = this.resultSlots.getItem(0);
            if (!result.isEmpty() && !EnchantmentHelper.getEnchantments(result).isEmpty()) {
                ItemStack input = this.inputSlots.getItem(0);

                Map<Enchantment, Integer> resultingEnchants = EnchantmentHelper.getEnchantments(result);
                Map<Enchantment, Integer> inputEnchants = EnchantmentHelper.getEnchantments(input);

                if (!Utils.containsSameEnchantmentsOfSameLevel(resultingEnchants, inputEnchants)) {
                    if (ESOCommon.CONFIG.disableAnvilEnchanting.get()) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                    } else if (ESOCommon.CONFIG.disableBookCombining.get() && result.is(Items.ENCHANTED_BOOK)) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                    } else {
                        EnchantabilityData data = Utils.getEnchantabilityData(result);
                        int curses = resultingEnchants.keySet().stream().filter(Enchantment::isCurse).mapToInt(e -> 1).sum();
                        int limit = Utils.getEnchantmentsLimit(curses, data);

                        if (result.isDamageableItem() && input.isDamageableItem() && result.getDamageValue() != input.getDamageValue()) {
                            EnchantmentHelper.setEnchantments(inputEnchants, result);
                        } else if (resultingEnchants.size() > limit + curses) {
                            this.resultSlots.setItem(0, ItemStack.EMPTY);
                            this.cost.set(0);
                        }
                    }
                }
            }
        }
    }

}
