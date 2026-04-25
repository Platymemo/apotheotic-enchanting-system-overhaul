package aiefu.eso.client;

import aiefu.eso.ESOCommon;
import aiefu.eso.client.gui.EnchantingTableScreen;
import aiefu.eso.data.ColorsDataLoader;
import aiefu.eso.data.client.ColorDataHolder;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ConcurrentHashMap;

public class ESOClient {
    private static final ConcurrentHashMap<Enchantment, MutableComponent> descriptions = new ConcurrentHashMap<>();
    public static ColorDataHolder colorData;
    public static KeyMapping recipeKey = new KeyMapping("eso.recipekeybind",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, "eso.modname");

    public static void onInitializeClient(FMLClientSetupEvent event) {
        MenuScreens.register(ESOCommon.ENCHANTMENT_MENU.get(), EnchantingTableScreen::new);
    }

    public static void registerToModBusEvent(IEventBus bus) {
        bus.register(new ClientEvents());
    }

    public static MutableComponent getEnchantmentDescription(Enchantment e) {
        return descriptions.computeIfAbsent(e, (enchantment) -> {
            String desc = enchantment.getDescriptionId();
            String ed = desc + ".desc";
            Language language = Language.getInstance();
            if (!language.has(ed) && language.has(desc + ".description")) {

                ed = desc + ".description";
            }
            return Component.translatable(ed).withStyle(ChatFormatting.DARK_GRAY);
        });
    }

    public static class ClientEvents {
        @SubscribeEvent
        public void registerClientReloadListener(RegisterClientReloadListenersEvent e) {
            e.registerReloadListener(ColorsDataLoader::reload);
        }

        @SubscribeEvent
        public void registerKeyBinds(RegisterKeyMappingsEvent e) {
            e.register(ESOClient.recipeKey);
        }
    }
}
