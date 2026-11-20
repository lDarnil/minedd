package com.randomevents.network;

import com.randomevents.config.EventConfig;
import com.randomevents.event.EventRegistry;
import com.randomevents.event.EventManager;
import com.randomevents.event.PlayerEventData;
import com.randomevents.event.RandomEvent;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Server side of the networking. The server is the only authority: clients just display what they
 * are told. Players that do not have the mod installed simply receive nothing.
 */
public final class NetworkHandler {
	private NetworkHandler() {
	}

	public static void registerPayloads() {
		PayloadTypeRegistry.clientboundPlay().register(SyncPayload.TYPE, SyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TriggerPayload.TYPE, TriggerPayload.CODEC);
	}

	public static void sendSync(ServerPlayer player, PlayerEventData data) {
		if (!ServerPlayNetworking.canSend(player, SyncPayload.TYPE)) {
			return;
		}
		List<SyncPayload.Entry> active = new ArrayList<>();
		data.active.forEach((id, a) -> {
			RandomEvent e = EventRegistry.get(id);
			if (e != null) {
				active.add(new SyncPayload.Entry(e.displayName(), e.description(), e.type().isPositive(),
					a.remainingTicks, a.totalTicks));
			}
		});
		RandomEvent last = data.lastEventId.isEmpty() ? null : EventRegistry.get(data.lastEventId);
		ServerPlayNetworking.send(player, new SyncPayload(
			EventManager.isRunning(), EventConfig.get().hudEnabled, Math.max(0, data.ticksUntilNext),
			last != null, last != null && last.type().isPositive(),
			last == null ? "" : last.displayName(), last == null ? "" : last.description(), active));
	}

	public static void sendTrigger(ServerPlayer player, RandomEvent event, int durationTicks) {
		if (!EventConfig.get().hudEnabled || !ServerPlayNetworking.canSend(player, TriggerPayload.TYPE)) {
			return;
		}
		ServerPlayNetworking.send(player, new TriggerPayload(event.displayName(), event.description(),
			event.type().isPositive(), durationTicks));
	}
}
