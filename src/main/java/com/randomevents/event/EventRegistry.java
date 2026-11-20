package com.randomevents.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Holds all registered events. Registration happens once at startup. */
public final class EventRegistry {
	private static final Map<String, RandomEvent> EVENTS = new LinkedHashMap<>();

	private EventRegistry() {
	}

	public static void register(RandomEvent event) {
		if (EVENTS.putIfAbsent(event.id(), event) != null) {
			throw new IllegalStateException("Duplicate event id: " + event.id());
		}
	}

	public static RandomEvent get(String id) {
		return EVENTS.get(id);
	}

	public static Collection<RandomEvent> all() {
		return Collections.unmodifiableCollection(EVENTS.values());
	}

	public static List<RandomEvent> ofType(EventType type) {
		List<RandomEvent> list = new ArrayList<>();
		for (RandomEvent e : EVENTS.values()) {
			if (e.type() == type) {
				list.add(e);
			}
		}
		return list;
	}

	public static void registerBuiltIn() {
		if (!EVENTS.isEmpty()) {
			return;
		}
		PositiveEvents.registerAll();
		NegativeEvents.registerAll();
	}
}
