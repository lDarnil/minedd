package com.randomevents.client;

import com.randomevents.network.SyncPayload;
import com.randomevents.network.TriggerPayload;

import java.util.List;

/**
 * Client copy of what the server told us. Only the server decides anything; this is display data.
 * Times are converted to absolute client timestamps on arrival so the HUD stays smooth between
 * the (once per second) sync packets.
 */
public final class ClientEventState {
	private ClientEventState() {
	}

	public record ActiveView(String name, String description, boolean positive, long endsAtMillis, long totalMillis) {
	}

	public static boolean running;
	public static boolean hudEnabled = true;
	public static long nextEventAtMillis;
	public static boolean hasLast;
	public static boolean lastPositive;
	public static String lastName = "";
	public static List<ActiveView> active = List.of();

	// centre-screen popup
	public static long popupStartMillis = -1;
	public static String popupName = "";
	public static String popupDescription = "";
	public static boolean popupPositive;

	public static void applySync(SyncPayload p) {
		long now = System.currentTimeMillis();
		running = p.running();
		hudEnabled = p.hudEnabled();
		nextEventAtMillis = now + p.ticksUntilNext() * 50L;
		hasLast = p.hasLast();
		lastPositive = p.lastPositive();
		lastName = p.lastName();
		active = p.active().stream()
			.map(e -> new ActiveView(e.name(), e.description(), e.positive(),
				now + e.remainingTicks() * 50L, e.totalTicks() * 50L))
			.toList();
	}

	public static void applyTrigger(TriggerPayload p) {
		popupStartMillis = System.currentTimeMillis();
		popupName = p.name();
		popupDescription = p.description();
		popupPositive = p.positive();
	}

	public static void reset() {
		running = false;
		hasLast = false;
		lastName = "";
		active = List.of();
		popupStartMillis = -1;
		nextEventAtMillis = 0;
	}
}
