package com.randomevents.event;

import net.minecraft.server.level.ServerPlayer;

/**
 * One random event. To add a new event: build it (see {@link EventBuilder}) and register it in
 * {@link PositiveEvents} or {@link NegativeEvents} - nothing else needs to change.
 */
public interface RandomEvent {
	String id();

	String displayName();

	String description();

	EventType type();

	/** Default duration in seconds (0 = instant event). Can be overridden in the config. */
	int defaultDurationSeconds();

	/** Called once when the event starts, on the server thread. */
	void start(ServerPlayer player, int durationTicks);

	/** How often (in ticks) {@link #tick} is called while active. 0 = never. */
	default int tickInterval() {
		return 0;
	}

	default void tick(ServerPlayer player, int elapsedTicks, int durationTicks) {
	}

	/** Called when the event expires, is stopped, or the player leaves. Must be idempotent. */
	default void end(ServerPlayer player) {
	}

	/** Called when a player with this event still active rejoins or respawns. */
	default void resume(ServerPlayer player, int remainingTicks) {
	}
}
