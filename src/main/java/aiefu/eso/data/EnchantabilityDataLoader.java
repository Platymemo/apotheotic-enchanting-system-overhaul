package aiefu.eso.data;

import aiefu.eso.ESOCommon;
import aiefu.eso.data.enchantability.EnchantabilityData;
import aiefu.eso.data.enchantability.EnchantabilityOverrides;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class EnchantabilityDataLoader {
    public static CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier preparationBarrier,
                                                 ResourceManager manager,
                                                 ProfilerFiller preparationProfiler,
                                                 ProfilerFiller applicationProfiler,
                                                 Executor backgroundExecutor,
                                                 Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> loadEnchantabilityData(manager), backgroundExecutor)
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(data -> {
                    ESOCommon.ENCHANTABILITY_OVERRIDES.getEnchantabilityDataMap().clear();
                    ESOCommon.ENCHANTABILITY_OVERRIDES.getEnchantabilityDataMap().putAll(data);
                }, gameExecutor);
    }

    private static Map<ResourceLocation, EnchantabilityData> loadEnchantabilityData(ResourceManager manager) {
        Map<ResourceLocation, Resource> resources = manager.listResources(EnchantabilityOverrides.RESOURCE, location -> location.getPath().endsWith(".json"));
        Map<ResourceLocation, EnchantabilityData> enchantabilityData = new HashMap<>();
        resources.forEach((location, resource) -> readEnchantabilityEntry(location, resource, enchantabilityData));
        return enchantabilityData;
    }

    private static void readEnchantabilityEntry(ResourceLocation location,
                                                Resource resource,
                                                Map<ResourceLocation, EnchantabilityData> enchantabilityData) {
        try (var reader = resource.openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            EnchantabilityData data = EnchantabilityData.fromJson(json);
            if (enchantabilityData.put(location, data) != null) {
                ESOCommon.LOGGER.warn("Duplicate enchantability override '{}'", location);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read enchantability override " + location, e);
        }
    }
}
