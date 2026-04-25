package aiefu.eso.data.enchantability;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Predicate;

public class EnchantabilityData implements Predicate<ItemStack> {
    private final Ingredient ingredient;
    private final int maxEnchantments;
    private final int maxCurses;
    private final int curseMultiplier;

    public EnchantabilityData(Ingredient ingredient, int maxEnchantments, int maxCurses, int curseMultiplier) {
        this.ingredient = ingredient;
        this.maxEnchantments = maxEnchantments;
        this.maxCurses = maxCurses;
        this.curseMultiplier = curseMultiplier;
    }

    @Override
    public boolean test(ItemStack stack) {
        return this.ingredient.test(stack);
    }

    public int getMaxEnchantments() {
        return this.maxEnchantments;
    }

    public int getMaxCurses() {
        return this.maxCurses;
    }

    public int getCurseMultiplier() {
        return this.curseMultiplier;
    }

    public static EnchantabilityData fromJson(JsonObject json) {
        Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"), false);
        return new EnchantabilityData(
                ingredient,
                json.get("max_enchantments").getAsInt(),
                json.get("max_curses").getAsInt(),
                json.get("curse_multiplier").getAsInt()
        );
    }

    public void toNetwork(FriendlyByteBuf buf) {
        this.ingredient.toNetwork(buf);
        buf.writeVarInt(this.maxEnchantments);
        buf.writeVarInt(this.maxCurses);
        buf.writeVarInt(this.curseMultiplier);
    }

    public static EnchantabilityData fromNetwork(FriendlyByteBuf buf) {
        return new EnchantabilityData(Ingredient.fromNetwork(buf), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }
}
