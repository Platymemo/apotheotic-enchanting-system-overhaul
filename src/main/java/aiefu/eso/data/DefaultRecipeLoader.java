package aiefu.eso.data;

import aiefu.eso.ESOCommon;
import aiefu.eso.recipe.EnchantmentRecipe;
import com.google.gson.JsonParser;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DefaultRecipeLoader {
    public static CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier preparationBarrier,
                                                 ResourceManager manager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        return EnchantmentRecipe.defaultFromJson(
                                ESOCommon.DEFAULT_RECIPE_ID,
                                JsonParser.parseReader(new FileReader(new File(FMLPaths.CONFIGDIR.get().toFile(), "aeso/default.json"))).getAsJsonObject()
                        );
                    } catch (FileNotFoundException e) {
                        ESOCommon.LOGGER.error("Could not load default enchantment recipe {}", e.getMessage());
                        return null;
                    }
                }, backgroundExecutor)
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(recipe -> ESOCommon.defaultRecipe = recipe, gameExecutor);
    }
}
