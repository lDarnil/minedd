package com.randomevents.event;

import com.randomevents.config.EventConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Records and formats the per-player event history. */
public final class EventHistory {
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

	private EventHistory() {
	}

	public static void record(PlayerEventData data, RandomEvent event, int durationSeconds) {
		data.history.add(new PlayerEventData.HistoryEntry(event.id(), event.type().isPositive(),
			System.currentTimeMillis(), durationSeconds));
		// keep a little more than shown so lowering/raising the limit in the config still works
		int keep = Math.max(EventConfig.get().maxHistoryEntries, 10);
		while (data.history.size() > keep) {
			data.history.remove(0);
		}
	}

	private static String ago(long millis) {
		long s = Math.max(0, (System.currentTimeMillis() - millis) / 1000);
		return s < 60 ? s + "s" : (s / 60) + "m " + (s % 60) + "s";
	}

	/** Chat lines: last N events (newest first) and the currently active ones. */
	public static List<Component> render(PlayerEventData data) {
		int shown = EventConfig.get().maxHistoryEntries;
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal("=== Event History (ultimele " + shown + ") ===").withStyle(ChatFormatting.GOLD));
		if (data.history.isEmpty()) {
			lines.add(Component.literal("  Niciun eveniment inca.").withStyle(ChatFormatting.GRAY));
		}
		int from = Math.max(0, data.history.size() - shown);
		for (int i = data.history.size() - 1; i >= from; i--) {
			PlayerEventData.HistoryEntry h = data.history.get(i);
			RandomEvent e = EventRegistry.get(h.eventId());
			String name = e == null ? h.eventId() : e.displayName();
			ChatFormatting color = h.positive() ? ChatFormatting.GREEN : ChatFormatting.RED;
			MutableComponent line = Component.literal("  [" + CLOCK.format(Instant.ofEpochMilli(h.epochMillis())) + "] ")
				.withStyle(ChatFormatting.GRAY)
				.append(Component.literal(name).withStyle(color))
				.append(Component.literal(" - " + (h.positive() ? "POZITIV" : "NEGATIV")
					+ " (acum " + ago(h.epochMillis()) + ")").withStyle(ChatFormatting.GRAY));
			lines.add(line);
		}
		lines.add(Component.literal("Evenimente active:").withStyle(ChatFormatting.GOLD));
		if (data.active.isEmpty()) {
			lines.add(Component.literal("  Niciunul.").withStyle(ChatFormatting.GRAY));
		}
		data.active.forEach((id, a) -> {
			RandomEvent e = EventRegistry.get(id);
			if (e == null) {
				return;
			}
			ChatFormatting color = e.type().isPositive() ? ChatFormatting.GREEN : ChatFormatting.RED;
			lines.add(Component.literal("  " + e.displayName()).withStyle(color)
				.append(Component.literal(" - " + (a.remainingTicks / 20) + "s ramase").withStyle(ChatFormatting.GRAY)));
		});
		return lines;
	}
}
