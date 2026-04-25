package aiefu.eso.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry.Stats;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.RecipeMatcher;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static dev.shadowsoffire.apotheosis.ench.table.EnchantingRecipe.NO_MAX;

@MethodsReturnNonnullByDefault
public class EnchantmentRecipe implements Recipe<SimpleContainer> {
    @Nullable
    public final ResourceLocation enchantment;
    public final boolean useExpPoints;
    /**
     * Maximum enchantment level.
     * If {@code -1}, defaults to {@link Enchantment#getMaxLevel()}.
     */
    public final int maxLevel;
    public final List<LevelData> levels;
    private final ResourceLocation recipeId;

    public EnchantmentRecipe(ResourceLocation recipeId, @Nullable ResourceLocation enchantment, boolean useExpPoints, int maxLevel, List<LevelData> levels) {
        this.recipeId = recipeId;
        this.enchantment = enchantment;
        this.useExpPoints = useExpPoints;
        this.maxLevel = maxLevel;
        // Ensure the list is sorted for easy retrieval
        levels.sort(Comparator.comparingInt(x -> x.level));
        this.levels = levels;
    }

    public static MutableComponent getFullName(Enchantment e, int level, int maxLevel) {
        MutableComponent mutableComponent = Component.translatable(e.getDescriptionId());
        if (level != 1 || maxLevel != 1) {
            mutableComponent.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + level));
        }
        return mutableComponent;
    }

    public boolean matchesEnchantment(ResourceLocation enchantment) {
        return enchantment.equals(this.enchantment);
    }

    /**
     * Gets the appropriate {@link LevelData} for the provided level of this {@link Enchantment}.
     *
     * @param level The level of the {@link Enchantment} to get data for.
     * @return The highest {@link LevelData} equal to or less than the provided level, or {@code null} if there is none.
     */
    @Nullable
    public LevelData getLevel(@Range(from = 1, to = Integer.MAX_VALUE) int level) {
        if (this.maxLevel > 0) {
            // TODO should this error or be allowed to happen?
            level = Math.min(level, this.maxLevel);
        }

        for (int i = Math.min(level, this.levels.size()) - 1; i >= 0; i--) {
            LevelData levelData = this.levels.get(i);

            if (levelData.level <= level) {
                return levelData;
            }
        }

        return null;
    }

    public boolean check(SimpleContainer container, int targetLevel, Player player) {
        if (targetLevel > this.maxLevel) return false;
        if (player.getAbilities().instabuild) return true;

        List<ItemStack> inputs = new ArrayList<>(container.getContainerSize());

        for (int i = 1; i < 5; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                inputs.add(stack);
            }
        }

        LevelData levelData = this.getLevel(targetLevel);
        if (levelData == null) {
             return false;
        }

        return RecipeMatcher.findMatches(inputs, levelData.itemCosts) != null && this.checkXPRequirements(player, levelData.xpCost);
    }

    public boolean checkAndConsume(SimpleContainer container, int targetLevel, Player player) {
        if (targetLevel > this.maxLevel) return false;
        if (player.getAbilities().instabuild) return true;

        List<ItemStack> inputs = new ArrayList<>(container.getContainerSize());

        for (int i = 1; i < 5; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                inputs.add(stack);
            }
        }

        LevelData levelData = this.getLevel(targetLevel);
        if (levelData == null) {
            return false;
        }

        if (!levelData.itemCosts.isEmpty()) {
            int[] inputMap = RecipeMatcher.findMatches(inputs, levelData.itemCosts);

            if (inputMap != null && this.checkXPRequirementsAndConsume(player, levelData.xpCost())) {
                for (int i = 0; i < inputMap.length; i++) {
                    ItemStack stack = inputs.get(i);
                    ItemCost itemCost = levelData.itemCosts.get(inputMap[i]);

                    ItemStack remainder = stack.getCraftingRemainingItem();
                    if (remainder != null && !remainder.isEmpty()) {
                        remainder.setCount(itemCost.count());

                        if (!player.addItem(remainder)) {
                            player.drop(remainder, false);
                        }
                    }

                    stack.shrink(itemCost.count());
                }
                return true;
            }
            return false;
        }

        return levelData.xpCost <= 0 || this.checkXPRequirementsAndConsume(player, levelData.xpCost);
    }

    public boolean checkXPRequirements(Player player, int cost) {
        if (cost <= 0) {
            return true;
        }

        if (this.useExpPoints) {
            return cost <= EnchantmentUtils.getExperience(player);
        }

        return player.experienceLevel >= cost;
    }

    public boolean checkXPRequirementsAndConsume(Player player, int cost) {
        if (cost < 1) {
            return true;
        }
        if (this.useExpPoints) {
            return EnchantmentUtils.chargeExperience(player, cost);
        } else if (player.experienceLevel >= cost) {
            player.giveExperienceLevels(-cost);
            return true;
        }
        return false;
    }

    public int getMaxLevel(Enchantment enchantment) {
        return this.maxLevel < 1 ? enchantment.getMaxLevel() : this.maxLevel;
    }

    /**
     * Unused method, as we need the {@link Player} to determine recipe eligibility for XP costs.
     */
    @Override
    @Deprecated
    public boolean matches(@NotNull SimpleContainer container, @NotNull Level level) {
        return false;
    }

    /**
     * Unused method, as we need the {@link Player} to determine recipe crafting for XP costs.
     */
    @Override
    @Deprecated
    public ItemStack assemble(@NotNull SimpleContainer container, @NotNull RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return this.levels.stream().allMatch(levelData -> levelData.itemCosts.size() >= width * height);
    }

    @Override
    public ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return recipeId;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ESORecipes.ENCHANTMENT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ESORecipes.ENCHANTMENT_RECIPE_TYPE.get();
    }

    public static EnchantmentRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
        ResourceLocation enchantmentId = ResourceLocation.tryParse(json.get("enchantment").getAsString());
        if (!ForgeRegistries.ENCHANTMENTS.containsKey(enchantmentId)) {
            throw new JsonSyntaxException("Unknown enchantment '" + enchantmentId + "'");
        }

        Enchantment enchant = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);

        boolean useExpPoints = json.has("useExpPoints") && json.get("useExpPoints").getAsBoolean();
        int maxLevel = json.has("maxLevel") ? json.get("maxLevel").getAsInt() : enchant.getMaxLevel();

        List<LevelData> levels = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("levels")) {
            JsonObject levelJson = element.getAsJsonObject();

            levels.add(LevelData.fromJson(recipeId, levelJson));
        }

        return new EnchantmentRecipe(recipeId, enchantmentId, useExpPoints, maxLevel, levels);
    }

    public static EnchantmentRecipe defaultFromJson(ResourceLocation recipeId, JsonObject json) {
        boolean useExpPoints = json.has("useExpPoints") && json.get("useExpPoints").getAsBoolean();

        List<LevelData> levels = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("levels")) {
            JsonObject levelJson = element.getAsJsonObject();

            levels.add(LevelData.fromJson(recipeId, levelJson));
        }

        return new EnchantmentRecipe(recipeId, null, useExpPoints, Integer.MAX_VALUE, levels);
    }

    public static EnchantmentRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
        ResourceLocation enchantment = buf.readResourceLocation();
        boolean useExpPoints = buf.readBoolean();
        int maxLevel = buf.readVarInt();

        int levelCount = buf.readVarInt();
        List<LevelData> levels = new ArrayList<>();
        for (int i = 0; i < levelCount; i++) {
            levels.add(LevelData.fromNetwork(buf));
        }

        return new EnchantmentRecipe(recipeId, enchantment, useExpPoints, maxLevel, levels);
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.enchantment);
        buf.writeBoolean(this.useExpPoints);
        buf.writeVarInt(this.maxLevel);

        buf.writeVarInt(this.levels.size());
        for (var level : this.levels) {
            level.toNetwork(buf);
        }
    }

    /**
     * This is the data for crafting a single Enchantment level in a {@link EnchantmentRecipe}.
     */
    public record LevelData(int level, int xpCost, List<ItemCost> itemCosts, Stats minRequirements,
                            Stats maxRequirements) {
        static LevelData fromNetwork(FriendlyByteBuf buf) {
            int level = buf.readVarInt();
            int xpCost = buf.readVarInt();

            int size = buf.readVarInt();
            List<ItemCost> itemCosts = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                itemCosts.add(ItemCost.read(buf));
            }

            Stats minStats = Stats.read(buf);

            Stats maxStats = Stats.read(buf);

            return new LevelData(level, xpCost, itemCosts, minStats, maxStats);
        }

        static LevelData fromJson(ResourceLocation id, JsonObject json) {
            int level = json.get("level").getAsInt();
            int xpCost = json.get("xpCost").getAsInt();

            List<ItemCost> itemCosts = new ArrayList<>(4);
            for (JsonElement entry : json.getAsJsonArray("ingredients")) {
                itemCosts.add(ItemCost.fromJson(entry.getAsJsonObject()));
            }

            Pair<Stats, Stats> requirements = readStats(id, json);

            return new LevelData(level, xpCost, itemCosts, requirements.left(), requirements.right());
        }

        /**
         * Copied from {@link dev.shadowsoffire.apotheosis.ench.table.EnchantingRecipe#readStats(ResourceLocation, JsonObject)}.
         */
        static Pair<Stats, Stats> readStats(ResourceLocation id, JsonObject obj) {
            Stats stats = Stats.CODEC.decode(JsonOps.INSTANCE, obj.get("requirements")).get().left().get().getFirst();
            Stats maxStats = obj.has("max_requirements") ? Stats.CODEC.decode(JsonOps.INSTANCE, obj.get("max_requirements")).get().left().get().getFirst() : NO_MAX;
            if (maxStats.eterna() != -1 && stats.eterna() > maxStats.eterna())
                throw new JsonParseException("An enchanting recipe (" + id + ") has invalid min/max eterna bounds (min > max).");
            if (maxStats.quanta() != -1 && stats.quanta() > maxStats.quanta())
                throw new JsonParseException("An enchanting recipe (" + id + ") has invalid min/max quanta bounds (min > max).");
            if (maxStats.arcana() != -1 && stats.arcana() > maxStats.arcana())
                throw new JsonParseException("An enchanting recipe (" + id + ") has invalid min/max arcana bounds (min > max).");
            return Pair.of(stats, maxStats);
        }

        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeVarInt(this.level);
            buf.writeVarInt(this.xpCost);

            buf.writeVarInt(this.itemCosts.size());
            for (ItemCost itemCost : this.itemCosts) {
                itemCost.write(buf);
            }

            this.minRequirements.write(buf);
            this.maxRequirements.write(buf);
        }
    }

    public record ItemCost(Ingredient ingredient, int count) implements Predicate<ItemStack> {
        public int size() {
            return this.ingredient().getItems().length;
        }

        /**
         * Returns an {@link ItemStack} at the given index, wrapping around if the index
         * exceeds the available number of items.
         *
         * <p>The index is normalized using modulo with {@link #size()}, allowing cyclic
         * access to the underlying item array.</p>
         *
         * @param index the index of the stack; may be greater than or equal to {@link #size()}
         * @return the {@link ItemStack} at the normalized index
         */
        public ItemStack getStack(int index) {
            return this.ingredient().getItems()[index % this.size()];
        }

        public static ItemCost read(FriendlyByteBuf buf) {
            return new ItemCost(Ingredient.fromNetwork(buf), buf.readVarInt());
        }

        public static ItemCost fromJson(JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"), false);
            int count = json.get("amount").getAsInt();

            return new ItemCost(ingredient, count);
        }

        public boolean test(ItemStack stack) {
            return this.ingredient().test(stack) && stack.getCount() >= this.count;
        }

        public void write(FriendlyByteBuf buf) {
            this.ingredient().toNetwork(buf);
            buf.writeVarInt(this.count);
        }

        public void toJson(JsonObject json) {
            json.add("id", this.ingredient().toJson());
            json.addProperty("amount", this.count);
        }
    }
}
