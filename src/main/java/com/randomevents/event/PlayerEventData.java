package com.randomevents.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the mod remembers about one player. Persisted with the Fabric Data Attachment API
 * (saved inside the player's own save data, copied on death), so it survives relogs and restarts.
 */
public final class PlayerEventData {
	/** One entry of the history. */
	public record HistoryEntry(String eventId, boolean positive, long epochMillis, int durationSeconds) {
		public static final Codec<HistoryEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("event").forGetter(HistoryEntry::eventId),
			Codec.BOOL.fieldOf("positive").forGetter(HistoryEntry::positive),
			Codec.LONG.fieldOf("time").forGetter(HistoryEntry::epochMillis),
			Codec.INT.fieldOf("duration").forGetter(HistoryEntry::durationSeconds)
		).apply(i, HistoryEntry::new));
	}

	/** An event that is currently running for the player. */
	public static final class Active {
		public int remainingTicks;
		public final int totalTicks;

		public Active(int remainingTicks, int totalTicks) {
			this.remainingTicks = remainingTicks;
			this.totalTicks = totalTicks;
		}

		public int elapsedTicks() {
			return totalTicks - remainingTicks;
		}
	}

	public static final Codec<PlayerEventData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.fieldOf("ticks_until_next").forGetter(d -> d.ticksUntilNext),
		Codec.STRING.fieldOf("last_event").forGetter(d -> d.lastEventId),
		Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("active_remaining").forGetter(d -> {
			Map<String, Integer> m = new LinkedHashMap<>();
			d.active.forEach((k, v) -> m.put(k, v.remainingTicks));
			return m;
		}),
		Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("active_total").forGetter(d -> {
			Map<String, Integer> m = new LinkedHashMap<>();
			d.active.forEach((k, v) -> m.put(k, v.totalTicks));
			return m;
		}),
		HistoryEntry.CODEC.listOf().fieldOf("history").forGetter(d -> new ArrayList<>(d.history))
	).apply(i, PlayerEventData::fromCodec));

	public int ticksUntilNext;
	public String lastEventId = "";
	public final Map<String, Active> active = new LinkedHashMap<>();
	public final List<HistoryEntry> history = new ArrayList<>(); // newest last
	/** Runtime only. */
	public int syncCountdown;

	public PlayerEventData(int ticksUntilNext) {
		this.ticksUntilNext = ticksUntilNext;
	}

	private static PlayerEventData fromCodec(int next, String last, Map<String, Integer> remaining,
											Map<String, Integer> total, List<HistoryEntry> history) {
		PlayerEventData data = new PlayerEventData(next);
		data.lastEventId = last;
		remaining.forEach((id, rem) -> data.active.put(id, new Active(rem, total.getOrDefault(id, rem))));
		data.history.addAll(history);
		return data;
	}
}
