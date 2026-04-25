package aiefu.eso.recipe;

import aiefu.eso.ESOCommon;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ESORecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS, ESOCommon.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, ESOCommon.MOD_ID);

    public static final RegistryObject<RecipeSerializer<EnchantmentRecipe>> ENCHANTMENT_SERIALIZER = RECIPE_SERIALIZERS.register("enchantment", Serializer::new);
    public static final RegistryObject<RecipeType<EnchantmentRecipe>> ENCHANTMENT_RECIPE_TYPE = RECIPE_TYPES.register("enchantment", () -> new RecipeType<>() {
        @Override
        public String toString() {
            return new ResourceLocation(ESOCommon.MOD_ID, "enchantment").toString();
        }
    });

    public static class Serializer implements RecipeSerializer<EnchantmentRecipe> {
        @Override
        public EnchantmentRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            return EnchantmentRecipe.fromJson(recipeId, json);
        }

        @Override
        public EnchantmentRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            return EnchantmentRecipe.fromNetwork(recipeId, buf);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, EnchantmentRecipe recipe) {
            recipe.toNetwork(buf);
        }
    }
}
