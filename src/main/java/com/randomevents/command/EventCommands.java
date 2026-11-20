package com.randomevents.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.randomevents.config.EventConfig;
import com.randomevents.event.EventHistory;
import com.randomevents.event.EventManager;
import com.randomevents.event.EventType;
import com.randomevents.event.PlayerEventData;
import com.randomevents.event.RandomEvent;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * /event start | stop | next | positive | negative | reload   (administrators)
 * /event history                                              (every player, own history)
 *
 * Permission: the Fabric Permission API node "random_events.admin" is checked first (works with
 * permission mods such as LuckPerms); without one, operators (game-master level) are allowed.
 */
public final class EventCommands {
	private EventCommands() {
	}

	private static final Identifier ADMIN = Identifier.fromNamespaceAndPath("random_events", "admin");

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("event")
			.then(Commands.literal("start")
				.requires(PermissionPredicates.require(ADMIN, PermissionLevel.GAMEMASTERS))
				.executes(ctx -> {
					EventManager.start(ctx.getSource().getServer());
					return ok(ctx, "Sistemul Random Events a fost PORNIT.");
				}))
			.then(Commands.literal("stop")
				.requires(PermissionPredicates.require(ADMIN, PermissionLevel.GAMEMASTERS))
				.executes(ctx -> {
					EventManager.stop(ctx.getSource().getServer());
					return ok(ctx, "Sistemul Random Events a fost OPRIT (evenimentele active au fost terminate).");
				}))
			.then(Commands.literal("next")
				.requires(PermissionPredicates.require(ADMIN, PermissionLevel.GAMEMASTERS))
				.executes(ctx -> trigger(ctx, null)))
			.then(Commands.literal("positive")
				.requires(PermissionPredicates.require(ADMIN, PermissionLevel.GAMEMASTERS))
				.executes(ctx -> trigger(ctx, EventType.POSITIVE)))
			.then(Commands.literal("negative")
				.requires(PermissionPredicates.require(ADMIN, PermissionLevel.GAMEMASTERS))
				.executes(ctx -> trigger(ctx, EventType.NEGATIVE)))
			.then(Commands.literal("reload")
				.requires(PermissionPredicates.require(ADMIN, PermissionLevel.GAMEMASTERS))
				.executes(ctx -> {
					EventManager.reload(ctx.getSource().getServer());
					return ok(ctx, "Configuratia a fost reincarcata din config/random_events.json.");
				}))
			.then(Commands.literal("history")
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayer();
					if (player == null) {
						ctx.getSource().sendSystemMessage(Component.literal("Comanda poate fi folosita doar de un jucator.")
							.withStyle(ChatFormatting.RED));
						return 0;
					}
					PlayerEventData data = EventManager.data(player);
					for (Component line : EventHistory.render(data)) {
						player.sendSystemMessage(line);
					}
					return 1;
				})));
	}

	private static int ok(CommandContext<CommandSourceStack> ctx, String message) {
		ctx.getSource().sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	/** "next/positive/negative" act on the player who ran the command, ignoring the timer. */
	private static int trigger(CommandContext<CommandSourceStack> ctx, EventType type) {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			ctx.getSource().sendSystemMessage(Component.literal("Comanda poate fi folosita doar de un jucator.")
				.withStyle(ChatFormatting.RED));
			return 0;
		}
		RandomEvent event = EventManager.pick(player, type);
		if (event == null) {
			ctx.getSource().sendSystemMessage(Component.literal(
				"Niciun eveniment disponibil (verifica config/random_events.json).").withStyle(ChatFormatting.RED));
			return 0;
		}
		EventManager.trigger(player, event);
		return 1;
	}
}
