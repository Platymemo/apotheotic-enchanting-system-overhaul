package aiefu.eso;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ConfigurationFile {
    public static final ConfigurationFile INSTANCE;
    public static final ForgeConfigSpec SPEC;

    static {
        Pair<ConfigurationFile, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(ConfigurationFile::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    public final ForgeConfigSpec.IntValue maxEnchantments;
    public final ForgeConfigSpec.BooleanValue enableEnchantability;
    public final ForgeConfigSpec.BooleanValue enableDefaultRecipe;
    public final ForgeConfigSpec.BooleanValue disableDiscoverySystem;
    public final ForgeConfigSpec.BooleanValue enableEnchantmentsLeveling;
    public final ForgeConfigSpec.IntValue maxEnchantmentsOnLootBooks;
    public final ForgeConfigSpec.IntValue maxEnchantmentsOnLootItems;
    public final ForgeConfigSpec.BooleanValue enableCursesAmplifier;
    public final ForgeConfigSpec.IntValue maxCurses;
    public final ForgeConfigSpec.IntValue enchantmentLimitIncreasePerCurse;
    public final ForgeConfigSpec.BooleanValue hideEnchantmentsWithoutRecipe;
    public final ForgeConfigSpec.BooleanValue disableAnvilEnchanting;
    public final ForgeConfigSpec.BooleanValue disableBookCombining;

    private ConfigurationFile(ForgeConfigSpec.Builder builder) {
        builder.push("enchanting");
        maxEnchantments = builder
                .comment("Controls how many enchantments are applicable to single item.")
                .comment("Default 3.")
                .defineInRange("maxEnchantments", 3, 0, Integer.MAX_VALUE);
        enableEnchantability = builder
                .comment("If this option is enabled, each item will have different amount of maximum enchantments depending on its enchantability.")
                .comment("If some enchantability rule was not configured, the max enchantments will be the max enchantments value from above.")
                .comment("Default true.")
                .define("enableEnchantability", true);
        enableDefaultRecipe = builder
                .comment("Whether the built-in default enchanting recipe should be used when no enchantment-specific recipe exists.")
                .comment("The default enchanting recipe is stored at /config/aeso/default.json.")
                .define("enableDefaultRecipe", true);
        disableDiscoverySystem = builder
                .comment("If true, all enchantments are treated as discovered.")
                .define("disableDiscoverySystem", false);
        enableEnchantmentsLeveling = builder
                .comment("If true, players must discover higher enchantment levels before applying them.")
                .define("enableEnchantmentsLeveling", false);
        hideEnchantmentsWithoutRecipe = builder
                .comment("Hide enchantments from the enchanting UI when there is no recipe for the next level.")
                .define("hideEnchantmentsWithoutRecipe", false);
        disableAnvilEnchanting = builder
                .comment("Prevent adding enchantments through anvils.")
                .define("disableAnvilEnchanting", false);
        disableBookCombining = builder
                .comment("Prevent combining enchanted books in anvils.")
                .define("disableBookCombining", false);
        builder.pop();

        builder.push("loot");
        maxEnchantmentsOnLootBooks = builder
                .comment("Maximum enchantments allowed on generated enchanted books.")
                .defineInRange("maxEnchantmentsOnLootBooks", 10, 0, Integer.MAX_VALUE);
        maxEnchantmentsOnLootItems = builder
                .comment("Maximum enchantments allowed on generated enchanted items.")
                .defineInRange("maxEnchantmentsOnLootItems", 3, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("curses");
        enableCursesAmplifier = builder
                .comment("If enabled, each curse applied to the item will increase the amount of regular enchantments allowed.")
                .comment("Default true.")
                .define("enableCursesAmplifier", true);
        maxCurses = builder
                .comment("Controls how many curses can be applied to items through the enchanting table.")
                .comment("Default 1.")
                .defineInRange("maxCurses", 1, 0, Integer.MAX_VALUE);
        enchantmentLimitIncreasePerCurse = builder
                .comment("How much each curse increases the enchantment limit on an item.")
                .defineInRange("enchantmentLimitIncreasePerCurse", 1, 0, Integer.MAX_VALUE);
        builder.pop();
    }
}
