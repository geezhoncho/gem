package com.yourname.magi.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.yourname.magi.MagiMod;
import com.yourname.magi.capability.MagiCapabilities;
import com.yourname.magi.djinn.DjinnDefinition;
import com.yourname.magi.djinn.DjinnEquipmentHandler;
import com.yourname.magi.djinn.DjinnManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.stream.Collectors;

/**
 * Admin/test commands (op level 2):
 * /magi djinn grant|revoke|equip <player> <djinn>, unequip|list <player>, setlevel <player> <djinn> <level>, info <djinn>
 */
@Mod.EventBusSubscriber(modid = MagiMod.MODID)
public final class MagiCommands {
    private static final SuggestionProvider<CommandSourceStack> DJINN_IDS =
            (ctx, builder) -> SharedSuggestionProvider.suggestResource(DjinnManager.ids(), builder);

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> djinnArg() {
        return Commands.argument("djinn", ResourceLocationArgument.id()).suggests(DJINN_IDS);
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("magi")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("djinn")
                        .then(Commands.literal("grant").then(Commands.argument("player", EntityArgument.player())
                                .then(djinnArg().executes(MagiCommands::grant))))
                        .then(Commands.literal("revoke").then(Commands.argument("player", EntityArgument.player())
                                .then(djinnArg().executes(MagiCommands::revoke))))
                        .then(Commands.literal("equip").then(Commands.argument("player", EntityArgument.player())
                                .then(djinnArg().executes(MagiCommands::equip))))
                        .then(Commands.literal("unequip").then(Commands.argument("player", EntityArgument.player())
                                .executes(MagiCommands::unequip)))
                        .then(Commands.literal("setlevel").then(Commands.argument("player", EntityArgument.player())
                                .then(djinnArg().then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                        .executes(MagiCommands::setLevel)))))
                        .then(Commands.literal("list").then(Commands.argument("player", EntityArgument.player())
                                .executes(MagiCommands::list)))
                        .then(Commands.literal("info").then(djinnArg().executes(MagiCommands::info)))));
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendFailure(Component.literal(msg));
        return 0;
    }

    private static int ok(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }

    private static int grant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "djinn");
        if (DjinnManager.get(id) == null) return fail(ctx, "Unknown Djinn: " + id);
        if (!DjinnEquipmentHandler.grant(target, id)) return fail(ctx, target.getName().getString() + " already owns " + id);
        return ok(ctx, "Bound " + id + " to " + target.getName().getString());
    }

    private static int revoke(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "djinn");
        if (!DjinnEquipmentHandler.revoke(target, id)) return fail(ctx, target.getName().getString() + " does not own " + id);
        return ok(ctx, "Removed " + id + " from " + target.getName().getString());
    }

    private static int equip(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "djinn");
        if (!DjinnEquipmentHandler.equip(target, id)) return fail(ctx, "Cannot equip " + id + " (unknown or not owned)");
        return ok(ctx, "Equipped " + id);
    }

    private static int unequip(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        DjinnEquipmentHandler.equip(EntityArgument.getPlayer(ctx, "player"), null);
        return ok(ctx, "Unequipped Djinn");
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "djinn");
        int level = IntegerArgumentType.getInteger(ctx, "level");
        if (!DjinnEquipmentHandler.setLevel(target, id, level)) return fail(ctx, target.getName().getString() + " does not own " + id);
        return ok(ctx, id + " set to level " + level + " (capped at the Djinn's max level)");
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        MagiCapabilities.get(target).ifPresent(data -> {
            String owned = data.ownedIds().isEmpty() ? "(none)" : data.ownedIds().stream()
                    .map(id -> id + " Lv" + data.level(id)).collect(Collectors.joining(", "));
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Owned: " + owned + " | Equipped: " + data.getEquipped()), false);
        });
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "djinn");
        DjinnDefinition def = DjinnManager.get(id);
        if (def == null) return fail(ctx, "Unknown Djinn: " + id);
        String attrs = def.attributes().stream()
                .map(b -> b.attribute() + " " + b.base() + (b.perLevel() != 0 ? " (+" + b.perLevel() + "/lv)" : ""))
                .collect(Collectors.joining(", "));
        ctx.getSource().sendSuccess(() -> Component.literal(id + ": element " + def.element() + ", max level " + def.maxLevel()
                + ", energy " + def.maxEnergy() + "\nBonuses: " + attrs + "\nAbilities: " + def.abilities()), false);
        return 1;
    }

    private MagiCommands() {}
}
