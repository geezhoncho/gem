package com.yourname.magi.item;

import com.yourname.magi.client.ClientDjinnDefinitions;
import com.yourname.magi.client.ClientDjinnState;
import com.yourname.magi.djinn.DjinnDefinition;
import com.yourname.magi.djinn.DjinnEquipmentHandler;
import com.yourname.magi.djinn.DjinnManager;
import com.yourname.magi.djinn.MagiDjinn;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Binding item. Phase 1: right-click binds the stored Djinn. Later phases add the binding ritual (altar),
 * an owner/encounter UUID in NBT, and a server-side claim ledger to prevent duplication.
 */
public class DjinnCoreItem extends Item {
    public static final String TAG = "Djinn";

    public DjinnCoreItem(Properties properties) {
        super(properties);
    }

    public static ItemStack forDjinn(Item core, ResourceLocation id) {
        ItemStack stack = new ItemStack(core);
        stack.getOrCreateTag().putString(TAG, id.toString());
        return stack;
    }

    @Nullable
    public static ResourceLocation getDjinn(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG, Tag.TAG_STRING)) return null;
        return ResourceLocation.tryParse(tag.getString(TAG));
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation id = getDjinn(stack);
        if (id == null) return super.getName(stack);
        return Component.translatable("item.magi.djinn_core.named", Component.translatable(MagiDjinn.nameKey(id)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = getDjinn(stack);
        if (id != null) {
            DjinnDefinition def = ClientDjinnDefinitions.get(id);
            if (def != null) {
                tooltip.add(Component.translatable("tooltip.magi.element",
                        Component.translatable("element.magi." + def.element()))
                        .withStyle(style -> style.withColor(def.color())));
            }
            if (ClientDjinnState.DATA.owns(id)) {
                tooltip.add(Component.translatable("tooltip.magi.djinn_core.bound").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        tooltip.add(Component.translatable("item.magi.djinn_core.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceLocation id = getDjinn(stack);
        if (id == null) return InteractionResultHolder.pass(stack);
        if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (DjinnManager.get(id) == null) {
            sp.displayClientMessage(Component.translatable("message.magi.djinn_unknown"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!DjinnEquipmentHandler.grant(sp, id)) {
            sp.displayClientMessage(Component.translatable("message.magi.djinn_already_bound"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        sp.displayClientMessage(Component.translatable("message.magi.djinn_bound",
                Component.translatable(MagiDjinn.nameKey(id))), true);
        level.playSound(null, sp.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
