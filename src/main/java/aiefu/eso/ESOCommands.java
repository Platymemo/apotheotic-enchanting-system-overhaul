package aiefu.eso;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

public class ESOCommands {
    public static final String BASE_COMMAND = "aeso";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        // Learn enchantment command
        dispatcher.register(Commands.literal(BASE_COMMAND)
                .requires(stack -> stack.hasPermission(4))
                .then(Commands.literal("learn")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("enchantment", ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                                        .executes(ctx -> learnEnchantmentById(ctx, EntityArgument.getPlayer(ctx, "player"), ResourceArgument.getEnchantment(ctx, "enchantment"))).then(
                                                Commands.argument("level", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> setLeveledEnchantment(
                                                                ctx,
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                ResourceArgument.getEnchantment(ctx, "enchantment"),
                                                                IntegerArgumentType.getInteger(ctx, "level"))))))
                        .then(Commands.argument("enchantment", ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                                .executes(ctx -> learnEnchantmentById(ctx, ctx.getSource().getPlayer(), ResourceArgument.getEnchantment(ctx, "enchantment"))).then(
                                        Commands.argument("level", IntegerArgumentType.integer(1))
                                                .executes(ctx -> setLeveledEnchantment(
                                                        ctx,
                                                        ctx.getSource().getPlayer(),
                                                        ResourceArgument.getEnchantment(ctx, "enchantment"),
                                                        IntegerArgumentType.getInteger(ctx, "level")))))));
        dispatcher.register(Commands.literal(BASE_COMMAND)
                .requires(stack -> stack.hasPermission(4))
                .then(Commands.literal("learnall")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> grantAll(ctx, EntityArgument.getPlayer(ctx, "player"))))
                        .executes(ctx -> grantAll(ctx, ctx.getSource().getPlayer()))));

        // Forget enchantment command
        dispatcher.register(Commands.literal(BASE_COMMAND).requires(stack -> stack.hasPermission(4)).then(Commands.literal("forget")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("enchantment", ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                                .executes(ctx -> forgetEnchantment(ctx, EntityArgument.getPlayer(ctx, "player"), ResourceArgument.getEnchantment(ctx, "enchantment")))))
                .then(Commands.argument("enchantment", ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                        .executes(ctx -> forgetEnchantment(ctx, ctx.getSource().getPlayer(), ResourceArgument.getEnchantment(ctx, "enchantment"))))));
        dispatcher.register(Commands.literal(BASE_COMMAND).requires(stack -> stack.hasPermission(4)).then(Commands.literal("forgetall")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> revokeAll(ctx, EntityArgument.getPlayer(ctx, "player"))))
                .executes(ctx -> revokeAll(ctx, ctx.getSource().getPlayer()))));
    }

    public static int setLeveledEnchantment(CommandContext<CommandSourceStack> ctx, ServerPlayer targetPlayer, Holder.Reference<Enchantment> enchantmentEntry, int level) {
        if (enchantmentEntry != null) {
            Enchantment enchantment = enchantmentEntry.value();
            MutableComponent c = Component.translatable(enchantment.getDescriptionId());
            Object2IntOpenHashMap<Enchantment> learnedEnchantments = ((UnlockedEnchantmentHolder) targetPlayer).enchantment_overhaul$getUnlockedEnchantments();
            int maxLevel = ESOCommon.getMaximumPossibleEnchantmentLevel(ctx.getSource().getServer().getRecipeManager(), enchantment);
            int i = learnedEnchantments.getInt(enchantment);
            int r = Math.min(maxLevel, i + level);
            learnedEnchantments.put(enchantment, r);
            ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.successfullyadded", c, r, targetPlayer.getDisplayName()), true);
            targetPlayer.sendSystemMessage(Component.translatable("eso.youlearned", getFormattedNameLeveled(enchantment, r)).withStyle(ChatFormatting.GOLD), true);
        }

        return 0;
    }

    public static MutableComponent getFormattedNameLeveled(Enchantment e, int l) {
        MutableComponent msg = Component.literal("[").withStyle(ChatFormatting.DARK_PURPLE);
        msg.append(e.getFullname(l));
        msg.append(Component.literal("]"));
        return msg;
    }

    public static int forgetEnchantment(CommandContext<CommandSourceStack> ctx, ServerPlayer player, Holder.Reference<Enchantment> enchantmentEntry) {
        if (enchantmentEntry != null && player instanceof UnlockedEnchantmentHolder acc) {
            MutableComponent discId = Component.translatable(enchantmentEntry.get().getDescriptionId());
            MutableComponent c = Component.literal("[").withStyle(ChatFormatting.DARK_PURPLE);
            c.append(discId);
            c.append(Component.literal("]"));
            if (acc.enchantment_overhaul$getUnlockedEnchantments().removeInt(enchantmentEntry.get()) != 0) {
                player.sendSystemMessage(Component.translatable("eso.youforgot", c), true);
                ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.removedEnchantment", discId, player.getDisplayName()), true);
            } else
                ctx.getSource().sendFailure(Component.translatable("eso.command.feedback.doesnotknow", player.getDisplayName(), discId));
        } else ctx.getSource().sendFailure(Component.translatable("eso.command.enchantmentnotfound", enchantmentEntry));
        return 1;
    }

    public static int learnEnchantmentById(CommandContext<CommandSourceStack> ctx, ServerPlayer player, Holder.Reference<Enchantment> enchantmentEntry) {
        if (enchantmentEntry != null && player instanceof UnlockedEnchantmentHolder acc) {
            MutableComponent discId = Component.translatable(enchantmentEntry.get().getDescriptionId());
            MutableComponent c = Component.literal("[").withStyle(ChatFormatting.DARK_PURPLE);
            c.append(discId);
            c.append(Component.literal("]"));
            if (acc.enchantment_overhaul$getUnlockedEnchantments().put(enchantmentEntry.get(), ESOCommon.getMaximumPossibleEnchantmentLevel(ctx.getSource().getServer().getRecipeManager(), enchantmentEntry.get())) < 1) {
                player.sendSystemMessage(Component.translatable("eso.youlearned", c).withStyle(ChatFormatting.GOLD), true);
                ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.addedEnchantment", discId, player.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
            } else {
                ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.playerknows", player.getDisplayName(), discId).withStyle(ChatFormatting.DARK_GREEN), true);
            }
        } else ctx.getSource().sendFailure(Component.translatable("eso.command.enchantmentnotfound", enchantmentEntry));
        return 1;
    }

    public static int grantAll(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        int successes = 0;
        if (player instanceof UnlockedEnchantmentHolder acc) {
            Object2IntOpenHashMap<Enchantment> enchantments = acc.enchantment_overhaul$getUnlockedEnchantments();
            for (Enchantment e : ForgeRegistries.ENCHANTMENTS) {
                enchantments.put(e, ESOCommon.getMaximumPossibleEnchantmentLevel(ctx.getSource().getServer().getRecipeManager(), e));
                successes++;
            }
            ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.grantall", player.getDisplayName()), true);
            player.sendSystemMessage(Component.translatable("eso.command.allknowledge").withStyle(ChatFormatting.GOLD));
        }
        return successes;
    }

    public static int revokeAll(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        int successes = 0;
        if (player instanceof UnlockedEnchantmentHolder acc) {
            successes = acc.enchantment_overhaul$getUnlockedEnchantments().size();
            acc.enchantment_overhaul$getUnlockedEnchantments().clear();
            ctx.getSource().sendSuccess(() -> Component.translatable("eso.command.feedback.revokeall", player.getDisplayName()), true);
            player.sendSystemMessage(Component.translatable("eso.command.lostallknowledge").withStyle(ChatFormatting.GOLD));
        }
        return successes;
    }
}
