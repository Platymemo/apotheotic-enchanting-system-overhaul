package aiefu.eso.network.packets;


import aiefu.eso.menu.OverhauledEnchantmentMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EnchantItemData {
    private final ResourceLocation enchantmentId;
    private final int ordinal;

    public EnchantItemData(ResourceLocation enchantmentId, int ordinal) {
        this.enchantmentId = enchantmentId;
        this.ordinal = ordinal;
    }

    public static EnchantItemData decode(FriendlyByteBuf buf) {
        return new EnchantItemData(buf.readResourceLocation(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.enchantmentId);
        buf.writeVarInt(this.ordinal);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof OverhauledEnchantmentMenu menu) {
                menu.checkRequirementsAndConsume(this.enchantmentId, player, this.ordinal);
            }
        });
    }
}
