package aiefu.eso.menu;

import aiefu.eso.ESOCommon;
import aiefu.eso.Utils;
import aiefu.eso.data.enchantability.EnchantabilityData;
import aiefu.eso.data.enchantability.EnchantabilityOverrides;
import aiefu.eso.recipe.EnchantmentRecipe;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OverhauledEnchantmentMenu extends AbstractContainerMenu {
    public static final ResourceLocation LAZURITE_EMPTY_ICON = new ResourceLocation("item/empty_slot_lapis_lazuli");
    public static final ResourceLocation SWORD_EMPTY_ICON = new ResourceLocation("item/empty_slot_sword");
    public static final ResourceLocation INGOT_EMPTY_ICON = new ResourceLocation("item/empty_slot_ingot");

    private final ContainerLevelAccess access;

    public final Object2IntOpenHashMap<Enchantment> allEnchantments = new Object2IntOpenHashMap<>();
    public final Object2IntOpenHashMap<Enchantment> enchantments = new Object2IntOpenHashMap<>();
    public final Object2IntOpenHashMap<Enchantment> curses = new Object2IntOpenHashMap<>();

    private final SimpleContainer tableInv;

    public OverhauledEnchantmentMenu(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, ContainerLevelAccess.NULL);
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(buf.readResourceLocation());
            int level = buf.readVarInt();
            if (enchantment != null) {
                this.allEnchantments.put(enchantment, level);
                if (enchantment.isCurse()) {
                    this.curses.put(enchantment, level);
                } else {
                    this.enchantments.put(enchantment, level);
                }
            }
        }
    }

    public OverhauledEnchantmentMenu(int syncId, Inventory inventory, ContainerLevelAccess access) {
        super(ESOCommon.ENCHANTMENT_MENU.get(), syncId);
        this.access = access;
        this.tableInv = new SimpleContainer(5) {
            @Override
            public void setChanged() {
                super.setChanged();
                OverhauledEnchantmentMenu.this.slotsChanged(this);
            }
        };

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 9 + column * 18, 99 + row * 18));
            }
        }

        for (int slot = 0; slot < 9; ++slot) {
            this.addSlot(new Slot(inventory, slot, 9 + slot * 18, 157));
        }

        this.addSlot(new Slot(this.tableInv, 0, 24, 31) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem().isEnchantable(stack) || stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, SWORD_EMPTY_ICON);
            }
        });

        this.addSlot(new Slot(this.tableInv, 1, 42, 31) {
            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, LAZURITE_EMPTY_ICON);
            }
        });

        for (int slot = 0; slot < 3; slot++) {
            this.addSlot(new Slot(this.tableInv, slot + 2, 15 + slot * 18, 49) {
                @Override
                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of(InventoryMenu.BLOCK_ATLAS, INGOT_EMPTY_ICON);
                }
            });
        }
    }

    public void checkRequirementsAndConsume(ResourceLocation enchantmentId, Player player, int ordinal) {
        this.access.execute((level, blockPos) -> {
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
            EnchantContext context = this.getContext();
            if (enchantment == null || !this.canShowEnchant(player, context, enchantment)) {
                return;
            }

            int targetLevel = this.getTargetLevel(context, enchantment);
            EnchantmentRecipe recipe = this.resolveRecipe(level.getRecipeManager(), enchantmentId, ordinal);

            if (recipe != null) {
                if (targetLevel > recipe.getMaxLevel(enchantment) || !recipe.checkAndConsume(this.tableInv, targetLevel, player)) {
                    return;
                }
            } else if (ordinal != -1 || !player.getAbilities().instabuild || targetLevel > enchantment.getMaxLevel()) {
                return;
            }

            LinkedHashMap<Enchantment, Integer> appliedEnchantments = new LinkedHashMap<>(context.appliedEnchantments());
            appliedEnchantments.put(enchantment, targetLevel);
            this.applyAndBroadcast(player, appliedEnchantments, context.stack(), context.book());
        });
    }

    public List<DisplayOption> getDisplayOptions(RecipeManager recipeManager, Player player) {
        EnchantContext context = this.getContext();
        if (!context.enchantableTarget()) {
            return List.of();
        }

        ArrayList<DisplayOption> options = new ArrayList<>();
        this.collectDisplayOptions(options, recipeManager, player, context, this.enchantments);
        this.collectDisplayOptions(options, recipeManager, player, context, this.curses);
        return options;
    }

    public int getCurrentEnchantmentCount() {
        EnchantContext context = this.getContext();
        return Utils.getCurrentLimit(context.appliedEnchantments().size(), context.curseCount());
    }

    public int getEnchantmentLimit() {
        EnchantContext context = this.getContext();
        if (!context.enchantableTarget()) {
            return 0;
        }
        return Utils.getEnchantmentsLimit(context.curseCount(), context.enchantabilityData());
    }

    public SimpleContainer getTableInv() {
        return this.tableInv;
    }

    public void applyAndBroadcast(Player player, Map<Enchantment, Integer> enchantments, ItemStack stack, boolean book) {
        if (book) {
            if (stack.is(Items.BOOK)) {
                stack = new ItemStack(Items.ENCHANTED_BOOK);
                this.tableInv.setItem(0, stack);
            }

            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(entry.getKey(), entry.getValue()));
            }
        } else {
            EnchantmentHelper.setEnchantments(enchantments, stack);
        }

        player.onEnchantmentPerformed(stack, 0);
        this.tableInv.setChanged();
        this.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, blockPos) -> this.clearContainer(player, this.tableInv));
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            if (slotIndex > 35 && !this.moveItemStackTo(stack, 0, 35, false)) {
                return ItemStack.EMPTY;
            }

            ItemStack singleItem = stack.copyWithCount(1);
            if (!this.slots.get(36).hasItem() && this.slots.get(36).mayPlace(singleItem)) {
                stack.shrink(1);
                this.slots.get(36).setByPlayer(singleItem);
                returnStack = ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 37, 41, false)) {
                return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == returnStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return returnStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Blocks.ENCHANTING_TABLE);
    }

    private void collectDisplayOptions(List<DisplayOption> options,
                                       RecipeManager recipeManager,
                                       Player player,
                                       EnchantContext context,
                                       Object2IntOpenHashMap<Enchantment> source) {
        for (Enchantment enchantment : source.keySet()) {
            if (!this.canShowEnchant(player, context, enchantment)) {
                continue;
            }

            int targetLevel = this.getTargetLevel(context, enchantment);
            ResourceLocation enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            if (enchantmentId == null) {
                continue;
            }

            List<ResolvedRecipe> displayedRecipes = this.resolveDisplayedRecipes(recipeManager, enchantmentId, enchantment, targetLevel);
            if (displayedRecipes.isEmpty()) {
                if ((player.getAbilities().instabuild || !ESOCommon.CONFIG.hideEnchantmentsWithoutRecipe.get()) && targetLevel <= enchantment.getMaxLevel()) {
                    options.add(new DisplayOption(enchantment, targetLevel, null, -1, player.getAbilities().instabuild));
                }
                continue;
            }

            for (ResolvedRecipe resolvedRecipe : displayedRecipes) {
                options.add(new DisplayOption(
                        enchantment,
                        targetLevel,
                        resolvedRecipe.recipe(),
                        resolvedRecipe.ordinal(),
                        this.canAffordRecipe(player, enchantment, targetLevel, resolvedRecipe.recipe())
                ));
            }
        }
    }

    private boolean canShowEnchant(Player player, EnchantContext context, Enchantment enchantment) {
        if (enchantment.isCurse() && !ESOCommon.CONFIG.enableCursesAmplifier.get()) {
            return false;
        }

        int targetLevel = this.getTargetLevel(context, enchantment);
        if (!this.passesDiscovery(player, enchantment, targetLevel)) {
            return false;
        }
        if (!this.passesCompatibility(context, enchantment)) {
            return false;
        }
        return this.passesMaterialRules(context, enchantment);
    }

    private boolean canAffordRecipe(Player player, Enchantment enchantment, int targetLevel, @Nullable EnchantmentRecipe recipe) {
        if (player.getAbilities().instabuild && targetLevel <= enchantment.getMaxLevel()) {
            return true;
        }
        return recipe != null && recipe.check(this.tableInv, targetLevel, player);
    }

    private boolean passesDiscovery(Player player, Enchantment enchantment, int targetLevel) {
        if (player.getAbilities().instabuild || ESOCommon.CONFIG.disableDiscoverySystem.get()) {
            return true;
        }

        int knownLevel = this.allEnchantments.getInt(enchantment);
        if (knownLevel <= 0) {
            return false;
        }

        return !ESOCommon.CONFIG.enableEnchantmentsLeveling.get() || targetLevel <= knownLevel;
    }

    private boolean passesCompatibility(EnchantContext context, Enchantment target) {
        if (context.book()) {
            return true;
        }
        if (!target.canEnchant(context.stack())) {
            return false;
        }

        for (Enchantment enchantment : context.appliedEnchantments().keySet()) {
            if (enchantment != target && !enchantment.isCompatibleWith(target)) {
                return false;
            }
        }
        return true;
    }

    private boolean passesMaterialRules(EnchantContext context, Enchantment target) {
        if (context.appliedEnchantments().containsKey(target)) {
            return true;
        }

        if (target.isCurse()) {
            return context.curseCount() < context.enchantabilityData().getMaxCurses();
        }

        return Utils.getCurrentLimit(context.appliedEnchantments().size(), context.curseCount())
                < Utils.getEnchantmentsLimit(context.curseCount(), context.enchantabilityData());
    }

    private int getTargetLevel(EnchantContext context, Enchantment enchantment) {
        return context.appliedEnchantments().getOrDefault(enchantment, 0) + 1;
    }

    private EnchantContext getContext() {
        ItemStack stack = this.tableInv.getItem(0);
        boolean book = isBook(stack);
        boolean enchantableTarget = !stack.isEmpty() && (book || stack.getItem().isEnchantable(stack));
        Map<Enchantment, Integer> appliedEnchantments = enchantableTarget ? EnchantmentHelper.getEnchantments(stack) : Map.of();
        int curseCount = 0;
        for (Enchantment enchantment : appliedEnchantments.keySet()) {
            if (enchantment.isCurse()) {
                curseCount++;
            }
        }

        EnchantabilityData enchantabilityData = enchantableTarget ? Utils.getEnchantabilityData(stack) : EnchantabilityOverrides.getDefaultEnchantabilityData();
        return new EnchantContext(stack, appliedEnchantments, curseCount, enchantabilityData, book, enchantableTarget);
    }

    private List<ResolvedRecipe> resolveDisplayedRecipes(RecipeManager recipeManager,
                                                         ResourceLocation enchantmentId,
                                                         Enchantment enchantment,
                                                         int targetLevel) {
        List<EnchantmentRecipe> recipes = ESOCommon.getRecipes(recipeManager, enchantmentId);
        ArrayList<ResolvedRecipe> displayedRecipes = new ArrayList<>();

        if (!recipes.isEmpty()) {
            for (int ordinal = 0; ordinal < recipes.size(); ordinal++) {
                EnchantmentRecipe recipe = recipes.get(ordinal);
                if (recipe.getLevel(targetLevel) != null && targetLevel <= recipe.getMaxLevel(enchantment)) {
                    displayedRecipes.add(new ResolvedRecipe(recipe, ordinal));
                }
            }
        }

        return displayedRecipes;
    }

    @Nullable
    private EnchantmentRecipe resolveRecipe(RecipeManager recipeManager, ResourceLocation enchantmentId, int ordinal) {
        if (ordinal < 0) {
            return ESOCommon.CONFIG.enableDefaultRecipe.get() ? ESOCommon.defaultRecipe : null;
        }

        List<EnchantmentRecipe> recipes = ESOCommon.getRecipes(recipeManager, enchantmentId);
        return ordinal < recipes.size() ? recipes.get(ordinal) : null;
    }

    private static boolean isBook(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
    }

    private record EnchantContext(ItemStack stack,
                                  Map<Enchantment, Integer> appliedEnchantments,
                                  int curseCount,
                                  EnchantabilityData enchantabilityData,
                                  boolean book,
                                  boolean enchantableTarget) {
    }

    public record DisplayOption(Enchantment enchantment,
                                int targetLevel,
                                @Nullable EnchantmentRecipe recipe,
                                int ordinal,
                                boolean affordableNow) {
    }

    private record ResolvedRecipe(@Nullable EnchantmentRecipe recipe, int ordinal) {
    }
}
