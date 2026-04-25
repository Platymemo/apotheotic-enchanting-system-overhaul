package aiefu.eso.network.packets;

import aiefu.eso.ESOCommon;
import aiefu.eso.data.enchantability.EnchantabilityData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncEnchantabilityData {
    private final Map<ResourceLocation, EnchantabilityData> enchantabilityData;

    public SyncEnchantabilityData() {
        this(ESOCommon.ENCHANTABILITY_OVERRIDES.getEnchantabilityDataMap());
    }

    public SyncEnchantabilityData(Map<ResourceLocation, EnchantabilityData> enchantabilityData) {
        this.enchantabilityData = enchantabilityData;
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(this.enchantabilityData.size());
        this.enchantabilityData.forEach((id, data) -> {
            buf.writeResourceLocation(id);
            data.toNetwork(buf);
        });
    }

    public static SyncEnchantabilityData fromNetwork(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, EnchantabilityData> enchantabilityData = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            enchantabilityData.put(buf.readResourceLocation(), EnchantabilityData.fromNetwork(buf));
        }
        return new SyncEnchantabilityData(enchantabilityData);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ESOCommon.ENCHANTABILITY_OVERRIDES.getEnchantabilityDataMap().clear();
            ESOCommon.ENCHANTABILITY_OVERRIDES.getEnchantabilityDataMap().putAll(this.enchantabilityData);
        });
    }
}
