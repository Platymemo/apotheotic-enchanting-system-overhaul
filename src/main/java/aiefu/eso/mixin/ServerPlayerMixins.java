package aiefu.eso.mixin;

import aiefu.eso.UnlockedEnchantmentHolder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixins implements UnlockedEnchantmentHolder {
    @Unique
    private Object2IntOpenHashMap<Enchantment> apoth_eso$unlockedEnchantments = new Object2IntOpenHashMap<>();

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveUnlockedEnchantmentsDataAESO(CompoundTag compound, CallbackInfo ci) {
        ListTag enchantments = new ListTag();
        apoth_eso$unlockedEnchantments.forEach((k, v) -> {
            ResourceLocation key = ForgeRegistries.ENCHANTMENTS.getKey(k);
            if (key != null) {
                CompoundTag enchantmentData = new CompoundTag();
                enchantmentData.putString("identifier", key.toString());
                enchantmentData.putInt("level", v);
                enchantments.add(enchantmentData);
            }
        });
        CompoundTag tag = new CompoundTag();
        tag.put("LearnedEnchantments", enchantments);
        compound.put("esodata", tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readUnlockedEnchantmentsDataAESO(CompoundTag compound, CallbackInfo ci) {
        this.apoth_eso$unlockedEnchantments.clear();
        if (compound.contains("esodata", Tag.TAG_COMPOUND)) {
            CompoundTag esoData = compound.getCompound("esodata");
            if (esoData.contains("LearnedEnchantments", Tag.TAG_LIST)) {
                ListTag enchantments = esoData.getList("LearnedEnchantments", Tag.TAG_COMPOUND);
                for (Tag t : enchantments) {
                    CompoundTag ct = (CompoundTag) t;
                    String id = ct.getString("identifier");
                    int level = ct.getInt("level");
                    Enchantment e = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(id));
                    if (e != null) {
                        this.apoth_eso$unlockedEnchantments.put(e, level);
                    }
                }
            }
        }

    }


    @Override
    public Object2IntOpenHashMap<Enchantment> enchantment_overhaul$getUnlockedEnchantments() {
        return apoth_eso$unlockedEnchantments;
    }

    @Override
    public void enchantment_overhaul$setUnlockedEnchantments(Object2IntOpenHashMap<Enchantment> map) {
        this.apoth_eso$unlockedEnchantments = map;
    }
}
