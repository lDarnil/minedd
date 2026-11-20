package com.randomevents.event;

import com.mojang.serialization.Codec;
import com.randomevents.RandomEventsMod;
import com.randomevents.config.EventConfig;
import com.randomevents.network.NetworkHandler;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Central, server-authoritative logic: per-player state, triggering, ending and persistence.
 * All state lives in one map keyed by player UUID, so dimension changes and respawns (new
 * ServerPlayer objects) never reset anything.
 */
public final class EventManager {
	private EventManager() {
	}

	/** Persistent storage inside each player's save data (Fabric Data Attachment API). */
	public static final AttachmentType<PlayerEventData> ATTACHMENT = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath("random_events", "player_data"),
		builder -> builder.persistent(PlayerEventData.CODEC));

	private static final Map<UUID, PlayerEventData> DATA = new HashMap<>();
	private static boolean running = false;

	public static boolean isRunning() {
		return running;
	}

	public static PlayerEventData peek(ServerPlayer player) {
		return DATA.get(player.getUUID());
	}

	public static PlayerEventData data(ServerPlayer player) {
		return DATA.computeIfAbsent(player.getUUID(), id -> new PlayerEventData(intervalTicks()));
	}

	private static int intervalTicks() {
		return EventConfig.get().intervalSeconds * 20;
	}

	// ------------------------------------------------------------------ lifecycle

	public static void onServerStarted(MinecraftServer server) {
		running = EventConfig.get().autoStart;
		RandomEventsMod.LOGGER.info("Random Events system is {}.", running ? "RUNNING" : "STOPPED (use /event start)");
	}

	/** Called on SERVER_STOPPING: persist, undo every active effect, and drop all static state. */
	public static void onServerStopping(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			onLeave(player);
		}
		DATA.clear();
		InventoryLock.clear();
		running = false;
	}

	public static void onJoin(ServerPlayer player) {
		PlayerEventData stored = player.getAttached(ATTACHMENT);
		PlayerEventData data;
		if (stored != null) {
			data = stored;
			if (data.ticksUntilNext <= 0) {
				data.ticksUntilNext = intervalTicks();
			}
		} else {
			data = new PlayerEventData(intervalTicks()); // timer starts when the player first joins
		}
		DATA.put(player.getUUID(), data);
		// events that were still running when the player left continue with the remaining time
		for (Map.Entry<String, PlayerEventData.Active> e : new ArrayList<>(data.active.entrySet())) {
			RandomEvent event = EventRegistry.get(e.getKey());
			if (event == null) {
				data.active.remove(e.getKey());
				continue;
			}
			safely(() -> event.resume(player, e.getValue().remainingTicks), event, "resume");
		}
		sync(player);
	}

	public static void onLeave(ServerPlayer player) {
		PlayerEventData data = DATA.remove(player.getUUID());
		if (data == null) {
			return;
		}
		// undo transient state (attribute modifiers, inventory lock, ...) but keep the remaining time
		for (String id : data.active.keySet()) {
			RandomEvent event = EventRegistry.get(id);
			if (event != null) {
				safely(() -> event.end(player), event, "end");
			}
		}
		player.setAttached(ATTACHMENT, data);
	}

	/** Autosave: copy the live data into every online player's attachment. */
	public static void saveAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			PlayerEventData data = DATA.get(player.getUUID());
			if (data != null) {
				player.setAttached(ATTACHMENT, data);
			}
		}
	}

	/** A real death ends all running events (like vanilla potions); the next-event timer is kept. */
	public static void onRespawn(ServerPlayer newPlayer, boolean alive) {
		PlayerEventData data = DATA.get(newPlayer.getUUID());
		if (data == null || alive) {
			return; // returning from the End is not a death
		}
		endAll(newPlayer, data);
		sync(newPlayer);
	}

	// ------------------------------------------------------------------ system control

	public static void start(MinecraftServer server) {
		running = true;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			sync(p);
		}
	}

	public static void stop(MinecraftServer server) {
		running = false;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			PlayerEventData data = DATA.get(p.getUUID());
			if (data != null) {
				endAll(p, data);
			}
			sync(p);
		}
	}

	public static void reload(MinecraftServer server) {
		EventConfig.load();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			sync(p);
		}
	}

	// ------------------------------------------------------------------ events

	public static void trigger(ServerPlayer player, RandomEvent event) {
		PlayerEventData data = data(player);
		int ticks = EventConfig.get().durationTicks(event);

		PlayerEventData.Active previous = data.active.remove(event.id());
		if (previous != null) {
			safely(() -> event.end(player), event, "end");
		}
		if (!safely(() -> event.start(player, ticks), event, "start")) {
			return;
		}
		if (ticks > 0) {
			data.active.put(event.id(), new PlayerEventData.Active(ticks, ticks));
		}
		data.lastEventId = event.id();
		EventHistory.record(data, event, ticks / 20);

		if (EventConfig.get().messagesEnabled) {
			ChatFormatting color = event.type().isPositive() ? ChatFormatting.GREEN : ChatFormatting.RED;
			player.sendSystemMessage(Component.literal("[Random Events] ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal(event.displayName()).withStyle(color, ChatFormatting.BOLD))
				.append(Component.literal(" - " + event.description()).withStyle(ChatFormatting.GRAY)));
		}
		NetworkHandler.sendTrigger(player, event, ticks);
		sync(player);
	}

	/** Picks a random event of the given type (or any type if null), never the previous one. */
	public static RandomEvent pick(ServerPlayer player, EventType forcedType) {
		PlayerEventData data = data(player);
		EventConfig cfg = EventConfig.get();
		EventType type = forcedType != null ? forcedType
			: (player.getRandom().nextInt(100) < cfg.positiveChancePercent ? EventType.POSITIVE : EventType.NEGATIVE);

		RandomEvent chosen = choose(player, type, data.lastEventId);
		if (chosen == null && forcedType == null) {
			chosen = choose(player, type == EventType.POSITIVE ? EventType.NEGATIVE : EventType.POSITIVE, data.lastEventId);
		}
		return chosen;
	}

	private static RandomEvent choose(ServerPlayer player, EventType type, String lastId) {
		List<RandomEvent> candidates = new ArrayList<>();
		for (RandomEvent e : EventRegistry.ofType(type)) {
			if (EventConfig.get().isEnabled(e) && !e.id().equals(lastId)) {
				candidates.add(e);
			}
		}
		return candidates.isEmpty() ? null : candidates.get(player.getRandom().nextInt(candidates.size()));
	}

	public static void endAll(ServerPlayer player, PlayerEventData data) {
		Iterator<Map.Entry<String, PlayerEventData.Active>> it = data.active.entrySet().iterator();
		while (it.hasNext()) {
			RandomEvent event = EventRegistry.get(it.next().getKey());
			it.remove();
			if (event != null) {
				safely(() -> event.end(player), event, "end");
			}
		}
	}

	public static void sync(ServerPlayer player) {
		PlayerEventData data = DATA.get(player.getUUID());
		if (data != null) {
			NetworkHandler.sendSync(player, data);
		}
	}

	/** An event must never be able to crash the server tick. */
	static boolean safely(Runnable action, RandomEvent event, String phase) {
		try {
			action.run();
			return true;
		} catch (Exception ex) {
			RandomEventsMod.LOGGER.error("Event '{}' failed during {}", event.id(), phase, ex);
			return false;
		}
	}
}
