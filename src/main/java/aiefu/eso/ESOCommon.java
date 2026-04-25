package aiefu.eso;

import aiefu.eso.client.ESOClient;
import aiefu.eso.data.DefaultRecipeLoader;
import aiefu.eso.data.EnchantabilityDataLoader;
import aiefu.eso.data.enchantability.EnchantabilityOverrides;
import aiefu.eso.menu.OverhauledEnchantmentMenu;
import aiefu.eso.network.NetworkManager;
import aiefu.eso.recipe.ESORecipes;
import aiefu.eso.recipe.EnchantmentRecipe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Mod(ESOCommon.MOD_ID)
public class ESOCommon {
    public static final String MOD_ID = "apotheotic_enchanting_system_overhaul";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ResourceLocation DEFAULT_RECIPE_ID = new ResourceLocation("default");
    public static final DeferredRegister<MenuType<?>> MENU_REGISTER = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ESOCommon.MOD_ID);
    public static final RegistryObject<MenuType<OverhauledEnchantmentMenu>> ENCHANTMENT_MENU =
            MENU_REGISTER.register("enchs_menu_ovr", () -> IForgeMenuType.create(OverhauledEnchantmentMenu::new));
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final ConfigurationFile CONFIG = ConfigurationFile.INSTANCE;
    public static final EnchantabilityOverrides ENCHANTABILITY_OVERRIDES = new EnchantabilityOverrides();
    public static EnchantmentRecipe defaultRecipe = null;

    public ESOCommon() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onInitialize);
        MENU_REGISTER.register(modEventBus);
        ESORecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ESORecipes.RECIPE_TYPES.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigurationFile.SPEC, "eso-server.toml");
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(ESOClient::onInitializeClient);
            ESOClient.registerToModBusEvent(modEventBus);
        }
    }

    public static List<EnchantmentRecipe> getRecipes(RecipeManager recipeManager, ResourceLocation enchantment) {
        List<EnchantmentRecipe> recipes = new ArrayList<>();
        for (EnchantmentRecipe recipe : recipeManager.getAllRecipesFor(ESORecipes.ENCHANTMENT_RECIPE_TYPE.get())) {
            if (recipe.matchesEnchantment(enchantment)) {
                recipes.add(recipe);
            }
        }
        if (!recipes.isEmpty()) {
            return recipes;
        }

        return CONFIG.enableDefaultRecipe.get() && ESOCommon.defaultRecipe != null ? List.of(ESOCommon.defaultRecipe) : List.of();
    }

    public static int getMaximumPossibleEnchantmentLevel(RecipeManager recipeManager, Enchantment enchantment) {
        ResourceLocation enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        List<EnchantmentRecipe> recipes = enchantmentId == null ? List.of() : getRecipes(recipeManager, enchantmentId);
        int maxLevel = 0;
        if (!recipes.isEmpty()) {
            for (EnchantmentRecipe recipe : recipes) {
                int l = recipe.getMaxLevel(enchantment);
                if (l > maxLevel) {
                    maxLevel = l;
                }
            }
        } else maxLevel = enchantment.getMaxLevel();
        return maxLevel;
    }

    public static Gson getGson() {
        return GSON;
    }

    public void onInitialize(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        NetworkManager.setup();
        LOGGER.info("AESO Initialized");
    }

    @SubscribeEvent
    public void reloadListeners(final AddReloadListenerEvent event) {
        event.addListener(DefaultRecipeLoader::reload);
        event.addListener(EnchantabilityDataLoader::reload);
    }

    @SubscribeEvent
    public void registerCommands(final RegisterCommandsEvent event) {
        ESOCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public void copyPlayerData(final PlayerEvent.Clone event) {
        UnlockedEnchantmentHolder old = ((UnlockedEnchantmentHolder) event.getOriginal());
        UnlockedEnchantmentHolder np = ((UnlockedEnchantmentHolder) event.getEntity());
        np.enchantment_overhaul$setUnlockedEnchantments(old.enchantment_overhaul$getUnlockedEnchantments());
    }
}
