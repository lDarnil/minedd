package com.randomevents.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.randomevents.RandomEventsMod;
import com.randomevents.event.EventRegistry;
import com.randomevents.event.RandomEvent;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON configuration stored in {@code config/random_events.json}.
 * Missing events/fields are filled with defaults and written back, so new events added in
 * the future appear in the file automatically.
 */
public final class EventConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("random_events.json");

	/** Seconds between two events for each player (default 300 = 5 minutes). */
	public int intervalSeconds = 300;
	/** Chance (0-100) that the next event is positive. The rest is negative. */
	public int positiveChancePercent = 50;
	/** Should the system start automatically when the server starts. */
	public boolean autoStart = true;
	public boolean hudEnabled = true;
	/** Chat messages when an event starts/ends. */
	public boolean messagesEnabled = true;
	/** Maximum number of entries kept in the history (shown by /event history). */
	public int maxHistoryEntries = 10;
	public Map<String, EventSettings> events = new LinkedHashMap<>();

	public static final class EventSettings {
		public boolean enabled = true;
		public int durationSeconds;

		public EventSettings() {
		}

		public EventSettings(int durationSeconds) {
			this.durationSeconds = durationSeconds;
		}
	}

	private static EventConfig current = new EventConfig();

	public static EventConfig get() {
		return current;
	}

	/** Loads (or creates) the config file. Safe to call repeatedly (/event reload). */
	public static synchronized void load() {
		EventConfig loaded = null;
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				loaded = GSON.fromJson(reader, EventConfig.class);
			} catch (Exception e) {
				RandomEventsMod.LOGGER.error("Could not read {} - using defaults for this session.", PATH, e);
			}
		}
		if (loaded == null) {
			loaded = new EventConfig();
		}
		loaded.sanitize();
		current = loaded;
		save();
	}

	private void sanitize() {
		intervalSeconds = Math.max(5, intervalSeconds);
		positiveChancePercent = Math.max(0, Math.min(100, positiveChancePercent));
		maxHistoryEntries = Math.max(1, Math.min(100, maxHistoryEntries));
		if (events == null) {
			events = new LinkedHashMap<>();
		}
		Map<String, EventSettings> merged = new LinkedHashMap<>();
		for (RandomEvent event : EventRegistry.all()) {
			EventSettings settings = events.get(event.id());
			if (settings == null) {
				settings = new EventSettings(event.defaultDurationSeconds());
			} else if (settings.durationSeconds < 0) {
				settings.durationSeconds = event.defaultDurationSeconds();
			}
			merged.put(event.id(), settings);
		}
		events = merged;
	}

	public static synchronized void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(current, writer);
			}
		} catch (IOException e) {
			RandomEventsMod.LOGGER.error("Could not write {}", PATH, e);
		}
	}

	public boolean isEnabled(RandomEvent event) {
		EventSettings s = events.get(event.id());
		return s == null || s.enabled;
	}

	public int durationTicks(RandomEvent event) {
		EventSettings s = events.get(event.id());
		int seconds = s == null ? event.defaultDurationSeconds() : s.durationSeconds;
		return Math.max(0, seconds) * 20;
	}
}
