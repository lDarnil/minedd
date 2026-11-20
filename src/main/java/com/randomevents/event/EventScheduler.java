package com.randomevents.event;

import com.randomevents.config.EventConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;

/**
 * Runs once per server tick. Per player and per tick it only does integer decrements; heavy work
 * (event ticks) happens at each event's own interval, and the client sync once per second.
 */
public final class EventScheduler {
	private EventScheduler() {
	}

	public static void tick(MinecraftServer server) {
		boolean running = EventManager.isRunning();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			PlayerEventData data = EventManager.peek(player);
			if (data == null) {
				continue;
			}
			if (running) {
				tickActive(player, data);
				if (--data.ticksUntilNext <= 0) {
					data.ticksUntilNext = EventConfig.get().intervalSeconds * 20;
					RandomEvent next = EventManager.pick(player, null);
					if (next != null) {
						EventManager.trigger(player, next);
					}
				}
			}
			if (--data.syncCountdown <= 0) {
				data.syncCountdown = 20;
				EventManager.sync(player);
			}
		}
	}

	private static void tickActive(ServerPlayer player, PlayerEventData data) {
		if (data.active.isEmpty()) {
			return;
		}
		boolean changed = false;
		Iterator<Map.Entry<String, PlayerEventData.Active>> it = data.active.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, PlayerEventData.Active> entry = it.next();
			PlayerEventData.Active active = entry.getValue();
			RandomEvent event = EventRegistry.get(entry.getKey());
			active.remainingTicks--;
			if (event == null) {
				it.remove();
				changed = true;
				continue;
			}
			int interval = event.tickInterval();
			if (interval > 0 && active.remainingTicks > 0 && active.elapsedTicks() % interval == 0) {
				EventManager.safely(() -> event.tick(player, active.elapsedTicks(), active.totalTicks), event, "tick");
			}
			if (active.remainingTicks <= 0) {
				it.remove();
				EventManager.safely(() -> event.end(player), event, "end");
				changed = true;
				if (EventConfig.get().messagesEnabled) {
					player.sendSystemMessage(Component.literal("[Random Events] ").withStyle(ChatFormatting.GOLD)
						.append(Component.literal(event.displayName() + " s-a terminat.").withStyle(ChatFormatting.GRAY)));
				}
			}
		}
		if (changed) {
			EventManager.sync(player);
		}
	}
}
